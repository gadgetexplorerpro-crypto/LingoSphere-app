package com.example.data.repository

import com.example.data.model.AcademyComprehensionQuestion
import com.example.data.model.CurriculumLesson
import com.example.data.model.Exercise
import com.example.data.model.ExerciseType
import com.example.data.model.FlashcardItem
import com.example.data.model.ListeningAcademyItem
import com.example.data.model.ListeningDialogueLine
import com.example.data.model.PlacementQuestion
import com.example.data.model.PlacementTestEvaluation
import com.example.data.model.ReadingAcademyPassage
import com.example.data.model.ReadingWordGloss
import com.example.data.model.DailyLearningCardItem


object CurriculumData {

  private val lessonsCache = java.util.concurrent.ConcurrentHashMap<String, List<CurriculumLesson>>()

  fun getCachedLessonsOrEmpty(targetLangCode: String): List<CurriculumLesson> {
    val prefix = targetLangCode.lowercase()
    return lessonsCache[prefix]
      ?: lessonsCache.entries.firstOrNull { it.key.startsWith("${prefix}_") }?.value
      ?: emptyList()
  }

  fun localizeExplanation(englishText: String, nativeLangCode: String): String {
    val nat = nativeLangCode.lowercase().substringBefore("-")
    if (nat == "en" || nat.isBlank()) return englishText
    val prefixNote = when (nat) {
      "ur" -> "اردو رہنمائی: "
      "ar" -> "شرح بالعربية: "
      "es" -> "Guía en español: "
      "fr" -> "Guide en français : "
      "de" -> "Erklärung auf Deutsch: "
      "hi" -> "हिंदी व्याख्या: "
      "zh" -> "中文解析："
      "ja" -> "日本語解説: "
      "ko" -> "한국어 해설: "
      "pt" -> "Guia em português: "
      "it" -> "Guida in italiano: "
      "tr" -> "Türkçe açıklama: "
      "ru" -> "Пояснение на русском: "
      "fa" -> "راهنمای فارسی: "
      "ps" -> "پښتو لارښود: "
      "bn" -> "বাংলা ব্যাখ্যা: "
      "id", "ms" -> "Panduan Bahasa: "
      else -> "[${com.example.data.model.Language.findByCode(nat).nativeName}] "
    }
    return if (englishText.startsWith(prefixNote)) englishText else "$prefixNote$englishText"
  }

  fun getLessonsForLanguage(targetLangCode: String, nativeLangCode: String = "en"): List<CurriculumLesson> {
    val prefix = targetLangCode.lowercase().substringBefore("-")
    val natPrefix = nativeLangCode.lowercase().substringBefore("-")
    val cacheKey = if (natPrefix == "en") prefix else "${prefix}_$natPrefix"
    return lessonsCache.getOrPut(cacheKey) {
      val base = when (prefix) {
        "es" -> spanishLessons
        "fr" -> frenchLessons
        "de" -> germanLessons
        "ar" -> arabicLessons
        "ja" -> japaneseLessons
        "zh" -> chineseLessons
        "it" -> italianLessons
        "ru" -> russianLessons
        "ur" -> urduLessons
        else -> genericLessons(prefix)
      }
      val allLessons = base + buildSupplementaryCefrModules(prefix) + buildExtendedJourneyCurriculum(prefix, natPrefix)
      val mapped = if (natPrefix == "en") {
        allLessons
      } else {
        allLessons.map { lesson ->
          lesson.copy(
            exercises = lesson.exercises.map { ex ->
              ex.copy(explanationNative = localizeExplanation(ex.explanationNative, natPrefix))
            }
          )
        }
      }
      lessonsCache[prefix] = mapped
      mapped
    }
  }

  private data class JourneyTopicSpec(
    val idSlug: String,
    val title: String,
    val category: String,
    val description: String,
    val targetPhrase: (String) -> String,
    val meaning: String,
    val outcome1: String,
    val outcome2: String
  )

  private val extendedCefrBands by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
      Triple("A1", "Beginner", "Unit 1: Foundations & Survival (A1)"),
      Triple("A2", "Beginner", "Unit 2: Everyday Life & Routines (A2)"),
      Triple("B1", "Intermediate", "Unit 3: Independent Communication & Travel (B1)"),
      Triple("B2", "Intermediate", "Unit 4: Vantage & Professional Fluency (B2)"),
      Triple("C1", "Advanced", "Unit 5: Autonomous Academic & Cultural Discourse (C1)"),
      Triple("C2", "Advanced", "Unit 6: Proficient Mastery & Rhetoric (C1–C2)")
    )
  }

  private fun fallbackTargetPhraseForLanguage(code: String, englishMeaning: String): String {
    val lang = com.example.data.model.Language.findByCode(code)
    return when (code.lowercase().substringBefore("-")) {
      "en" -> englishMeaning
      "nl" -> "Laten we oefenen: $englishMeaning (${lang.nativeName})"
      "pl" -> "Ćwiczmy razem po polsku: ${lang.sampleGreeting}"
      "sv" -> "Låt oss öva på svenska: ${lang.sampleGreeting}"
      "el" -> "Ας εξασκηθούμε στα ελληνικά: ${lang.sampleGreeting}"
      "he" -> "בואו نتعلم בעברית: ${lang.sampleGreeting}"
      "fa" -> "بیایید به فارسی تمرین کنیم: ${lang.sampleGreeting}"
      "ps" -> "راځئ چې په پښتو تمرین وکړو: ${lang.sampleGreeting}"
      "bn" -> "চলুন বাংলায় অনুশীলন করি: ${lang.sampleGreeting}"
      "pa" -> "ਆਓ ਪੰਜਾਬੀ ਵਿੱਚ ਅਭਿਆਸ ਕਰੀਏ: ${lang.sampleGreeting}"
      "ta" -> "தமிழில் பயிற்சி செய்வோம்: ${lang.sampleGreeting}"
      "te" -> "తెలుగులో సాధన చేద్దాం: ${lang.sampleGreeting}"
      "id", "ms" -> "Mari berlatih dalam ${lang.nativeName}: ${lang.sampleGreeting}"
      "vi" -> "Hãy cùng luyện tập bằng Tiếng Việt: ${lang.sampleGreeting}"
      "th" -> "มาฝึกภาษาไทยด้วยกัน: ${lang.sampleGreeting}"
      "sw" -> "hebu tujifunze kwa Kiswahili: ${lang.sampleGreeting}"
      else -> "${lang.sampleGreeting} • [${lang.nativeName}]"
    }
  }

  private val extendedCoreTopics by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
      JourneyTopicSpec(
        idSlug = "greetings_social",
        title = "Basic Greetings & Polite Expressions",
        category = "Speaking",
        description = "Master warm greetings, polite openings, and culturally natural small talk.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Hello! It is a genuine pleasure to greet you today."
            "es" -> "¡Hola! Es un verdadero placer saludarte hoy."
            "fr" -> "Bonjour ! C'est un vrai plaisir de vous saluer aujourd'hui."
            "de" -> "Hallo! Es freut mich sehr, Sie heute zu begrüßen."
            "ar" -> "مرحباً! يسعدني جداً لقاؤك والتحية عليك اليوم."
            "ur" -> "السلام علیکم! آج آپ سے مل کر بہت خوشی ہوئی۔"
            "hi" -> "नमस्ते! आज आपसे मिलकर बहुत खुशी हुई।"
            "pt" -> "Olá! É um verdadeiro prazer cumprimentá-lo hoje."
            "it" -> "Ciao! È un vero piacere salutarti oggi."
            "tr" -> "Merhaba! Bugün sizi selamlamak gerçek bir zevk."
            "ru" -> "Здравствуйте! Мне очень приятно приветствовать вас сегодня."
            "ja" -> "こんにちは！今日お会いできて本当に嬉しいです。"
            "ko" -> "안녕하세요! 오늘 만나 뵙게 되어 정말 반갑습니다."
            "zh" -> "你好！今天很高兴向你问好。"
            else -> fallbackTargetPhraseForLanguage(code, "Hello! It is a genuine pleasure to greet you today.")
          }
        },
        meaning = "Hello! It is a genuine pleasure to greet you today.",
        outcome1 = "Initiate polite conversations with confidence and warmth",
        outcome2 = "Respond naturally to formal and informal greetings"
      ),
      JourneyTopicSpec(
        idSlug = "self_introductions",
        title = "Names, Age, Country & Personal Information",
        category = "Speaking",
        description = "Introduce your name, hometown, profession, and personal passions clearly.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "My name is Alex, I work in technology and love learning languages."
            "es" -> "Me llamo Alex, trabajo en tecnología y me apasiona aprender idiomas."
            "fr" -> "Je m'appelle Alex, je travaille dans la technologie et j'adore les langues."
            "de" -> "Ich heiße Alex, arbeite in der Technikbranche und lerne gern Sprachen."
            "ar" -> "اسمي أليكس، أعمل في مجال التكنولوجيا وأحب تعلم اللغات."
            "ur" -> "میرا نام ایلیکس ہے، میں ٹیکنالوجی میں کام کرتا ہوں اور زبانیں سیکھنے کا شوقین ہوں۔"
            "hi" -> "मेरा नाम एलेक्स है, मैं टेक्नोलॉजी में काम करता हूँ और मुझे भाषाएँ सीखना पसंद है।"
            "pt" -> "Chamo-me Alex, trabalho com tecnologia e adoro aprender idiomas."
            "it" -> "Mi chiamo Alex, lavoro nella tecnologia e amo imparare le lingue."
            "tr" -> "Adım Alex, teknoloji alanında çalışıyorum ve dil öğrenmeyi seviyorum."
            "ru" -> "Меня зовут Алекс, я работаю в сфере технологий и люблю изучать языки."
            "ja" -> "私はアレックスです。IT業界で働いていて、語学が大好きです。"
            "ko" -> "제 이름은 알렉스이고 IT 분야에서 일하며 언어 배우는 것을 좋아합니다."
            "zh" -> "我叫亚历克斯，在科技行业工作，非常热爱学习语言。"
            else -> fallbackTargetPhraseForLanguage(code, "My name is Alex, I work in technology and love learning languages.")
          }
        },
        meaning = "My name is Alex, I work in technology and love learning languages.",
        outcome1 = "Present your background, occupation, and interests fluently",
        outcome2 = "Ask engaging follow-up questions when meeting new people"
      ),
      JourneyTopicSpec(
        idSlug = "everyday_questions",
        title = "Asking & Answering Simple Questions",
        category = "Grammar",
        description = "Formulate clear who, what, where, when, and how questions in daily life.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Could you explain to me how this service works and when it opens?"
            "es" -> "¿Podrías explicarme cómo funciona este servicio y cuándo abre?"
            "fr" -> "Pourriez-vous m'expliquer comment fonctionne ce service et quand il ouvre ?"
            "de" -> "Könnten Sie mir erklären, wie dieser Service funktioniert und wann er öffnet?"
            "ar" -> "هل يمكنك أن تشرح لي كيف تعمل هذه الخدمة ومتى تفتح؟"
            "ur" -> "کیا آپ مجھے بتا سکتے ہیں کہ یہ سروس کیسے کام کرتی ہے اور کب کھلتی ہے؟"
            "hi" -> "क्या आप मुझे समझा सकते हैं कि यह सेवा कैसे काम करती है और कब खुलती है?"
            "pt" -> "Poderia explicar-me como funciona este serviço e quando abre?"
            "it" -> "Potresti spiegarmi come funziona questo servizio e quando apre?"
            "tr" -> "Bu hizmetin nasıl çalıştığını ve ne zaman açıldığını açıklayabilir misiniz?"
            "ru" -> "Не могли бы вы объяснить мне, как работает эта служба и когда она открывается?"
            "ja" -> "このサービスがどのように機能し、いつ開くのか教えていただけますか？"
            "ko" -> "이 서비스가 어떻게 작동하는지, 언제 여는지 설명해 주시겠어요?"
            "zh" -> "请问您能解释一下这项服务是怎么运作的以及什么时候开放吗？"
            else -> fallbackTargetPhraseForLanguage(code, "Could you explain to me how this service works and when it opens?")
          }
        },
        meaning = "Could you explain to me how this service works and when it opens?",
        outcome1 = "Ask open and closed questions using accurate word order",
        outcome2 = "Request polite clarification whenever details are unclear"
      ),
      JourneyTopicSpec(
        idSlug = "numbers_time_dates",
        title = "Numbers, Counting, Time, Dates & Weather",
        category = "Vocabulary",
        description = "Coordinate appointments, tell time accurately, count prices, and discuss calendar dates.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Our meeting is scheduled for Tuesday the fifteenth at ten thirty."
            "es" -> "Nuestra reunión está programada para el martes quince a las diez y media."
            "fr" -> "Notre réunion est prévue pour le mardi quinze à dix heures et demie."
            "de" -> "Unser Treffen ist für Dienstag, den fünfzehnten, um halb elf geplant."
            "ar" -> "اجتماعنا مقرر يوم الثلاثاء الخامس عشر في الساعة العاشرة والنصف."
            "ur" -> "ہماری ملاقات منگل پندرہ تاریخ کو صبح ساڑھے دس بجے طے ہے۔"
            "hi" -> "हमारी बैठक मंगलवार पंद्रह तारीख को सुबह साढ़े दस बजे निर्धारित है।"
            "pt" -> "A nossa reunião está marcada para terça-feira, dia quinze, às dez e meia."
            "it" -> "La nostra riunione è fissata per martedì quindici alle dieci e mezza."
            "tr" -> "Toplantımız on beş salı günü saat on buçukta planlandı."
            "ru" -> "Наша встреча назначена на вторник пятнадцатого числа в десять тридцать."
            "ja" -> "私たちの会議は15日火曜日の10時半に予定されています。"
            "ko" -> "우리 회의는 15일 화요일 10시 30분으로 예정되어 있습니다."
            "zh" -> "我们的会议定在十五号星期二上午十点半。"
            else -> fallbackTargetPhraseForLanguage(code, "Our meeting is scheduled for Tuesday the fifteenth at ten thirty.")
          }
        },
        meaning = "Our meeting is scheduled for Tuesday the fifteenth at ten thirty.",
        outcome1 = "Express times, dates, prices, and schedules without hesitation",
        outcome2 = "Confirm or reschedule appointments politely"
      ),
      JourneyTopicSpec(
        idSlug = "family_relationships",
        title = "Family, Friends & Relationships",
        category = "Vocabulary",
        description = "Describe family members, close friendships, and shared traditions.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "My family usually gathers every Sunday to share a homemade meal."
            "es" -> "Mi familia suele reunirse todos los domingos para compartir una comida casera."
            "fr" -> "Ma famille se réunit tous les dimanches pour partager un repas fait maison."
            "de" -> "Meine Familie trifft sich jeden Sonntag, um gemeinsam hausgemachtes Essen zu teilen."
            "ar" -> "تجتمع عائلتي كل يوم أحد لمشاركة وجبة منزلية دافئة."
            "ur" -> "میرا خاندان ہر اتوار کو گھر کا بنا ہوا کھانا بانٹنے کے لیے جمع ہوتا ہے۔"
            "hi" -> "मेरा परिवार हर रविवार को घर का बना खाना साझा करने के लिए मिलता है।"
            "pt" -> "A minha família reúne-se todos os domingos para partilhar uma refeição caseira."
            "it" -> "La mia famiglia si riunisce ogni domenica per condividere un pasto fatto in casa."
            "tr" -> "Ailem her pazar ev yapımı bir yemeği paylaşmak için bir araya gelir."
            "ru" -> "Моя семья обычно собирается каждое воскресенье за домашним обедом."
            "ja" -> "私の家族は毎週日曜日に集まって手料理を囲みます。"
            "ko" -> "우리 가족은 매주 일요일마다 모여 집밥을 함께 나눕니다."
            "zh" -> "我的家人每个星期天都会聚在一起享用家常菜。"
            else -> fallbackTargetPhraseForLanguage(code, "My family usually gathers every Sunday to share a homemade meal.")
          }
        },
        meaning = "My family usually gathers every Sunday to share a homemade meal.",
        outcome1 = "Describe family relationships, personalities, and shared habits",
        outcome2 = "Share personal stories about loved ones and community"
      ),
      JourneyTopicSpec(
        idSlug = "shopping_markets",
        title = "Shopping, Clothes & Everyday Items",
        category = "Speaking",
        description = "Compare items, ask about sizes and quality, and complete purchases smoothly.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "I would like to try on this jacket in medium, do you accept card?"
            "es" -> "Me gustaría probarme esta chaqueta en talla mediana, ¿aceptan tarjeta?"
            "fr" -> "J'aimerais essayer cette veste en taille moyenne, acceptez-vous la carte ?"
            "de" -> "Ich möchte diese Jacke in Größe M anprobieren, nehmen Sie Kreditkarten?"
            "ar" -> "أود تجربة هذه السترة بالمقاس المتوسط، هل تقبلون الدفع بالبطاقة؟"
            "ur" -> "میں درمیانے سائز میں یہ جیکٹ آزمانا چاہوں گا، کیا آپ کارڈ قبول کرتے ہیں؟"
            "hi" -> "मैं मध्यम आकार में यह जैकेट ट्राई करना चाहूँगा, क्या आप कार्ड स्वीकार करते हैं?"
            "pt" -> "Gostaria de experimentar este casaco no tamanho médio, aceitam cartão?"
            "it" -> "Vorrei provare questa giacca nella taglia media, accettate la carta?"
            "tr" -> "Bu ceketi orta bedende denemek istiyorum, kart kabul ediyor musunuz?"
            "ru" -> "Я хотел бы примерить эту куртку среднего размера, вы принимаете карты?"
            "ja" -> "このジャケットのMサイズを試着したいのですが、カードは使えますか？"
            "ko" -> "이 재킷 미디엄 사이즈를 입어 보고 싶은데, 카드 결제 되나요?"
            "zh" -> "我想试穿这件中号夹克，请问可以刷卡吗？"
            else -> fallbackTargetPhraseForLanguage(code, "I would like to try on this jacket in medium, do you accept card?")
          }
        },
        meaning = "I would like to try on this jacket in medium, do you accept card?",
        outcome1 = "Inquire about sizes, materials, discounts, and return policies",
        outcome2 = "Complete retail and market transactions naturally"
      ),
      JourneyTopicSpec(
        idSlug = "restaurants_food",
        title = "Restaurants, Cafés, Food & Ordering",
        category = "Speaking",
        description = "Order meals, ask about ingredients or allergies, and discuss local cuisine.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Which traditional house dish do you recommend we try tonight?"
            "es" -> "¿Qué plato tradicional de la casa nos recomienda probar esta noche?"
            "fr" -> "Quel plat traditionnel de la maison nous recommandez-vous ce soir ?"
            "de" -> "Welches traditionelle Gericht des Hauses empfehlen Sie uns heute Abend?"
            "ar" -> "ما هو الطبق التقليدي المميز الذي تنصحنا بتجربته هذه الليلة؟"
            "ur" -> "آج رات آپ ہمیں کون سی روایتی ڈش آزمانے کا مشورہ دیتے ہیں؟"
            "hi" -> "आज रात आप हमें कौन सा पारंपरिक व्यंजन चखने की सलाह देते हैं?"
            "pt" -> "Que prato tradicional da casa nos recomenda experimentar esta noite?"
            "it" -> "Quale piatto tradizionale della casa ci consiglia di provare stasera?"
            "tr" -> "Bu akşam denememiz için hangi geleneksel spesiyali önerirsiniz?"
            "ru" -> "Какое традиционное фирменное блюдо вы рекомендуете попробовать сегодня вечером?"
            "ja" -> "今夜おすすめの伝統的な看板料理はどれですか？"
            "ko" -> "오늘 저녁에 맛볼 만한 이곳의 전통 추천 요리는 무엇인가요?"
            "zh" -> "今晚您推荐我们品尝哪道传统招牌菜？"
            else -> fallbackTargetPhraseForLanguage(code, "Which traditional house dish do you recommend we try tonight?")
          }
        },
        meaning = "Which traditional house dish do you recommend we try tonight?",
        outcome1 = "Order food and beverages while specifying dietary preferences",
        outcome2 = "Ask for chef recommendations and handle the bill politely"
      ),
      JourneyTopicSpec(
        idSlug = "transportation_commute",
        title = "Public Transportation & Daily Routines",
        category = "Listening",
        description = "Navigate trains, buses, transfers, daily commutes, and ticket machines with ease.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Does this express train go straight downtown or do I need to transfer?"
            "es" -> "¿Este tren expreso va directo al centro o necesito hacer transbordo?"
            "fr" -> "Ce train express va-t-il directement au centre ou dois-je changer ?"
            "de" -> "Fährt dieser Expresszug direkt ins Zentrum oder muss ich umsteigen?"
            "ar" -> "هل يذهب هذا القطار السريع مباشرة إلى المركز أم أحتاج لتغيير الخط؟"
            "ur" -> "کیا یہ ایکسپریس ٹرین براہ راست مرکز جاتی ہے یا مجھے ٹرین بدلنی ہوگی؟"
            "hi" -> "क्या यह एक्सप्रेस ट्रेन सीधे केंद्र तक जाती है या मुझे बदलना होगा?"
            "pt" -> "Este comboio expresso vai direto ao centro ou preciso fazer transbordo?"
            "it" -> "Questo treno espresso va dritto in centro o devo fare cambio?"
            "tr" -> "Bu ekspres tren doğrudan merkeze mi gidiyor yoksa aktarma yapmalı mıyım?"
            "ru" -> "Этот экспресс идёт прямо в центр или мне нужно сделать пересадку?"
            "ja" -> "この急行電車は都心へ直通ですか、それとも乗り換えが必要ですか？"
            "ko" -> "이 급행열차는 시내 중심가로 바로 가나요, 아니면 환승해야 하나요?"
            "zh" -> "这趟快车直达市中心吗，还是需要换乘？"
            else -> fallbackTargetPhraseForLanguage(code, "Does this express train go straight downtown or do I need to transfer?")
          }
        },
        meaning = "Does this express train go straight downtown or do I need to transfer?",
        outcome1 = "Understand station announcements, routes, and transit connections",
        outcome2 = "Purchase fares and verify platform directions accurately"
      ),
      JourneyTopicSpec(
        idSlug = "travel_hotels",
        title = "Travel, Hotels, Airports & Transportation",
        category = "Speaking",
        description = "Manage bookings, hotel check-ins, luggage inquiries, and sightseeing.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "I would like to confirm my booking and request a quiet room with a view."
            "es" -> "Quisiera confirmar mi reserva y solicitar una habitación tranquila con vista."
            "fr" -> "Je voudrais confirmer ma réservation et demander une chambre calme avec vue."
            "de" -> "Ich möchte meine Reservierung bestätigen und um ein ruhiges Zimmer bitten."
            "ar" -> "أود تأكيد حجزي وطلب غرفة هادئة ذات إطلالة جميلة."
            "ur" -> "میں اپنی بکنگ کی تصدیق کرنا اور ایک پرسکون کمرے کی درخواست کرنا چاہوں گا۔"
            "hi" -> "मैं अपनी बुकिंग की पुष्टि करना और एक शांत कमरे का अनुरोध करना चाहूँगा।"
            "pt" -> "Gostaria de confirmar a minha reserva e pedir um quarto calmo com vista."
            "it" -> "Vorrei confermare la mia prenotazione e chiedere una camera tranquilla."
            "tr" -> "Rezervasyonumu onaylamak ve manzaralı sessiz bir oda rica etmek istiyorum."
            "ru" -> "Я хотел бы подтвердить бронирование и попросить тихий номер с красивым видом."
            "ja" -> "予約を確認し、眺めの良い静かな部屋をお願いしたいのですが。"
            "ko" -> "예약을 확인하고 전망이 좋은 조용한 객실을 요청하고 싶습니다."
            "zh" -> "我想确认我的预订，并申请一间安静且视野好的房间。"
            else -> fallbackTargetPhraseForLanguage(code, "I would like to confirm my booking and request a quiet room with a view.")
          }
        },
        meaning = "I would like to confirm my booking and request a quiet room with a view.",
        outcome1 = "Handle hotel check-ins, special requests, and itinerary adjustments",
        outcome2 = "Resolve travel delays or luggage questions calmly"
      ),
      JourneyTopicSpec(
        idSlug = "directions_navigation",
        title = "Directions, Maps & Describing Locations",
        category = "Listening",
        description = "Follow street landmarks, turns, distances, and spatial prepositions.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Go straight ahead two blocks and turn left next to the museum."
            "es" -> "Sigue todo recto dos cuadras y gira a la izquierda junto al museo."
            "fr" -> "Continuez tout droit sur deux pâtés de maisons et tournez à gauche près du musée."
            "de" -> "Gehen Sie zwei Straßen geradeaus und biegen Sie am Museum links ab."
            "ar" -> "سر للأمام مباشرة لمسافة شارعين ثم انعطف يساراً بجانب المتحف."
            "ur" -> "دو بلاک سیدھے چلیں اور عجائب گھر کے پاس سے بائیں مڑ جائیں۔"
            "hi" -> "दो ब्लॉक सीधे चलें और संग्रहालय के पास बाएँ मुड़ें।"
            "pt" -> "Siga sempre em frente por dois quarteirões e vire à esquerda junto ao museu."
            "it" -> "Prosegui dritto per due isolati e gira a sinistra accanto al museo."
            "tr" -> "İki blok düz devam edin ve müzenin yanından sola dönün."
            "ru" -> "Идите прямо два квартала и поверните налево рядом с музеем."
            "ja" -> "2ブロックまっすぐ進んで、博物館の隣を左に曲がってください。"
            "ko" -> "두 블록을 직진한 다음 박물관 옆에서 좌회전하세요."
            "zh" -> "直走两个街区，然后在博物馆旁边向左转。"
            else -> fallbackTargetPhraseForLanguage(code, "Go straight ahead two blocks and turn left next to the museum.")
          }
        },
        meaning = "Go straight ahead two blocks and turn left next to the museum.",
        outcome1 = "Ask for and give accurate walking or driving directions",
        outcome2 = "Recognize urban landmarks and spatial instructions"
      ),
      JourneyTopicSpec(
        idSlug = "work_office",
        title = "Work, Office & Professional Collaboration",
        category = "Speaking",
        description = "Collaborate with colleagues, share project updates, and coordinate deadlines.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Let's review the project priorities to meet Friday's delivery deadline."
            "es" -> "Revisemos las prioridades del proyecto para cumplir con la entrega del viernes."
            "fr" -> "Examinons les priorités du projet afin de respecter l'échéance de vendredi."
            "de" -> "Lassen Sie uns die Projektprioritäten prüfen, um die Frist am Freitag einzuhalten."
            "ar" -> "دعنا نراجع أولويات المشروع للالتزام بموعد التسليم يوم الجمعة."
            "ur" -> "آئیے جمعہ کی ڈیڈ لائن کو پورا کرنے کے لیے پراجیکٹ کی ترجیحات کا جائزہ لیں۔"
            "hi" -> "आइए शुक्रवार की समय-सीमा को पूरा करने के लिए प्रोजेक्ट प्राथमिकताओं की समीक्षा करें।"
            "pt" -> "Vamos rever as prioridades do projeto para cumprir o prazo de sexta-feira."
            "it" -> "Rivediamo le priorità del progetto per rispettare la scadenza di venerdì."
            "tr" -> "Cuma günkü teslim tarihine yetişmek için proje önceliklerini gözden geçirelim."
            "ru" -> "Давайте пересмотрим приоритеты проекта, чтобы уложиться в срок к пятнице."
            "ja" -> "金曜日の納期を守るために、プロジェクトの優先順位を確認しましょう。"
            "ko" -> "금요일 마감일을 맞추기 위해 프로젝트 우선순위를 검토해 봅시다."
            "zh" -> "让我们梳理一下项目优先级，以确保周五按时交付。"
            else -> fallbackTargetPhraseForLanguage(code, "Let's review the project priorities to meet Friday's delivery deadline.")
          }
        },
        meaning = "Let's review the project priorities to meet Friday's delivery deadline.",
        outcome1 = "Share structured status updates in professional team meetings",
        outcome2 = "Coordinate workflows and offer constructive support to colleagues"
      ),
      JourneyTopicSpec(
        idSlug = "job_interviews",
        title = "Jobs, Interviews & Career Achievements",
        category = "Speaking",
        description = "Highlight professional strengths, problem-solving skills, and career goals.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "My experience coordinating international teams brings direct value to the role."
            "es" -> "Mi experiencia coordinando equipos internacionales aporta un valor directo al puesto."
            "fr" -> "Mon expérience à coordonner des équipes internationales apporte une vraie valeur."
            "de" -> "Meine Erfahrung in der Leitung internationaler Teams bringt direkten Mehrwert."
            "ar" -> "خبرتي في تنسيق الفرق الدولية تضيف قيمة مباشرة لهذا المنصب."
            "ur" -> "بین الاقوامی ٹیموں کے ساتھ کام کرنے کا میرا تجربہ اس عہدے کے لیے بہت مفید ہے۔"
            "hi" -> "अंतरराष्ट्रीय टीमों के समन्वय का मेरा अनुभव इस पद के लिए सीधा मूल्य जोड़ता है।"
            "pt" -> "A minha experiência a coordenar equipas internacionais traz valor direto ao cargo."
            "it" -> "La mia esperienza nel coordinare team internazionali porta un valore diretto al ruolo."
            "tr" -> "Uluslararası ekipleri koordine etme deneyimim bu pozisyona doğrudan değer katıyor."
            "ru" -> "Мой опыт координации международных команд приносит прямую пользу этой должности."
            "ja" -> "国際的なチームをまとめた私の経験は、この役職に直接貢献できます。"
            "ko" -> "글로벌 팀을 조율한 제 경험은 이 직무에 직접적인 가치를 더할 것입니다."
            "zh" -> "我协调国际团队的经验能为这个岗位带来直接价值。"
            else -> fallbackTargetPhraseForLanguage(code, "My experience coordinating international teams brings direct value to the role.")
          }
        },
        meaning = "My experience coordinating international teams brings direct value to the role.",
        outcome1 = "Articulate career accomplishments and strengths persuasively",
        outcome2 = "Answer behavioral interview questions with structured examples"
      ),
      JourneyTopicSpec(
        idSlug = "healthcare_wellness",
        title = "Health, Doctor, Pharmacy & Emergencies",
        category = "Vocabulary",
        description = "Describe symptoms clearly, understand medical advice, and discuss healthy habits.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Since yesterday I have a sore throat and need something to relieve the fever."
            "es" -> "Desde ayer tengo dolor de garganta y necesito algo para aliviar la fiebre."
            "fr" -> "Depuis hier j'ai mal à la gorge et j'ai besoin de quelque chose contre la fièvre."
            "de" -> "Seit gestern habe ich Halsschmerzen und brauche etwas gegen das Fieber."
            "ar" -> "منذ الأمس أشعر بألم في الحلق وأحتاج إلى دواء لتخفيف الحمى."
            "ur" -> "کل سے میرے گلے میں درد ہے اور مجھے بخار کم کرنے کے لیے دوائی چاہیے۔"
            "hi" -> "कल से मेरे गले में दर्द है और मुझे बुखार कम करने के लिए कुछ चाहिए।"
            "pt" -> "Desde ontem tenho dor de garganta e preciso de algo para aliviar a febre."
            "it" -> "Da ieri ho mal di gola e ho bisogno di qualcosa per abbassare la febbre."
            "tr" -> "Dünden beri boğazım ağrıyor ve ateşi düşürmek için bir şeye ihtiyacım var."
            "ru" -> "Со вчерашнего дня у меня болит горло, и мне нужно средство от температуры."
            "ja" -> "昨日から喉が痛くて、熱を下げる薬が必要です。"
            "ko" -> "어제부터 목이 아파서 열을 내리는 약이 필요합니다."
            "zh" -> "从昨天开始我嗓子疼，需要一些退烧药。"
            else -> fallbackTargetPhraseForLanguage(code, "Since yesterday I have a sore throat and need something to relieve the fever.")
          }
        },
        meaning = "Since yesterday I have a sore throat and need something to relieve the fever.",
        outcome1 = "Communicate symptoms, allergies, and medical needs accurately",
        outcome2 = "Understand dosage instructions and wellness recommendations"
      ),
      JourneyTopicSpec(
        idSlug = "education_academic",
        title = "School, Study & Academic Education",
        category = "Reading",
        description = "Discuss courses, research topics, study strategies, and lifelong learning.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Consistent practice and active reading strengthen long-term memory."
            "es" -> "La práctica constante y la lectura activa fortalecen la memoria a largo plazo."
            "fr" -> "La pratique constante et la lecture active renforcent la mémoire à long terme."
            "de" -> "Beständiges Üben und aktives Lesen stärken das Langzeitgedächtnis nachhaltig."
            "ar" -> "الممارسة المستمرة والقراءة النشطة تقويان الذاكرة طويلة المدى."
            "ur" -> "مسلسل مشق اور فعال مطالعہ طویل مدتی یادداشت کو مضبوط بناتے ہیں۔"
            "hi" -> "निरंतर अभ्यास और सक्रिय पठन दीर्घकालिक स्मृति को मजबूत करते हैं।"
            "pt" -> "A prática constante e a leitura ativa fortalecem a memória de longo prazo."
            "it" -> "La pratica costante e la lettura attiva rafforzano la memoria a lungo termine."
            "tr" -> "Sürekli pratik ve aktif okuma uzun süreli hafızayı güçlendirir."
            "ru" -> "Регулярная практика и активное чтение укрепляют долговременную память."
            "ja" -> "継続的な練習と能動的な読書は、長期記憶を強化します。"
            "ko" -> "꾸준한 연습과 능동적인 독서는 장기 기억력을 강화합니다."
            "zh" -> "持续的练习和主动阅读能够强化长期记忆。"
            else -> fallbackTargetPhraseForLanguage(code, "Consistent practice and active reading strengthen long-term memory.")
          }
        },
        meaning = "Consistent practice and active reading strengthen long-term memory.",
        outcome1 = "Discuss academic subjects, seminars, and learning strategies",
        outcome2 = "Summarize key findings from articles and lectures"
      ),
      JourneyTopicSpec(
        idSlug = "social_opinions",
        title = "Feelings, Opinions, Likes & Dislikes",
        category = "Speaking",
        description = "Share balanced viewpoints, agree or disagree politely, and keep conversations flowing.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "I understand your point of view, though I consider it important to analyze other options."
            "es" -> "Entiendo tu punto de vista, aunque considero importante analizar otras opciones."
            "fr" -> "Je comprends votre point de vue, même s'il importe d'analyser d'autres options."
            "de" -> "Ich verstehe Ihren Standpunkt, finde es aber wichtig, Alternativen zu prüfen."
            "ar" -> "أتفهم وجهة نظرك، رغم أنني أرى من المهم دراسة خيارات أخرى."
            "ur" -> "میں آپ کا نقطہ نظر سمجھتا ہوں، حالانکہ دیگر اختیارات کا جائزہ لینا بھی اہم ہے۔"
            "hi" -> "मैं आपके दृष्टिकोण को समझता हूँ, हालांकि अन्य विकल्पों का विश्लेषण करना भी महत्वपूर्ण है।"
            "pt" -> "Compreendo o seu ponto de vista, embora considere importante analisar outras opções."
            "it" -> "Capisco il tuo punto di vista, anche se ritengo importante valutare altre opzioni."
            "tr" -> "Bakış açınızı anlıyorum, ancak diğer seçenekleri de değerlendirmek önemli."
            "ru" -> "Я понимаю вашу точку зрения, хотя считаю важным проанализировать и другие варианты."
            "ja" -> "あなたの視点は理解できますが、他の選択肢も検討することが大切だと思います。"
            "ko" -> "당신의 관점은 이해하지만, 다른 대안들도 분석해 보는 것이 중요하다고 생각합니다."
            "zh" -> "我理解你的观点，不过我认为分析其他选择也很重要。"
            else -> fallbackTargetPhraseForLanguage(code, "I understand your point of view, though I consider it important to analyze other options.")
          }
        },
        meaning = "I understand your point of view, though I consider it important to analyze other options.",
        outcome1 = "Express nuanced opinions using polite discourse markers",
        outcome2 = "Acknowledge differing perspectives while defending your thesis"
      ),
      JourneyTopicSpec(
        idSlug = "storytelling_past_future",
        title = "Past Events, Future Plans & Storytelling",
        category = "Grammar",
        description = "Connect past events, memorable experiences, and future ambitions seamlessly.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Last year we traveled along the coast and next summer we plan to return."
            "es" -> "El año pasado recorrimos la costa y el próximo verano planeamos volver."
            "fr" -> "L'année dernière nous avons parcouru la côte et l'été prochain nous reviendrons."
            "de" -> "Letztes Jahr bereisten wir die Küste und nächsten Sommer planen wir zurückzukehren."
            "ar" -> "في العام الماضي سافرنا على طول الساحل ونخطط للعودة الصيف القادم."
            "ur" -> "پچھلے سال ہم نے ساحلی علاقے کی سیر کی اور اگلی گرمیوں میں دوبارہ جانے کا ارادہ ہے۔"
            "hi" -> "पिछले साल हमने तट की यात्रा की और अगली गर्मियों में हम वापस जाने की योजना बना रहे हैं।"
            "pt" -> "No ano passado percorremos a costa e no próximo verão planeamos voltar."
            "it" -> "L'anno scorso abbiamo visitato la costa e la prossima estate contiamo di tornare."
            "tr" -> "Geçen yıl sahili gezdik ve gelecek yaz tekrar dönmeyi planlıyoruz."
            "ru" -> "В прошлом году мы путешествовали по побережью, а следующим летом планируем вернуться."
            "ja" -> "昨年は海岸沿いを旅しましたが、来年の夏もまた訪れる予定です。"
            "ko" -> "작년에 해안을 따라 여행했는데 내년 여름에 다시 방문할 계획입니다."
            "zh" -> "去年我们游览了海岸，明年夏天我们计划再去一次。"
            else -> fallbackTargetPhraseForLanguage(code, "Last year we traveled along the coast and next summer we plan to return.")
          }
        },
        meaning = "Last year we traveled along the coast and next summer we plan to return.",
        outcome1 = "Narrate past anecdotes and future goals with accurate tense transitions",
        outcome2 = "Sequence events clearly with temporal connectors"
      ),
      JourneyTopicSpec(
        idSlug = "pronunciation_listening",
        title = "How Are You, Everyday Responses & Intonation",
        category = "Listening",
        description = "Refine cadence, everyday responses, connected speech recognition, and natural stress patterns.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Listening to the natural rhythm of each sentence improves your pronunciation daily."
            "es" -> "Escuchar el ritmo natural de cada frase mejora tu pronunciación cada día."
            "fr" -> "Écouter le rythme naturel de chaque phrase améliore votre prononciation."
            "de" -> "Auf den natürlichen Rhythmus jedes Satzes zu hören verbessert Ihre Aussprache."
            "ar" -> "الاستماع إلى الإيقاع الطبيعي لكل جملة يحسن نطقك كل يوم."
            "ur" -> "ہر جملے کے قدرتی آہنگ کو سننا آپ کے تلفظ کو روز بروز بہتر بناتا ہے۔"
            "hi" -> "प्रत्येक वाक्य की स्वाभाविक लय को सुनने से आपका उच्चारण प्रतिदिन बेहतर होता है।"
            "pt" -> "Ouvir o ritmo natural de cada frase melhora a sua pronúncia todos os dias."
            "it" -> "Ascoltare il ritmo naturale di ogni frase migliora la tua pronuncia ogni giorno."
            "tr" -> "Her cümlenin doğal ritmini dinlemek telaffuzunuzu her gün geliştirir."
            "ru" -> "Прослушивание естественного ритма каждой фразы улучшает ваше произношение каждый день."
            "ja" -> "それぞれの文の自然なリズムを聴くことで、発音が毎日上達します。"
            "ko" -> "각 문장의 자연스러운 리듬을 귀 기울여 들으면 발음이 날마다 좋아집니다."
            "zh" -> "倾听每个句子的自然节奏能让你的发音每天都取得进步。"
            else -> fallbackTargetPhraseForLanguage(code, "Listening to the natural rhythm of each sentence improves your pronunciation daily.")
          }
        },
        meaning = "Listening to the natural rhythm of each sentence improves your pronunciation daily.",
        outcome1 = "Capture key meaning from natural-speed connected speech",
        outcome2 = "Speak with clear word stress and natural sentence melody"
      ),
      JourneyTopicSpec(
        idSlug = "reading_writing_fluency",
        title = "Describing People, Places & Structured Writing",
        category = "Reading",
        description = "Describe people, places, and objects while crafting clear messages, emails, and essays.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "I am writing this message to summarize the main agreements of our session."
            "es" -> "Te escribo este mensaje para resumir los acuerdos principales de nuestra sesión."
            "fr" -> "Je vous écris ce message pour résumer les principaux accords de notre session."
            "de" -> "Ich schreibe Ihnen diese Nachricht, um die wichtigsten Ergebnisse zusammenzufassen."
            "ar" -> "أكتب لك هذه الرسالة لتلخيص الاتفاقيات الرئيسية في جلستنا."
            "ur" -> "میں اپنے اجلاس کے اہم فیصلوں کا خلاصہ کرنے کے لیے یہ پیغام لکھ رہا ہوں۔"
            "hi" -> "मैं हमारे सत्र के मुख्य समझौतों को संक्षेप में प्रस्तुत करने के लिए यह संदेश लिख रहा हूँ।"
            "pt" -> "Escrevo esta mensagem para resumir os principais acordos da nossa sessão."
            "it" -> "Ti scrivo questo messaggio per riassumere gli accordi principali della sessione."
            "tr" -> "Oturumumuzun ana kararlarını özetlemek için size bu mesajı yazıyorum."
            "ru" -> "Я пишу это сообщение, чтобы подвести итоги основных договорённостей нашей встречи."
            "ja" -> "セッションの主な合意事項をまとめるために、このメッセージを書いています。"
            "ko" -> "우리 회의의 주요 합의 사항을 요약하기 위해 이 메시지를 작성합니다."
            "zh" -> "我写这条消息是为了总结我们会议达成的主要共识。"
            else -> fallbackTargetPhraseForLanguage(code, "I am writing this message to summarize the main agreements of our session.")
          }
        },
        meaning = "I am writing this message to summarize the main agreements of our session.",
        outcome1 = "Identify main ideas and supporting details in written passages",
        outcome2 = "Compose well-structured paragraphs with cohesive transitions"
      ),
      JourneyTopicSpec(
        idSlug = "culture_register_problem_solving",
        title = "Daily Routines, Cultural Nuance & Problem Solving",
        category = "Speaking",
        description = "Adapt between formal and informal tone while resolving real-life daily challenges.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "I greatly appreciate your understanding; let's find a practical solution together."
            "es" -> "Agradezco mucho su comprensión; busquemos juntos una solución práctica ahora."
            "fr" -> "Je vous remercie de votre compréhension ; cherchons ensemble une solution pratique."
            "de" -> "Ich danke Ihnen für Ihr Verständnis; suchen wir gemeinsam eine praktische Lösung."
            "ar" -> "أقدر تفهمكم كثيراً؛ دعونا نبحث معاً عن حل عملي الآن."
            "ur" -> "میں آپ کے تعاون کا شکر گزار ہوں؛ آئیے مل کر ایک عملی حل تلاش کریں۔"
            "hi" -> "मैं आपकी समझ की सराहना करता हूँ; आइए मिलकर एक व्यावहारिक समाधान खोजें।"
            "pt" -> "Agradeço muito a sua compreensão; vamos procurar juntos uma solução prática."
            "it" -> "Apprezzo molto la sua comprensione; cerchiamo insieme una soluzione pratica."
            "tr" -> "Anlayışınız için çok teşekkür ederim; şimdi birlikte pratik bir çözüm bulalım."
            "ru" -> "Я очень ценю ваше понимание; давайте вместе найдём практическое решение."
            "ja" -> "ご理解いただきありがとうございます。一緒に実用的な解決策を見つけましょう。"
            "ko" -> "이해해 주셔서 대단히 감사합니다. 함께 실질적인 해결책을 찾아봅시다."
            "zh" -> "非常感谢您的理解，让我们一起寻找切实可行的解决方案。"
            else -> fallbackTargetPhraseForLanguage(code, "I greatly appreciate your understanding; let's find a practical solution together.")
          }
        },
        meaning = "I greatly appreciate your understanding; let's find a practical solution together.",
        outcome1 = "Shift smoothly between polite formal register and warm everyday speech",
        outcome2 = "Negotiate practical solutions when unexpected obstacles arise"
      ),
      JourneyTopicSpec(
        idSlug = "level_mastery_assessment",
        title = "Real-World Conversations, Debates & Fluency Mastery",
        category = "Speaking",
        description = "Demonstrate integrated listening, vocabulary, grammar, and speaking mastery.",
        targetPhrase = { code ->
          when (code) {
            "en" -> "Every real conversation strengthens my fluency and confidence to communicate."
            "es" -> "Cada conversación real fortalece mi fluidez y confianza para comunicarme."
            "fr" -> "Chaque conversation réelle renforce ma fluidité et ma confiance à communiquer."
            "de" -> "Jedes echte Gespräch stärkt meine Sprachgewandtheit und mein Selbstvertrauen."
            "ar" -> "كل محادثة حقيقية تعزز طلاقتي وثقتي في التواصل بفعالية."
            "ur" -> "ہر حقیقی گفتگو میری روانی اور بات چیت کے اعتماد کو مضبوط کرتی ہے۔"
            "hi" -> "प्रत्येक वास्तविक बातचीत मेरी प्रवाह क्षमता और संवाद करने के आत्मविश्वास को बढ़ाती है।"
            "pt" -> "Cada conversa real fortalece a minha fluência e confiança para comunicar."
            "it" -> "Ogni conversazione reale rafforza la mia scioltezza e fiducia nel comunicare."
            "tr" -> "Her gerçek sohbet akıcılığımı ve iletişim kurma özgüvenimi güçlendirir."
            "ru" -> "Каждый живой разговор укрепляет мою беглость и уверенность в общении."
            "ja" -> "実践的な会話の一つひとつが、私の流暢さと伝える自信を高めてくれます。"
            "ko" -> "모든 실제 대화는 나의 유창함과 소통에 대한 자신감을 키워 줍니다."
            "zh" -> "每一次真实的对话都能增强我的流利度和沟通自信。"
            else -> fallbackTargetPhraseForLanguage(code, "Every real conversation strengthens my fluency and confidence to communicate.")
          }
        },
        meaning = "Every real conversation strengthens my fluency and confidence to communicate.",
        outcome1 = "Synthesize vocabulary and grammar spontaneously without hesitation",
        outcome2 = "Verify readiness for the next CEFR proficiency milestone"
      )
    )
  }

  private data class SubStepTemplate(
    val stepIndex: Int,
    val stepBadge: String,
    val categoryOverride: String?,
    val focusDescription: String
  )

  private val progressiveSubSteps = listOf(
    SubStepTemplate(1, "Core Vocabulary & Script", "Vocabulary", "Master essential words, script recognition, and pronunciation."),
    SubStepTemplate(2, "Listening & Comprehension", "Listening", "Train your ear on native rhythm, intonation, and context."),
    SubStepTemplate(3, "Grammar & Sentence Building", "Grammar", "Construct accurate sentences using natural word order and grammar rules."),
    SubStepTemplate(4, "Interactive Dialogue & Speaking", "Speaking", "Practice real-life spoken responses and conversational fluency.")
  )

  private fun buildExtendedJourneyCurriculum(langCode: String, nativeLangCode: String = "en"): List<CurriculumLesson> {
    val cefrBands = extendedCefrBands
    val coreTopics = extendedCoreTopics
    val langMeta = com.example.data.model.Language.findByCode(langCode)
    // 6 CEFR bands * (20 topics * 4 sub-steps + 5 milestone revisions) = 510 progressive lessons
    val generated = ArrayList<CurriculumLesson>(510)
    var globalStepCounter = 1

    cefrBands.forEachIndexed { bandIdx, (cefr, profLevel, unitTitle) ->
      coreTopics.forEachIndexed { topicIdx, spec ->
        val phrase = spec.targetPhrase(langCode)
        progressiveSubSteps.forEach { subStep ->
          val lessonId = if (subStep.stepIndex == 1) {
            "${langCode}_journey_${cefr.lowercase()}_${spec.idSlug}_${topicIdx + 1}"
          } else {
            "${langCode}_journey_${cefr.lowercase()}_${spec.idSlug}_${topicIdx + 1}_s${subStep.stepIndex}"
          }
          val resolvedCategory = subStep.categoryOverride ?: spec.category
          val stepTitle = "$cefr • ${spec.title}: ${subStep.stepBadge}"
          val stepDesc = "${spec.description} — ${subStep.focusDescription} (${langMeta.writingSystem} • Step #$globalStepCounter)"

          val exercises = when (subStep.stepIndex) {
            1 -> listOf(
              Exercise(
                id = "${lessonId}_ex_1",
                type = ExerciseType.VOCABULARY,
                prompt = "Select the accurate meaning of this ${langMeta.name} ($cefr) expression: \"$phrase\"",
                explanationNative = "Understanding this $cefr expression builds real-world ${spec.title.lowercase()} vocabulary in ${langMeta.name}.",
                targetAudioText = phrase,
                phoneticGuide = "${langMeta.writingSystem} • $cefr Core Phrase",
                options = listOf(
                  spec.meaning,
                  "I lost my umbrella at the old train station yesterday",
                  "The museum closes for renovations every winter",
                  "Please turn off all electronic devices immediately"
                ),
                correctOptionIndex = 0
              ),
              Exercise(
                id = "${lessonId}_ex_2",
                type = ExerciseType.SPEAKING,
                prompt = "Speak aloud with clear $cefr intonation: \"$phrase\"",
                explanationNative = "Speaking this key phrase aloud reinforces muscle memory and natural rhythm.",
                targetAudioText = phrase,
                acceptableAnswers = listOf(phrase.lowercase(), spec.meaning.lowercase())
              )
            )
            2 -> listOf(
              Exercise(
                id = "${lessonId}_ex_1",
                type = ExerciseType.LISTENING,
                prompt = "Listen carefully and identify the communicative intent of: \"$phrase\"",
                explanationNative = "Active listening trains your ear to recognize ${spec.title.lowercase()} in real conversations.",
                targetAudioText = phrase,
                phoneticGuide = "${langMeta.writingSystem} • Connected Speech",
                options = listOf(
                  spec.meaning,
                  "Apologizing for canceling a flight next month",
                  "Asking a mechanic to repair a bicycle tire",
                  "Ordering winter boots from an online catalog"
                ),
                correctOptionIndex = 0
              ),
              Exercise(
                id = "${lessonId}_ex_2",
                type = ExerciseType.QUIZ,
                prompt = "In which real-life context would you most naturally use \"$phrase\"?",
                explanationNative = "Matching expressions to social context is essential for $cefr pragmatic fluency.",
                targetAudioText = phrase,
                options = listOf(
                  "During ${spec.title.lowercase()} interactions (${spec.outcome1})",
                  "While reading silent laboratory safety labels only",
                  "When declining to speak in any language",
                  "Only in medieval poetry competitions"
                ),
                correctOptionIndex = 0
              )
            )
            3 -> listOf(
              Exercise(
                id = "${lessonId}_ex_1",
                type = ExerciseType.GRAMMAR,
                prompt = "Which sentence structure in ${langMeta.name} ($cefr) accurately expresses: \"${spec.meaning}\"?",
                explanationNative = "Notice the ${langMeta.writingSystem} word order and grammatical agreement in \"$phrase\".",
                targetAudioText = phrase,
                phoneticGuide = "${langMeta.writingSystem} • Grammar Pattern",
                options = listOf(
                  phrase,
                  "??? ${langMeta.nativeName} ---",
                  "12345",
                  "None of the above"
                ),
                correctOptionIndex = 0
              ),
              Exercise(
                id = "${lessonId}_ex_2",
                type = ExerciseType.WRITING,
                prompt = "Write or type the ${langMeta.name} expression for: \"${spec.meaning}\"",
                explanationNative = "Writing in ${langMeta.writingSystem} strengthens spelling and structural recall.",
                targetAudioText = phrase,
                acceptableAnswers = listOf(phrase.lowercase(), spec.meaning.lowercase(), langMeta.sampleGreeting.lowercase())
              )
            )
            else -> listOf(
              Exercise(
                id = "${lessonId}_ex_1",
                type = ExerciseType.SPEAKING,
                prompt = "Deliver this $cefr conversational turn aloud with natural confidence: \"$phrase\"",
                explanationNative = "Fluid spoken delivery prepares you for live AI Tutor and real-world conversations.",
                targetAudioText = phrase,
                acceptableAnswers = listOf(phrase.lowercase(), spec.meaning.lowercase())
              ),
              Exercise(
                id = "${lessonId}_ex_2",
                type = ExerciseType.READING,
                prompt = "Read the statement and confirm its primary takeaway: \"$phrase\"",
                explanationNative = "${spec.outcome2} — this completes your $cefr mastery of ${spec.title}.",
                targetAudioText = phrase,
                options = listOf(
                  spec.meaning,
                  "A weather forecast for heavy snow",
                  "A recipe for baking sourdough bread",
                  "An automated voicemail greeting"
                ),
                correctOptionIndex = 0
              )
            )
          }

          generated.add(
            CurriculumLesson(
              id = lessonId,
              title = stepTitle,
              description = stepDesc,
              category = resolvedCategory,
              proficiencyLevel = profLevel,
              targetLanguageCode = langCode,
              estimatedMinutes = 5 + (bandIdx % 4),
              xpReward = 40 + (bandIdx * 5),
              cefrLevel = cefr,
              unitTitle = unitTitle,
              learningOutcomes = listOf(spec.outcome1, spec.outcome2),
              exercises = exercises
            )
          )
          globalStepCounter++
        }

        // Every 4 topics, insert a Spaced-Repetition Milestone & Cultural Checkpoint Lesson
        if ((topicIdx + 1) % 4 == 0) {
          val checkpointIndex = (topicIdx + 1) / 4
          val revId = "${langCode}_journey_${cefr.lowercase()}_milestone_$checkpointIndex"
          generated.add(
            CurriculumLesson(
              id = revId,
              title = "$cefr Milestone Review #$checkpointIndex: ${spec.title}",
              description = "Consolidate vocabulary, grammar, listening, and speaking across the last 16 progressive steps (#$globalStepCounter).",
              category = "Speaking",
              proficiencyLevel = profLevel,
              targetLanguageCode = langCode,
              estimatedMinutes = 6,
              xpReward = 60 + (bandIdx * 5),
              cefrLevel = cefr,
              unitTitle = unitTitle,
              learningOutcomes = listOf(
                "Retain key $cefr expressions in long-term memory",
                "Demonstrate spontaneous recall across multiple real-life scenarios"
              ),
              isRevision = true,
              exercises = listOf(
                Exercise(
                  id = "${revId}_ex_1",
                  type = ExerciseType.VOCABULARY,
                  prompt = "Milestone Recall: Select the correct meaning of \"$phrase\"",
                  explanationNative = "Spaced repetition across units locks in permanent fluency.",
                  targetAudioText = phrase,
                  options = listOf(
                    spec.meaning,
                    "The library is closed on national holidays",
                    "Please fasten your seatbelt during takeoff",
                    "We ordered two tickets for the symphony"
                  ),
                  correctOptionIndex = 0
                ),
                Exercise(
                  id = "${revId}_ex_2",
                  type = ExerciseType.SPEAKING,
                  prompt = "Milestone Speaking Check: Say \"$phrase\" aloud clearly:",
                  explanationNative = "Great job completing $cefr Milestone #$checkpointIndex!",
                  targetAudioText = phrase,
                  acceptableAnswers = listOf(phrase.lowercase(), spec.meaning.lowercase())
                )
              )
            )
          )
          globalStepCounter++
        }
      }
    }
    return generated
  }

  private fun buildSupplementaryCefrModules(langCode: String): List<CurriculumLesson> {
    val greetingsPhrase = when (langCode) {
      "es" -> "¡Hola! Mucho gusto en conocerte, ¿cómo estás hoy?"
      "fr" -> "Bonjour ! Enchanté de vous rencontrer, comment allez-vous ?"
      "de" -> "Hallo! Schön, dich kennenzulernen. Wie geht es dir heute?"
      "it" -> "Ciao! Molto piacere di conoscerti, come stai oggi?"
      "ja" -> "はじめまして、お会いできて嬉しいです。今日はお元気ですか？"
      "zh" -> "你好！很高兴认识你，你今天过得怎么样？"
      "ar" -> "مرحباً! سررت بلقائك، كيف حالك اليوم؟"
      else -> "Hello! Nice to meet you, how are you doing today?"
    }

    val restaurantPhrase = when (langCode) {
      "es" -> "Por favor, ¿me puede traer la carta y un vaso de agua?"
      "fr" -> "S'il vous plaît, pouvez-vous m'apporter le menu et un verre d'eau ?"
      "de" -> "Könnten Sie mir bitte die Speisekarte und ein Glas Wasser bringen?"
      "it" -> "Per favore, potrebbe portarmi il menu e un bicchiere d'acqua?"
      "ja" -> "メニューとお冷を一杯いただけますか？"
      "zh" -> "请给我菜单和一杯水好吗？"
      "ar" -> "من فضلك، هل يمكنك إحضار قائمة الطعام وكوب ماء؟"
      else -> "Could you please bring me the menu and a glass of water?"
    }

    val shoppingPhrase = when (langCode) {
      "es" -> "¿Cuánto cuesta esto y tienen alguna talla más grande?"
      "fr" -> "Combien cela coûte-t-il et avez-vous une taille plus grande ?"
      "de" -> "Wie viel kostet das und haben Sie eine größere Größe?"
      "it" -> "Quanto costa questo e avete una taglia più grande?"
      "ja" -> "これはいくらですか？もっと大きいサイズはありますか？"
      "zh" -> "这个多少钱？有大一点的尺码吗？"
      "ar" -> "كم سعر هذا، وهل لديكم مقاس أكبر؟"
      else -> "How much is this, and do you have a larger size?"
    }

    val transitPhrase = when (langCode) {
      "es" -> "Disculpe, ¿dónde está la estación de metro más cercana?"
      "fr" -> "Excusez-moi, où se trouve la station de métro la plus proche ?"
      "de" -> "Entschuldigung, wo ist die nächste U-Bahn-Station?"
      "it" -> "Mi scusi, dov'è la stazione della metropolitana più vicina?"
      "ja" -> "すみません、一番近い地下鉄の駅はどこですか？"
      "zh" -> "请问最近的地铁站在哪里？"
      "ar" -> "عذراً، أين أقرب محطة مترو من فضلك؟"
      else -> "Excuse me, where is the nearest subway station?"
    }

    val hotelPhrase = when (langCode) {
      "es" -> "Tengo una reserva a nombre de John para tres noches con desayuno incluido"
      "fr" -> "J'ai une réservation au nom de John pour trois nuits avec petit-déjeuner inclus"
      "de" -> "Ich habe eine Reservierung auf den Namen John für drei Nächte inklusive Frühstück"
      "it" -> "Ho una prenotazione a nome di John per tre notti con colazione inclusa"
      "ja" -> "朝食付き3泊でジョンという名前で予約しています"
      "zh" -> "我有约翰名下的预订，三晚并包含早餐"
      "ar" -> "لدي حجز باسم جون لمدة ثلاث ليالٍ مع وجبة الإفطار"
      else -> "I have a reservation under the name John for three nights with breakfast included"
    }

    val airportPhrase = when (langCode) {
      "es" -> "¿A qué puerta de embarque debo ir para el vuelo hacia Madrid?"
      "fr" -> "À quelle porte d'embarquement dois-je me rendre pour le vol vers Madrid ?"
      "de" -> "Zu welchem Flugsteig muss ich für den Flug nach Madrid gehen?"
      "it" -> "A quale gate di imbarco devo andare per il volo per Madrid?"
      "ja" -> "マドリード行きの便の搭乗口はどこですか？"
      "zh" -> "去马德里的航班在哪个登机口？"
      "ar" -> "إلى أي بوابة صعود يجب أن أذهب للرحلة المتجهة إلى مدريد؟"
      else -> "Which boarding gate should I go to for the flight to Madrid?"
    }

    val phonePhrase = when (langCode) {
      "es" -> "Buenos días, ¿podría comunicarme con el departamento de atención al cliente?"
      "fr" -> "Bonjour, pourrais-je être mis en relation avec le service client ?"
      "de" -> "Guten Tag, könnten Sie mich bitte mit dem Kundenservice verbinden?"
      "it" -> "Buongiorno, potrebbe passarmi il servizio clienti?"
      "ja" -> "おはようございます、カスタマーサービスにつないでいただけますか？"
      "zh" -> "早上好，请问能帮我转接客户服务部吗？"
      "ar" -> "صباح الخير، هل يمكن تحويلي إلى قسم خدمة العملاء؟"
      else -> "Good morning, could you please connect me with customer service?"
    }

    val friendsPhrase = when (langCode) {
      "es" -> "¿Te apetece tomar un café o almorzar juntos este sábado por la tarde?"
      "fr" -> "Est-ce que ça te dirait de prendre un café ou déjeuner ensemble ce samedi après-midi ?"
      "de" -> "Hast du Lust, diesen Samstagnachmittag zusammen einen Kaffee zu trinken oder zu Mittag zu essen?"
      "it" -> "Ti andrebbe di prendere un caffè o pranzare insieme questo sabato pomeriggio?"
      "ja" -> "今週の土曜日の午後に、一緒にお茶かランチでもしませんか？"
      "zh" -> "这个周六下午你想一起喝杯咖啡或吃个午饭吗？"
      "ar" -> "هل ترغب في تناول القهوة أو الغداء معاً بعد ظهر هذا السبت؟"
      else -> "Would you like to grab coffee or lunch together this Saturday afternoon?"
    }

    val doctorPhrase = when (langCode) {
      "es" -> "Tengo dolor de garganta, fiebre leve y mareos desde ayer por la noche"
      "fr" -> "J'ai mal à la gorge, une légère fièvre et des vertiges depuis hier soir"
      "de" -> "Ich habe seit gestern Abend Halsschmerzen, leichtes Fieber und Schwindel"
      "it" -> "Ho mal di gola, una leggera febbre e vertigini da ieri sera"
      "ja" -> "昨晩から喉の痛み、微熱、めまいがあります"
      "zh" -> "我从昨晚开始喉咙痛、轻微发烧和头晕"
      "ar" -> "أعاني من ألم في الحلق وحمى خفيفة ودوار منذ مساء أمس"
      else -> "I have a sore throat, mild fever, and dizziness since yesterday evening"
    }

    val travelPhrase = when (langCode) {
      "es" -> "¿Cuáles son los museos y barrios históricos más recomendados para visitar?"
      "fr" -> "Quels sont les musées et quartiers historiques les plus recommandés à visiter ?"
      "de" -> "Welche Museen und historischen Viertel sind am meisten zu empfehlen?"
      "it" -> "Quali sono i musei e i quartieri storici più consigliati da visitare?"
      "ja" -> "訪れるのに最もおすすめの博物館や歴史的地区はどこですか？"
      "zh" -> "最推荐参观的博物馆和历史街区有哪些？"
      "ar" -> "ما هي المتاحف والأحياء التاريخية الأكثر ترشيحاً للزيارة؟"
      else -> "Which museums and historic neighborhoods are most recommended to visit?"
    }

    val interviewPhrase = when (langCode) {
      "es" -> "Cuento con sólida experiencia liderando proyectos multiculturales y resolviendo desafíos complejos"
      "fr" -> "Je dispose d'une solide expérience dans la direction de projets multiculturels et la résolution de défis complexes"
      "de" -> "Ich verfüge über fundierte Erfahrung in der Leitung multikultureller Projekte und Lösung komplexer Aufgaben"
      "it" -> "Ho una solida esperienza nella gestione di progetti multiculturali e nella risoluzione di sfide complesse"
      "ja" -> "多文化プロジェクトのリードと複雑な課題解決において確かな経験があります"
      "zh" -> "我在领导多元文化项目和解决复杂挑战方面拥有丰富的经验"
      "ar" -> "لدي خبرة قوية في قيادة المشاريع متعددة الثقافات وحل التحديات المعقدة"
      else -> "I have strong experience leading multicultural projects and solving complex challenges"
    }

    val workplacePhrase = when (langCode) {
      "es" -> "Propongo ajustar el calendario de entregas para garantizar los más altos estándares de calidad"
      "fr" -> "Je propose d'ajuster le calendrier de livraison afin de garantir les normes de qualité les plus élevées"
      "de" -> "Ich schlage vor, den Zeitplan anzupassen, um höchste Qualitätsstandards sicherzustellen"
      "it" -> "Propongo di adeguare il calendario delle consegne per garantire i massimi standard qualitativi"
      "ja" -> "最高水準の品質を確保するため、納品スケジュールの調整をご提案します"
      "zh" -> "我建议调整交付时间表，以确保达到最高的质量标准"
      "ar" -> "أقترح تعديل الجدول الزمني للتسليم لضمان تحقيق أعلى معايير الجودة"
      else -> "I propose adjusting the delivery timeline to ensure the highest quality standards"
    }

    val opinionsPhrase = when (langCode) {
      "es" -> "Desde mi punto de vista, el intercambio cultural fomenta la empatía global y el pensamiento crítico"
      "fr" -> "De mon point de vue, l'échange culturel favorise l'empathie mondiale et la pensée critique"
      "de" -> "Meiner Ansicht nach fördert der kulturelle Austausch globales Verständnis und kritisches Denken"
      "it" -> "Dal mio punto di vista, lo scambio culturale favorisce l'empatia globale e il pensiero critico"
      "ja" -> "私の見解では、異文化交流は世界的な共感と批判的思考を促進します"
      "zh" -> "在我看来，文化交流能够促进全球共鸣与批判性思维"
      "ar" -> "من وجهة نظري، يعزز التبادل الثقافي التعاطف الإنساني العالمي والتفكير النقدي"
      else -> "From my perspective, cultural exchange fosters global empathy and critical thinking"
    }

    val debatePhrase = when (langCode) {
      "es" -> "Es imprescindible sopesar rigurosamente las implicaciones éticas y los beneficios a largo plazo"
      "fr" -> "Il est indispensable d'évaluer rigoureusement les implications éthiques et les bénéfices à long terme"
      "de" -> "Es ist unerlässlich, die ethischen Implikationen und die langfristigen Vorteile sorgfältig abzuwägen"
      "it" -> "È imprescindibile valutare rigorosamente le implicazioni etiche e i benefici a lungo termine"
      "ja" -> "倫理的影響と長期的メリットを厳密に比較検討することが極めて重要です"
      "zh" -> "严格权衡伦理影响与长远效益是至关重要的"
      "ar" -> "من الضروري للغاية الموازنة الدقيقة بين التداعيات الأخلاقية والفوائد طويلة الأجل"
      else -> "It is essential to rigorously weigh the ethical implications alongside long-term benefits"
    }

    val rhetoricPhrase = when (langCode) {
      "es" -> "Más allá de las apariencias inmediatas, subyace una complejidad insoslayable que exige un análisis matizado"
      "fr" -> "Au-delà des apparences immédiates, subsiste une complexité incontournable exigeant une analyse nuancée"
      "de" -> "Jenseits unmittelbarer Eindrücke verbirgt sich eine unvermeidliche Komplexität, die differenzierte Betrachtung erfordert"
      "it" -> "Al di là delle apparenze immediate, sottostà una complessità ineludibile che esige un'analisi sfumata"
      "ja" -> "目先の現象にとどまらず、その根底には緻密な分析を要する見逃せない複雑さが存在します"
      "zh" -> "透过表象，其底层潜藏着不可忽视的复杂性，需要进行深刻细致的分析"
      "ar" -> "وراء المظاهر العابرة، تكمن تعقيدات أصيلة تتطلب تحليلاً فكرياً دقيقاً ومتوازناً"
      else -> "Beyond immediate appearances lies an inescapable complexity demanding nuanced, rigorous analysis"
    }

    return listOf(
      // 1. A1 Greetings & Introductions
      CurriculumLesson(
        id = "${langCode}_conv_greetings_a1",
        title = "A1 Greetings & First Introductions",
        description = "Break the ice naturally: introduce yourself, ask friendly questions, and use polite cultural openers.",
        category = "Speaking",
        proficiencyLevel = "Beginner",
        targetLanguageCode = langCode,
        estimatedMinutes = 5,
        xpReward = 35,
        cefrLevel = "A1",
        unitTitle = "Unit 1: Breakthrough Foundations (A1)",
        learningOutcomes = listOf(
          "Introduce yourself clearly in social and professional settings",
          "Exchange polite greetings and ask how someone is doing"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_grt_ex_1",
            type = ExerciseType.VOCABULARY,
            prompt = "Learn the essential greeting phrase for meeting new people:",
            explanationNative = "Warm greetings establish immediate rapport. Notice the rising tone when asking how someone is doing.",
            targetAudioText = greetingsPhrase,
            phoneticGuide = "Clear initial stress with a welcoming upward intonation on the closing question.",
            options = listOf(
              "Greeting someone and asking how they are doing",
              "Asking where the bathroom is located",
              "Complaining about late bus schedules",
              "Requesting a store receipt"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_grt_ex_2",
            type = ExerciseType.LISTENING,
            prompt = "Listen to the speaker and identify what they are saying:",
            explanationNative = "Distinguishing friendly greeting phrases prepares your ear for real conversations.",
            targetAudioText = greetingsPhrase,
            options = listOf(
              "A warm greeting and introduction",
              "A weather warning for tomorrow",
              "An apology for arriving late",
              "A store closing announcement"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_grt_ex_3",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Say this warm greeting clearly into your microphone:",
            explanationNative = "Speaking greetings aloud trains natural speech cadence and confidence.",
            targetAudioText = greetingsPhrase,
            acceptableAnswers = listOf(greetingsPhrase.lowercase(), greetingsPhrase.lowercase().replace("¡", "").replace("¿", "").replace("?", "").replace("!", ""))
          ),
          Exercise(
            id = "${langCode}_grt_ex_4",
            type = ExerciseType.WRITING,
            prompt = "Type or write the polite greeting you just practiced:",
            explanationNative = "Writing builds orthographic accuracy and reinforces word order.",
            targetAudioText = greetingsPhrase,
            acceptableAnswers = listOf(greetingsPhrase.lowercase(), "hola mucho gusto", "bonjour enchante", "hello nice to meet you")
          ),
          Exercise(
            id = "${langCode}_grt_ex_5",
            type = ExerciseType.QUIZ,
            prompt = "When someone greets you warmly in a new country, what is the best conversational response?",
            explanationNative = "Returning the greeting with enthusiasm keeps the interaction flowing positively.",
            targetAudioText = greetingsPhrase,
            options = listOf(
              "Smile, return the polite greeting, and ask how they are doing",
              "Look down at your phone and walk away silently",
              "Demand immediate translation in your native language",
              "Hand over your passport without speaking"
            ),
            correctOptionIndex = 0
          )
        )
      ),

      // 2. A1 Restaurant & Cafe Ordering
      CurriculumLesson(
        id = "${langCode}_conv_restaurant_a1",
        title = "A1 Ordering at Restaurants & Cafes",
        description = "Order meals, request drinks, ask for the menu, and handle table dining with effortless courtesy.",
        category = "Listening",
        proficiencyLevel = "Beginner",
        targetLanguageCode = langCode,
        estimatedMinutes = 5,
        xpReward = 40,
        cefrLevel = "A1",
        unitTitle = "Unit 1: Breakthrough Foundations (A1)",
        learningOutcomes = listOf(
          "Ask for the menu and water politely at any cafe or bistro",
          "Understand server questions regarding drinks and specials"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_rest_ex_1",
            type = ExerciseType.LISTENING,
            prompt = "Listen to the customer at the restaurant counter:",
            explanationNative = "Polite modal requests ('¿Me puede traer...?' / 'Pouvez-vous m'apporter...?') are standard dining etiquette.",
            targetAudioText = restaurantPhrase,
            options = listOf(
              "Asking for the menu and a glass of water",
              "Asking for directions to the train station",
              "Reporting a lost credit card",
              "Inquiring about hotel check-out hours"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_rest_ex_2",
            type = ExerciseType.SPEAKING,
            prompt = "Practice speaking your dining request aloud:",
            explanationNative = "Clear dining requests guarantee great service and build travel self-reliance.",
            targetAudioText = restaurantPhrase,
            acceptableAnswers = listOf(restaurantPhrase.lowercase())
          ),
          Exercise(
            id = "${langCode}_rest_ex_3",
            type = ExerciseType.WRITING,
            prompt = "Complete the restaurant order in writing:",
            explanationNative = "Practicing food and beverage vocabulary solidifies spelling.",
            targetAudioText = restaurantPhrase,
            acceptableAnswers = listOf(restaurantPhrase.lowercase(), "traer la carta", "apporter le menu", "bring the menu")
          )
        )
      ),

      // 3. A2 Shopping, Prices & Bargaining
      CurriculumLesson(
        id = "${langCode}_conv_shopping_a2",
        title = "A2 Shopping, Prices & Bargaining",
        description = "Ask for prices, check different sizes, and inquire about discounts at neighborhood shops and markets.",
        category = "Speaking",
        proficiencyLevel = "Beginner",
        targetLanguageCode = langCode,
        estimatedMinutes = 5,
        xpReward = 40,
        cefrLevel = "A2",
        unitTitle = "Unit 2: Everyday Communication (A2)",
        learningOutcomes = listOf(
          "Inquire about item costs and available garment sizes",
          "Navigate marketplace interactions and polite bargaining"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_shop_ex_1",
            type = ExerciseType.VOCABULARY,
            prompt = "Learn how to ask about prices and garment sizes:",
            explanationNative = "Price inquiries followed by size checks are essential for hassle-free shopping.",
            targetAudioText = shoppingPhrase,
            options = listOf(
              "Asking the price and whether a larger size is available",
              "Returning a broken electrical appliance",
              "Asking what time the store opens tomorrow",
              "Paying with an expired gift card"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_shop_ex_2",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Ask the shopkeeper for the price and size:",
            explanationNative = "Speaking prices clearly ensures you never overpay or pick the wrong size.",
            targetAudioText = shoppingPhrase,
            acceptableAnswers = listOf(shoppingPhrase.lowercase())
          )
        )
      ),

      // 4. A2 Transit & City Directions
      CurriculumLesson(
        id = "${langCode}_conv_transit_a2",
        title = "A2 Transit & City Directions",
        description = "Navigate subway networks, buses, and city streets with confidence by asking clear directional questions.",
        category = "Listening",
        proficiencyLevel = "Beginner",
        targetLanguageCode = langCode,
        estimatedMinutes = 5,
        xpReward = 40,
        cefrLevel = "A2",
        unitTitle = "Unit 2: Everyday Communication (A2)",
        learningOutcomes = listOf(
          "Ask for the nearest subway station or transit hub",
          "Understand spatial directions (left, right, straight ahead)"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_trnst_ex_1",
            type = ExerciseType.LISTENING,
            prompt = "Listen to the street question and choose the correct meaning:",
            explanationNative = "Starting with 'Disculpe / Excusez-moi' gets pedestrian attention respectfully.",
            targetAudioText = transitPhrase,
            options = listOf(
              "Asking where the nearest subway station is located",
              "Inquiring about taxi fares to the airport",
              "Asking for a good seafood restaurant",
              "Asking if the park is closed"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_trnst_ex_2",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Ask a passerby for the subway station:",
            explanationNative = "Oral practice of transit directions helps in busy, noisy transit stations.",
            targetAudioText = transitPhrase,
            acceptableAnswers = listOf(transitPhrase.lowercase())
          )
        )
      ),

      // 5. A2 Hotel Bookings & Check-In
      CurriculumLesson(
        id = "${langCode}_conv_hotels_a2",
        title = "A2 Booking Hotels & Checking In",
        description = "Check into your accommodations, confirm reservation details, breakfast hours, and room amenities.",
        category = "Grammar",
        proficiencyLevel = "Beginner",
        targetLanguageCode = langCode,
        estimatedMinutes = 6,
        xpReward = 45,
        cefrLevel = "A2",
        unitTitle = "Unit 2: Everyday Communication (A2)",
        learningOutcomes = listOf(
          "Confirm your hotel booking name and duration of stay",
          "Verify included amenities like breakfast and Wi-Fi"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_htl_ex_1",
            type = ExerciseType.GRAMMAR,
            prompt = "Study this standard hotel check-in phrase:",
            explanationNative = "Specifying duration and breakfast upfront prevents check-in confusion.",
            targetAudioText = hotelPhrase,
            options = listOf(
              "Confirming a 3-night stay under your name with breakfast included",
              "Complaining about noisy street traffic outside",
              "Asking for a taxi ride to the harbor",
              "Requesting early luggage storage only"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_htl_ex_2",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Present your reservation to the hotel concierge:",
            explanationNative = "Confident front desk communication speeds up check-in after long flights.",
            targetAudioText = hotelPhrase,
            acceptableAnswers = listOf(hotelPhrase.lowercase())
          )
        )
      ),

      // 6. B1 Airport, Boarding & Customs
      CurriculumLesson(
        id = "${langCode}_conv_airport_b1",
        title = "B1 Airport, Boarding & Flight Customs",
        description = "Find boarding gates, locate luggage carousels, and answer routine passport control and customs inquiries.",
        category = "Listening",
        proficiencyLevel = "Intermediate",
        targetLanguageCode = langCode,
        estimatedMinutes = 6,
        xpReward = 50,
        cefrLevel = "B1",
        unitTitle = "Unit 3: Independent User Consolidation (B1)",
        learningOutcomes = listOf(
          "Identify flight gate announcements and boarding calls",
          "Respond clearly to customs and border agents"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_air_ex_1",
            type = ExerciseType.LISTENING,
            prompt = "Listen to the airport announcement and determine the request:",
            explanationNative = "Terminal announcements require identifying destination city and gate terminology.",
            targetAudioText = airportPhrase,
            options = listOf(
              "Asking which boarding gate to go to for the flight",
              "Reporting an overweight carry-on suitcase",
              "Looking for duty-free perfume shops",
              "Asking where lost passports are stored"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_air_ex_2",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Inquire about your flight's boarding gate:",
            explanationNative = "Practicing airport terminology reduces stress in international terminals.",
            targetAudioText = airportPhrase,
            acceptableAnswers = listOf(airportPhrase.lowercase())
          )
        )
      ),

      // 7. B1 Phone Calls & Messaging
      CurriculumLesson(
        id = "${langCode}_conv_phone_b1",
        title = "B1 Phone Calls & Messaging Etiquette",
        description = "Handle phone conversations without visual cues: connect to departments, leave messages, and follow up.",
        category = "Speaking",
        proficiencyLevel = "Intermediate",
        targetLanguageCode = langCode,
        estimatedMinutes = 6,
        xpReward = 50,
        cefrLevel = "B1",
        unitTitle = "Unit 3: Independent User Consolidation (B1)",
        learningOutcomes = listOf(
          "Open telephone calls with professional greetings and department requests",
          "Ask callers to repeat or clarify without anxiety"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_phn_ex_1",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Make a polite phone transfer request:",
            explanationNative = "Telephone communication relies purely on auditory cues and clear articulation.",
            targetAudioText = phonePhrase,
            acceptableAnswers = listOf(phonePhrase.lowercase())
          ),
          Exercise(
            id = "${langCode}_phn_ex_2",
            type = ExerciseType.WRITING,
            prompt = "Draft the customer support phone message:",
            explanationNative = "Writing formal correspondence improves vocabulary precision.",
            targetAudioText = phonePhrase,
            acceptableAnswers = listOf(phonePhrase.lowercase(), "atencion al cliente", "service client", "customer service")
          )
        )
      ),

      // 8. B1 Making Friends & Socializing
      CurriculumLesson(
        id = "${langCode}_conv_friends_b1",
        title = "B1 Making Friends & Social Plans",
        description = "Invite new friends for coffee, suggest weekend outings, negotiate times, and build lasting friendships.",
        category = "Revision",
        proficiencyLevel = "Intermediate",
        targetLanguageCode = langCode,
        estimatedMinutes = 6,
        xpReward = 50,
        cefrLevel = "B1",
        isRevision = true,
        unitTitle = "Unit 3: Independent User Consolidation (B1)",
        learningOutcomes = listOf(
          "Propose informal weekend plans with natural conversational invitations",
          "Accept or decline invitations politely with alternative suggestions"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_frnd_ex_1",
            type = ExerciseType.VOCABULARY,
            prompt = "Study this friendly weekend invitation:",
            explanationNative = "Using '¿Te apetece...?' / 'Ça te dirait...?' sounds warm and native-like.",
            targetAudioText = friendsPhrase,
            options = listOf(
              "Inviting a friend to grab coffee or lunch on Saturday afternoon",
              "Canceling a doctor appointment",
              "Asking for a salary advance",
              "Returning borrowed library books"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_frnd_ex_2",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Invite your language partner out for coffee:",
            explanationNative = "Practicing social invitations makes community bonding effortless.",
            targetAudioText = friendsPhrase,
            acceptableAnswers = listOf(friendsPhrase.lowercase())
          )
        )
      ),

      // 9. B1 Doctor & Medical Consultations
      CurriculumLesson(
        id = "${langCode}_conv_doctor_b1",
        title = "B1 Doctor & Medical Consultations",
        description = "Describe symptoms, duration of illness, pain levels, and understand prescription dosage instructions.",
        category = "Listening",
        proficiencyLevel = "Intermediate",
        targetLanguageCode = langCode,
        estimatedMinutes = 6,
        xpReward = 55,
        cefrLevel = "B1",
        unitTitle = "Unit 3: Independent User Consolidation (B1)",
        learningOutcomes = listOf(
          "Describe physical symptoms, onset timeline, and pain intensity",
          "Understand pharmacy dosage instructions and doctor advice"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_doc_ex_1",
            type = ExerciseType.LISTENING,
            prompt = "Listen to the patient explaining symptoms to the doctor:",
            explanationNative = "Communicating health symptoms accurately is crucial when traveling or living abroad.",
            targetAudioText = doctorPhrase,
            options = listOf(
              "Describing a sore throat, mild fever, and dizziness since yesterday",
              "Requesting a dental cleaning appointment next month",
              "Asking for a gym membership discount",
              "Ordering herbal tea at a supermarket"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_doc_ex_2",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Clearly explain your symptoms to the physician:",
            explanationNative = "Clear clinical communication helps doctors diagnose accurately.",
            targetAudioText = doctorPhrase,
            acceptableAnswers = listOf(doctorPhrase.lowercase())
          )
        )
      ),

      // 10. B1 Travel & Cultural Exploration
      CurriculumLesson(
        id = "${langCode}_conv_travel_b1",
        title = "B1 Travel & Cultural Exploration",
        description = "Ask locals for hidden cultural gems, historical landmarks, museum passes, and authentic experiences.",
        category = "Reading",
        proficiencyLevel = "Intermediate",
        targetLanguageCode = langCode,
        estimatedMinutes = 6,
        xpReward = 55,
        cefrLevel = "B1",
        unitTitle = "Unit 3: Independent User Consolidation (B1)",
        learningOutcomes = listOf(
          "Ask residents for authentic cultural recommendations",
          "Read museum guides and heritage site plaques with comprehension"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_trvl_ex_1",
            type = ExerciseType.READING,
            prompt = "Read the cultural inquiry and determine its purpose:",
            explanationNative = "Locals appreciate travelers who seek out historical context rather than only tourist traps.",
            targetAudioText = travelPhrase,
            options = listOf(
              "Asking for recommended museums and historic neighborhoods",
              "Asking about cheap fast food chains near the highway",
              "Reporting lost keys at the information booth",
              "Inquiring about car rental insurance rates"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_trvl_ex_2",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Inquire about recommended historical districts:",
            explanationNative = "Cultural questions unlock rich conversations with native speakers.",
            targetAudioText = travelPhrase,
            acceptableAnswers = listOf(travelPhrase.lowercase())
          )
        )
      ),

      // 11. B2 Job Interviews & Career Goals
      CurriculumLesson(
        id = "${langCode}_conv_interview_b2",
        title = "B2 Job Interviews & Professional Background",
        description = "Highlight your career accomplishments, leadership experiences, problem-solving skills, and ambitions.",
        category = "Speaking",
        proficiencyLevel = "Intermediate",
        targetLanguageCode = langCode,
        estimatedMinutes = 7,
        xpReward = 60,
        cefrLevel = "B2",
        unitTitle = "Unit 4: Vantage & Professional Fluency (B2)",
        learningOutcomes = listOf(
          "Articulate professional skills and multicultural leadership background",
          "Handle behavioral interview questions with structured responses"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_int_ex_1",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Present your leadership background to the interviewer:",
            explanationNative = "Using action verbs and highlighting multicultural experience demonstrates B2 professional polish.",
            targetAudioText = interviewPhrase,
            acceptableAnswers = listOf(interviewPhrase.lowercase())
          ),
          Exercise(
            id = "${langCode}_int_ex_2",
            type = ExerciseType.WRITING,
            prompt = "Type this career statement for your resume profile:",
            explanationNative = "Writing career summaries develops professional vocabulary and complex syntax.",
            targetAudioText = interviewPhrase,
            acceptableAnswers = listOf(interviewPhrase.lowercase(), "liderando proyectos", "leading projects")
          )
        )
      ),

      // 12. B2 Workplace Meetings & Negotiations
      CurriculumLesson(
        id = "${langCode}_conv_workplace_b2",
        title = "B2 Workplace Meetings & Negotiations",
        description = "Diplomatically propose timeline changes, balance stakeholder feedback, and negotiate compromises.",
        category = "Grammar",
        proficiencyLevel = "Intermediate",
        targetLanguageCode = langCode,
        estimatedMinutes = 7,
        xpReward = 60,
        cefrLevel = "B2",
        unitTitle = "Unit 4: Vantage & Professional Fluency (B2)",
        learningOutcomes = listOf(
          "Propose constructive timeline adjustments diplomatically",
          "Facilitate consensus and negotiate win-win compromises in meetings"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_wrk_ex_1",
            type = ExerciseType.GRAMMAR,
            prompt = "Analyze how this proposal frames a schedule adjustment diplomatically:",
            explanationNative = "Framing proposals around quality standards prevents resistance during project reviews.",
            targetAudioText = workplacePhrase,
            options = listOf(
              "Proposing an adjusted schedule to safeguard high quality standards",
              "Refusing to work on the project altogether",
              "Blaming coworkers for project delays",
              "Demanding an immediate double payment"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_wrk_ex_2",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Deliver this diplomatic proposal in your team meeting:",
            explanationNative = "Mastering professional tone builds respect in international workplaces.",
            targetAudioText = workplacePhrase,
            acceptableAnswers = listOf(workplacePhrase.lowercase())
          )
        )
      ),

      // 13. C1 Expressing Opinions & Cultural Exchange
      CurriculumLesson(
        id = "${langCode}_conv_opinions_c1",
        title = "C1 Expressing Opinions & Cultural Exchange",
        description = "Articulate complex personal perspectives, support claims with logic, and foster intercultural dialogue.",
        category = "Speaking",
        proficiencyLevel = "Advanced",
        targetLanguageCode = langCode,
        estimatedMinutes = 8,
        xpReward = 65,
        cefrLevel = "C1",
        unitTitle = "Unit 5: Autonomous Academic & Cultural Discourse (C1)",
        learningOutcomes = listOf(
          "Deploy nuanced opinion markers ('From my perspective...', 'It bears noting...')",
          "Synthesize cross-cultural insights into cohesive verbal essays"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_opn_ex_1",
            type = ExerciseType.SPEAKING,
            prompt = "Deliver this articulated perspective aloud with rhythmic conviction:",
            explanationNative = "C1 speakers connect abstract concepts (empathy, critical thinking) effortlessly.",
            targetAudioText = opinionsPhrase,
            acceptableAnswers = listOf(opinionsPhrase.lowercase())
          ),
          Exercise(
            id = "${langCode}_opn_ex_2",
            type = ExerciseType.READING,
            prompt = "What is the core argument of this cultural reflection?",
            explanationNative = "Understanding subtle philosophical claims is the hallmark of C1 fluency.",
            targetAudioText = opinionsPhrase,
            options = listOf(
              "Cultural exchange actively cultivates global empathy and critical thinking",
              "People should avoid interacting with different cultures",
              "Only standardized tests determine linguistic proficiency",
              "Grammar rules should never change over time"
            ),
            correctOptionIndex = 0
          )
        )
      ),

      // 14. C1 Academic & Complex Debates
      CurriculumLesson(
        id = "${langCode}_conv_debates_c1",
        title = "C1 Academic & Complex Debates",
        description = "Dissect ethical dilemmas, hedge arguments diplomatically, and construct persuasive evidence-based points.",
        category = "Listening",
        proficiencyLevel = "Advanced",
        targetLanguageCode = langCode,
        estimatedMinutes = 8,
        xpReward = 65,
        cefrLevel = "C1",
        unitTitle = "Unit 5: Autonomous Academic & Cultural Discourse (C1)",
        learningOutcomes = listOf(
          "Evaluate complex debate theses on ethics and long-term implications",
          "Adopt high-level academic register and counter-argument hedging"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_dbt_ex_1",
            type = ExerciseType.LISTENING,
            prompt = "Listen to the formal academic debate argument and identify its core thesis:",
            explanationNative = "Hedging with modal adjectives ('imprescindible / indispensable') is vital for academic integrity.",
            targetAudioText = debatePhrase,
            options = listOf(
              "It is essential to rigorously weigh ethical implications alongside long-term benefits",
              "Quick profits should always take priority over ethical concerns",
              "Debates are unproductive and should be avoided in seminars",
              "Technology should be banned in academic institutions"
            ),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${langCode}_dbt_ex_2",
            type = ExerciseType.SPEAKING,
            prompt = "Speak aloud: Deliver this debate argument with confident formal intonation:",
            explanationNative = "Delivering formal debate statements strengthens rhetorical presence.",
            targetAudioText = debatePhrase,
            acceptableAnswers = listOf(debatePhrase.lowercase())
          )
        )
      ),

      // 15. C2 Nuanced Perspectives & Rhetoric
      CurriculumLesson(
        id = "${langCode}_conv_rhetoric_c2",
        title = "C2 Nuanced Perspectives & Rhetoric",
        description = "Master native idiomatic subtlety, philosophical discourse, layered metaphors, and eloquent prose.",
        category = "Speaking",
        proficiencyLevel = "Advanced",
        targetLanguageCode = langCode,
        estimatedMinutes = 8,
        xpReward = 70,
        cefrLevel = "C2",
        unitTitle = "Unit 6: Proficient Mastery & Rhetoric (C1–C2)",
        learningOutcomes = listOf(
          "Command high-register rhetorical vocabulary and stylistic nuance",
          "Express layered philosophical perspectives with native-like eloquence"
        ),
        exercises = listOf(
          Exercise(
            id = "${langCode}_rht_ex_1",
            type = ExerciseType.SPEAKING,
            prompt = "Deliver this masterwork statement aloud with expressive prosody:",
            explanationNative = "C2 mastery involves effortless command of literary phrasing and evocative cadence.",
            targetAudioText = rhetoricPhrase,
            acceptableAnswers = listOf(rhetoricPhrase.lowercase())
          ),
          Exercise(
            id = "${langCode}_rht_ex_2",
            type = ExerciseType.QUIZ,
            prompt = "What characterizes C2 communication in advanced academic and intellectual contexts?",
            explanationNative = "C2 speakers navigate ambiguity, appreciate stylistic subtext, and express thought with precision.",
            targetAudioText = rhetoricPhrase,
            options = listOf(
              "Expressive nuance, seamless register adaptation, and deep sensitivity to subtext",
              "Repeating single-syllable tourist phrases louder",
              "Translating word-for-word using pocket dictionaries",
              "Avoiding complex subordinate clauses entirely"
            ),
            correctOptionIndex = 0
          )
        )
      )
    )
  }

  fun getDailyLearningItem(targetLangCode: String): DailyLearningCardItem {
    val cleanCode = targetLangCode.lowercase().substringBefore("-")
    val langMeta = com.example.data.model.Language.findByCode(cleanCode)
    return when (cleanCode) {
      "es" -> DailyLearningCardItem(
        id = "daily_es_today",
        languageCode = "es",
        phraseOrIdiom = "Echar una mano a los demás",
        translation = "To lend a hand to others",
        explanation = "A beloved everyday Spanish idiom meaning to help someone out generously without being asked.",
        phoneticGuide = "eh-CHAR oo-nah MAH-noh ah lohs deh-MAHS",
        category = "Idiom of the Day"
      )
      "fr" -> DailyLearningCardItem(
        id = "daily_fr_today",
        languageCode = "fr",
        phraseOrIdiom = "Avoir le coup de foudre",
        translation = "Love at first sight (struck by lightning)",
        explanation = "Literally 'to be struck by lightning', French speakers use this when instantly captivated by a person, place, or idea.",
        phoneticGuide = "ah-vwar luh koo duh foodr",
        category = "Cultural Expression"
      )
      "de" -> DailyLearningCardItem(
        id = "daily_de_today",
        languageCode = "de",
        phraseOrIdiom = "Übung macht den Meister",
        translation = "Practice makes perfect (creates the master)",
        explanation = "The classic German proverb emphasizing daily consistency over rushed cramming in learning.",
        phoneticGuide = "OO-boong mahkht dehn MYS-ter",
        category = "Proverb of the Day"
      )
      "it" -> DailyLearningCardItem(
        id = "daily_it_today",
        languageCode = "it",
        phraseOrIdiom = "In bocca al lupo!",
        translation = "Good luck! (Into the mouth of the wolf)",
        explanation = "The quintessential Italian way to wish good luck. The traditional response is always 'Crepi il lupo!' (May the wolf die!).",
        phoneticGuide = "een BOHK-kah ahl LOO-poh",
        category = "Cultural Superstition"
      )
      "ja" -> DailyLearningCardItem(
        id = "daily_ja_today",
        languageCode = "ja",
        phraseOrIdiom = "一期一会 (Ichigo Ichie)",
        translation = "Treasure every encounter; once in a lifetime",
        explanation = "A profound tea-ceremony philosophy reminding us that every meeting with another person is unique and never to be repeated.",
        phoneticGuide = "ee-chee-go ee-chee-eh",
        category = "Wisdom & Culture"
      )
      "zh" -> DailyLearningCardItem(
        id = "daily_zh_today",
        languageCode = "zh",
        phraseOrIdiom = "千里之行，始于足下",
        translation = "A journey of a thousand miles begins with a single step",
        explanation = "Famous Laozi quote celebrating the power of small daily learning habits in achieving grand fluency goals.",
        phoneticGuide = "qiān lǐ zhī xíng, shǐ yú zú xià",
        category = "Proverb of the Day"
      )
      "ar" -> DailyLearningCardItem(
        id = "daily_ar_today",
        languageCode = "ar",
        phraseOrIdiom = "طلب العلم من المهد إلى اللحد",
        translation = "Seek knowledge from the cradle to the grave",
        explanation = "An inspiring traditional proverb honoring lifelong learning, intellectual curiosity, and self-improvement.",
        phoneticGuide = "Talab al-’ilm min al-mahd ila al-lahd",
        category = "Golden Wisdom"
      )
      "ur" -> DailyLearningCardItem(
        id = "daily_ur_today",
        languageCode = "ur",
        phraseOrIdiom = "قطرہ قطرہ مل کر دریا بنتا ہے",
        translation = "Drop by drop, a river is formed",
        explanation = "A classic Urdu proverb highlighting how small daily efforts accumulate into extraordinary mastery.",
        phoneticGuide = "Qatra qatra mil kar darya banta hai",
        category = "Proverb of the Day"
      )
      "hi" -> DailyLearningCardItem(
        id = "daily_hi_today",
        languageCode = "hi",
        phraseOrIdiom = "बूंद-बूंद से घड़ा भरता है",
        translation = "Drop by drop, the pitcher fills up",
        explanation = "A timeless Hindi proverb reminding learners that steady daily practice builds complete fluency.",
        phoneticGuide = "Boond-boond se ghada bharta hai",
        category = "Wisdom & Culture"
      )
      "ko" -> DailyLearningCardItem(
        id = "daily_ko_today",
        languageCode = "ko",
        phraseOrIdiom = "천 리 길도 한 걸음부터",
        translation = "Even a thousand-mile journey begins with a single step",
        explanation = "An encouraging Korean proverb used when starting a rewarding long-term endeavor.",
        phoneticGuide = "Cheon ri gil-do han geol-eum-bu-teo",
        category = "Proverb of the Day"
      )
      "pt" -> DailyLearningCardItem(
        id = "daily_pt_today",
        languageCode = "pt",
        phraseOrIdiom = "Devagar se vai ao longe",
        translation = "Slowly one goes far (Steady progress wins)",
        explanation = "A popular Portuguese proverb celebrating patience and steady consistency.",
        phoneticGuide = "deh-vah-GAR see vay ow LOHN-zhee",
        category = "Proverb of the Day"
      )
      "tr" -> DailyLearningCardItem(
        id = "daily_tr_today",
        languageCode = "tr",
        phraseOrIdiom = "Damlaya damlaya göl olur",
        translation = "Drop by drop, a lake is formed",
        explanation = "A beloved Turkish proverb teaching that consistent small steps create lasting achievements.",
        phoneticGuide = "dahm-lah-yah dahm-lah-yah goel oh-loor",
        category = "Proverb of the Day"
      )
      "ru" -> DailyLearningCardItem(
        id = "daily_ru_today",
        languageCode = "ru",
        phraseOrIdiom = "Повторение — мать учения",
        translation = "Repetition is the mother of learning",
        explanation = "A famous Russian proverb underscoring spaced repetition in mastering any skill.",
        phoneticGuide = "Paf-ta-rye-ni-ye — mat' u-che-ni-ya",
        category = "Proverb of the Day"
      )
      else -> DailyLearningCardItem(
        id = "daily_${cleanCode}_today",
        languageCode = targetLangCode,
        phraseOrIdiom = langMeta.sampleGreeting,
        translation = "Hello! How are you doing today? (${langMeta.name})",
        explanation = "Practicing ${langMeta.name} (${langMeta.nativeName}) in ${langMeta.writingSystem} for 10 minutes each day builds lasting fluency.",
        phoneticGuide = "${langMeta.writingSystem} • ${langMeta.localeCode}",
        category = "${langMeta.name} Expression of the Day"
      )
    }
  }


  fun getPlacementQuestions(targetLangCode: String): List<PlacementQuestion> {
    val code = targetLangCode.lowercase().substringBefore("-")
    val langMeta = com.example.data.model.Language.findByCode(code)
    return when (code) {
      "es" -> listOf(
        PlacementQuestion(
          id = "pl_es_a1",
          cefrBand = "A1",
          skillArea = "Vocabulary",
          prompt = "Choose the natural greeting when entering a shop in the morning:",
          options = listOf("¡Buenos días! ¿Tienen pan fresco?", "¡Buenas noches! Hasta mañana.", "No entiendo el ayer.", "Mañana fui al mercado."),
          correctIndex = 0,
          explanation = "'¡Buenos días!' is the standard morning greeting (A1)."
        ),
        PlacementQuestion(
          id = "pl_es_a2",
          cefrBand = "A2",
          skillArea = "Grammar",
          prompt = "Complete the past sentence: 'Ayer nosotros _____ en un restaurante cerca del puerto.'",
          options = listOf("cenamos", "cenaremos", "cenando", "ceno"),
          correctIndex = 0,
          explanation = "'Cenamos' is the first-person plural preterite form used for completed actions yesterday (A2)."
        ),
        PlacementQuestion(
          id = "pl_es_b1",
          cefrBand = "B1",
          skillArea = "Grammar",
          prompt = "Select the correct form: 'Espero que tengas un buen viaje y que me _____ cuando llegues.'",
          options = listOf("llames", "llamas", "llamarás", "llamaste"),
          correctIndex = 0,
          explanation = "After 'Espero que...' and 'cuando' referring to a future event, Spanish requires the present subjunctive ('llames', B1)."
        ),
        PlacementQuestion(
          id = "pl_es_b2",
          cefrBand = "B2",
          skillArea = "Pragmatics",
          prompt = "Complete the conditional hypothesis: 'Si hubiera sabido que venías, _____ preparado tu plato favorito.'",
          options = listOf("habría", "habré", "he", "tenía"),
          correctIndex = 0,
          explanation = "Past counterfactuals pair 'Si hubiera + participle' with 'habría + participle' (B2)."
        ),
        PlacementQuestion(
          id = "pl_es_c1",
          cefrBand = "C1",
          skillArea = "Reading & Register",
          prompt = "Which connector best introduces a concession in a formal executive report?",
          options = listOf("Si bien los resultados iniciales son prometedores, conviene mantener la prudencia.", "Porque sí los resultados son buenos.", "Pero mañana es otro día.", "Así que hola a todos."),
          correctIndex = 0,
          explanation = "'Si bien...' and 'conviene mantener la prudencia' reflect formal C1–C2 discourse register."
        )
      )
      "fr" -> listOf(
        PlacementQuestion(
          id = "pl_fr_a1",
          cefrBand = "A1",
          skillArea = "Vocabulary",
          prompt = "How do you politely order a croissant in a French bakery?",
          options = listOf("Je voudrais un croissant, s'il vous plaît.", "Au revoir le croissant.", "Je suis un croissant.", "Hier je mange demain."),
          correctIndex = 0,
          explanation = "'Je voudrais..., s'il vous plaît' is the essential polite A1 request."
        ),
        PlacementQuestion(
          id = "pl_fr_a2",
          cefrBand = "A2",
          skillArea = "Grammar",
          prompt = "Complete in the passé composé: 'Hier soir, nous _____ allés au cinéma.'",
          options = listOf("sommes", "avons", "allons", "étions"),
          correctIndex = 0,
          explanation = "'Aller' takes the auxiliary 'être' ('nous sommes allés') in the passé composé (A2)."
        ),
        PlacementQuestion(
          id = "pl_fr_b1",
          cefrBand = "B1",
          skillArea = "Grammar",
          prompt = "Complete: 'Il faut que tu _____ tes devoirs avant de sortir.'",
          options = listOf("fasses", "fais", "feras", "faisais"),
          correctIndex = 0,
          explanation = "'Il faut que' triggers the present subjunctive ('fasses', B1)."
        ),
        PlacementQuestion(
          id = "pl_fr_b2",
          cefrBand = "B2",
          skillArea = "Grammar",
          prompt = "Complete: 'Si j'avais eu plus de temps, je t'_____ accompagné à la gare.'",
          options = listOf("aurais", "avais", "aurai", "ai"),
          correctIndex = 0,
          explanation = "Past conditional ('aurais accompagné') completes a 'Si + plus-que-parfait' clause (B2)."
        ),
        PlacementQuestion(
          id = "pl_fr_c1",
          cefrBand = "C1",
          skillArea = "Register",
          prompt = "Which sentence demonstrates C1 formal synthesis?",
          options = listOf("Quoique cette initiative présente des défis, elle n'en demeure pas moins essentielle.", "C'est bon mais non.", "Alors on va voir demain.", "Merci pour tout."),
          correctIndex = 0,
          explanation = "'Quoique + subjunctive' and 'ne... pas moins' mark C1–C2 mastery."
        )
      )
      else -> {
        val topics = extendedCoreTopics
        listOf(
          PlacementQuestion(
            id = "pl_${code}_a1",
            cefrBand = "A1",
            skillArea = "Vocabulary",
            prompt = "Which ${langMeta.name} expression is a warm greeting used when meeting someone?",
            options = listOf(langMeta.sampleGreeting, "12345", "No connection", "Closed for winter"),
            correctIndex = 0,
            explanation = "'${langMeta.sampleGreeting}' is a foundational A1 greeting in ${langMeta.name} (${langMeta.writingSystem})."
          ),
          PlacementQuestion(
            id = "pl_${code}_a2",
            cefrBand = "A2",
            skillArea = "Grammar",
            prompt = "Select the ${langMeta.name} sentence that expresses: '${topics[1].meaning}'",
            options = listOf(topics[1].targetPhrase(code), "Tomorrow never knows", "Only numbers", "Silent mode"),
            correctIndex = 0,
            explanation = "Introducing your personal background and work is core to A2 proficiency in ${langMeta.name}."
          ),
          PlacementQuestion(
            id = "pl_${code}_b1",
            cefrBand = "B1",
            skillArea = "Pragmatics",
            prompt = "How do you ask about transit transfers in ${langMeta.name}: '${topics[7].meaning}'?",
            options = listOf(topics[7].targetPhrase(code), "Pointing without speaking", "Reciting the alphabet", "Leaving the station"),
            correctIndex = 0,
            explanation = "B1 Independent Users navigate transit and travel situations in ${langMeta.name}."
          ),
          PlacementQuestion(
            id = "pl_${code}_b2",
            cefrBand = "B2",
            skillArea = "Discourse",
            prompt = "Which ${langMeta.name} statement expresses a balanced viewpoint: '${topics[14].meaning}'?",
            options = listOf(topics[14].targetPhrase(code), "One-word yes/no reply", "Repeating the question", "Avoiding reasons"),
            correctIndex = 0,
            explanation = "B2 speakers sustain viewpoints clearly using polite discourse connectors in ${langMeta.name}."
          ),
          PlacementQuestion(
            id = "pl_${code}_c1",
            cefrBand = "C1",
            skillArea = "Register",
            prompt = "Select the formal ${langMeta.name} expression for collaborative problem solving: '${topics[18].meaning}'",
            options = listOf(topics[18].targetPhrase(code), "Basic tourist nouns only", "Word-by-word literal gloss", "Relying on gestures"),
            correctIndex = 0,
            explanation = "C1–C2 Proficient Users adapt register and negotiate solutions effortlessly in ${langMeta.name}."
          )
        )
      }
    }
  }

  fun evaluatePlacementScore(correctCount: Int, totalCount: Int): PlacementTestEvaluation {
    val safeTotal = totalCount.coerceAtLeast(1)
    val scorePercent = ((correctCount * 100.0) / safeTotal).toInt().coerceIn(0, 100)
    return when {
      scorePercent >= 90 -> PlacementTestEvaluation(
        recommendedCefrBand = "C1",
        mappedProficiencyLevel = "Advanced",
        scorePercent = scorePercent,
        summaryFeedback = "Outstanding command of grammar, register, and complex structures! We recommend starting at C1–C2 Advanced modules and nuanced AI debates."
      )
      scorePercent >= 75 -> PlacementTestEvaluation(
        recommendedCefrBand = "B2",
        mappedProficiencyLevel = "Advanced",
        scorePercent = scorePercent,
        summaryFeedback = "Strong upper-intermediate foundation! You are ready for B2–C1 complex roleplays, workplace discussions, and authentic reading."
      )
      scorePercent >= 55 -> PlacementTestEvaluation(
        recommendedCefrBand = "B1",
        mappedProficiencyLevel = "Intermediate",
        scorePercent = scorePercent,
        summaryFeedback = "Solid conversational foundation! We recommend starting at B1 Intermediate to master connectors, past/future narration, and travel fluency."
      )
      scorePercent >= 35 -> PlacementTestEvaluation(
        recommendedCefrBand = "A2",
        mappedProficiencyLevel = "Beginner",
        scorePercent = scorePercent,
        summaryFeedback = "Good grasp of basic greetings! Start at A2 Waystage to build everyday sentences, shopping, dining, and past-tense storytelling."
      )
      else -> PlacementTestEvaluation(
        recommendedCefrBand = "A1",
        mappedProficiencyLevel = "Beginner",
        scorePercent = scorePercent,
        summaryFeedback = "Welcome to your language journey! Start at A1 Breakthrough to master essential greetings, pronunciation, and core survival vocabulary."
      )
    }
  }

  fun getListeningAcademyItems(targetLangCode: String): List<ListeningAcademyItem> {
    val cleanCode = targetLangCode.lowercase().substringBefore("-")
    val langMeta = com.example.data.model.Language.findByCode(cleanCode)
    return when (cleanCode) {
      "es" -> listOf(
        ListeningAcademyItem(
          id = "listen_es_cafe_a1",
          languageCode = "es",
          cefrLevel = "A1",
          category = "Everyday Situation",
          title = "Morning Order at a Madrid Café",
          regionalAccents = listOf("Spain (Madrid)", "Mexico (CDMX)", "Colombia (Bogotá)"),
          lines = listOf(
            ListeningDialogueLine("Camarero", "¡Buenos días! ¿Qué le pongo hoy?", "Good morning! What can I get for you today?", "Qué le pongo", "What can I get you (Spain café idiom)"),
            ListeningDialogueLine("Cliente", "Hola, quería un café con leche y una tostada con tomate, por favor.", "Hello, I would like a coffee with milk and a toast with tomato, please.", "tostada", "toasted bread"),
            ListeningDialogueLine("Camarero", "¡Enseguida! ¿Para tomar aquí o para llevar?", "Right away! For here or to go?", "para llevar", "to go / takeaway"),
            ListeningDialogueLine("Cliente", "Para tomar aquí en la terraza, muchas gracias.", "For here on the terrace, thank you very much.", "terraza", "outdoor patio / terrace")
          ),
          questions = listOf(
            AcademyComprehensionQuestion(
              question = "Where does the customer want to have their breakfast?",
              options = listOf("On the terrace (para tomar aquí)", "Takeaway on the train", "In their hotel room", "At the office"),
              correctIndex = 0,
              explanation = "The customer says 'Para tomar aquí en la terraza'."
            ),
            AcademyComprehensionQuestion(
              question = "What food item did the customer order with their coffee?",
              options = listOf("Toast with tomato (una tostada con tomate)", "Chocolate churros", "An omelet", "Fruit salad"),
              correctIndex = 0,
              explanation = "'Una tostada con tomate' is a classic Spanish breakfast."
            )
          )
        ),
        ListeningAcademyItem(
          id = "listen_es_train_b1",
          languageCode = "es",
          cefrLevel = "B1",
          category = "Short Conversation",
          title = "Changing a High-Speed Train Ticket",
          regionalAccents = listOf("Mexico (CDMX)", "Spain (Madrid)", "Argentina (Buenos Aires)"),
          lines = listOf(
            ListeningDialogueLine("Viajera", "Disculpe, perdí el tren de las nueve por el tráfico. ¿Puedo cambiar mi boleto?", "Excuse me, I missed the 9:00 train due to traffic. Can I change my ticket?", "boleto", "ticket"),
            ListeningDialogueLine("Agente", "Déjeme revisar su tarifa. Sí, hay asientos disponibles en el próximo tren de las diez y media.", "Let me check your fare. Yes, there are seats available on the next train at 10:30.", "asientos disponibles", "available seats"),
            ListeningDialogueLine("Viajera", "¡Qué alivio! ¿De qué vía sale ese tren?", "What a relief! Which platform/track does that train leave from?", "Qué alivio", "What a relief"),
            ListeningDialogueLine("Agente", "Sale de la vía número cuatro. Le imprimo su nueva tarjeta de embarque ahora mismo.", "It departs from track 4. I'll print your new boarding pass right now.", "vía", "track / platform")
          ),
          questions = listOf(
            AcademyComprehensionQuestion(
              question = "Why did the traveler miss her original train?",
              options = listOf("Because of heavy traffic (por el tráfico)", "She overslept", "The train was canceled", "She forgot her passport"),
              correctIndex = 0,
              explanation = "She explains: 'perdí el tren de las nueve por el tráfico'."
            ),
            AcademyComprehensionQuestion(
              question = "When does the next available train depart?",
              options = listOf("At 10:30 from track 4", "At 9:15 from track 1", "Tomorrow morning", "At midnight"),
              correctIndex = 0,
              explanation = "The agent confirms seats on the 'tren de las diez y media' from 'vía número cuatro'."
            )
          )
        ),
        ListeningAcademyItem(
          id = "listen_es_remote_c1",
          languageCode = "es",
          cefrLevel = "C1",
          category = "Story & Cultural Discussion",
          title = "The Future of Hybrid Work & Historic Towns",
          regionalAccents = listOf("Colombia (Bogotá)", "Spain (Madrid)", "Mexico (CDMX)"),
          lines = listOf(
            ListeningDialogueLine("Narrador", "En los últimos años, el teletrabajo ha revitalizado pequeños pueblos históricos que antes sufrían despoblación.", "In recent years, remote work has revitalized small historic towns that previously suffered depopulation.", "despoblación", "depopulation"),
            ListeningDialogueLine("Elena", "A pesar de los retos de conectividad inicial, la calidad de vida y el sentido de comunidad son inigualables.", "Despite initial connectivity challenges, the quality of life and sense of community are unmatched.", "inigualables", "unmatched / peerless"),
            ListeningDialogueLine("Narrador", "El equilibrio entre la innovación digital y la preservación cultural define esta nueva etapa.", "The balance between digital innovation and cultural preservation defines this new stage.", "equilibrio", "balance")
          ),
          questions = listOf(
            AcademyComprehensionQuestion(
              question = "What positive effect has remote work had on small historic towns?",
              options = listOf("It has revitalized communities that faced depopulation", "It closed local cafés", "It eliminated all traditions", "It reduced internet access"),
              correctIndex = 0,
              explanation = "The narrator notes that 'el teletrabajo ha revitalizado pequeños pueblos históricos'."
            )
          )
        )
      )
      "fr" -> listOf(
        ListeningAcademyItem(
          id = "listen_fr_boulangerie_a1",
          languageCode = "fr",
          cefrLevel = "A1",
          category = "Everyday Situation",
          title = "At a Neighborhood Boulangerie in Lyon",
          regionalAccents = listOf("France (Paris/Lyon)", "Canada (Montréal)", "Belgium (Brussels)"),
          lines = listOf(
            ListeningDialogueLine("Boulangère", "Bonjour monsieur ! Qu'est-ce qui vous ferait plaisir aujourd'hui ?", "Hello sir! What would you like today?", "plaisir", "pleasure"),
            ListeningDialogueLine("Client", "Bonjour ! Je vais prendre une baguette tradition bien cuite et deux pains au chocolat.", "Hello! I'll take a well-baked traditional baguette and two chocolate croissants.", "bien cuite", "well-baked / crispy"),
            ListeningDialogueLine("Boulangère", "Et avec ceci ? Ce sera tout ?", "And with this? Will that be all?", "Ce sera tout", "Will that be all"),
            ListeningDialogueLine("Client", "Oui, c'est tout, merci. Je peux payer par carte ?", "Yes, that's all, thank you. Can I pay by card?", "par carte", "by card")
          ),
          questions = listOf(
            AcademyComprehensionQuestion(
              question = "How does the customer prefer his baguette?",
              options = listOf("Well-baked (bien cuite)", "Lightly baked", "Sliced for sandwiches", "Gluten-free"),
              correctIndex = 0,
              explanation = "He requests 'une baguette tradition bien cuite'."
            )
          )
        ),
        ListeningAcademyItem(
          id = "listen_fr_gare_b1",
          languageCode = "fr",
          cefrLevel = "B1",
          category = "Short Conversation",
          title = "Platform Change at Gare de Lyon",
          regionalAccents = listOf("France (Paris/Lyon)", "Switzerland (Geneva)", "Canada (Montréal)"),
          lines = listOf(
            ListeningDialogueLine("Voyageuse", "Pardon monsieur, le TGV pour Marseille est-il retardé ?", "Excuse me sir, is the high-speed train to Marseille delayed?", "retardé", "delayed"),
            ListeningDialogueLine("Agent SNCF", "Non madame, il part à l'heure mais la voie a changé. Dirigez-vous vers le quai B.", "No ma'am, it departs on time but the track changed. Head toward platform B.", "voie", "track / platform"),
            ListeningDialogueLine("Voyageuse", "Merci infiniment ! J'y vais tout de suite.", "Thank you so much! I'm heading there right away.", "tout de suite", "immediately / right away")
          ),
          questions = listOf(
            AcademyComprehensionQuestion(
              question = "What happened to the TGV train to Marseille?",
              options = listOf("It is on time, but the departure platform changed to Quai B", "It was canceled", "It was delayed by two hours", "It already departed"),
              correctIndex = 0,
              explanation = "The agent states: 'il part à l'heure mais la voie a changé. Dirigez-vous vers le quai B.'"
            )
          )
        )
      )
      else -> {
        val greetLine = extendedCoreTopics[0].targetPhrase(cleanCode)
        val hotelLine = extendedCoreTopics[8].targetPhrase(cleanCode)
        val restaurantLine = extendedCoreTopics[6].targetPhrase(cleanCode)
        val transitLine = extendedCoreTopics[7].targetPhrase(cleanCode)
        listOf(
          ListeningAcademyItem(
            id = "listen_${cleanCode}_a1",
            languageCode = targetLangCode,
            cefrLevel = "A1",
            category = "Everyday Situation",
            title = "Checking into a Boutique City Hotel (${langMeta.name})",
            regionalAccents = listOf("Standard ${langMeta.name} (${langMeta.localeCode})", "Northern Regional", "Coastal Regional"),
            lines = listOf(
              ListeningDialogueLine("Receptionist", greetLine, extendedCoreTopics[0].meaning, langMeta.nativeName, "Warm greeting"),
              ListeningDialogueLine("Traveler", hotelLine, extendedCoreTopics[8].meaning, "booking", "hotel reservation"),
              ListeningDialogueLine("Receptionist", restaurantLine, extendedCoreTopics[6].meaning, "specialty", "house dish")
            ),
            questions = listOf(
              AcademyComprehensionQuestion(
                question = "What does the traveler request at check-in?",
                options = listOf(extendedCoreTopics[8].meaning, "One night only without windows", "A bicycle rental", "Cancelation of all flights"),
                correctIndex = 0,
                explanation = "The traveler states: '${extendedCoreTopics[8].meaning}'."
              )
            )
          ),
          ListeningAcademyItem(
            id = "listen_${cleanCode}_b1",
            languageCode = targetLangCode,
            cefrLevel = "B1",
            category = "Short Conversation",
            title = "Navigating Downtown Transit & Dining (${langMeta.name})",
            regionalAccents = listOf("Standard ${langMeta.name}", "Metropolitan Accent", "International Standard"),
            lines = listOf(
              ListeningDialogueLine("Traveler", transitLine, extendedCoreTopics[7].meaning, "express train", "direct route"),
              ListeningDialogueLine("Local Guide", extendedCoreTopics[9].targetPhrase(cleanCode), extendedCoreTopics[9].meaning, "museum", "landmark"),
              ListeningDialogueLine("Traveler", extendedCoreTopics[18].targetPhrase(cleanCode), extendedCoreTopics[18].meaning, "understanding", "appreciation")
            ),
            questions = listOf(
              AcademyComprehensionQuestion(
                question = "What directions does the local guide give?",
                options = listOf(extendedCoreTopics[9].meaning, "Take a helicopter across the river", "Wait until midnight", "Turn back immediately"),
                correctIndex = 0,
                explanation = "The guide explains: '${extendedCoreTopics[9].meaning}'."
              )
            )
          )
        )
      }
    }
  }

  fun getReadingAcademyPassages(targetLangCode: String): List<ReadingAcademyPassage> {
    val cleanCode = targetLangCode.lowercase().substringBefore("-")
    val langMeta = com.example.data.model.Language.findByCode(cleanCode)
    return when (cleanCode) {
      "es" -> listOf(
        ReadingAcademyPassage(
          id = "read_es_mercado_a1",
          languageCode = "es",
          cefrLevel = "A1",
          category = "Graded Story",
          title = "Un Sábado en el Mercado Central",
          paragraphs = listOf(
            "Cada sábado por la mañana, Lucía camina hacia el mercado central de su barrio con una cesta de mimbre.",
            "Primero visita el puesto de frutas frescas, donde compra naranjas dulces de Valencia y aguacates maduros.",
            "Después charla con el panadero y elige una hogaza de pan artesanal recién horneado para compartir con su familia."
          ),
          paragraphTranslations = listOf(
            "Every Saturday morning, Lucía walks toward the central market of her neighborhood with a wicker basket.",
            "First she visits the fresh fruit stall, where she buys sweet oranges from Valencia and ripe avocados.",
            "Afterwards she chats with the baker and chooses a loaf of freshly baked artisanal bread to share with her family."
          ),
          glossary = mapOf(
            "mercado" to ReadingWordGloss("mercado", "market", "noun (m)", "A public place where food and artisanal goods are sold.", "mer-KAH-doh"),
            "barrio" to ReadingWordGloss("barrio", "neighborhood / district", "noun (m)", "A local community area within a town or city.", "BAH-rryoh"),
            "cesta" to ReadingWordGloss("cesta", "basket", "noun (f)", "A woven container used to carry groceries.", "SES-tah"),
            "puesto" to ReadingWordGloss("puesto", "stall / stand", "noun (m)", "A vendor's booth inside a market.", "PWES-toh"),
            "maduros" to ReadingWordGloss("maduros", "ripe", "adjective", "Ready to eat; fully developed.", "mah-DOO-rohs"),
            "panadero" to ReadingWordGloss("panadero", "baker", "noun (m)", "A person who makes and sells bread.", "pah-nah-DEH-roh"),
            "horneado" to ReadingWordGloss("horneado", "baked", "participle", "Cooked in an oven.", "ohr-neh-AH-doh")
          ),
          questions = listOf(
            AcademyComprehensionQuestion(
              question = "When does Lucía visit the central market?",
              options = listOf("Every Saturday morning", "Every Monday night", "On Sunday afternoon", "Only on holidays"),
              correctIndex = 0,
              explanation = "The first sentence states 'Cada sábado por la mañana'."
            ),
            AcademyComprehensionQuestion(
              question = "What does she buy from the baker?",
              options = listOf("A loaf of freshly baked artisanal bread", "A birthday cake", "Sweet oranges", "Coffee beans"),
              correctIndex = 0,
              explanation = "She chooses 'una hogaza de pan artesanal recién horneado'."
            )
          )
        ),
        ReadingAcademyPassage(
          id = "read_es_cafe_b1",
          languageCode = "es",
          cefrLevel = "B1",
          category = "Everyday Dialogue",
          title = "Encuentro Inesperado en la Librería",
          paragraphs = listOf(
            "Mientras buscaba una novela histórica en una librería antigua de Madrid, Mateo coincidió con su antigua profesora de literatura.",
            "Ella le recomendó un autor contemporáneo cuya prosa combina el misterio urbano con memorias familiares inolvidables.",
            "Decidieron tomar un café juntos en la plaza cercana para conversar sobre sus libros favoritos del año."
          ),
          paragraphTranslations = listOf(
            "While looking for a historical novel in an old Madrid bookshop, Mateo ran into his former literature professor.",
            "She recommended a contemporary author whose prose blends urban mystery with unforgettable family memories.",
            "They decided to have coffee together in the nearby square to chat about their favorite books of the year."
          ),
          glossary = mapOf(
            "librería" to ReadingWordGloss("librería", "bookshop", "noun (f)", "A store where books are sold (false friend: library is 'biblioteca').", "lee-breh-REE-ah"),
            "coincidió" to ReadingWordGloss("coincidió", "happened to meet / ran into", "verb (past)", "Met by chance at the same place and time.", "koh-een-see-DYOH"),
            "prosa" to ReadingWordGloss("prosa", "prose / writing style", "noun (f)", "Written or spoken language in its ordinary form.", "PROH-sah"),
            "cercana" to ReadingWordGloss("cercana", "nearby", "adjective", "Located a short distance away.", "ser-KAH-nah")
          ),
          questions = listOf(
            AcademyComprehensionQuestion(
              question = "Who did Mateo meet inside the Madrid bookshop?",
              options = listOf("His former literature professor", "A famous chef", "His cousin from Valencia", "The mayor"),
              correctIndex = 0,
              explanation = "The text notes: 'Mateo coincidió con su antigua profesora de literatura'."
            )
          )
        ),
        ReadingAcademyPassage(
          id = "read_es_arquitectura_b2",
          languageCode = "es",
          cefrLevel = "B2",
          category = "Cultural Article",
          title = "Patios Andaluces: Arquitectura y Clima",
          paragraphs = listOf(
            "Los tradicionales patios de Córdoba y Sevilla no son únicamente un espectáculo visual de flores y azulejos, sino también una obra maestra de arquitectura bioclimática.",
            "Al organizar las habitaciones alrededor de un patio interior con una fuente central y abundante vegetación, el aire fresco se conserva durante las horas más calurosas del verano."
          ),
          paragraphTranslations = listOf(
            "The traditional courtyards of Córdoba and Seville are not only a visual spectacle of flowers and tiles, but also a masterpiece of bioclimatic architecture.",
            "By organizing the rooms around an interior courtyard with a central fountain and abundant vegetation, cool air is preserved during the hottest hours of summer."
          ),
          glossary = mapOf(
            "azulejos" to ReadingWordGloss("azulejos", "ceramic tiles", "noun (m pl)", "Glazed decorative ceramic tiles traditional in Iberia.", "ah-soo-LEH-hohs"),
            "obra" to ReadingWordGloss("obra", "work / masterpiece", "noun (f)", "An artistic or architectural creation.", "OH-brah"),
            "fuente" to ReadingWordGloss("fuente", "fountain", "noun (f)", "A water feature in the center of a courtyard.", "FWEN-teh"),
            "calurosas" to ReadingWordGloss("calurosas", "hot / warm", "adjective", "Having high temperature.", "kah-loo-ROH-sahs")
          ),
          questions = listOf(
            AcademyComprehensionQuestion(
              question = "Besides aesthetics, what functional purpose do Andalusian courtyards serve?",
              options = listOf("Natural cooling and bioclimatic temperature regulation", "Storing winter snow", "Parking carriages", "Blocking all sunlight permanently"),
              correctIndex = 0,
              explanation = "They act as 'una obra maestra de arquitectura bioclimática' that preserves cool air."
            )
          )
        ),
        ReadingAcademyPassage(
          id = "read_es_ciencia_c1",
          languageCode = "es",
          cefrLevel = "C1",
          category = "Literature & Essay",
          title = "El Bilingüismo y la Flexibilidad Cognitiva",
          paragraphs = listOf(
            "Diversas investigaciones neurocientíficas sugieren que alternar entre dos o más sistemas lingüísticos fortalece las funciones ejecutivas del cerebro a lo largo de toda la vida.",
            "Más allá de la ventaja comunicativa inmediata, comprender matices idiomáticos ajenos fomenta una empatía intercultural profunda y una mayor tolerancia ante la ambigüedad."
          ),
          paragraphTranslations = listOf(
            "Various neuroscientific studies suggest that switching between two or more linguistic systems strengthens the brain's executive functions throughout life.",
            "Beyond the immediate communicative advantage, understanding unfamiliar idiomatic nuances fosters deep intercultural empathy and greater tolerance for ambiguity."
          ),
          glossary = mapOf(
            "alternar" to ReadingWordGloss("alternar", "to alternate / switch", "verb", "To shift back and forth between two states or languages.", "ahl-ter-NAHR"),
            "matices" to ReadingWordGloss("matices", "nuances / shades of meaning", "noun (m pl)", "Subtle distinctions in expression or tone.", "mah-TEE-ses"),
            "ambigüedad" to ReadingWordGloss("ambigüedad", "ambiguity", "noun (f)", "Openness to more than one interpretation.", "ahm-bee-gweh-DAHD")
          ),
          questions = listOf(
            AcademyComprehensionQuestion(
              question = "According to the essay, what cognitive benefit comes from switching between multiple languages?",
              options = listOf("It strengthens the brain's executive functions and intercultural empathy", "It replaces long-term memory", "It eliminates all grammar rules", "It slows down reading speed"),
              correctIndex = 0,
              explanation = "The passage states that it 'fortalece las funciones ejecutivas del cerebro' and 'fomenta una empatía intercultural profunda'."
            )
          )
        )
      )
      else -> {
        val p1Target = extendedCoreTopics[4].targetPhrase(cleanCode)
        val p2Target = extendedCoreTopics[15].targetPhrase(cleanCode)
        val p3Target = extendedCoreTopics[13].targetPhrase(cleanCode)
        val p4Target = extendedCoreTopics[19].targetPhrase(cleanCode)
        listOf(
          ReadingAcademyPassage(
            id = "read_${cleanCode}_story_a2",
            languageCode = targetLangCode,
            cefrLevel = "A2",
            category = "Graded Story",
            title = "Family Gatherings & Coastal Journeys (${langMeta.name})",
            paragraphs = listOf(p1Target, p2Target),
            paragraphTranslations = listOf(extendedCoreTopics[4].meaning, extendedCoreTopics[15].meaning),
            glossary = mapOf(
              langMeta.nativeName to ReadingWordGloss(langMeta.nativeName, langMeta.name, "noun", "Native name of ${langMeta.name} (${langMeta.writingSystem}).", langMeta.localeCode),
              "harbor" to ReadingWordGloss("harbor", "sheltered port", "noun", "A place on the coast where vessels find shelter.", "HAR-bor"),
              "tradition" to ReadingWordGloss("tradition", "shared family custom", "noun", "A custom passed down through generations.", "truh-DISH-un")
            ),
            questions = listOf(
              AcademyComprehensionQuestion(
                question = "What does the family do every Sunday according to the first paragraph?",
                options = listOf(extendedCoreTopics[4].meaning, "They travel to the North Pole", "They close the train station", "They work at midnight"),
                correctIndex = 0,
                explanation = "The passage states: '${extendedCoreTopics[4].meaning}'."
              )
            )
          ),
          ReadingAcademyPassage(
            id = "read_${cleanCode}_culture_b2",
            languageCode = targetLangCode,
            cefrLevel = "B2",
            category = "Cultural Article",
            title = "Active Reading & Conversational Mastery (${langMeta.name})",
            paragraphs = listOf(p3Target, p4Target),
            paragraphTranslations = listOf(extendedCoreTopics[13].meaning, extendedCoreTopics[19].meaning),
            glossary = mapOf(
              "fluency" to ReadingWordGloss("fluency", "smooth, effortless expression", "noun", "The ability to express oneself easily and articulately.", "FLOO-uhn-see"),
              "memory" to ReadingWordGloss("memory", "long-term retention", "noun", "The faculty by which the mind stores and remembers information.", "MEM-uh-ree")
            ),
            questions = listOf(
              AcademyComprehensionQuestion(
                question = "What strengthens long-term memory and confidence according to this article?",
                options = listOf("Consistent practice, active reading, and real conversations", "Never speaking aloud", "Skipping all vocabulary reviews", "Avoiding native speakers"),
                correctIndex = 0,
                explanation = "Both paragraphs emphasize consistent practice, active reading, and real conversations."
              )
            )
          )
        )
      }
    }
  }

  fun getStarterFlashcards(targetLangCode: String): List<FlashcardItem> {
    val cleanCode = targetLangCode.lowercase().substringBefore("-")
    val langMeta = com.example.data.model.Language.findByCode(cleanCode)
    return when (cleanCode) {
      "es" -> listOf(
        FlashcardItem("es_1", "es", "Hola", "Hello / Hi", "oh-lah", "Hola, ¿cómo estás?", 1),
        FlashcardItem("es_2", "es", "Por favor", "Please", "por fah-vor", "¿Me traes agua, por favor?", 1),
        FlashcardItem("es_3", "es", "Gracias", "Thank you", "grah-see-ahs", "Muchas gracias por tu ayuda.", 2),
        FlashcardItem("es_4", "es", "¿Cuánto cuesta?", "How much does it cost?", "kwan-toh kwes-tah", "¿Cuánto cuesta este café?", 2),
        FlashcardItem("es_5", "es", "Mucho gusto", "Nice to meet you", "moo-choh goos-toh", "Mucho gusto en conocerte.", 1),
        FlashcardItem("es_6", "es", "Buenos días", "Good morning", "bway-nohs dee-ahs", "Buenos días a todos.", 3),
        FlashcardItem("es_7", "es", "¿Dónde está el baño?", "Where is the bathroom?", "dohn-deh es-tah el bah-nyoh", "Disculpe, ¿dónde está el baño?", 1),
        FlashcardItem("es_8", "es", "La cuenta, por favor", "The bill, please", "lah kwen-tah por fah-vor", "La cuenta, por favor, señorita.", 2)
      )
      "fr" -> listOf(
        FlashcardItem("fr_1", "fr", "Bonjour", "Hello / Good day", "bohn-zhoor", "Bonjour, comment allez-vous?", 1),
        FlashcardItem("fr_2", "fr", "Merci beaucoup", "Thank you very much", "mair-see boh-koo", "Merci beaucoup pour le cadeau.", 1),
        FlashcardItem("fr_3", "fr", "S'il vous plaît", "Please (formal)", "seel voo pleh", "Un café, s'il vous plaît.", 2),
        FlashcardItem("fr_4", "fr", "Enchanté", "Nice to meet you", "ahn-shahn-tay", "Enchanté de vous rencontrer.", 1),
        FlashcardItem("fr_5", "fr", "Où sont les toilettes?", "Where are the restrooms?", "oo sohn lay twah-let", "Pardon, où sont les toilettes?", 2)
      )
      "ar" -> listOf(
        FlashcardItem("ar_1", "ar", "مرحباً (Marhaban)", "Hello", "mar-ha-ban", "مرحباً، كيف حالك؟", 1),
        FlashcardItem("ar_2", "ar", "شكراً (Shukran)", "Thank you", "shook-ran", "شكراً جزيلاً لك.", 1),
        FlashcardItem("ar_3", "ar", "من فضلك (Min fadlik)", "Please", "min fad-lik", "كوب ماء من فضلك.", 2),
        FlashcardItem("ar_4", "ar", "أهلاً وسهلاً (Ahlan wa Sahlan)", "Welcome", "ah-lan wa sah-lan", "أهلاً وسهلاً بكم في بيتنا.", 1),
        FlashcardItem("ar_5", "ar", "مع السلامة (Ma'a as-salama)", "Goodbye", "ma-a as-sa-la-ma", "مع السلامة وأتمنى لك يوماً سعيداً.", 2)
      )
      "ur" -> listOf(
        FlashcardItem("ur_1", "ur", "السلام علیکم (Assalam-o-Alaikum)", "Hello / Peace be upon you", "as-sa-laam-o-alai-kum", "السلام علیکم، آپ کیسے ہیں؟", 1),
        FlashcardItem("ur_2", "ur", "شکریہ (Shukriya)", "Thank you", "shook-ree-yah", "آپ کی مدد کا بہت شکریہ۔", 1),
        FlashcardItem("ur_3", "ur", "براہ کرم (Bara-e-Karam)", "Please", "ba-rah-e-ka-ram", "براہ کرم مجھے پانی دیں۔", 2),
        FlashcardItem("ur_4", "ur", "خوش آمدید (Khush Aamdeed)", "Welcome", "khush aam-deed", "پاکستان میں خوش آمدید۔", 1),
        FlashcardItem("ur_5", "ur", "خدا حافظ (Khuda Hafiz)", "Goodbye", "khu-da haa-fiz", "خدا حافظ، کل ملیں گے۔", 2)
      )
      "hi" -> listOf(
        FlashcardItem("hi_1", "hi", "नमस्ते (Namaste)", "Hello / Greetings", "na-mas-te", "नमस्ते, आप कैसे हैं?", 1),
        FlashcardItem("hi_2", "hi", "धन्यवाद (Dhanyavaad)", "Thank you", "dhan-ya-vaad", "आपकी सहायता के लिए धन्यवाद।", 1),
        FlashcardItem("hi_3", "hi", "कृपया (Kripaya)", "Please", "kri-pa-ya", "कृपया यहाँ बैठिए।", 2),
        FlashcardItem("hi_4", "hi", "फिर मिलेंगे (Phir Milenge)", "See you again", "phir mi-len-ge", "कल फिर मिलेंगे!", 1)
      )
      "de" -> listOf(
        FlashcardItem("de_1", "de", "Guten Tag", "Good day / Hello", "goo-ten tahk", "Guten Tag, wie geht es Ihnen?", 1),
        FlashcardItem("de_2", "de", "Bitte", "Please / You're welcome", "bit-teh", "Ein Wasser, bitte.", 1),
        FlashcardItem("de_3", "de", "Danke schön", "Thank you very much", "dahn-keh shoen", "Danke schön für Ihre Hilfe.", 2),
        FlashcardItem("de_4", "de", "Auf Wiedersehen", "Goodbye", "owf vee-der-zay-en", "Auf Wiedersehen und bis bald!", 1)
      )
      "ja" -> listOf(
        FlashcardItem("ja_1", "ja", "こんにちは (Konnichiwa)", "Hello / Good afternoon", "kohn-nee-chee-wah", "皆さん、こんにちは。", 1),
        FlashcardItem("ja_2", "ja", "ありがとう (Arigatou)", "Thank you", "ah-ree-gah-toh", "本当にありがとう。", 1),
        FlashcardItem("ja_3", "ja", "すみません (Sumimasen)", "Excuse me / Sorry", "soo-mee-mah-sen", "すみません、お会計をお願いします。", 2),
        FlashcardItem("ja_4", "ja", "はじめまして (Hajimemashite)", "Nice to meet you", "hah-jee-meh-mah-shee-teh", "はじめまして、田中です。", 1)
      )
      "zh" -> listOf(
        FlashcardItem("zh_1", "zh", "你好 (Nǐ hǎo)", "Hello", "nee-how", "你好，很高兴认识你。", 1),
        FlashcardItem("zh_2", "zh", "谢谢 (Xièxie)", "Thank you", "shyeh-shyeh", "非常谢谢你的帮助。", 1),
        FlashcardItem("zh_3", "zh", "请 (Qǐng)", "Please", "cheeng", "请给我一杯茶。", 2),
        FlashcardItem("zh_4", "zh", "再见 (Zàijiàn)", "Goodbye", "zai-jyen", "再见，明天见！", 1)
      )
      "ko" -> listOf(
        FlashcardItem("ko_1", "ko", "안녕하세요 (Annyeonghaseyo)", "Hello", "ahn-nyeong-ha-se-yo", "안녕하세요, 반갑습니다.", 1),
        FlashcardItem("ko_2", "ko", "감사합니다 (Gamsahamnida)", "Thank you", "gam-sa-ham-ni-da", "도와주셔서 감사합니다.", 1),
        FlashcardItem("ko_3", "ko", "주세요 (Juseyo)", "Please give me", "ju-se-yo", "물 한 잔 주세요.", 2),
        FlashcardItem("ko_4", "ko", "안녕히 계세요 (Annyeonghi gyeseyo)", "Goodbye", "ahn-nyeong-hee gye-se-yo", "내일 또 만나요!", 1)
      )
      "pt" -> listOf(
        FlashcardItem("pt_1", "pt", "Olá / Bom dia", "Hello / Good morning", "oh-lah / bohm dee-ah", "Olá! Tudo bem com você?", 1),
        FlashcardItem("pt_2", "pt", "Obrigado / Obrigada", "Thank you", "oh-bree-gah-doo", "Muito obrigado pela ajuda.", 1),
        FlashcardItem("pt_3", "pt", "Por favor", "Please", "por fah-vor", "Um café, por favor.", 2)
      )
      "it" -> listOf(
        FlashcardItem("it_1", "it", "Ciao / Buongiorno", "Hello / Good morning", "chow / bwohn-jor-noh", "Buongiorno, come sta?", 1),
        FlashcardItem("it_2", "it", "Grazie mille", "Thank you very much", "graht-see-eh meel-leh", "Grazie mille per tutto.", 1),
        FlashcardItem("it_3", "it", "Per favore", "Please", "pehr fah-voh-reh", "Un espresso, per favore.", 2)
      )
      "tr" -> listOf(
        FlashcardItem("tr_1", "tr", "Merhaba", "Hello", "mer-ha-ba", "Merhaba, بugün nasılsın?", 1),
        FlashcardItem("tr_2", "tr", "Teşekkür ederim", "Thank you", "te-shek-kuer e-de-rim", "Yardımınız için teşekkür ederim.", 1),
        FlashcardItem("tr_3", "tr", "Lütfen", "Please", "luet-fen", "Bir çay, lütfen.", 2)
      )
      "ru" -> listOf(
        FlashcardItem("ru_1", "ru", "Здравствуйте (Zdravstvuyte)", "Hello (Formal)", "zdrah-stvooy-tye", "Здравствуйте! Как ваши дела?", 1),
        FlashcardItem("ru_2", "ru", "Спасибо (Spasibo)", "Thank you", "spah-see-bah", "Большое спасибо за помощь.", 1),
        FlashcardItem("ru_3", "ru", "Пожалуйста (Pozhaluysta)", "Please / You're welcome", "pah-zhah-loo-stah", "Чашку кофе, пожалуйста.", 2)
      )
      else -> listOf(
        FlashcardItem("${cleanCode}_1", targetLangCode, langMeta.sampleGreeting, "Hello! How are you today?", langMeta.localeCode, extendedCoreTopics[0].targetPhrase(cleanCode), 1),
        FlashcardItem("${cleanCode}_2", targetLangCode, extendedCoreTopics[1].targetPhrase(cleanCode), extendedCoreTopics[1].meaning, langMeta.writingSystem, extendedCoreTopics[1].targetPhrase(cleanCode), 1),
        FlashcardItem("${cleanCode}_3", targetLangCode, extendedCoreTopics[5].targetPhrase(cleanCode), extendedCoreTopics[5].meaning, langMeta.writingSystem, extendedCoreTopics[5].targetPhrase(cleanCode), 2),
        FlashcardItem("${cleanCode}_4", targetLangCode, extendedCoreTopics[6].targetPhrase(cleanCode), extendedCoreTopics[6].meaning, langMeta.writingSystem, extendedCoreTopics[6].targetPhrase(cleanCode), 1)
      )
    }
  }

  private val spanishLessons by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    CurriculumLesson(
      id = "es_lesson_1",
      title = "Essential Greetings & First Words",
      description = "Master everyday hellos, polite manners, and natural introductions in Spanish.",
      category = "Vocabulary",
      proficiencyLevel = "Beginner",
      targetLanguageCode = "es",
      estimatedMinutes = 4,
      xpReward = 30,
      cefrLevel = "A1",
      unitTitle = "Unit 1: Breakthrough Foundations (A1)",
      learningOutcomes = listOf(
        "Greet locals naturally at any time of day",
        "Introduce yourself and express gratitude politely"
      ),
      exercises = listOf(
        Exercise(
          id = "es_ex_1",
          type = ExerciseType.VOCABULARY,
          prompt = "How do you say 'Hello' in Spanish?",
          explanationNative = "'Hola' is the universal greeting used at any time of day in all Spanish-speaking countries.",
          targetAudioText = "Hola",
          phoneticGuide = "oh-lah",
          options = listOf("Hola", "Adiós", "Gracias", "Por favor"),
          correctOptionIndex = 0
        ),
        Exercise(
          id = "es_ex_2",
          type = ExerciseType.LISTENING,
          prompt = "Listen to the speaker and choose what they said:",
          explanationNative = "'Mucho gusto' means 'Pleased to meet you' or 'Nice to meet you'.",
          targetAudioText = "Mucho gusto",
          phoneticGuide = "moo-choh goos-toh",
          options = listOf("Buenos días", "Mucho gusto", "Hasta luego", "Buenas noches"),
          correctOptionIndex = 1
        ),
        Exercise(
          id = "es_ex_3",
          type = ExerciseType.GRAMMAR,
          prompt = "Which phrase politely asks 'How are you?' to a new friend?",
          explanationNative = "'¿Cómo estás?' is the informal version used with peers and friends.",
          targetAudioText = "¿Cómo estás?",
          phoneticGuide = "koh-moh es-tahs",
          options = listOf("¿Cómo te llamas?", "¿Cómo estás?", "¿De dónde eres?", "¿Qué hora es?"),
          correctOptionIndex = 1
        ),
        Exercise(
          id = "es_ex_4",
          type = ExerciseType.WRITING,
          prompt = "Type the Spanish word for 'Thank you':",
          explanationNative = "'Gracias' is spelled G-R-A-C-I-A-S.",
          targetAudioText = "Gracias",
          phoneticGuide = "grah-see-ahs",
          acceptableAnswers = listOf("gracias", "muchas gracias")
        )
      )
    ),
    CurriculumLesson(
      id = "es_lesson_2",
      title = "At the Café & Tapas Bar",
      description = "Order coffee, delicious pastries, ask for the bill, and converse with servers.",
      category = "Vocabulary",
      proficiencyLevel = "Beginner",
      targetLanguageCode = "es",
      estimatedMinutes = 5,
      xpReward = 40,
      cefrLevel = "A1",
      unitTitle = "Unit 1: Breakthrough Foundations (A1)",
      learningOutcomes = listOf(
        "Order food and beverages politely using 'por favor'",
        "Understand server questions and request the check"
      ),
      exercises = listOf(
        Exercise(
          id = "es_ex_5",
          type = ExerciseType.VOCABULARY,
          prompt = "What does 'Un café con leche, por favor' mean?",
          explanationNative = "It translates directly to 'A coffee with milk, please'.",
          targetAudioText = "Un café con leche, por favor",
          phoneticGuide = "oon kah-feh kohn leh-cheh, por fah-vor",
          options = listOf("A black tea, please", "A coffee with milk, please", "A cold water, please", "A fruit juice, please"),
          correctOptionIndex = 1
        ),
        Exercise(
          id = "es_ex_6",
          type = ExerciseType.READING,
          prompt = "Read the waiter's question: '¿Desean pedir la comida o solo bebidas?' What is being asked?",
          explanationNative = "'Pedir' means to order; 'comida' is food and 'bebidas' is drinks.",
          targetAudioText = "¿Desean pedir la comida o solo bebidas?",
          phoneticGuide = "deh-seh-ahn peh-deer lah koh-mee-dah",
          options = listOf("Are you ready for the bill?", "Do you wish to order food or just drinks?", "Where would you like to sit?", "Is everything delicious?"),
          correctOptionIndex = 1
        ),
        Exercise(
          id = "es_ex_7",
          type = ExerciseType.WRITING,
          prompt = "Translate 'The bill, please':",
          explanationNative = "'La cuenta, por favor' is the standard polite request at a restaurant.",
          targetAudioText = "La cuenta, por favor",
          phoneticGuide = "lah kwen-tah por fah-vor",
          acceptableAnswers = listOf("la cuenta por favor", "la cuenta, por favor", "la cuenta")
        )
      )
    ),
    CurriculumLesson(
      id = "es_lesson_3",
      title = "Mastering Ser vs. Estar",
      description = "Understand the golden grammar difference between permanent identity and temporary state.",
      category = "Grammar",
      proficiencyLevel = "Intermediate",
      targetLanguageCode = "es",
      estimatedMinutes = 6,
      xpReward = 50,
      cefrLevel = "B1",
      unitTitle = "Unit 3: Independent Grammar & Nuance (B1)",
      learningOutcomes = listOf(
        "Distinguish temporary states ('estar') from identity and professions ('ser')",
        "Construct accurate descriptive sentences in real time"
      ),
      exercises = listOf(
        Exercise(
          id = "es_ex_8",
          type = ExerciseType.GRAMMAR,
          prompt = "Complete: 'Yo _____ cansado después de trabajar.' (I am tired after working)",
          explanationNative = "'Estar' is used for temporary physical and emotional states, hence 'estoy'.",
          targetAudioText = "Yo estoy cansado después de trabajar.",
          phoneticGuide = "yoh es-toy kahn-sah-doh",
          options = listOf("soy", "estoy", "tengo", "hago"),
          correctOptionIndex = 1
        ),
        Exercise(
          id = "es_ex_9",
          type = ExerciseType.GRAMMAR,
          prompt = "Complete: 'Ella _____ médica en el hospital.' (She is a doctor)",
          explanationNative = "Professions and inherent characteristics take 'ser' ('es').",
          targetAudioText = "Ella es médica en el hospital.",
          phoneticGuide = "eh-yah es meh-dee-kah",
          options = listOf("está", "es", "tiene", "queda"),
          correctOptionIndex = 1
        )
      )
    ),
    CurriculumLesson(
      id = "es_lesson_4",
      title = "Job Interview & Professional Spanish",
      description = "Present your achievements, discuss work experience, and answer behavioral questions.",
      category = "Reading",
      proficiencyLevel = "Advanced",
      targetLanguageCode = "es",
      estimatedMinutes = 7,
      xpReward = 60,
      cefrLevel = "B2",
      unitTitle = "Unit 4: Professional & Workplace Fluency (B2)",
      learningOutcomes = listOf(
        "Describe multi-year professional experience and leadership",
        "Respond to behavioral interview questions using professional register"
      ),
      exercises = listOf(
        Exercise(
          id = "es_ex_10",
          type = ExerciseType.READING,
          prompt = "Read: 'Tengo cinco años de experiencia liderando proyectos de software.' What is the speaker's background?",
          explanationNative = "They have 5 years of experience leading software development projects.",
          targetAudioText = "Tengo cinco años de experiencia liderando proyectos de software.",
          options = listOf("5 years in sales", "5 years leading software projects", "5 years studying computer science", "5 years as a teacher"),
          correctOptionIndex = 1
        ),
        Exercise(
          id = "es_ex_11",
          type = ExerciseType.QUIZ,
          prompt = "Which response best demonstrates problem-solving in a job interview?",
          explanationNative = "'Resolví el conflicto escuchando las opiniones de todos' demonstrates collaboration and resolution.",
          targetAudioText = "Resolví el conflicto escuchando las opiniones de todos.",
          options = listOf("No me gustan los problemas", "Resolví el conflicto escuchando las opiniones de todos", "Esperé a que mi jefe lo arreglara", "Cambié de proyecto"),
          correctOptionIndex = 1
        )
      )
    )
  )
  }

  private val frenchLessons by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    CurriculumLesson(
      id = "fr_lesson_1",
      title = "Bonjour! First Steps in French",
      description = "Learn warm French greetings, how to introduce yourself, and everyday courtesies.",
      category = "Vocabulary",
      proficiencyLevel = "Beginner",
      targetLanguageCode = "fr",
      estimatedMinutes = 4,
      xpReward = 30,
      cefrLevel = "A1",
      unitTitle = "Unit 1: Breakthrough Foundations (A1)",
      exercises = listOf(
        Exercise(
          id = "fr_ex_1",
          type = ExerciseType.VOCABULARY,
          prompt = "How do you say 'Thank you very much' in French?",
          explanationNative = "'Merci beaucoup' is the polite expression for 'Thank you very much'.",
          targetAudioText = "Merci beaucoup",
          phoneticGuide = "mair-see boh-koo",
          options = listOf("Merci beaucoup", "De rien", "Au revoir", "S'il vous plaît"),
          correctOptionIndex = 0
        ),
        Exercise(
          id = "fr_ex_2",
          type = ExerciseType.LISTENING,
          prompt = "Listen to the phrase and select its meaning:",
          explanationNative = "'Comment vous appelez-vous?' asks 'What is your name?' formally.",
          targetAudioText = "Comment vous appelez-vous?",
          phoneticGuide = "koh-mahn voo zah-play voo",
          options = listOf("Where do you live?", "What is your name?", "How old are you?", "Are you French?"),
          correctOptionIndex = 1
        )
      )
    )
  )
  }

  private val germanLessons by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    CurriculumLesson(
      id = "de_lesson_1",
      title = "Guten Tag! German Essentials",
      description = "Learn greetings, ordering bread and coffee, and basic sentence construction.",
      category = "Vocabulary",
      proficiencyLevel = "Beginner",
      targetLanguageCode = "de",
      estimatedMinutes = 4,
      xpReward = 30,
      cefrLevel = "A1",
      unitTitle = "Unit 1: Breakthrough Foundations (A1)",
      exercises = listOf(
        Exercise(
          id = "de_ex_1",
          type = ExerciseType.VOCABULARY,
          prompt = "What does 'Guten Morgen' mean?",
          explanationNative = "'Guten Morgen' is used to wish someone Good Morning until around 11am.",
          targetAudioText = "Guten Morgen",
          phoneticGuide = "goo-ten mor-gen",
          options = listOf("Good evening", "Good morning", "Goodbye", "See you soon"),
          correctOptionIndex = 1
        )
      )
    )
  )
  }

  private val arabicLessons by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    CurriculumLesson(
      id = "ar_lesson_1",
      title = "مرحباً! Welcome to Arabic",
      description = "Learn foundational greetings, pleasantries, and polite expressions.",
      category = "Vocabulary",
      proficiencyLevel = "Beginner",
      targetLanguageCode = "ar",
      estimatedMinutes = 5,
      xpReward = 30,
      cefrLevel = "A1",
      unitTitle = "Unit 1: Breakthrough Foundations (A1)",
      exercises = listOf(
        Exercise(
          id = "ar_ex_1",
          type = ExerciseType.VOCABULARY,
          prompt = "What is the standard Arabic greeting for 'Hello'?",
          explanationNative = "'مرحباً' (Marhaban) is widely understood across all Arabic-speaking countries.",
          targetAudioText = "مرحباً",
          phoneticGuide = "mar-ha-ban",
          options = listOf("مرحباً", "شكراً", "إلى اللقاء", "نعم"),
          correctOptionIndex = 0
        )
      )
    )
  )
  }

  private val japaneseLessons by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    CurriculumLesson(
      id = "ja_lesson_1",
      title = "Konnichiwa! Everyday Japanese",
      description = "Essential greetings, saying thank you, and polite everyday phrases.",
      category = "Vocabulary",
      proficiencyLevel = "Beginner",
      targetLanguageCode = "ja",
      estimatedMinutes = 4,
      xpReward = 30,
      cefrLevel = "A1",
      unitTitle = "Unit 1: Breakthrough Foundations (A1)",
      exercises = listOf(
        Exercise(
          id = "ja_ex_1",
          type = ExerciseType.VOCABULARY,
          prompt = "How do you say 'Thank you' in Japanese?",
          explanationNative = "'ありがとう' (Arigatou) or politely 'ありがとうございます' (Arigatou gozaimasu).",
          targetAudioText = "ありがとうございます",
          phoneticGuide = "ah-ree-gah-toh goh-zah-ee-mahs",
          options = listOf("さようなら", "ありがとうございます", "おはよう", "すみません"),
          correctOptionIndex = 1
        )
      )
    )
  )
  }

  private val chineseLessons by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    CurriculumLesson(
      id = "zh_lesson_1",
      title = "Ni Hao! Mandarin Foundations",
      description = "Tones, introductions, and essential words for meeting people.",
      category = "Vocabulary",
      proficiencyLevel = "Beginner",
      targetLanguageCode = "zh",
      estimatedMinutes = 4,
      xpReward = 30,
      cefrLevel = "A1",
      unitTitle = "Unit 1: Breakthrough Foundations (A1)",
      exercises = listOf(
        Exercise(
          id = "zh_ex_1",
          type = ExerciseType.VOCABULARY,
          prompt = "What does '你好' (Nǐ hǎo) mean?",
          explanationNative = "'你好' is the standard greeting meaning 'Hello' or 'You well'.",
          targetAudioText = "你好",
          phoneticGuide = "nee-how",
          options = listOf("Goodbye", "Hello", "Thank you", "Sorry"),
          correctOptionIndex = 1
        )
      )
    )
  )
  }

  private val italianLessons by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    CurriculumLesson(
      id = "it_lesson_1",
      title = "Ciao! Beautiful Italian",
      description = "Greetings, ordering in trattorias, and polite everyday conversation.",
      category = "Vocabulary",
      proficiencyLevel = "Beginner",
      targetLanguageCode = "it",
      estimatedMinutes = 4,
      xpReward = 30,
      cefrLevel = "A1",
      unitTitle = "Unit 1: Breakthrough Foundations (A1)",
      exercises = listOf(
        Exercise(
          id = "it_ex_1",
          type = ExerciseType.VOCABULARY,
          prompt = "What does 'Per favore' mean in Italian?",
          explanationNative = "'Per favore' means 'Please'.",
          targetAudioText = "Per favore",
          phoneticGuide = "pehr fah-voh-reh",
          options = listOf("Thank you", "Please", "Excuse me", "Good night"),
          correctOptionIndex = 1
        )
      )
    )
  )
  }

  private val russianLessons by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    CurriculumLesson(
      id = "ru_lesson_1",
      title = "Привет! Russian Foundations",
      description = "Learn greetings, pleasantries, and basic introductions.",
      category = "Vocabulary",
      proficiencyLevel = "Beginner",
      targetLanguageCode = "ru",
      estimatedMinutes = 4,
      xpReward = 30,
      cefrLevel = "A1",
      unitTitle = "Unit 1: Breakthrough Foundations (A1)",
      exercises = listOf(
        Exercise(
          id = "ru_ex_1",
          type = ExerciseType.VOCABULARY,
          prompt = "What is the polite Russian word for 'Thank you'?",
          explanationNative = "'Спасибо' (Spasibo) is the universal word for 'Thank you'.",
          targetAudioText = "Спасибо",
          phoneticGuide = "spah-see-bah",
          options = listOf("Пожалуйста", "Спасибо", "Здравствуйте", "До свидания"),
          correctOptionIndex = 1
        )
      )
    )
  )
  }

  private val urduLessons by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    CurriculumLesson(
      id = "ur_lesson_1",
      title = "آداب! Urdu Essentials",
      description = "Polite greetings, introductions, and everyday expressions in Urdu.",
      category = "Vocabulary",
      proficiencyLevel = "Beginner",
      targetLanguageCode = "ur",
      estimatedMinutes = 4,
      xpReward = 30,
      cefrLevel = "A1",
      unitTitle = "Unit 1: Breakthrough Foundations (A1)",
      exercises = listOf(
        Exercise(
          id = "ur_ex_1",
          type = ExerciseType.VOCABULARY,
          prompt = "What is the traditional polite greeting in Urdu?",
          explanationNative = "'آداب' (Aadaab) or 'السلام علیکم' (Assalam-o-Alaikum) are standard polite greetings.",
          targetAudioText = "السلام علیکم",
          phoneticGuide = "as-sa-lahm oh ah-lay-koom",
          options = listOf("السلام علیکم", "شکریہ", "خدا حافظ", "جی ہاں"),
          correctOptionIndex = 0
        )
      )
    )
  )
  }

  private fun genericLessons(targetLangCode: String): List<CurriculumLesson> {
    val cleanCode = targetLangCode.lowercase().substringBefore("-")
    val langMeta = com.example.data.model.Language.findByCode(cleanCode)
    val greetingPhrase = langMeta.sampleGreeting
    val introPhrase = extendedCoreTopics[1].targetPhrase(cleanCode)
    val thankPhrase = extendedCoreTopics[5].targetPhrase(cleanCode)
    return listOf(
      CurriculumLesson(
        id = "${targetLangCode}_lesson_1",
        title = "${langMeta.name} Essential Greetings & Script",
        description = "Learn how to greet others, introduce yourself, and use ${langMeta.writingSystem} naturally in ${langMeta.name}.",
        category = "Vocabulary",
        proficiencyLevel = "Beginner",
        targetLanguageCode = targetLangCode,
        estimatedMinutes = 4,
        xpReward = 30,
        cefrLevel = "A1",
        unitTitle = "Unit 1: Breakthrough Foundations (A1)",
        learningOutcomes = listOf(
          "Greet native speakers in ${langMeta.name} (${langMeta.nativeName})",
          "Recognize ${langMeta.writingSystem} script and core polite expressions"
        ),
        exercises = listOf(
          Exercise(
            id = "${targetLangCode}_ex_1",
            type = ExerciseType.VOCABULARY,
            prompt = "Select the authentic ${langMeta.name} greeting:",
            explanationNative = "'$greetingPhrase' is a foundational greeting in ${langMeta.name} (${langMeta.writingSystem}).",
            targetAudioText = greetingPhrase,
            phoneticGuide = "${langMeta.writingSystem} • ${langMeta.localeCode}",
            options = listOf(greetingPhrase, "Goodbye", "12345", "Closed"),
            correctOptionIndex = 0
          ),
          Exercise(
            id = "${targetLangCode}_ex_2",
            type = ExerciseType.LISTENING,
            prompt = "Listen carefully to the ${langMeta.name} audio and select what you heard:",
            explanationNative = "Developing your ear for native ${langMeta.name} pronunciation accelerates fluency.",
            targetAudioText = introPhrase,
            phoneticGuide = "${langMeta.writingSystem} • ${langMeta.localeCode}",
            options = listOf(introPhrase, thankPhrase, "What is the time?", "See you tomorrow"),
            correctOptionIndex = 0
          )
        )
      )
    )
  }

  data class GrammarStudioTopicData(
    val id: String,
    val title: String,
    val cefrLevel: String,
    val category: String,
    val ruleSummary: String,
    val formula: String,
    val correctExamples: List<Pair<String, String>>,
    val commonMistakes: List<Triple<String, String, String>>
  )

  data class GrammarStudioQuizQuestionData(
    val id: String,
    val promptSentence: String,
    val options: List<String>,
    val correctIndex: Int,
    val ruleTitle: String,
    val explanation: String
  )

  fun getGrammarStudioTopics(targetLangCode: String, nativeLangCode: String = "en"): List<GrammarStudioTopicData> {
    val code = targetLangCode.lowercase().substringBefore("-")
    val langMeta = com.example.data.model.Language.findByCode(code)
    val baseTopics = when (code) {
      "es" -> listOf(
        GrammarStudioTopicData(
          id = "gram_es_1",
          title = "Present Indicative & Regular Conjugations (-ar, -er, -ir)",
          cefrLevel = "A1",
          category = "Tenses",
          ruleSummary = "Use the Presente de Indicativo for habits, routines, and universal facts in Spanish.",
          formula = "Stem + (-o, -as/-es, -a/-e, -amos/-emos/-imos, -an/-en)",
          correctExamples = listOf(
            "Yo hablo español todos los días." to "I speak Spanish every day.",
            "Ella trabaja en Madrid." to "She works in Madrid.",
            "Nosotros vivimos cerca del centro." to "We live near downtown."
          ),
          commonMistakes = listOf(
            Triple("Yo hablar español.", "Yo hablo español.", "Always conjugate the infinitive verb to match the subject."),
            Triple("Ella trabajas aquí.", "Ella trabaja aquí.", "Third-person singular ('ella') takes '-a', without '-s'.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_es_2",
          title = "Ser vs. Estar (Identity vs. State)",
          cefrLevel = "A2",
          category = "Sentence Structure",
          ruleSummary = "Use 'ser' for permanent identity, origin, and profession; use 'estar' for temporary states and locations.",
          formula = "Ser (soy, eres, es) vs. Estar (estoy, estás, está)",
          correctExamples = listOf(
            "Soy ingeniero de software." to "I am a software engineer (Identity).",
            "Estoy muy contento hoy." to "I am very happy today (State).",
            "El museo está en el centro." to "The museum is downtown (Location)."
          ),
          commonMistakes = listOf(
            Triple("Estoy médico.", "Soy médico.", "Professions take 'ser' in Spanish."),
            Triple("Soy cansado.", "Estoy cansado.", "Temporary physical states like tiredness require 'estar'.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_es_3",
          title = "Present Subjunctive (El Subjuntivo)",
          cefrLevel = "B1",
          category = "Conditionals",
          ruleSummary = "Use the subjunctive after expressions of desire, emotion, doubt, or impersonal recommendations ('Es importante que...').",
          formula = "Main Clause + que + Subject 2 + Subjunctive Verb",
          correctExamples = listOf(
            "Espero que tengas un excelente día." to "I hope you have an excellent day.",
            "Es necesario que estudiemos juntos." to "It is necessary that we study together."
          ),
          commonMistakes = listOf(
            Triple("Espero que tienes buen día.", "Espero que tengas buen día.", "'Esperar que' triggers the subjunctive ('tengas').")
          )
        )
      )
      "fr" -> listOf(
        GrammarStudioTopicData(
          id = "gram_fr_1",
          title = "Present Tense & Être / Avoir Foundations",
          cefrLevel = "A1",
          category = "Tenses",
          ruleSummary = "Master 'être' (to be) and 'avoir' (to have), which serve both as core verbs and auxiliaries in French.",
          formula = "Je suis / J'ai • Tu es / Tu as • Il/Elle est / a",
          correctExamples = listOf(
            "Je suis ravi de vous rencontrer." to "I am delighted to meet you.",
            "J'ai vingt-cinq ans." to "I am 25 years old (lit. I have 25 years)."
          ),
          commonMistakes = listOf(
            Triple("Je suis 25 ans.", "J'ai 25 ans.", "French uses 'avoir' (to have) to express age, not 'être'.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_fr_2",
          title = "Passé Composé (Avoir vs. Être)",
          cefrLevel = "A2",
          category = "Tenses",
          ruleSummary = "Form completed past actions with 'avoir' or 'être' + past participle. Motion/reflexive verbs take 'être'.",
          formula = "Subject + avoir/être + Past Participle",
          correctExamples = listOf(
            "J'ai visité le musée hier." to "I visited the museum yesterday.",
            "Nous sommes allés à Lyon." to "We went to Lyon."
          ),
          commonMistakes = listOf(
            Triple("J'ai allé au café.", "Je suis allé au café.", "The verb 'aller' requires the auxiliary 'être'.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_fr_3",
          title = "Le Subjonctif Présent",
          cefrLevel = "B1",
          category = "Conditionals",
          ruleSummary = "Used after 'il faut que', 'bien que', and expressions of wish or emotion.",
          formula = "Il faut que + Subject + Subjunctive",
          correctExamples = listOf(
            "Il faut que tu viennes demain." to "You must come tomorrow.",
            "Bien qu'il pleuve, nous sortons." to "Although it is raining, we are going out."
          ),
          commonMistakes = listOf(
            Triple("Il faut que tu viens.", "Il faut que tu viennes.", "'Il faut que' requires the subjunctive form 'viennes'.")
          )
        )
      )
      "ar" -> listOf(
        GrammarStudioTopicData(
          id = "gram_ar_1",
          title = "Nominal Sentences (الجملة الاسمية) & Gender Agreement",
          cefrLevel = "A1",
          category = "Sentence Structure",
          ruleSummary = "In Arabic present tense, 'to be' is implied. The predicate agrees with the subject in gender and number.",
          formula = "المبتدأ (Definite Subject) + الخبر (Indefinite Predicate)",
          correctExamples = listOf(
            "البيتُ كبيرٌ." to "The house is big.",
            "المدرسةُ جميلةٌ." to "The school is beautiful (feminine agreement with ة)."
          ),
          commonMistakes = listOf(
            Triple("المدرسةُ جميلٌ.", "المدرسةُ جميلةٌ.", "Feminine nouns ending in ة require a feminine adjective ending in ة.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_ar_2",
          title = "Idafa Possessive Construction (الإضافة)",
          cefrLevel = "A2",
          category = "Articles & Prepositions",
          ruleSummary = "Link two nouns to show possession. Only the final noun takes the definite article 'ال' (Al-).",
          formula = "Noun 1 (no ال) + Noun 2 (with ال)",
          correctExamples = listOf(
            "كتابُ الطالبِ" to "The student's book.",
            "بابُ البيتِ" to "The door of the house."
          ),
          commonMistakes = listOf(
            Triple("الكتابُ الطالبِ", "كتابُ الطالبِ", "Never place 'ال' on the first noun of an Idafa construction.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_ar_3",
          title = "Present & Future Verb Prefixation (المضارع والمستقبل)",
          cefrLevel = "B1",
          category = "Tenses",
          ruleSummary = "Prefix 'سـ' (sa-) or 'سوف' (sawfa) to the present tense verb to express the future in Arabic.",
          formula = "سـ / سوف + الفعل المضارع",
          correctExamples = listOf(
            "سأدرسُ اللغة العربية كل يوم." to "I will study Arabic every day.",
            "سوف نسافرُ غداً." to "We will travel tomorrow."
          ),
          commonMistakes = listOf(
            Triple("سوف درستُ غداً.", "سوف أدرسُ غداً.", "Use the present tense verb after 'سوف', not the past tense.")
          )
        )
      )
      "ur" -> listOf(
        GrammarStudioTopicData(
          id = "gram_ur_1",
          title = "SOV Word Order & Honorific Pronouns (آپ، تم، تو)",
          cefrLevel = "A1",
          category = "Sentence Structure",
          ruleSummary = "Urdu places the verb at the end of the sentence (Subject–Object–Verb) and uses 'آپ' (Aap) for polite formal address.",
          formula = "فاعل (Subject) + مفعول (Object) + فعل (Verb)",
          correctExamples = listOf(
            "میں اردو سیکھ رہا ہوں۔" to "I am learning Urdu.",
            "آپ کیسے ہیں؟" to "How are you? (Polite formal)"
          ),
          commonMistakes = listOf(
            Triple("آپ کیسا ہے؟", "آپ کیسے ہیں؟", "'آپ' requires the plural respectful verb ending 'ہیں'.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_ur_2",
          title = "Postpositions & Oblique Case (نے، کو، میں، پر)",
          cefrLevel = "A2",
          category = "Articles & Prepositions",
          ruleSummary = "Unlike English prepositions, Urdu markers ('میں' in, 'پر' on, 'کو' to) follow the noun.",
          formula = "Noun (Oblique) + Postposition (میں / پر / سے / کو)",
          correctExamples = listOf(
            "کتاب میز پر ہے۔" to "The book is on the table.",
            "وہ گھر میں ہے۔" to "He/She is at home."
          ),
          commonMistakes = listOf(
            Triple("پر میز کتاب ہے۔", "کتاب میز پر ہے۔", "Postpositions always come immediately after the noun in Urdu.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_ur_3",
          title = "Ergative Past Tense with 'نے' (ماضی مطلق)",
          cefrLevel = "B1",
          category = "Tenses",
          ruleSummary = "Transitive verbs in completed past tenses require 'نے' (ne) after the subject, and the verb agrees with the object.",
          formula = "Subject + نے + Object + Past Transitive Verb",
          correctExamples = listOf(
            "احمد نے چائے پی۔" to "Ahmad drank tea (verb agrees with feminine 'چائے').",
            "میں نے خط لکھا۔" to "I wrote a letter."
          ),
          commonMistakes = listOf(
            Triple("میں خط لکھا۔", "میں نے خط لکھا۔", "Transitive past verbs require the ergative marker 'نے'.")
          )
        )
      )
      "hi" -> listOf(
        GrammarStudioTopicData(
          id = "gram_hi_1",
          title = "SOV Word Order & Present Habitual (ता है / ती है)",
          cefrLevel = "A1",
          category = "Tenses",
          ruleSummary = "Hindi verbs stand at the end of the clause and agree in gender and number with the subject.",
          formula = "Subject + Object + Verb Stem + ता/ती/ते + हूँ/है/हैं",
          correctExamples = listOf(
            "मैं हिंदी बोलता हूँ।" to "I speak Hindi (masculine speaker).",
            "वह रोज़ किताब पढ़ती है।" to "She reads a book every day."
          ),
          commonMistakes = listOf(
            Triple("आप कैसा है?", "आप कैसे हैं?", "'आप' requires the respectful plural form 'कैसे हैं'.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_hi_2",
          title = "Postpositions (में, पर, से, को) & Oblique Nouns",
          cefrLevel = "A2",
          category = "Articles & Prepositions",
          ruleSummary = "Postpositions follow the noun and shift masculine '-आ' nouns to the oblique '-ए' ending.",
          formula = "Oblique Noun + Postposition (कमरे में, मेज़ पर)",
          correctExamples = listOf(
            "लड़के को किताब दो।" to "Give the book to the boy.",
            "वह कमरे में है।" to "He/She is in the room."
          ),
          commonMistakes = listOf(
            Triple("कमरा में", "कमरे में", "Masculine '-आ' nouns change to '-ए' before a postposition.")
          )
        )
      )
      "zh" -> listOf(
        GrammarStudioTopicData(
          id = "gram_zh_1",
          title = "SVO Word Order, '是' (shì) & '很' (hěn) Adjectives",
          cefrLevel = "A1",
          category = "Sentence Structure",
          ruleSummary = "Mandarin uses '是' to link nouns, but uses '很' (not '是') before predicate adjectives.",
          formula = "Subject + 很 + Adjective (今天很冷)",
          correctExamples = listOf(
            "我是学生。" to "I am a student.",
            "今天天气很好。" to "The weather is very good today."
          ),
          commonMistakes = listOf(
            Triple("我是好。", "我很好。", "Do not use '是' before adjectives in Mandarin; use '很'.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_zh_2",
          title = "Time & Location Before the Verb",
          cefrLevel = "A2",
          category = "Sentence Structure",
          ruleSummary = "Time expressions and '在 + Location' always precede the main action verb in Chinese.",
          formula = "Subject + Time + 在 + Location + Verb + Object",
          correctExamples = listOf(
            "我明天在图书馆学习。" to "I will study at the library tomorrow.",
            "他昨天去了北京。" to "He went to Beijing yesterday."
          ),
          commonMistakes = listOf(
            Triple("我学习在图书馆明天。", "我明天在图书馆学习。", "Place time and location phrases before the verb.")
          )
        )
      )
      "ja" -> listOf(
        GrammarStudioTopicData(
          id = "gram_ja_1",
          title = "Core Particles は (wa), を (o), に (ni), で (de)",
          cefrLevel = "A1",
          category = "Articles & Prepositions",
          ruleSummary = "Particles follow nouns to mark their grammatical role: は (topic), を (direct object), に (destination/time), で (location of action).",
          formula = "Topic は + Place で + Object を + Verb ます",
          correctExamples = listOf(
            "私はカフェでコーヒーを飲みます。" to "I drink coffee at a café.",
            "明日、東京に行きます。" to "Tomorrow I will go to Tokyo."
          ),
          commonMistakes = listOf(
            Triple("カフェにコーヒーを飲みます。", "カフェでコーヒーを飲みます。", "Use 'で' (not 'に') for the place where an active action occurs.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_ja_2",
          title = "Polite Past & Negative Forms (ました / ません)",
          cefrLevel = "A2",
          category = "Tenses",
          ruleSummary = "Change '-ます' to '-ました' for polite past, and '-ません' for polite present/future negative.",
          formula = "Verb stem + ます / ません / ました / ませんでした",
          correctExamples = listOf(
            "昨日、映画を見ました。" to "Yesterday, I watched a movie.",
            "今日は仕事をしません。" to "I am not working today."
          ),
          commonMistakes = listOf(
            Triple("昨日、映画を見ます。", "昨日、映画を見ました。", "Past time markers like '昨日' require '-ました'.")
          )
        )
      )
      "ko" -> listOf(
        GrammarStudioTopicData(
          id = "gram_ko_1",
          title = "Topic vs. Subject Markers (은/는 vs. 이/가) & Polite -아/어요",
          cefrLevel = "A1",
          category = "Sentence Structure",
          ruleSummary = "Attach 은/는 to mark the topic or contrast, 이/가 for the subject, and end sentences politely with -아요/-어요.",
          formula = "Noun + 은/는 + Object + 을/를 + Verb-아/어요",
          correctExamples = listOf(
            "저는 한국어를 공부해요." to "I study Korean.",
            "오늘 날씨가 정말 좋아요." to "Today the weather is really good."
          ),
          commonMistakes = listOf(
            Triple("저는 공부해요 한국어를.", "저는 한국어를 공부해요.", "Korean follows Subject–Object–Verb order; the verb always comes last.")
          )
        )
      )
      "de" -> listOf(
        GrammarStudioTopicData(
          id = "gram_de_1",
          title = "V2 Word Order & Verb Conjugation",
          cefrLevel = "A1",
          category = "Sentence Structure",
          ruleSummary = "In German main clauses, the conjugated verb always occupies the second grammatical position (V2).",
          formula = "Element 1 (Subject or Time) + Verb (Position 2) + Subject + Rest",
          correctExamples = listOf(
            "Ich lerne jeden Tag Deutsch." to "I learn German every day.",
            "Heute gehe ich ins Kino." to "Today I am going to the cinema."
          ),
          commonMistakes = listOf(
            Triple("Heute ich gehe ins Kino.", "Heute gehe ich ins Kino.", "When a time phrase starts the sentence, the verb stays in 2nd position ('gehe ich').")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_de_2",
          title = "Nominative, Accusative & Dative Cases",
          cefrLevel = "A2",
          category = "Articles & Prepositions",
          ruleSummary = "Masculine 'der/ein' changes to 'den/einen' as a direct object (Accusative) and 'dem/einem' as an indirect object (Dative).",
          formula = "der -> den (Acc) -> dem (Dat)",
          correctExamples = listOf(
            "Ich trinke einen Kaffee." to "I am drinking a coffee (Accusative).",
            "Ich helfe dem Freund." to "I help the friend (Dative)."
          ),
          commonMistakes = listOf(
            Triple("Ich habe ein Hund.", "Ich habe einen Hund.", "'Haben' takes the accusative case ('einen Hund').")
          )
        )
      )
      else -> listOf(
        GrammarStudioTopicData(
          id = "gram_${code}_1",
          title = "${langMeta.name} Core Sentence Structure & Present Tense",
          cefrLevel = "A1",
          category = "Tenses",
          ruleSummary = "Build clear everyday statements and polite questions in ${langMeta.name} (${langMeta.writingSystem}).",
          formula = "Subject + Core Verb + Polite Register (${langMeta.nativeName})",
          correctExamples = listOf(
            extendedCoreTopics[0].targetPhrase(code) to extendedCoreTopics[0].meaning,
            extendedCoreTopics[1].targetPhrase(code) to extendedCoreTopics[1].meaning
          ),
          commonMistakes = listOf(
            Triple("Informal fragment", extendedCoreTopics[0].targetPhrase(code), "Use complete polite greetings in ${langMeta.name}.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_${code}_2",
          title = "${langMeta.name} Past Narrative & Time Connectors",
          cefrLevel = "A2",
          category = "Tenses",
          ruleSummary = "Combine time markers with past and future expressions in ${langMeta.name}.",
          formula = "Time Marker + Clause + Connector",
          correctExamples = listOf(
            extendedCoreTopics[4].targetPhrase(code) to extendedCoreTopics[4].meaning,
            extendedCoreTopics[8].targetPhrase(code) to extendedCoreTopics[8].meaning
          ),
          commonMistakes = listOf(
            Triple("Unlinked clauses", extendedCoreTopics[8].targetPhrase(code), "Use natural ${langMeta.name} time phrases.")
          )
        ),
        GrammarStudioTopicData(
          id = "gram_${code}_3",
          title = "${langMeta.name} Formal Register & Conditional Nuance",
          cefrLevel = "B1",
          category = "Conditionals",
          ruleSummary = "Express polite hypotheses, workplace proposals, and structured arguments in ${langMeta.name}.",
          formula = "Condition Clause + Polite Result Clause",
          correctExamples = listOf(
            extendedCoreTopics[12].targetPhrase(code) to extendedCoreTopics[12].meaning,
            extendedCoreTopics[15].targetPhrase(code) to extendedCoreTopics[15].meaning
          ),
          commonMistakes = listOf(
            Triple("Direct command", extendedCoreTopics[12].targetPhrase(code), "Use polite workplace phrasing in ${langMeta.name}.")
          )
        )
      )
    }
    return baseTopics.map { topic ->
      topic.copy(
        ruleSummary = localizeExplanation(topic.ruleSummary, nativeLangCode)
      )
    }
  }

  fun getGrammarStudioQuizQuestions(targetLangCode: String, nativeLangCode: String = "en"): List<GrammarStudioQuizQuestionData> {
    val code = targetLangCode.lowercase().substringBefore("-")
    val langMeta = com.example.data.model.Language.findByCode(code)
    val rawQuestions = when (code) {
      "es" -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_es_1",
          promptSentence = "Yo _____ muy contento de practicar español contigo hoy.",
          options = listOf("soy", "estoy", "eres", "están"),
          correctIndex = 1,
          ruleTitle = "Ser vs. Estar",
          explanation = "Temporary emotions and states ('contento hoy') require 'estar' ('estoy')."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_es_2",
          promptSentence = "Ayer nosotros _____ en un restaurante tradicional.",
          options = listOf("cenamos", "cenaremos", "cenando", "ceno"),
          correctIndex = 0,
          ruleTitle = "Preterite Past (A2)",
          explanation = "'Ayer' signals a completed past action requiring 'cenamos'."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_es_3",
          promptSentence = "Es importante que tú _____ antes de la reunión.",
          options = listOf("llegas", "llegues", "llegarás", "llegaste"),
          correctIndex = 1,
          ruleTitle = "Present Subjunctive (B1)",
          explanation = "'Es importante que' triggers the present subjunctive ('llegues')."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_es_4",
          promptSentence = "Si tuviera más tiempo libre, _____ por toda Sudamérica.",
          options = listOf("viajaría", "viajo", "viajé", "he viajado"),
          correctIndex = 0,
          ruleTitle = "Conditional Clauses (B2)",
          explanation = "'Si + imperfect subjunctive' pairs with the conditional ('viajaría')."
        )
      )
      "fr" -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_fr_1",
          promptSentence = "Hier soir, nous _____ allés au théâtre avec nos amis.",
          options = listOf("avons", "sommes", "allons", "étions"),
          correctIndex = 1,
          ruleTitle = "Passé Composé with Être",
          explanation = "The verb of motion 'aller' takes the auxiliary 'être' ('nous sommes allés')."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_fr_2",
          promptSentence = "Il faut que vous _____ à l'heure demain matin.",
          options = listOf("soyez", "êtes", "serez", "étiez"),
          correctIndex = 0,
          ruleTitle = "Subjonctif Présent",
          explanation = "'Il faut que' triggers the subjunctive form 'soyez'."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_fr_3",
          promptSentence = "Si j'avais su, je _____ venu plus tôt.",
          options = listOf("serais", "suis", "serai", "étais"),
          correctIndex = 0,
          ruleTitle = "Conditionnel Passé",
          explanation = "Past counterfactuals pair 'Si + plus-que-parfait' with 'conditionnel passé' ('serais venu')."
        )
      )
      "ar" -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_ar_1",
          promptSentence = "المدرسةُ _____ وقريبةٌ من البيت.",
          options = listOf("جميلةٌ", "جميلٌ", "جميلون", "جميلات"),
          correctIndex = 0,
          ruleTitle = "Gender Agreement (الجملة الاسمية)",
          explanation = "'المدرسةُ' is feminine singular, so its adjective must also be feminine singular ('جميلةٌ')."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_ar_2",
          promptSentence = "غداً _____ إلى مدينة الرياض لحضور المؤتمر.",
          options = listOf("سأسافرُ", "سافرتُ", "مسافر", "سافروا أمس"),
          correctIndex = 0,
          ruleTitle = "Future Prefix سـ (المستقبل)",
          explanation = "'غداً' (tomorrow) requires the future prefix 'سـ' + present tense ('سأسافرُ')."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_ar_3",
          promptSentence = "قرأتُ _____ الجديدَ في المكتبة.",
          options = listOf("كتابَ الطالبِ", "الكتابَ الطالبِ", "كتابُ الطالبَ", "طالبٌ كتاب"),
          correctIndex = 0,
          ruleTitle = "Idafa Construction (الإضافة)",
          explanation = "In an Idafa possessive phrase, the first noun never takes 'ال' ('كتابَ الطالبِ')."
        )
      )
      "ur" -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_ur_1",
          promptSentence = "آپ آج کیسے _____؟",
          options = listOf("ہیں", "ہے", "ہوں", "تھا"),
          correctIndex = 0,
          ruleTitle = "Honorific Agreement with آپ",
          explanation = "The polite pronoun 'آپ' always takes the plural respectful verb form 'ہیں'."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_ur_2",
          promptSentence = "کل ہم نے ایک دلچسپ کتاب _____۔",
          options = listOf("پڑھی", "پڑھتا", "پڑھیں گے", "پڑھو"),
          correctIndex = 0,
          ruleTitle = "Ergative Past Tense (نے)",
          explanation = "With 'نے' in the past tense, the verb agrees with the feminine singular object 'کتاب' ('پڑھی')."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_ur_3",
          promptSentence = "اگر میرے پاس وقت ہوتا تو میں ضرور _____۔",
          options = listOf("آتا", "آیا", "آؤں گا", "آ رہا"),
          correctIndex = 0,
          ruleTitle = "Conditional Clauses (شرطیہ جملے)",
          explanation = "Counterfactual conditionals pair 'ہوتا' with 'آتا'."
        )
      )
      "hi" -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_hi_1",
          promptSentence = "आप आज कैसे _____?",
          options = listOf("हैं", "है", "हूँ", "था"),
          correctIndex = 0,
          ruleTitle = "Honorific Agreement (आप)",
          explanation = "'आप' takes the respectful plural verb 'हैं'."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_hi_2",
          promptSentence = "कल मैंने एक नई किताब _____।",
          options = listOf("पढ़ी", "पढ़ता", "पढ़ूँगा", "पढ़ो"),
          correctIndex = 0,
          ruleTitle = "Ergative Past with ने",
          explanation = "With 'ने', the past verb agrees with the feminine object 'किताब' ('पढ़ी')."
        )
      )
      "zh" -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_zh_1",
          promptSentence = "今天天气 _____ 好，我们去公园吧。",
          options = listOf("很", "是", "在", "有"),
          correctIndex = 0,
          ruleTitle = "Adjective Predicates with 很",
          explanation = "Mandarin links subjects to adjectives using '很' (not '是')."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_zh_2",
          promptSentence = "我昨天已经 _____ 那本书了。",
          options = listOf("看了", "看", "会看", "在看"),
          correctIndex = 0,
          ruleTitle = "Completed Aspect Particle 了",
          explanation = "'昨天' (yesterday) and '已经' (already) pair with '看了' to mark completion."
        )
      )
      "ja" -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_ja_1",
          promptSentence = "私は毎日カフェ _____ コーヒーを飲みます。",
          options = listOf("で", "に", "を", "へ"),
          correctIndex = 0,
          ruleTitle = "Location of Action Particle で",
          explanation = "Use 'で' to mark the place where an active action takes place."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_ja_2",
          promptSentence = "昨日、友達と一緒に映画を _____。",
          options = listOf("見ました", "見ます", "見ません", "見て"),
          correctIndex = 0,
          ruleTitle = "Polite Past Tense (ました)",
          explanation = "'昨日' (yesterday) requires the past polite ending '見ました'."
        )
      )
      "ko" -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_ko_1",
          promptSentence = "저는 매일 아침 커피 _____ 마셔요.",
          options = listOf("를", "은", "이", "에"),
          correctIndex = 0,
          ruleTitle = "Object Particle 을/를",
          explanation = "'커피' ends in a vowel and is the direct object of '마셔요', so it takes '를'."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_ko_2",
          promptSentence = "어제 도서관에서 한국어를 _____.",
          options = listOf("공부했어요", "공부해요", "공부할 거예요", "공부하고"),
          correctIndex = 0,
          ruleTitle = "Past Tense -았/었어요",
          explanation = "'어제' (yesterday) requires the past polite form '공부했어요'."
        )
      )
      "de" -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_de_1",
          promptSentence = "Morgen _____ wir gemeinsam nach Berlin.",
          options = listOf("fahren", "wir fahren", "gefahren", "fährt"),
          correctIndex = 0,
          ruleTitle = "V2 Word Order",
          explanation = "When 'Morgen' starts the clause, the conjugated verb 'fahren' must come second."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_de_2",
          promptSentence = "Ich möchte bitte _____ heißen Kaffee bestellen.",
          options = listOf("einen", "ein", "einem", "einer"),
          correctIndex = 0,
          ruleTitle = "Accusative Case (der Kaffee -> einen Kaffee)",
          explanation = "Direct objects of 'bestellen' take the accusative case ('einen')."
        )
      )
      "en" -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_en_1",
          promptSentence = "She _____ to the office every morning at 8 AM.",
          options = listOf("go", "goes", "going", "gone"),
          correctIndex = 1,
          ruleTitle = "Present Simple (3rd Person Singular)",
          explanation = "Third-person singular subjects (he/she/it) take '-s' or '-es' in the Present Simple ('goes')."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_en_2",
          promptSentence = "I have lived in this city _____ 2021.",
          options = listOf("for", "since", "during", "from"),
          correctIndex = 1,
          ruleTitle = "Present Perfect (Since vs. For)",
          explanation = "Use 'since' with a specific starting point in time (2021) and 'for' with a duration."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_en_3",
          promptSentence = "If I _____ more free time, I would learn a third language.",
          options = listOf("have", "had", "will have", "having"),
          correctIndex = 1,
          ruleTitle = "Second Conditional",
          explanation = "The Second Conditional uses 'If + Past Simple (had)' + 'would + infinitive' for hypothetical situations."
        )
      )
      else -> listOf(
        GrammarStudioQuizQuestionData(
          id = "gq_${code}_1",
          promptSentence = "Select the authentic ${langMeta.name} sentence for '${extendedCoreTopics[0].meaning}':",
          options = listOf(extendedCoreTopics[0].targetPhrase(code), extendedCoreTopics[5].targetPhrase(code), extendedCoreTopics[6].targetPhrase(code), "12345"),
          correctIndex = 0,
          ruleTitle = "${langMeta.name} Polite Greeting Structure",
          explanation = "'${extendedCoreTopics[0].targetPhrase(code)}' is the natural polite greeting in ${langMeta.name}."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_${code}_2",
          promptSentence = "Select the ${langMeta.name} sentence for '${extendedCoreTopics[4].meaning}':",
          options = listOf(extendedCoreTopics[4].targetPhrase(code), extendedCoreTopics[1].targetPhrase(code), extendedCoreTopics[2].targetPhrase(code), "0000"),
          correctIndex = 0,
          ruleTitle = "${langMeta.name} Daily Routine Syntax",
          explanation = "'${extendedCoreTopics[4].targetPhrase(code)}' expresses daily routine actions accurately in ${langMeta.name}."
        ),
        GrammarStudioQuizQuestionData(
          id = "gq_${code}_3",
          promptSentence = "Select the ${langMeta.name} sentence for '${extendedCoreTopics[12].meaning}':",
          options = listOf(extendedCoreTopics[12].targetPhrase(code), extendedCoreTopics[3].targetPhrase(code), extendedCoreTopics[7].targetPhrase(code), "---"),
          correctIndex = 0,
          ruleTitle = "${langMeta.name} Professional Register",
          explanation = "'${extendedCoreTopics[12].targetPhrase(code)}' models formal workplace register in ${langMeta.name}."
        )
      )
    }
    return rawQuestions.map { q ->
      q.copy(explanation = localizeExplanation(q.explanation, nativeLangCode))
    }
  }
}

