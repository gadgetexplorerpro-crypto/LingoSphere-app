package com.example.data.repository

import com.example.data.model.Language

enum class MiniGameCategory(val label: String, val emoji: String) {
  VOCAB_BLITZ("Vocabulary Blitz", "⚡"),
  GRAMMAR_REPAIR("Grammar Repair", "📐"),
  SENTENCE_BUILDER("Sentence Builder", "🧩"),
  LISTENING_EAR("Listening & Accent", "🎧"),
  SLANG_DETECTIVE("Local Slang & Idioms", "🗣️"),
  TRAVEL_SURVIVAL("Travel & Culture", "✈️")
}

data class GameRoundItem(
  val prompt: String,
  val audioPhrase: String,
  val phonetic: String,
  val options: List<String>,
  val correctIndex: Int,
  val explanation: String
)

data class LanguageMiniGame(
  val id: String,
  val number: Int,
  val title: String,
  val subtitle: String,
  val category: MiniGameCategory,
  val cefrLevel: String,
  val xpReward: Int,
  val timeLimitSeconds: Int,
  val rounds: List<GameRoundItem>
)

data class LocalSlangTraditionItem(
  val id: String,
  val phrase: String,
  val phonetic: String,
  val literalMeaning: String,
  val realMeaning: String,
  val culturalTraditionNote: String,
  val exampleUsage: String,
  val categoryTag: String // "Local Slang", "Cultural Tradition", "Travel Conversation", "Native Idiom"
)

object LanguageGamesData {

  private data class LocalizedPack(
    val greeting: Triple<String, String, String>, // target, phonetic, english
    val thankYou: Triple<String, String, String>,
    val orderCoffee: Triple<String, String, String>,
    val askDirections: Triple<String, String, String>,
    val howMuch: Triple<String, String, String>,
    val niceToMeet: Triple<String, String, String>,
    val trainStation: Triple<String, String, String>,
    val deliciousFood: Triple<String, String, String>,
    val speakSlowly: Triple<String, String, String>,
    val checkPlease: Triple<String, String, String>,
    val slangCool: Triple<String, String, String>,
    val slangNoWorry: Triple<String, String, String>,
    val grammarPast: Triple<String, String, String>,
    val grammarFuture: Triple<String, String, String>,
    val emergencyHelp: Triple<String, String, String>,
    val festivalToast: Triple<String, String, String>
  )

  private fun getPack(langCode: String): LocalizedPack {
    return when (langCode.lowercase()) {
      "es" -> LocalizedPack(
        greeting = Triple("¡Buenos días! ¿Cómo amaneciste?", "bweh-nohs DEE-ahs, KOH-moh ah-mah-neh-SEES-teh", "Good morning! How did you wake up today?"),
        thankYou = Triple("Muchísimas gracias por tu amabilidad", "moo-CHEE-see-mahs GRAH-syahs por too ah-mah-bee-lee-DAHD", "Thank you very much for your kindness"),
        orderCoffee = Triple("Me gustaría pedir un café con leche caliente", "meh goos-tah-REE-ah peh-DEER oon kah-FEH kohn LEH-cheh", "I would like to order a hot coffee with milk"),
        askDirections = Triple("Disculpe, ¿dónde queda la estación central?", "dees-KOOL-peh, DOHN-deh KEH-dah lah es-tah-SYOHN", "Excuse me, where is the central station located?"),
        howMuch = Triple("¿Cuánto cuesta esta artesanía tradicional?", "KWAHN-toh KWES-tah ES-tah ar-teh-sah-NEE-ah", "How much does this traditional handicraft cost?"),
        niceToMeet = Triple("Es un verdadero placer conocerte en persona", "es oon ber-dah-DEH-roh plah-SER koh-noh-SER-teh", "It is a true pleasure to meet you in person"),
        trainStation = Triple("El próximo tren rápido sale del andén cuatro", "el PROK-see-moh tren RAH-pee-doh SAH-leh del ahn-DEN KWAH-troh", "The next express train departs from platform four"),
        deliciousFood = Triple("¡Este plato típico está para chuparse los dedos!", "ES-teh PLAH-toh TEE-pee-koh es-TAH PAH-rah choo-PAR-seh", "This traditional dish is finger-licking good!"),
        speakSlowly = Triple("¿Podrías hablar un poco más despacio, por favor?", "poh-DREE-ahs ah-BLAR oon POH-koh mahs des-PAH-syoh", "Could you speak a little more slowly, please?"),
        checkPlease = Triple("Nos trae la cuenta cuando pueda, por favor", "nohs TRAH-eh lah KWEN-tah KWAHN-doh PWEH-dah", "Bring us the bill whenever you can, please"),
        slangCool = Triple("¡Qué guay! / ¡Qué chévere!", "keh gway / keh CHEH-veh-reh", "How cool! / That's awesome! (Local slang)"),
        slangNoWorry = Triple("No pasa nada, todo está bajo control", "noh PAH-sah NAH-dah, TOH-doh es-TAH BAH-hoh kohn-TROL", "No worries at all, everything is under control"),
        grammarPast = Triple("Ayer visitamos el museo histórico de la ciudad", "ah-YER bee-see-TAH-mohs el moo-SEH-oh ees-TOH-ree-koh", "Yesterday we visited the historic museum of the city"),
        grammarFuture = Triple("Mañana viajaremos temprano hacia la costa", "mah-NYAH-nah byah-hah-REH-mohs tem-PRAH-noh", "Tomorrow we will travel early toward the coast"),
        emergencyHelp = Triple("Necesito ayuda para encontrar una farmacia abierta", "neh-seh-SEE-toh ah-YOO-dah PAH-rah en-kohn-TRAR", "I need help finding an open pharmacy"),
        festivalToast = Triple("¡Salud, dinero y amor para toda la familia!", "sah-LOOD, dee-NEH-roh ee ah-MOR", "Health, prosperity, and love for the whole family! (Traditional toast)")
      )
      "fr" -> LocalizedPack(
        greeting = Triple("Bonjour ! Comment se passe votre journée ?", "bohn-ZHOOR, koh-mahn suh pahs voh-truh zhoor-NAY", "Hello! How is your day going?"),
        thankYou = Triple("Je vous remercie infiniment pour votre accueil", "zhuh voo ruh-mer-SEE ahn-fee-nee-MAHN", "I thank you infinitely for your warm welcome"),
        orderCoffee = Triple("Je prendrais bien un café crème et un croissant", "zhuh prahn-DRAY byen uhn kah-FAY krem", "I would gladly have a coffee with cream and a croissant"),
        askDirections = Triple("Excusez-moi, pourriez-vous m'indiquer le métro ?", "eks-koo-zay MWAH, poo-ryay voo mahn-dee-KAY luh may-TROH", "Excuse me, could you point me to the metro?"),
        howMuch = Triple("Quel est le prix de cette spécialité locale ?", "kel eh luh pree duh set spay-syah-lee-TAY loh-KAHL", "What is the price of this local specialty?"),
        niceToMeet = Triple("Enchanté de faire votre connaissance aujourd'hui", "ahn-shahn-TAY duh fair voh-truh koh-neh-SAHNS", "Delighted to make your acquaintance today"),
        trainStation = Triple("Le TGV à destination de Lyon part du quai numéro deux", "luh tay-zhay-VAY par doo kay noo-may-ROH duh", "The high-speed train to Lyon leaves from platform two"),
        deliciousFood = Triple("Ce repas traditionnel est un vrai régal !", "suh ruh-PAH trah-dee-syoh-NEL eh uhn vray ray-GAHL", "This traditional meal is a true delight!"),
        speakSlowly = Triple("Pourriez-vous parler un peu plus lentement ?", "poo-ryay VOO par-LAY uhn puh ploo lahn-tuh-MAHN", "Could you speak a little more slowly?"),
        checkPlease = Triple("L'addition s'il vous plaît, quand vous aurez un instant", "lah-dee-SYOHN seel voo PLEH", "The check please, whenever you have a moment"),
        slangCool = Triple("C'est carrément génial ! / C'est chouette !", "seh kah-ray-MAHN zhay-NYAHL", "That is totally awesome! / Really cool! (Local slang)"),
        slangNoWorry = Triple("Ne t'en fais pas, ça va aller tout seul", "nuh tahn feh PAH, sah vah ah-LAY too suhl", "Don't worry about it, it'll go smoothly"),
        grammarPast = Triple("Hier soir, nous avons découvert un bistro charmant", "yer SWAHR, nooz ah-VOHN day-koo-VER uhn bees-TROH", "Last night, we discovered a charming bistro"),
        grammarFuture = Triple("Demain matin, nous visiterons le marché provençal", "duh-MAHN mah-TAHN, noo vee-zee-tuh-ROHN luh mar-SHAY", "Tomorrow morning, we will visit the Provençal market"),
        emergencyHelp = Triple("Pouvez-vous m'indiquer la pharmacie de garde ?", "poo-vay VOO mahn-dee-KAY lah far-mah-SEE duh gard", "Can you direct me to the on-duty pharmacy?"),
        festivalToast = Triple("À la vôtre ! Santé et bonheur à tous !", "ah lah VOH-truh! sahn-TAY ay boh-NUHR", "Cheers to you! Health and happiness to all!")
      )
      "de" -> LocalizedPack(
        greeting = Triple("Guten Morgen! Wie läuft dein Tag bisher?", "GOO-ten MOR-gen! vee loyft dayn tahk bis-HAIR", "Good morning! How is your day going so far?"),
        thankYou = Triple("Herzlichen Dank für Ihre großartige Unterstützung", "HERTS-li-khen dahnk fyoor EE-ruh GROHS-ar-ti-guh", "Heartfelt thanks for your wonderful support"),
        orderCoffee = Triple("Ich hätte gerne einen heißen Kaffee und Brezeln", "ikh HET-tuh GAIR-nuh AY-nen HAY-sen kah-FAY", "I would like a hot coffee and pretzels"),
        askDirections = Triple("Entschuldigung, wie komme ich zum Hauptbahnhof?", "ent-SHOOL-di-goong, vee KOM-muh ikh tsoom HOWPT-bahn-hohf", "Excuse me, how do I get to the main train station?"),
        howMuch = Triple("Was kostet dieses handgemachte Souvenir?", "vahs KOS-tet DEE-zes HAHND-guh-makh-tuh", "How much does this handmade souvenir cost?"),
        niceToMeet = Triple("Es freut mich sehr, Sie persönlich kennenzulernen", "es froyt mikh zayr, zee pair-ZURN-likh KEN-nen-tsoo-lair-nen", "I am very pleased to meet you in person"),
        trainStation = Triple("Der Schnellzug fährt pünktlich von Gleis sieben ab", "dair SHNEL-tsook fairt PYOONKT-likh fon glays ZEE-ben ahp", "The express train departs punctually from track seven"),
        deliciousFood = Triple("Dieses regionale Gericht schmeckt einfach fantastisch!", "DEE-zes ray-gyoh-NAH-luh guh-RIKHT shmekt fahn-TAHS-tish", "This regional dish tastes simply fantastic!"),
        speakSlowly = Triple("Könnten Sie bitte ein wenig langsamer sprechen?", "KURN-ten zee BIT-tuh ayn VAY-nikh LAHNG-zah-mer SHPRE-khen", "Could you please speak a little more slowly?"),
        checkPlease = Triple("Wir möchten bitte zusammen bezahlen", "veer MURKH-ten BIT-tuh tsoo-ZAHM-men buh-TSAH-len", "We would like to pay together, please"),
        slangCool = Triple("Das ist ja mega stark! / Echt spitze!", "dahs ist yah MAY-gah shtark", "That is super awesome! / Truly top-notch! (Slang)"),
        slangNoWorry = Triple("Alles halb so wild, mach dir keinen Kopf!", "AHL-les hahlp zoh vilt, mahkh deer KAY-nen kopf", "No big deal at all, don't stress about it!"),
        grammarPast = Triple("Gestern haben wir die historische Altstadt besichtigt", "GES-tern HAH-ben veer dee AHLT-shtaht buh-ZIKH-tikt", "Yesterday we toured the historic old town"),
        grammarFuture = Triple("Morgen werden wir gemeinsam in den Alpen wandern", "MOR-gen VAIR-den veer in den AHL-pen VAHN-dern", "Tomorrow we will hike together in the Alps"),
        emergencyHelp = Triple("Wo finde ich bitte die nächste Apotheke?", "voh FIN-duh ikh BIT-tuh dee NEKH-stuh ah-poh-TAY-kuh", "Where can I find the nearest pharmacy, please?"),
        festivalToast = Triple("Zum Wohl! Auf gute Freundschaft und Gesundheit!", "tsoom vohl! owf GOO-tuh FROYND-shahft", "Cheers! To good friendship and health!")
      )
      "ja" -> LocalizedPack(
        greeting = Triple("おはようございます！今日の調子はいかがですか？", "Ohayō gozaimasu! Kyō no chōshi wa ikaga desu ka?", "Good morning! How are you feeling today?"),
        thankYou = Triple("ご親切に教えていただき、ありがとうございます", "Go-shinsetsu ni oshiete itadaki, arigatō gozaimasu", "Thank you very much for kindly teaching me"),
        orderCoffee = Triple("すみません、ホットコーヒーを一つお願いします", "Sumimasen, hotto kōhī o hitotsu onegaishimasu", "Excuse me, one hot coffee please"),
        askDirections = Triple("あのう、一番近い地下鉄の駅はどこですか？", "Anō, ichiban chikai chikatetsu no eki wa doko desu ka?", "Excuse me, where is the nearest subway station?"),
        howMuch = Triple("この伝統的なお土産はいくらですか？", "Kono dentōteki na omiyage wa ikura desu ka?", "How much is this traditional souvenir?"),
        niceToMeet = Triple("はじめまして、お会いできて本当に嬉しいです", "Hajimemashite, o-ai dekite hontō ni ureshii desu", "Nice to meet you, I am truly happy to meet you"),
        trainStation = Triple("次の新幹線は３番ホームから出発します", "Tsugi no shinkansen wa san-ban hōmu kara shuppatsu shimasu", "The next bullet train departs from platform 3"),
        deliciousFood = Triple("この地元の料理は本当にほっぺたが落ちそうです！", "Kono jimoto no ryōri wa hoppeta ga ochisō desu!", "This local dish is so delicious my cheeks might fall off!"),
        speakSlowly = Triple("すみません、もう少しゆっくり話していただけますか？", "Sumimasen, mō sukoshi yukkuri hanashite itadakemasu ka?", "Excuse me, could you speak a little more slowly?"),
        checkPlease = Triple("ごちそうさまでした。お会計をお願いします", "Gochisōsama deshita. O-kaikei o onegaishimasu", "Thank you for the meal. May I have the check please?"),
        slangCool = Triple("めっちゃ最高！ / すごくいいね！", "Metcha saikō! / Sugoku ii ne!", "Super awesome! / That's really great! (Colloquial)"),
        slangNoWorry = Triple("全然大丈夫だよ！気にしないでね", "Zenzen daijōbu da yo! Ki ni shinaide ne", "It's totally fine! Don't worry about it"),
        grammarPast = Triple("昨日、京都の美しいお寺を見学しました", "Kinō, Kyōto no utsukushii o-tera o kengaku shimashita", "Yesterday, I visited a beautiful temple in Kyoto"),
        grammarFuture = Triple("明日の朝、友達と一緒に市場へ行きます", "Ashita no asa, tomodachi to issho ni ichiba e ikimasu", "Tomorrow morning, I will go to the market with my friend"),
        emergencyHelp = Triple("近くに開いている薬局はありますか？", "Chikaku ni aite iru yakkyoku wa arimasu ka?", "Is there an open pharmacy nearby?"),
        festivalToast = Triple("乾杯！皆様の健康と幸せを願って！", "Kanpai! Minasama no kenkō to shiawase o negatte!", "Cheers! Wishing health and happiness to everyone!")
      )
      else -> {
        val langName = Language.findByCode(langCode).name
        LocalizedPack(
          greeting = Triple("Good morning! How is your $langName practice going today?", "Good morning greeting in $langName", "Polite daily morning greeting"),
          thankYou = Triple("Thank you so much for your generous help and patience", "Expressing warm gratitude", "Polite appreciation phrase"),
          orderCoffee = Triple("I would like to order a warm coffee and a local pastry, please", "Ordering politely at a café", "Restaurant & café ordering phrase"),
          askDirections = Triple("Excuse me, could you tell me how to reach the central station?", "Asking directions politely", "Navigation & transit question"),
          howMuch = Triple("How much does this handmade local craft cost?", "Inquiring about prices at a market", "Shopping & bargaining phrase"),
          niceToMeet = Triple("It is a wonderful pleasure to meet you in person today", "Warm first-time introduction", "Polite social introduction"),
          trainStation = Triple("The express train departs from platform four in ten minutes", "Understanding transit announcements", "Train & metro travel phrase"),
          deliciousFood = Triple("Compliments to the chef! This traditional dish is delicious!", "Praising authentic local cuisine", "Dining compliment"),
          speakSlowly = Triple("Could you please speak a little more slowly so I can follow?", "Requesting slower speech", "Communication repair strategy"),
          checkPlease = Triple("Could we please have the bill whenever you have a moment?", "Asking for the check politely", "Restaurant closing etiquette"),
          slangCool = Triple("That's awesome! / Spot on!", "Everyday enthusiastic expression", "Natural colloquial reaction"),
          slangNoWorry = Triple("No worries at all, take your time!", "Reassuring a friend casually", "Friendly everyday idiom"),
          grammarPast = Triple("Yesterday we explored the historic cultural district together", "Past tense narrative structure", "Completed past action"),
          grammarFuture = Triple("Tomorrow morning we will visit the traditional street market", "Future tense planning structure", "Upcoming planned event"),
          emergencyHelp = Triple("Could you help me find the nearest open pharmacy or clinic?", "Urgent assistance phrase", "Travel safety & medical help"),
          festivalToast = Triple("Cheers to friendship, health, and cultural connection!", "Festive celebratory toast", "Cultural celebration expression")
        )
      }
    }
  }

  /**
   * Generates 54 distinct, playable interactive language-learning games across 6 categories.
   */
  fun getGamesForLanguage(langCode: String): List<LanguageMiniGame> {
    val pack = getPack(langCode)
    val langName = Language.findByCode(langCode).name

    val baseRounds = listOf(
      GameRoundItem(
        prompt = "What is the best meaning of: \"${pack.greeting.first}\"?",
        audioPhrase = pack.greeting.first,
        phonetic = pack.greeting.second,
        options = listOf(pack.greeting.third, "Where is my luggage?", "I would like the bill", "The train is delayed"),
        correctIndex = 0,
        explanation = "\"${pack.greeting.first}\" (${pack.greeting.second}) is a natural native greeting."
      ),
      GameRoundItem(
        prompt = "Which phrase should you say when ordering at a local café?",
        audioPhrase = pack.orderCoffee.first,
        phonetic = pack.orderCoffee.second,
        options = listOf(pack.orderCoffee.first, pack.trainStation.first, pack.emergencyHelp.first, pack.grammarPast.first),
        correctIndex = 0,
        explanation = "Use \"${pack.orderCoffee.first}\" to politely order drinks or pastries."
      ),
      GameRoundItem(
        prompt = "Listen/Read carefully: \"${pack.askDirections.first}\" — What is the speaker asking?",
        audioPhrase = pack.askDirections.first,
        phonetic = pack.askDirections.second,
        options = listOf(pack.askDirections.third, "How to cook a recipe", "What time the movie starts", "Whether it will rain tomorrow"),
        correctIndex = 0,
        explanation = "This polite question asks for directions to the central transit station."
      ),
      GameRoundItem(
        prompt = "How do you politely ask a native speaker to slow down?",
        audioPhrase = pack.speakSlowly.first,
        phonetic = pack.speakSlowly.second,
        options = listOf(pack.speakSlowly.first, pack.checkPlease.first, pack.festivalToast.first, pack.slangCool.first),
        correctIndex = 0,
        explanation = "\"${pack.speakSlowly.first}\" keeps the conversation flowing when native speech is fast."
      )
    )

    val grammarRounds = listOf(
      GameRoundItem(
        prompt = "Select the sentence that correctly expresses a completed PAST action in $langName:",
        audioPhrase = pack.grammarPast.first,
        phonetic = pack.grammarPast.second,
        options = listOf(pack.grammarPast.first, pack.grammarFuture.first, pack.orderCoffee.first, pack.howMuch.first),
        correctIndex = 0,
        explanation = "\"${pack.grammarPast.first}\" (${pack.grammarPast.third}) uses past tense markers."
      ),
      GameRoundItem(
        prompt = "Select the sentence that correctly expresses a FUTURE plan in $langName:",
        audioPhrase = pack.grammarFuture.first,
        phonetic = pack.grammarFuture.second,
        options = listOf(pack.grammarFuture.first, pack.grammarPast.first, pack.thankYou.first, pack.checkPlease.first),
        correctIndex = 0,
        explanation = "\"${pack.grammarFuture.first}\" (${pack.grammarFuture.third}) expresses future action."
      ),
      GameRoundItem(
        prompt = "Which polite register phrase is appropriate when thanking a host?",
        audioPhrase = pack.thankYou.first,
        phonetic = pack.thankYou.second,
        options = listOf(pack.thankYou.first, pack.trainStation.first, pack.howMuch.first, pack.emergencyHelp.first),
        correctIndex = 0,
        explanation = "\"${pack.thankYou.first}\" conveys warm, grammatically complete gratitude."
      ),
      GameRoundItem(
        prompt = "When asking for the check at a restaurant, which structure is most polite?",
        audioPhrase = pack.checkPlease.first,
        phonetic = pack.checkPlease.second,
        options = listOf(pack.checkPlease.first, pack.askDirections.first, pack.grammarPast.first, pack.slangCool.first),
        correctIndex = 0,
        explanation = "\"${pack.checkPlease.first}\" is the standard polite dining request."
      )
    )

    val slangAndCultureRounds = listOf(
      GameRoundItem(
        prompt = "What does the colloquial native expression \"${pack.slangCool.first}\" convey?",
        audioPhrase = pack.slangCool.first,
        phonetic = pack.slangCool.second,
        options = listOf(pack.slangCool.third, "I am running late", "The weather is cold", "Please close the door"),
        correctIndex = 0,
        explanation = "Native speakers use \"${pack.slangCool.first}\" to show enthusiasm and agreement."
      ),
      GameRoundItem(
        prompt = "When a friend says \"${pack.slangNoWorry.first}\", what do they mean?",
        audioPhrase = pack.slangNoWorry.first,
        phonetic = pack.slangNoWorry.second,
        options = listOf(pack.slangNoWorry.third, "You must pay immediately", "The store is closed", "I lost my ticket"),
        correctIndex = 0,
        explanation = "\"${pack.slangNoWorry.first}\" is a warm, reassuring everyday idiom."
      ),
      GameRoundItem(
        prompt = "During a traditional celebration, what is the meaning of \"${pack.festivalToast.first}\"?",
        audioPhrase = pack.festivalToast.first,
        phonetic = pack.festivalToast.second,
        options = listOf(pack.festivalToast.third, "Where is the exit?", "Can I have water?", "Goodnight everyone"),
        correctIndex = 0,
        explanation = "This traditional toast celebrates health, friendship, and togetherness."
      ),
      GameRoundItem(
        prompt = "How do locals enthusiastically compliment a delicious traditional meal?",
        audioPhrase = pack.deliciousFood.first,
        phonetic = pack.deliciousFood.second,
        options = listOf(pack.deliciousFood.first, pack.trainStation.first, pack.emergencyHelp.first, pack.askDirections.first),
        correctIndex = 0,
        explanation = "\"${pack.deliciousFood.first}\" is an authentic culinary compliment."
      )
    )

    val travelRounds = listOf(
      GameRoundItem(
        prompt = "At a local artisan market, how do you ask the price of an item?",
        audioPhrase = pack.howMuch.first,
        phonetic = pack.howMuch.second,
        options = listOf(pack.howMuch.first, pack.festivalToast.first, pack.grammarPast.first, pack.niceToMeet.first),
        correctIndex = 0,
        explanation = "\"${pack.howMuch.first}\" (${pack.howMuch.third}) is essential for market shopping."
      ),
      GameRoundItem(
        prompt = "At the rail station, you hear: \"${pack.trainStation.first}\" — What does it mean?",
        audioPhrase = pack.trainStation.first,
        phonetic = pack.trainStation.second,
        options = listOf(pack.trainStation.third, "The museum is closed", "Breakfast is served", "Please show your passport"),
        correctIndex = 0,
        explanation = "Recognizing platform and departure announcements is key for transit navigation."
      ),
      GameRoundItem(
        prompt = "If you need medical or pharmacy assistance while traveling, what should you say?",
        audioPhrase = pack.emergencyHelp.first,
        phonetic = pack.emergencyHelp.second,
        options = listOf(pack.emergencyHelp.first, pack.orderCoffee.first, pack.slangCool.first, pack.festivalToast.first),
        correctIndex = 0,
        explanation = "\"${pack.emergencyHelp.first}\" clearly communicates an urgent need for a pharmacy."
      ),
      GameRoundItem(
        prompt = "When meeting a language exchange partner in person, what is the best opening?",
        audioPhrase = pack.niceToMeet.first,
        phonetic = pack.niceToMeet.second,
        options = listOf(pack.niceToMeet.first, pack.checkPlease.first, pack.howMuch.first, pack.trainStation.first),
        correctIndex = 0,
        explanation = "\"${pack.niceToMeet.first}\" sets a warm, respectful tone."
      )
    )

    val gameBlueprints = listOf(
      // Category 1: Vocabulary Blitz (9 games)
      Triple("Speed Greeting Sprint", "Master essential daily greetings & openings", MiniGameCategory.VOCAB_BLITZ),
      Triple("Café & Bistro Menu Dash", "Order drinks, pastries & meals under time pressure", MiniGameCategory.VOCAB_BLITZ),
      Triple("Numbers, Prices & Currency", "Bargain & count accurately in local markets", MiniGameCategory.VOCAB_BLITZ),
      Triple("Time, Days & Schedules", "Coordinate appointments & travel departures", MiniGameCategory.VOCAB_BLITZ),
      Triple("Family, Friends & Relationships", "Describe the people who matter most", MiniGameCategory.VOCAB_BLITZ),
      Triple("Weather & Seasons Flash", "Small talk about climate & outdoor plans", MiniGameCategory.VOCAB_BLITZ),
      Triple("Professions & Workplace Terms", "Career, office & business vocabulary", MiniGameCategory.VOCAB_BLITZ),
      Triple("Colors, Clothing & Style", "Shop for outfits & describe appearances", MiniGameCategory.VOCAB_BLITZ),
      Triple("Emotions & Personality Match", "Express feelings, moods & nuanced reactions", MiniGameCategory.VOCAB_BLITZ),

      // Category 2: Grammar Repair (9 games)
      Triple("Past Tense Time Machine", "Narrate yesterday's adventures & memories", MiniGameCategory.GRAMMAR_REPAIR),
      Triple("Future Plans Architect", "Build accurate sentences about tomorrow", MiniGameCategory.GRAMMAR_REPAIR),
      Triple("Polite vs. Casual Register", "Choose the right formality for any situation", MiniGameCategory.GRAMMAR_REPAIR),
      Triple("Question Builder Challenge", "Who, what, where, when & why mastery", MiniGameCategory.GRAMMAR_REPAIR),
      Triple("Connector & Transition Flow", "Link ideas with 'because', 'however' & 'therefore'", MiniGameCategory.GRAMMAR_REPAIR),
      Triple("Verb Conjugation Arena", "Match subjects with accurate verb forms", MiniGameCategory.GRAMMAR_REPAIR),
      Triple("Preposition Navigator", "In, on, at, toward & from in real context", MiniGameCategory.GRAMMAR_REPAIR),
      Triple("Negation & Polite Refusal", "Say no gracefully without sounding abrupt", MiniGameCategory.GRAMMAR_REPAIR),
      Triple("Conditional & Subjunctive Lab", "Express wishes, possibilities & polite requests", MiniGameCategory.GRAMMAR_REPAIR),

      // Category 3: Sentence Builder (9 games)
      Triple("Morning Routine Scrambler", "Assemble natural sentences about daily habits", MiniGameCategory.SENTENCE_BUILDER),
      Triple("Restaurant Order Composer", "Put multi-part dining requests in native order", MiniGameCategory.SENTENCE_BUILDER),
      Triple("Airport & Boarding Builder", "Construct clear check-in & customs sentences", MiniGameCategory.SENTENCE_BUILDER),
      Triple("Hotel Concierge Request", "Build polite room service & tour inquiries", MiniGameCategory.SENTENCE_BUILDER),
      Triple("Job Interview Pitch Maker", "Structure professional strengths & experience", MiniGameCategory.SENTENCE_BUILDER),
      Triple("Doctor & Symptom Describer", "Explain health needs in clear sentence order", MiniGameCategory.SENTENCE_BUILDER),
      Triple("Weekend Plans Storyteller", "Combine time phrases with social activities", MiniGameCategory.SENTENCE_BUILDER),
      Triple("Opinion & Debate Composer", "Express agreement, contrast & nuanced views", MiniGameCategory.SENTENCE_BUILDER),
      Triple("Phone Call Opening & Closing", "Natural telephone etiquette sentence flow", MiniGameCategory.SENTENCE_BUILDER),

      // Category 4: Listening & Accent (9 games)
      Triple("Native Ear Dictation", "Identify fast-spoken native phrases by ear", MiniGameCategory.LISTENING_EAR),
      Triple("Train & Airport PA Listener", "Catch platform numbers & gate changes", MiniGameCategory.LISTENING_EAR),
      Triple("Café Noise Comprehension", "Understand spoken orders in a busy bistro", MiniGameCategory.LISTENING_EAR),
      Triple("Question Intonation Detector", "Spot rising vs. falling native pitch patterns", MiniGameCategory.LISTENING_EAR),
      Triple("Minimal Pairs Sound Lab", "Distinguish tricky vowels & consonant sounds", MiniGameCategory.LISTENING_EAR),
      Triple("Speed News Headline Ear", "Catch key facts from spoken broadcasts", MiniGameCategory.LISTENING_EAR),
      Triple("Phone Voicemail Decoder", "Extract times & callback details from audio", MiniGameCategory.LISTENING_EAR),
      Triple("Street Directions Audio Map", "Follow spoken left, right & landmark cues", MiniGameCategory.LISTENING_EAR),
      Triple("Rhythm & Syllable Stress", "Match native cadence and word emphasis", MiniGameCategory.LISTENING_EAR),

      // Category 5: Local Slang & Idioms (9 games)
      Triple("Street Slang Detective", "Decode what young locals actually say", MiniGameCategory.SLANG_DETECTIVE),
      Triple("Foodie Idioms & Metaphors", "Culinary expressions used in daily life", MiniGameCategory.SLANG_DETECTIVE),
      Triple("Texting & Chat Abbreviations", "Modern messaging expressions & reactions", MiniGameCategory.SLANG_DETECTIVE),
      Triple("Regional Dialect Spotter", "Compare expressions across different cities", MiniGameCategory.SLANG_DETECTIVE),
      Triple("Humor, Wit & Banter", "Friendly jokes & playful conversational idioms", MiniGameCategory.SLANG_DETECTIVE),
      Triple("Proverbs & Folk Wisdom", "Timeless cultural sayings & their meanings", MiniGameCategory.SLANG_DETECTIVE),
      Triple("Compliments & Warm Praise", "Sound like a native when praising friends", MiniGameCategory.SLANG_DETECTIVE),
      Triple("Empathy & Reassurance Idioms", "Comforting phrases like 'No worries!'", MiniGameCategory.SLANG_DETECTIVE),
      Triple("Celebration & Toast Mastery", "What to say at weddings, birthdays & festivals", MiniGameCategory.SLANG_DETECTIVE),

      // Category 6: Travel & Culture Survival (9 games)
      Triple("High-Speed Rail & Metro Pro", "Buy tickets, transfer lines & find platforms", MiniGameCategory.TRAVEL_SURVIVAL),
      Triple("Artisan Market Bargaining", "Negotiate prices with warmth & cultural respect", MiniGameCategory.TRAVEL_SURVIVAL),
      Triple("Dining Etiquette & Tipping", "Table manners, toasts & bill splitting", MiniGameCategory.TRAVEL_SURVIVAL),
      Triple("Pharmacy & Urgent Care", "Get remedies & explain allergies abroad", MiniGameCategory.TRAVEL_SURVIVAL),
      Triple("Local Festival & Traditions", "Navigate holidays, gifts & sacred sites", MiniGameCategory.TRAVEL_SURVIVAL),
      Triple("Taxi & Ride-Share Chat", "Confirm routes, landmarks & drop-off points", MiniGameCategory.TRAVEL_SURVIVAL),
      Triple("Museum & Heritage Explorer", "Ask about history, architecture & art", MiniGameCategory.TRAVEL_SURVIVAL),
      Triple("SIM Card & Banking Setup", "Get connected & manage money abroad", MiniGameCategory.TRAVEL_SURVIVAL),
      Triple("Making Local Friends", "Turn a brief chat into a lasting friendship", MiniGameCategory.TRAVEL_SURVIVAL)
    )

    val cefrLevels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    val langKey = langCode.lowercase().take(2)
    val lexicon = GameCurriculumAndAntiRepetitionEngine.THEMATIC_LEXICON

    fun buildValidatedRound(
      prompt: String,
      audioPhrase: String,
      phonetic: String,
      correctText: String,
      distractorPool: List<String>,
      explanation: String,
      seed: Int
    ): GameRoundItem {
      val cleanCorrect = correctText.trim().ifBlank { audioPhrase.trim() }
      val correctCanon = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(cleanCorrect)
      val distinctDistractors = distractorPool
        .map { it.trim() }
        .filter { d ->
          d.isNotBlank() &&
            !d.equals(cleanCorrect, ignoreCase = true) &&
            GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(d) != correctCanon
        }
        .distinctBy { it.lowercase() }
        .take(3)
      val rotated = mutableListOf(cleanCorrect).apply { addAll(distinctDistractors) }
      while (rotated.size < 4) {
        rotated.add("Option ${rotated.size + 1}")
      }
      val shift = ((seed % 4) + 4) % 4
      val finalOptions = List(4) { i -> rotated[(i + shift) % 4] }
      val finalCorrectIndex = finalOptions.indexOfFirst { it.equals(cleanCorrect, ignoreCase = true) }.coerceIn(0, 3)
      return GameRoundItem(
        prompt = prompt,
        audioPhrase = audioPhrase,
        phonetic = phonetic,
        options = finalOptions,
        correctIndex = finalCorrectIndex,
        explanation = explanation
      )
    }

    val allBasePools = baseRounds + grammarRounds + slangAndCultureRounds + travelRounds

    return gameBlueprints.mapIndexed { idx, (title, subtitle, category) ->
      val number = idx + 1
      val cefr = cefrLevels[idx % cefrLevels.size]
      val categoryPool = when (category) {
        MiniGameCategory.GRAMMAR_REPAIR -> grammarRounds
        MiniGameCategory.SLANG_DETECTIVE -> slangAndCultureRounds
        MiniGameCategory.TRAVEL_SURVIVAL -> travelRounds
        MiniGameCategory.LISTENING_EAR -> (travelRounds.take(2) + baseRounds.take(2))
        MiniGameCategory.SENTENCE_BUILDER -> (grammarRounds.take(2) + baseRounds.drop(2))
        MiniGameCategory.VOCAB_BLITZ -> baseRounds
      }
      val anchorBase = categoryPool[idx % categoryPool.size]
      val uniqueAnchor = buildValidatedRound(
        prompt = "[$title] ${anchorBase.prompt}",
        audioPhrase = anchorBase.audioPhrase,
        phonetic = anchorBase.phonetic,
        correctText = anchorBase.options.getOrNull(anchorBase.correctIndex) ?: anchorBase.options.first(),
        distractorPool = anchorBase.options.filterIndexed { oIdx, _ -> oIdx != anchorBase.correctIndex } +
          allBasePools.map { it.options.first() },
        explanation = anchorBase.explanation,
        seed = idx
      )

      val dynamicLexiconRounds = if (lexicon.size >= 4) {
        (0 until 3).map { step ->
          val item = lexicon[(idx * 3 + step * 7) % lexicon.size]
          val (wordTarget, sentenceTarget) = item.translations[langKey] ?: (item.enWord to item.enSentence)
          val phon = item.phonetics[langKey] ?: item.enWord.lowercase()
          val distractorItems = (1..6).map { off ->
            lexicon[(idx * 3 + step * 7 + off * 5) % lexicon.size]
          }.filter { it.enWord != item.enWord }

          when (category) {
            MiniGameCategory.GRAMMAR_REPAIR -> {
              if (langKey == "en" && step % 2 == 0) {
                buildValidatedRound(
                  prompt = "[$title • $cefr] Fix the grammar error in: \"${item.enFlawedSentence}\"",
                  audioPhrase = item.enSentence,
                  phonetic = phon,
                  correctText = item.enCorrectedWord,
                  distractorPool = listOf(item.enErrorWord) + distractorItems.map { it.enCorrectedWord } + listOf("was", "were", "has"),
                  explanation = "\"${item.enCorrectedWord}\" makes \"${item.enSentence}\" grammatically accurate.",
                  seed = idx + step
                )
              } else {
                buildValidatedRound(
                  prompt = "[$title • $cefr] Choose the natural $langName sentence using \"$wordTarget\" (${item.enWord}):",
                  audioPhrase = sentenceTarget,
                  phonetic = phon,
                  correctText = sentenceTarget,
                  distractorPool = distractorItems.map { d -> (d.translations[langKey] ?: (d.enWord to d.enSentence)).second },
                  explanation = "\"$sentenceTarget\" means \"${item.enSentence}\".",
                  seed = idx + step
                )
              }
            }
            MiniGameCategory.SENTENCE_BUILDER -> buildValidatedRound(
              prompt = "[$title • $cefr] Which $langName sentence accurately expresses: \"${item.enSentence}\"?",
              audioPhrase = sentenceTarget,
              phonetic = phon,
              correctText = sentenceTarget,
              distractorPool = distractorItems.map { d -> (d.translations[langKey] ?: (d.enWord to d.enSentence)).second },
              explanation = "Structure: \"$sentenceTarget\" ($wordTarget = ${item.enWord}).",
              seed = idx + step
            )
            MiniGameCategory.LISTENING_EAR -> buildValidatedRound(
              prompt = "[$title • $cefr] Listen to the $langName audio clue ($wordTarget) and select its English meaning:",
              audioPhrase = sentenceTarget,
              phonetic = phon,
              correctText = "${item.enWord} — ${item.enSentence}",
              distractorPool = distractorItems.map { "${it.enWord} — ${it.enSentence}" },
              explanation = "\"$sentenceTarget\" ($phon) translates to: ${item.enSentence}",
              seed = idx + step
            )
            MiniGameCategory.SLANG_DETECTIVE -> buildValidatedRound(
              prompt = "[$title • $cefr] In \"${item.domainLabel}\", which $langName expression fits \"${item.enWord}\" in context?",
              audioPhrase = sentenceTarget,
              phonetic = phon,
              correctText = wordTarget,
              distractorPool = distractorItems.map { d -> (d.translations[langKey] ?: (d.enWord to d.enSentence)).first },
              explanation = "\"$wordTarget\" means \"${item.enWord}\". Example: $sentenceTarget",
              seed = idx + step
            )
            MiniGameCategory.TRAVEL_SURVIVAL -> buildValidatedRound(
              prompt = "[$title • $cefr] You see or hear \"$wordTarget\" while out and about. What does it mean?",
              audioPhrase = wordTarget,
              phonetic = phon,
              correctText = item.enWord,
              distractorPool = distractorItems.map { it.enWord },
              explanation = "\"$wordTarget\" (${item.domainLabel}) means \"${item.enWord}\".",
              seed = idx + step
            )
            MiniGameCategory.VOCAB_BLITZ -> buildValidatedRound(
              prompt = "[$title • $cefr] Quick Match (${item.domainLabel}): Which $langName word means \"${item.enWord}\"?",
              audioPhrase = wordTarget,
              phonetic = phon,
              correctText = wordTarget,
              distractorPool = distractorItems.map { d -> (d.translations[langKey] ?: (d.enWord to d.enSentence)).first },
              explanation = "\"$wordTarget\" ($phon) means \"${item.enWord}\" — $sentenceTarget",
              seed = idx + step
            )
          }
        }
      } else {
        categoryPool.drop(1)
      }

      val roundsForGame = listOf(uniqueAnchor) + dynamicLexiconRounds
      LanguageMiniGame(
        id = "game_${langCode}_$number",
        number = number,
        title = "$title ($langName)",
        subtitle = subtitle,
        category = category,
        cefrLevel = cefr,
        xpReward = 35 + ((idx % 4) * 10),
        timeLimitSeconds = 45,
        rounds = roundsForGame
      )
    }
  }

  /**
   * Provides authentic Local Slang, Cultural Traditions, Native Idioms, and Travel Conversations
   * for the active language.
   */
  fun getLocalSlangAndTraditions(langCode: String): List<LocalSlangTraditionItem> {
    val pack = getPack(langCode)
    val langName = Language.findByCode(langCode).name
    return listOf(
      LocalSlangTraditionItem(
        id = "slang_1_$langCode",
        phrase = pack.slangCool.first,
        phonetic = pack.slangCool.second,
        literalMeaning = "Colloquial exclamation of excitement",
        realMeaning = pack.slangCool.third,
        culturalTraditionNote = "Used daily among friends and peers in $langName-speaking regions to react enthusiastically to good news or plans.",
        exampleUsage = "${pack.slangCool.first} — ${pack.grammarFuture.first}",
        categoryTag = "Local Slang"
      ),
      LocalSlangTraditionItem(
        id = "slang_2_$langCode",
        phrase = pack.slangNoWorry.first,
        phonetic = pack.slangNoWorry.second,
        literalMeaning = "Nothing happens / Don't trouble yourself",
        realMeaning = pack.slangNoWorry.third,
        culturalTraditionNote = "Reflects the warm, relationship-first communication style where reassuring a conversation partner builds immediate rapport.",
        exampleUsage = pack.slangNoWorry.first,
        categoryTag = "Native Idiom"
      ),
      LocalSlangTraditionItem(
        id = "trad_1_$langCode",
        phrase = pack.festivalToast.first,
        phonetic = pack.festivalToast.second,
        literalMeaning = "To health, joy, and togetherness",
        realMeaning = pack.festivalToast.third,
        culturalTraditionNote = "In $langName dining tradition, making eye contact around the table when offering this toast is a cherished sign of respect and friendship.",
        exampleUsage = pack.festivalToast.first,
        categoryTag = "Cultural Tradition"
      ),
      LocalSlangTraditionItem(
        id = "trad_2_$langCode",
        phrase = pack.deliciousFood.first,
        phonetic = pack.deliciousFood.second,
        literalMeaning = "High praise for regional cuisine",
        realMeaning = pack.deliciousFood.third,
        culturalTraditionNote = "Complimenting regional home cooking or street food with this phrase delights local hosts and chefs.",
        exampleUsage = pack.deliciousFood.first,
        categoryTag = "Cultural Tradition"
      ),
      LocalSlangTraditionItem(
        id = "travel_1_$langCode",
        phrase = pack.askDirections.first,
        phonetic = pack.askDirections.second,
        literalMeaning = "Polite street navigation inquiry",
        realMeaning = pack.askDirections.third,
        culturalTraditionNote = "Always begin with a polite attention-getter before asking for transit or street directions.",
        exampleUsage = "${pack.askDirections.first} — ${pack.trainStation.first}",
        categoryTag = "Travel Conversation"
      ),
      LocalSlangTraditionItem(
        id = "travel_2_$langCode",
        phrase = pack.howMuch.first,
        phonetic = pack.howMuch.second,
        literalMeaning = "Polite market & artisan price inquiry",
        realMeaning = pack.howMuch.third,
        culturalTraditionNote = "In open-air markets and artisan stalls, asking about the craft's story before asking the price is appreciated.",
        exampleUsage = pack.howMuch.first,
        categoryTag = "Travel Conversation"
      )
    )
  }
}
