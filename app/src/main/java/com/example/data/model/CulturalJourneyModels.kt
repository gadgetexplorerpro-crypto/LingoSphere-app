package com.example.data.model

data class CulturalVocabCard(
  val phrase: String,
  val phonetic: String,
  val meaning: String,
  val culturalNote: String
)

data class CulturalListeningExercise(
  val settingTitle: String,
  val audioScriptInTarget: String,
  val phoneticHint: String,
  val question: String,
  val options: List<String>,
  val correctIndex: Int,
  val culturalExplanation: String
)

data class CulturalSpeakingSituation(
  val situationTitle: String,
  val scenarioSetting: String,
  val localSpeakerLine: String,
  val localSpeakerTranslation: String,
  val suggestedResponseInTarget: String,
  val suggestedPhonetic: String,
  val suggestedMeaning: String,
  val etiquetteTip: String
)

data class CulturalEtiquetteItem(
  val category: String,
  val iconEmoji: String,
  val doRule: String,
  val dontRule: String,
  val culturalWhy: String
)

data class VirtualTravelDestination(
  val id: String,
  val landmarkName: String,
  val city: String,
  val badgeEmoji: String,
  val sceneDescription: String,
  val localPersonaName: String,
  val localPersonaRole: String,
  val openingGreetingInTarget: String,
  val openingGreetingPhonetic: String,
  val openingGreetingTranslation: String,
  val streetPhraseInTarget: String,
  val streetPhrasePhonetic: String,
  val streetPhraseMeaning: String
)

data class RegionalDialectComparison(
  val id: String,
  val conceptTitle: String,
  val regionAName: String,
  val regionAFlag: String,
  val regionAPhrase: String,
  val regionAPhonetic: String,
  val regionBName: String,
  val regionBFlag: String,
  val regionBPhrase: String,
  val regionBPhonetic: String,
  val culturalNuance: String
)

data class CultureMission(
  val id: String,
  val title: String,
  val description: String,
  val xpReward: Int,
  val missionType: String // "VOCAB", "LISTENING", "SPEAKING", "ETIQUETTE", "QUIZ"
)

data class CultureQuizQuestion(
  val id: String,
  val question: String,
  val options: List<String>,
  val correctIndex: Int,
  val explanation: String
)

data class CountryJourneyStage(
  val stageNumber: Int,
  val title: String,
  val subtitle: String,
  val culturalContext: String,
  val keyPhrases: List<CulturalVocabCard>,
  val listeningExercise: CulturalListeningExercise,
  val speakingChallenge: CulturalSpeakingSituation,
  val culturalInsightTip: String,
  val xpReward: Int = 50
)

data class CountryCultureJourney(
  val countryCode: String,
  val countryName: String,
  val flagEmoji: String,
  val languageCode: String,
  val capitalCity: String,
  val cefrRange: String,
  val passportStampCode: String,
  val motto: String,
  val culturalOverview: String,
  val survivalVocab: List<CulturalVocabCard>,
  val listeningExercise: CulturalListeningExercise,
  val speakingSituation: CulturalSpeakingSituation,
  val etiquetteRules: List<CulturalEtiquetteItem>,
  val virtualLandmarks: List<VirtualTravelDestination>,
  val regionalDialects: List<RegionalDialectComparison>,
  val missions: List<CultureMission>,
  val quizBattleQuestions: List<CultureQuizQuestion>,
  val stages: List<CountryJourneyStage> = emptyList()
)

data class VoiceDiaryAiEvaluation(
  val correctedText: String,
  val fluencyScore: Int,
  val grammarExplanation: String,
  val naturalIdiomSuggestion: String,
  val encouragement: String
)

data class CulturalAdvisorResponse(
  val etiquetteAdvice: String,
  val essentialPhraseInTarget: String,
  val phonetics: String,
  val phraseTranslation: String,
  val tabooWarning: String
)

object CulturalJourneyRepository {

  /**
   * Helper that builds the 10 progressive cultural immersion stages required by the specification.
   */
  private fun build10Stages(
    countryCode: String,
    countryName: String,
    langCode: String,
    langName: String,
    capital: String,
    greetingInTarget: String,
    greetingPhonetic: String,
    greetingMeaning: String,
    foodPhraseInTarget: String,
    foodPhrasePhonetic: String,
    foodPhraseMeaning: String,
    marketPhraseInTarget: String,
    marketPhrasePhonetic: String,
    marketPhraseMeaning: String,
    transitPhraseInTarget: String,
    transitPhrasePhonetic: String,
    transitPhraseMeaning: String,
    etiquettePhraseInTarget: String,
    etiquettePhrasePhonetic: String,
    etiquettePhraseMeaning: String,
    festivalName: String,
    festivalPhraseInTarget: String,
    festivalPhrasePhonetic: String,
    festivalPhraseMeaning: String,
    dialectPhraseA: String,
    dialectPhraseB: String,
    dialectRegionA: String,
    dialectRegionB: String
  ): List<CountryJourneyStage> {
    return listOf(
      CountryJourneyStage(
        stageNumber = 1,
        title = "Stage 1: Greetings & Introductions",
        subtitle = "First impressions, titles, bows & polite handshakes",
        culturalContext = "In $countryName, the opening greeting sets the emotional tone for all future interactions. Politeness and genuine attentiveness are deeply valued.",
        keyPhrases = listOf(
          CulturalVocabCard(greetingInTarget, greetingPhonetic, greetingMeaning, "Spoken warmly with appropriate eye contact and body language."),
          CulturalVocabCard("Nice to meet you", "Polite opening", "Expressing sincere pleasure at connecting", "Used right after your name is shared.")
        ),
        listeningExercise = CulturalListeningExercise(
          settingTitle = "Meeting a Host in $capital",
          audioScriptInTarget = greetingInTarget,
          phoneticHint = greetingPhonetic,
          question = "What is the primary cultural purpose of this greeting in $countryName?",
          options = listOf("To establish mutual respect and warm rapport", "To inquire about train schedules", "To decline an invitation", "To finalize a purchase"),
          correctIndex = 0,
          culturalExplanation = "Respectful openings in $countryName convey appreciation and good intentions."
        ),
        speakingChallenge = CulturalSpeakingSituation(
          situationTitle = "Greeting a Local Resident in $capital",
          scenarioSetting = "You meet a friendly local at a community gathering in $capital.",
          localSpeakerLine = greetingInTarget,
          localSpeakerTranslation = "Hello and welcome! It is wonderful to meet you.",
          suggestedResponseInTarget = greetingInTarget,
          suggestedPhonetic = greetingPhonetic,
          suggestedMeaning = greetingMeaning,
          etiquetteTip = "Smile sincerely and speak clearly at a natural, calm pace."
        ),
        culturalInsightTip = "Notice how locals modulate their tone. Acknowledging people with a respectful nod or greeting is warmly appreciated everywhere in $countryName.",
        xpReward = 50
      ),
      CountryJourneyStage(
        stageNumber = 2,
        title = "Stage 2: Everyday Expressions",
        subtitle = "Gratitude, apologies, and friendly conversational glue",
        culturalContext = "Everyday filler phrases, polite acknowledgments, and gracious apologies are the building blocks of natural fluency in $countryName.",
        keyPhrases = listOf(
          CulturalVocabCard("Thank you very much", "Polite gratitude", "Expressing heartfelt appreciation", "Essential whenever anyone assists you."),
          CulturalVocabCard("Excuse me / Pardon me", "Polite notice", "Used before asking a question or navigating tight spaces", "Shows consideration for shared public space.")
        ),
        listeningExercise = CulturalListeningExercise(
          settingTitle = "Overhearing a Polite Exchange",
          audioScriptInTarget = "Excuse me, thank you very much!",
          phoneticHint = "Clear polite rhythm",
          question = "When should you use this combination of phrases in $countryName?",
          options = listOf("When someone holds a door or gives directions", "Only on official public holidays", "When demanding a refund", "Never in public"),
          correctIndex = 0,
          culturalExplanation = "Combining gentle apologies with thanks is universal polite etiquette."
        ),
        speakingChallenge = CulturalSpeakingSituation(
          situationTitle = "Thanking a Helpful Clerk",
          scenarioSetting = "A shop assistant takes extra time to find what you were searching for.",
          localSpeakerLine = "Here you are, enjoy your time in our city!",
          localSpeakerTranslation = "Here is what you needed!",
          suggestedResponseInTarget = "Thank you so much for your kind help!",
          suggestedPhonetic = "Warm and appreciative tone",
          suggestedMeaning = "Expressing sincere thanks",
          etiquetteTip = "Use positive eye contact and maintain an open posture."
        ),
        culturalInsightTip = "Politeness is never seen as weakness; it is a sign of emotional maturity and cultural polish.",
        xpReward = 50
      ),
      CountryJourneyStage(
        stageNumber = 3,
        title = "Stage 3: Food & Dining Customs",
        subtitle = "Table etiquette, ordering, toasting & appreciating local cuisine",
        culturalContext = "Dining in $countryName is a communal celebration of culinary heritage. Knowing how to order, toast, and compliment the chef transforms every meal into a memorable bond.",
        keyPhrases = listOf(
          CulturalVocabCard(foodPhraseInTarget, foodPhrasePhonetic, foodPhraseMeaning, "Spoken when ordering or tasting traditional specialties."),
          CulturalVocabCard("Cheers / Good health!", "Traditional toast", "Celebratory toast with dining companions", "Raise your glass and make friendly eye contact.")
        ),
        listeningExercise = CulturalListeningExercise(
          settingTitle = "Ordering at a Traditional Eatery",
          audioScriptInTarget = foodPhraseInTarget,
          phoneticHint = foodPhrasePhonetic,
          question = "What is the guest ordering or asking for in this clip?",
          options = listOf("Asking for the specialty of the house", "Inquiring about luggage storage", "Asking for the airport bus", "Requesting a doctor"),
          correctIndex = 0,
          culturalExplanation = "Asking for regional recommendations shows pride in the local culinary traditions."
        ),
        speakingChallenge = CulturalSpeakingSituation(
          situationTitle = "Complimenting the Chef or Server",
          scenarioSetting = "You have just tasted a signature dish that was exceptionally delicious.",
          localSpeakerLine = "How is everything tasting tonight?",
          localSpeakerTranslation = "Are you enjoying your meal?",
          suggestedResponseInTarget = foodPhraseInTarget,
          suggestedPhonetic = foodPhrasePhonetic,
          suggestedMeaning = foodPhraseMeaning,
          etiquetteTip = "Compliment specific flavors or the presentation rather than just saying 'good'."
        ),
        culturalInsightTip = "Take your time with meals. Dining is an opportunity for storytelling and building long-lasting friendships.",
        xpReward = 50
      ),
      CountryJourneyStage(
        stageNumber = 4,
        title = "Stage 4: Shopping & Local Markets",
        subtitle = "Bargaining, artisan crafts, sizing, payment & market etiquette",
        culturalContext = "Traditional markets in $countryName offer an authentic pulse of daily life. Interacting with local artisans requires warmth, respectful bargaining (where customary), and polite handling of goods.",
        keyPhrases = listOf(
          CulturalVocabCard(marketPhraseInTarget, marketPhrasePhonetic, marketPhraseMeaning, "Used to ask prices and appreciate handmade craftsmanship."),
          CulturalVocabCard("Could I take a closer look please?", "Polite request", "Politely asking permission before handling delicate items", "Artisans appreciate careful handling.")
        ),
        listeningExercise = CulturalListeningExercise(
          settingTitle = "At an Artisan Market Stall",
          audioScriptInTarget = marketPhraseInTarget,
          phoneticHint = marketPhrasePhonetic,
          question = "What is the shopper inquiring about at the market stall?",
          options = listOf("The price and material of the handmade item", "The departure time of a ferry", "How to apply for a credit card", "The weather forecast"),
          correctIndex = 0,
          culturalExplanation = "Always greet the vendor before asking prices to establish friendly respect."
        ),
        speakingChallenge = CulturalSpeakingSituation(
          situationTitle = "Purchasing a Traditional Souvenir",
          scenarioSetting = "You are at a local market in $capital speaking with the craftsperson.",
          localSpeakerLine = "This is handcrafted using local cedar and natural dyes.",
          localSpeakerTranslation = "This is made by hand with regional materials.",
          suggestedResponseInTarget = marketPhraseInTarget,
          suggestedPhonetic = marketPhrasePhonetic,
          suggestedMeaning = marketPhraseMeaning,
          etiquetteTip = "Hand money or cards using both hands or place on the change tray as customary."
        ),
        culturalInsightTip = "Never begin by aggressively demanding discounts. Compliment the craft first; vendors will often offer their best price spontaneously.",
        xpReward = 50
      ),
      CountryJourneyStage(
        stageNumber = 5,
        title = "Stage 5: Transportation & Directions",
        subtitle = "Trains, subways, taxis, asking for help & navigating streets",
        culturalContext = "Public transit in $countryName is efficient and orderly. Knowing transit vocabulary and asking directions politely ensures smooth travels across bustling cities.",
        keyPhrases = listOf(
          CulturalVocabCard(transitPhraseInTarget, transitPhrasePhonetic, transitPhraseMeaning, "Crucial phrase for navigation and purchasing tickets."),
          CulturalVocabCard("Which platform for the central station?", "Transit query", "Asking transit staff for directional help", "Look for numbered boards and polite staff.")
        ),
        listeningExercise = CulturalListeningExercise(
          settingTitle = "Platform Announcement in $capital",
          audioScriptInTarget = transitPhraseInTarget,
          phoneticHint = transitPhrasePhonetic,
          question = "What instruction is being given in this transit notice?",
          options = listOf("Directions to the platform or transfer gate", "Notice of a restaurant opening", "A lost wallet inquiry", "A sports score update"),
          correctIndex = 0,
          culturalExplanation = "Transit announcements follow standardized, polite phrasing."
        ),
        speakingChallenge = CulturalSpeakingSituation(
          situationTitle = "Asking a Commuter for Directions",
          scenarioSetting = "You are looking for the historic district exit at the main transit hub.",
          localSpeakerLine = "Can I help you? You look like you are searching for a line.",
          localSpeakerTranslation = "Need any help with directions?",
          suggestedResponseInTarget = transitPhraseInTarget,
          suggestedPhonetic = transitPhrasePhonetic,
          suggestedMeaning = transitPhraseMeaning,
          etiquetteTip = "Point with an open hand or fingers together rather than an aggressive single finger."
        ),
        culturalInsightTip = "Keep phone audio muted and speak at low volume on trains and buses in $countryName out of respect for resting commuters.",
        xpReward = 50
      ),
      CountryJourneyStage(
        stageNumber = 6,
        title = "Stage 6: Social Etiquette & Taboos",
        subtitle = "Visiting homes, gift giving, body language & unspoken rules",
        culturalContext = "Every society has sacred social norms. From taking off shoes to choosing the right guest gift, mastering these subtleties protects you from unintended blunders.",
        keyPhrases = listOf(
          CulturalVocabCard(etiquettePhraseInTarget, etiquettePhrasePhonetic, etiquettePhraseMeaning, "Spoken when entering homes or receiving hospitality."),
          CulturalVocabCard("A small token of my appreciation", "Gift presentation", "Polite modesty when handing a host gift", "Always wrap gifts neatly.")
        ),
        listeningExercise = CulturalListeningExercise(
          settingTitle = "Arriving at a Host's Home",
          audioScriptInTarget = etiquettePhraseInTarget,
          phoneticHint = etiquettePhrasePhonetic,
          question = "What does the speaker convey upon entering the private home?",
          options = listOf("Polite gratitude for being invited into their private space", "A complaint about the weather", "An order for room service", "A demand to leave"),
          correctIndex = 0,
          culturalExplanation = "Acknowledging the host's hospitality honors their generosity."
        ),
        speakingChallenge = CulturalSpeakingSituation(
          situationTitle = "Presenting a Gift to Your Host",
          scenarioSetting = "You have been invited for weekend dinner at a local family's home.",
          localSpeakerLine = "Please come inside and make yourself at home!",
          localSpeakerTranslation = "Welcome into our home!",
          suggestedResponseInTarget = etiquettePhraseInTarget,
          suggestedPhonetic = etiquettePhrasePhonetic,
          suggestedMeaning = etiquettePhraseMeaning,
          etiquetteTip = "Present gifts with both hands and never insist on having them opened immediately."
        ),
        culturalInsightTip = "Observe the doorway: if there is an elevated entryway or slipper rack, always remove outdoor shoes immediately.",
        xpReward = 50
      ),
      CountryJourneyStage(
        stageNumber = 7,
        title = "Stage 7: Festivals & Cultural Traditions",
        subtitle = "National holidays, seasonal ceremonies, music & folk art",
        culturalContext = "Festivals celebrate the historical identity and shared joy of $countryName. Participating in traditional festivals connects you directly with the soul of the people.",
        keyPhrases = listOf(
          CulturalVocabCard(festivalPhraseInTarget, festivalPhrasePhonetic, festivalPhraseMeaning, "Traditional festival greeting or joyful exclamation."),
          CulturalVocabCard("Wishing you peace, joy and abundance!", "Seasonal blessing", "Spoken during key holiday seasons to friends and elders", "Expresses goodwill.")
        ),
        listeningExercise = CulturalListeningExercise(
          settingTitle = "Celebration of $festivalName",
          audioScriptInTarget = festivalPhraseInTarget,
          phoneticHint = festivalPhrasePhonetic,
          question = "What is celebrated during this traditional event?",
          options = listOf("Seasonal harvest, heritage and community unity", "A corporate tax deadline", "A silent exam week", "A train departure"),
          correctIndex = 0,
          culturalExplanation = "Traditional festivals in $countryName unite generations in shared song, food, and dance."
        ),
        speakingChallenge = CulturalSpeakingSituation(
          situationTitle = "Greeting Friends at $festivalName",
          scenarioSetting = "You are gathered at the town square illuminated by lanterns and festive lights.",
          localSpeakerLine = "Happy $festivalName! Let's celebrate together!",
          localSpeakerTranslation = "Joyous holiday celebrations to you and your family!",
          suggestedResponseInTarget = festivalPhraseInTarget,
          suggestedPhonetic = festivalPhrasePhonetic,
          suggestedMeaning = festivalPhraseMeaning,
          etiquetteTip = "Wear festive or respectful attire and join in shared clapping or dances when invited."
        ),
        culturalInsightTip = "Learn the story behind $festivalName. Locals are delighted when visitors understand the historical significance of their celebrations.",
        xpReward = 50
      ),
      CountryJourneyStage(
        stageNumber = 8,
        title = "Stage 8: Real-Life Daily Conversations",
        subtitle = "Casual small talk, weather, hobbies, and making local friends",
        culturalContext = "Moving from formal textbook dialogue to authentic colloquial conversation is the true test of fluency. Small talk opens doors to genuine friendships.",
        keyPhrases = listOf(
          CulturalVocabCard("What do you like to do on weekends?", "Social query", "Inquiring about personal interests politely", "Shows active interest in the other person."),
          CulturalVocabCard("That sounds really fascinating!", "Active listening", "Encouraging your partner to elaborate", "Nodding and affirmative sounds show active listening.")
        ),
        listeningExercise = CulturalListeningExercise(
          settingTitle = "Casual Chat over Coffee",
          audioScriptInTarget = "The weather today is truly beautiful, isn't it?",
          phoneticHint = "Relaxed conversational tone",
          question = "What conversational technique is being used here?",
          options = listOf("Using the pleasant weather as a natural conversational icebreaker", "A formal legal dispute", "A complaint to an airline", "An academic lecture"),
          correctIndex = 0,
          culturalExplanation = "Observing shared surroundings is the most comfortable way to break the ice in $countryName."
        ),
        speakingChallenge = CulturalSpeakingSituation(
          situationTitle = "Chatting with a New Friend",
          scenarioSetting = "You are sitting next to a university student at a lively neighborhood café.",
          localSpeakerLine = "What places in our country have you enjoyed visiting the most?",
          localSpeakerTranslation = "What has been your favorite place so far?",
          suggestedResponseInTarget = "I love the warm hospitality, the historic streets, and the delicious food!",
          suggestedPhonetic = "Natural upbeat cadence",
          suggestedMeaning = "Expressing genuine admiration for their country",
          etiquetteTip = "Be open and share positive reflections on your experiences."
        ),
        culturalInsightTip = "Ask open-ended questions about local culture; people love recommending their favorite hidden gems.",
        xpReward = 50
      ),
      CountryJourneyStage(
        stageNumber = 9,
        title = "Stage 9: Regional Expressions & Dialects",
        subtitle = "$dialectRegionA vs. $dialectRegionB idioms, accents & slang",
        culturalContext = "No language is uniform. In $countryName, rich regional dialects and slang reveal a person's roots, humor, and local pride.",
        keyPhrases = listOf(
          CulturalVocabCard(dialectPhraseA, "Dialect A", "$dialectRegionA expression", "Used in the capital and northern regions."),
          CulturalVocabCard(dialectPhraseB, "Dialect B", "$dialectRegionB expression", "Used in southern and coastal regions with unique rhythm.")
        ),
        listeningExercise = CulturalListeningExercise(
          settingTitle = "Comparing Dialects Across Regions",
          audioScriptInTarget = "$dialectPhraseA vs. $dialectPhraseB",
          phoneticHint = "Distinct regional pitch and vowel color",
          question = "How do these two expressions relate culturally?",
          options = listOf("They express similar ideas using distinct regional slang and cadence", "They are unrelated mathematical terms", "They are insults", "They are only spoken by infants"),
          correctIndex = 0,
          culturalExplanation = "Understanding regional variations proves deep cultural immersion."
        ),
        speakingChallenge = CulturalSpeakingSituation(
          situationTitle = "Recognizing a Local Slang Term",
          scenarioSetting = "A vendor in $dialectRegionB uses a colorful regional idiom.",
          localSpeakerLine = "$dialectPhraseB! Have an amazing day!",
          localSpeakerTranslation = "Regional expression wishing you well!",
          suggestedResponseInTarget = dialectPhraseA,
          suggestedPhonetic = "Warm reciprocal phrase",
          suggestedMeaning = "Thank you friend, take care as well!",
          etiquetteTip = "Appreciate regional words with curiosity without mocking the accent."
        ),
        culturalInsightTip = "Mentioning that you noticed their melodic regional accent is often considered a delightful compliment by proud locals.",
        xpReward = 50
      ),
      CountryJourneyStage(
        stageNumber = 10,
        title = "Stage 10: Cultural Mastery Challenge",
        subtitle = "Full immersion capstone, nuanced dilemmas & diplomat mastery",
        culturalContext = "Congratulations on reaching Stage 10! This capstone challenge tests your holistic cultural intelligence: blending respect, spontaneous conversation, dining etiquette, and deep cross-cultural empathy.",
        keyPhrases = listOf(
          CulturalVocabCard("It has been a true honor to immerse myself in your culture", "Capstone respect", "Spoken to mentors, hosts, and colleagues upon milestone completion", "Deepest level of cultural appreciation."),
          CulturalVocabCard("Harmony and mutual learning connect our worlds", "Philosophical bridge", "Celebrating international friendship", "Reflects true diplomatic empathy.")
        ),
        listeningExercise = CulturalListeningExercise(
          settingTitle = "Diplomatic Exchange Ceremony in $capital",
          audioScriptInTarget = "Thank you for honoring our traditions and speaking our language with such genuine respect.",
          phoneticHint = "Formal ceremonial tone",
          question = "What does this milestone acknowledgment represent in $countryName?",
          options = listOf("Recognition as a culturally fluent international ambassador", "A cancellation of your passport", "An invitation to leave immediately", "A reminder to pay transit fares"),
          correctIndex = 0,
          culturalExplanation = "Mastering both language and etiquette earns the deepest esteem from native speakers."
        ),
        speakingChallenge = CulturalSpeakingSituation(
          situationTitle = "Delivering Your Cultural Reflection",
          scenarioSetting = "You are delivering a brief cultural toast at a farewell gathering in $capital.",
          localSpeakerLine = "We are so proud of how deeply you have learned our customs!",
          localSpeakerTranslation = "We celebrate your dedication to our culture!",
          suggestedResponseInTarget = "Thank you with all my heart. I will carry the spirit of your hospitality wherever I go.",
          suggestedPhonetic = "Poised, resonant and heartfelt delivery",
          suggestedMeaning = "Enduring gratitude and cultural connection",
          etiquetteTip = "Stand tall, maintain gentle eye contact with the entire room, and raise your glass with both hands."
        ),
        culturalInsightTip = "Cultural mastery is not an endpoint—it is an ongoing posture of curiosity, humility, and warmth toward every person you meet.",
        xpReward = 100
      )
    )
  }

  // ==========================================================================
  // CURATED JOURNEYS FOR 12 CORE COUNTRIES & GLOBAL FALLBACK
  // ==========================================================================

  private val curatedJourneys: List<CountryCultureJourney> = listOf(
    // 1. JAPAN (ja)
    CountryCultureJourney(
      countryCode = "JP",
      countryName = "Japan",
      flagEmoji = "🇯🇵",
      languageCode = "ja",
      capitalCity = "Tokyo",
      cefrRange = "A1 – C1 Cultural Immersion",
      passportStampCode = "PASSPORT_JP",
      motto = "Harmony (Wa), Respect & Seasonal Mindfulness",
      culturalOverview = "Japanese communication blends spoken politeness levels (Keigo) with non-verbal attentiveness, bowing etiquette, and deep respect for shared public spaces.",
      survivalVocab = listOf(
        CulturalVocabCard("よろしくお願いします", "Yoroshiku onegaishimasu", "I look forward to connecting/working with you", "Used when meeting someone new or making a polite request."),
        CulturalVocabCard("いただきます", "Itadakimasu", "Humbly receiving this meal", "Said with palms pressed before eating to honor nature and cook."),
        CulturalVocabCard("すみません", "Sumimasen", "Excuse me / I am sorry / Thank you", "The universal Japanese phrase of polite consideration.")
      ),
      listeningExercise = CulturalListeningExercise("In a Tokyo Café", "いらっしゃいませ！何名様ですか？", "Irasshaimase! Nan-mei-sama desu ka?", "What is the staff asking?", listOf("How many guests are in your party?", "Do you have an umbrella?", "Is it raining outside?", "Are you staying overnight?"), 0, "Standard warm restaurant greeting across Japan."),
      speakingSituation = CulturalSpeakingSituation("Ordering Matcha in Kyoto", "You are at a traditional tea house in Gion.", "ご注文はお決まりですか？", "Are you ready to order?", "抹茶と和菓子をお願いします", "Matcha to wagashi wo onegaishimasu", "Matcha and traditional sweet, please", "Speak in a calm, clear voice and bow lightly."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("Footwear & Genkan", "👟", "Remove shoes at the genkan entryway and step directly onto tatami.", "Never step on the wooden threshold or wear shoes indoors.", "Preserves sacred domestic purity and clean flooring."),
        CulturalEtiquetteItem("Chopsticks (Hashi)", "🥢", "Rest chopsticks on the hashioki stand beside your bowl.", "Never stick chopsticks vertically into a bowl of rice (tsukitate-bashi).", "Vertical chopsticks resemble funeral incense offerings and are strictly taboo.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("jp_sensoji", "Sensō-ji Temple", "Tokyo", "🏮", "Tokyo's oldest Buddhist temple framed by the giant red Kaminarimon lantern.", "Yuki Tanaka", "Tokyo Historic Guide", "こんにちは！浅草寺へようこそ！", "Konnichiwa! Sensō-ji e yōkoso!", "Hello! Welcome to Sensō-ji!", "仲見世通りを歩きましょう", "Nakamise-dōri wo arukimashou", "Let's stroll down Nakamise street!"),
        VirtualTravelDestination("jp_fushimi", "Fushimi Inari-Taisha", "Kyoto", "⛩️", "Thousands of vermilion torii gates winding up the sacred mountain of Kyoto.", "Kenji Morimoto", "Kyoto Shinto Scholar", "ようこそ京都へ。鳥居をくぐりましょう。", "Yōkoso Kyōto e. Torii wo kugurimashou.", "Welcome to Kyoto. Let us pass under the torii.", "一期一会", "Ichigo ichie", "Once in a lifetime encounter")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("jp_kanto_kansai", "Tokyo vs. Osaka Hospitality", "Tokyo (Kanto)", "🇯🇵", "ありがとうございます", "Arigatō gozaimasu", "Osaka (Kansai)", "🏯", "おおきに！", "Ōkini!", "Kansai dialect is famous for rapid humor, warmth, and friendly banter.")
      ),
      missions = listOf(
        CultureMission("MISSION_JP_VOCAB", "Master 5 Essential Japanese Phrases", "Practice survival Keigo vocabulary.", 50, "VOCAB"),
        CultureMission("MISSION_JP_SPEAK", "Bow & Greet in Kyoto", "Complete the Kyoto tea house speaking challenge.", 75, "SPEAKING"),
        CultureMission("MISSION_JP_STAMP", "Earn Your Japan Visa Stamp", "Achieve top score on the Japan Culture Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("jp_q1", "What should you do before entering a traditional tatami room in Japan?", listOf("Remove your shoes and step onto slippers or bare socks", "Keep your outdoor boots on", "Run across the mats quickly", "Leave your luggage on the floor outside"), 0, "Tatami mats are delicate and strictly kept immaculate."),
        CultureQuizQuestion("jp_q2", "Why is tipping generally not accepted in Japan?", listOf("Excellent service is considered an intrinsic standard, not an extra fee", "Tips are illegal by tax code", "Servers are offended by paper money", "Only coins are allowed"), 0, "Hospitality (Omotenashi) is delivered wholeheartedly as standard.")
      ),
      stages = build10Stages(
        countryCode = "JP",
        countryName = "Japan",
        langCode = "ja",
        langName = "Japanese",
        capital = "Tokyo",
        greetingInTarget = "はじめまして、よろしくお願いします",
        greetingPhonetic = "Hajimemashite, yoroshiku onegaishimasu",
        greetingMeaning = "Pleased to meet you; I look forward to connecting with you",
        foodPhraseInTarget = "おすすめは何ですか？これをお願いします",
        foodPhrasePhonetic = "Osusume wa nan desu ka? Kore wo onegaishimasu",
        foodPhraseMeaning = "What is your recommendation? I would like this please",
        marketPhraseInTarget = "これはいくらですか？手作りですか？",
        marketPhrasePhonetic = "Kore wa ikura desu ka? Tedukuri desu ka?",
        marketPhraseMeaning = "How much is this? Is it handcrafted?",
        transitPhraseInTarget = "東京駅行きの電車はどれですか？",
        transitPhrasePhonetic = "Tōkyō-eki yuki no densha wa dore desu ka?",
        transitPhraseMeaning = "Which train is heading toward Tokyo Station?",
        etiquettePhraseInTarget = "お邪魔します、お招きいただきありがとうございます",
        etiquettePhrasePhonetic = "Ojama shimasu, omaneki itadaki arigatō gozaimasu",
        etiquettePhraseMeaning = "Pardon my intrusion; thank you so much for inviting me",
        festivalName = "Gion Matsuri",
        festivalPhraseInTarget = "お祭りおめでとうございます！",
        festivalPhrasePhonetic = "Omatsuri omedetō gozaimasu!",
        festivalPhraseMeaning = "Happy festival celebrations!",
        dialectPhraseA = "とてもおいしいです (Tokyo)",
        dialectPhraseB = "めっちゃうまいわ！ (Osaka)",
        dialectRegionA = "Tokyo Kanto",
        dialectRegionB = "Osaka Kansai"
      )
    ),

    // 2. UNITED KINGDOM (GB)
    CountryCultureJourney(
      countryCode = "GB",
      countryName = "United Kingdom",
      flagEmoji = "🇬🇧",
      languageCode = "en",
      capitalCity = "London",
      cefrRange = "A1 – C2 British Immersion",
      passportStampCode = "PASSPORT_GB",
      motto = "Courtesy, Queuing, Wit & Understatement",
      culturalOverview = "British culture treasures understated politeness, dry wit, queuing orderliness, cozy pub camaraderie, and diverse regional accents from Edinburgh to London.",
      survivalVocab = listOf(
        CulturalVocabCard("Cheers / Many thanks", "Cheers", "Universal friendly thanks / toast", "Used across Britain for thanks, toasting, or saying goodbye."),
        CulturalVocabCard("Mind the gap", "Mind the gap", "Pay attention to the space between train and platform", "The iconic British transit announcement known globally."),
        CulturalVocabCard("I'm terribly sorry", "Polite British apology", "Polite acknowledgment even when someone bumps into you", "Core to British social harmony.")
      ),
      listeningExercise = CulturalListeningExercise("London Underground", "Mind the gap between the train and the platform.", "Standard British RP", "What is the recorded safety advisory?", listOf("Watch your footing when boarding", "The train is delayed", "Purchase a ticket immediately", "Quiet carriage ahead"), 0, "Famous safety announcement on the London Tube."),
      speakingSituation = CulturalSpeakingSituation("Ordering Tea in an English Tea Room", "At a cozy tearoom in Covent Garden, London.", "Would you care for Earl Grey or English Breakfast with your scones?", "Tea choice request", "English Breakfast with a splash of milk, please.", "Polite British cadence", "Selecting classic tea with milk", "Keep your tone friendly and polite."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("The British Queue", "🚶", "Always join the back of the queue and wait patiently.", "Never push in or jump the line under any circumstances.", "Queuing is an almost sacred expression of fairness and civility in the UK."),
        CulturalEtiquetteItem("Pub Etiquette", "🍺", "Order and pay directly at the bar; return empty glasses if practical.", "Do not wait at tables expecting waiter service in standard pubs.", "British pubs operate on friendly bar counter service and round-buying.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("gb_bigben", "Big Ben & Westminster", "London", "🕰️", "The iconic neo-gothic clock tower beside the River Thames and Parliament.", "Oliver Wright", "Westminster Historian", "Good day! Welcome to historic London.", "British RP accent", "Greeting in London", "Fancy a cuppa tea?", "Colloquial British", "Would you like a cup of tea?"),
        VirtualTravelDestination("gb_edinburgh", "Edinburgh Castle", "Edinburgh", "🏰", "The dramatic fortress towering over the volcanic Castle Rock of Scotland.", "Fiona MacLeod", "Scottish Heritage Curator", "Fàilte! Welcome to the Royal Mile of Edinburgh.", "Melodic Scottish lilt", "Scottish welcome", "Auld Reekie", "Historic nickname", "Edinburgh's affectionate local name")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("gb_rp_cockney", "RP vs. London Cockney", "Received Pronunciation", "🇬🇧", "I am very pleased to meet you", "Standard Queen's/King's English", "East London Cockney", "💂", "Alright mate, 'ow ya doin'?", "Glottal stops and rhyming slang", "London Cockney is energetic, informal, and celebrated in music and markets.")
      ),
      missions = listOf(
        CultureMission("MISSION_GB_VOCAB", "Master British Idioms & Slang", "Learn key British expressions like 'quid' and 'cheers'.", 50, "VOCAB"),
        CultureMission("MISSION_GB_SPEAK", "Order at a London Pub", "Converse with a London publican aloud.", 75, "SPEAKING"),
        CultureMission("MISSION_GB_STAMP", "Earn Your UK Visa Stamp", "Pass the British Culture & Etiquette Challenge.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("gb_q1", "What does 'fancy a cuppa?' mean in British culture?", listOf("Would you like a cup of tea?", "Do you want to buy fancy clothing?", "Are you angry with me?", "Is the museum closed?"), 0, "Tea is the undisputed social glue of British daily life."),
        CultureQuizQuestion("gb_q2", "What is the proper etiquette when escalating onto a London Underground escalator?", listOf("Stand on the right, walk on the left", "Stand on the left, walk on the right", "Block both sides with luggage", "Sit down on the steps"), 0, "Standing on the right allows hurried commuters to pass on the left.")
      ),
      stages = build10Stages(
        countryCode = "GB",
        countryName = "United Kingdom",
        langCode = "en",
        langName = "English",
        capital = "London",
        greetingInTarget = "Good morning! Splendid to meet you, hope you're having a lovely day.",
        greetingPhonetic = "Polite British cadence",
        greetingMeaning = "Warm, courteous British introduction",
        foodPhraseInTarget = "Could I have the fish and chips with mushy peas, please?",
        foodPhrasePhonetic = "Classic London order",
        foodPhraseMeaning = "Ordering the national British favorite dish",
        marketPhraseInTarget = "Excuse me, how much is this vintage teapot on display?",
        marketPhrasePhonetic = "Polite antique market inquiry",
        marketPhraseMeaning = "Asking price politely at Portobello Road Market",
        transitPhraseInTarget = "Which Underground line takes me directly to King's Cross?",
        transitPhrasePhonetic = "Clear transit inquiry",
        transitPhraseMeaning = "Navigating the London Tube network",
        etiquettePhraseInTarget = "Thank you so much for having me, you've been a marvelous host.",
        etiquettePhrasePhonetic = "Heartfelt British gratitude",
        etiquettePhraseMeaning = "Gracious thanks after visiting a British home",
        festivalName = "Bonfire Night & Glastonbury",
        festivalPhraseInTarget = "Remember, remember the fifth of November!",
        festivalPhrasePhonetic = "Historic folk rhyme",
        festivalPhraseMeaning = "Celebrating traditional British Bonfire Night",
        dialectPhraseA = "Splendid, I am rather chuffed! (Standard)",
        dialectPhraseB = "Proper sorted, mate! (Cockney/Northern)",
        dialectRegionA = "London & Oxford (RP)",
        dialectRegionB = "Manchester & Yorkshire"
      )
    ),

    // 3. UNITED STATES (US)
    CountryCultureJourney(
      countryCode = "US",
      countryName = "United States",
      flagEmoji = "🇺🇸",
      languageCode = "en",
      capitalCity = "Washington, D.C.",
      cefrRange = "A1 – C2 American Immersion",
      passportStampCode = "PASSPORT_US",
      motto = "Opportunity, Diversity, Warmth & Directness",
      culturalOverview = "American culture is vibrant, enthusiastic, and direct. People communicate with warm smiles, informal first-name friendliness, energetic small talk, and pride in regional traditions from NYC to California.",
      survivalVocab = listOf(
        CulturalVocabCard("How's it going? / How are you doing?", "Casual American greeting", "Friendly icebreaker", "The standard everyday American greeting in shops, offices, and streets."),
        CulturalVocabCard("No problem / You got it!", "Friendly receipt of thanks", "Glad to help out", "Informal and warm American substitute for 'you are welcome'."),
        CulturalVocabCard("Can I get this to go?", "Takeout request", "Requesting food packaged for takeaway", "Takeaway food culture is central to fast-paced American life.")
      ),
      listeningExercise = CulturalListeningExercise("New York City Deli", "Hey there! What can I get started for you today?", "Upbeat NYC Deli tone", "What is the counter worker asking?", listOf("Taking your food order enthusiastically", "Asking for your passport", "Directing you to the subway", "Closing the store"), 0, "Fast, friendly customer greeting across the USA."),
      speakingSituation = CulturalSpeakingSituation("Ordering Coffee in Seattle", "At an independent Pacific Northwest coffee house.", "What size and what kind of roast would you like today?", "Barista inquiry", "I'll have a medium oat milk latte to go, please!", "Direct friendly American tone", "Specifying size and beverage with warm smile", "Keep it upbeat and tip 15-20% on service."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("Tipping Culture", "💵", "Tip 18% to 22% at sit-down restaurants and for personal services.", "Never leave without tipping servers unless service was disastrous.", "Servers depend on gratuities as the major portion of their legal earnings in the US."),
        CulturalEtiquetteItem("Personal Space & Smiling", "🤝", "Maintain about an arm's length of personal space and smile warmly.", "Avoid standing overly close in elevators or lines.", "Americans value physical personal bubble combined with expressive friendliness.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("us_times_square", "Times Square & Broadway", "New York City", "🎭", "The dazzling crossroads of neon billboards, theater marquee lights, and yellow cabs.", "Marcus Bennett", "Broadway Stage Manager", "Welcome to the Big Apple! The city that never sleeps.", "Energetic New Yorker cadence", "NYC welcome", "Fuhgeddaboudit!", "New York idiom", "Forget about it / No worries at all!"),
        VirtualTravelDestination("us_golden_gate", "Golden Gate Bridge", "San Francisco", "🌉", "The majestic orange suspension bridge spanning the foggy bay into Marin County.", "Chloe Adams", "Bay Area Tech Guide", "Hey there! Welcome to beautiful San Francisco.", "Laid-back California warmth", "West Coast greeting", "Heella cool", "Northern California slang", "Extremely cool or impressive")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("us_nyc_south", "General American vs. Southern Hospitality", "General American (Midwest/West)", "🇺🇸", "You guys are welcome to join us!", "Everyday neutral American", "Southern Drawl", "🤠", "Y'all come on in and make yourselves at home!", "Southern warmth and elongated vowels", "Southern American English is famous for hospitality, charm, and 'y'all'.")
      ),
      missions = listOf(
        CultureMission("MISSION_US_VOCAB", "Master American Idioms & Slang", "Learn everyday American terms like 'hit the road' and 'ballpark figure'.", 50, "VOCAB"),
        CultureMission("MISSION_US_SPEAK", "Order at a New York Diner", "Speak with the diner server in real time.", 75, "SPEAKING"),
        CultureMission("MISSION_US_STAMP", "Earn Your USA Visa Stamp", "Ace the American Culture & History Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("us_q1", "What is the customary tip percentage at sit-down dining restaurants in the US?", listOf("18% to 20% of the pre-tax bill", "0% (tipping is considered rude)", "Exactly 50 cents", "5% only on weekends"), 0, "Tipping is essential compensation for American restaurant waitstaff."),
        CultureQuizQuestion("us_q2", "What is the cultural meaning of the greeting 'What's up?' in America?", listOf("A casual friendly hello, not a literal request for vertical direction", "An inquiry about ceiling height", "A warning of danger", "A formal royal greeting"), 0, "'What's up?' is the quintessential informal American conversation opener.")
      ),
      stages = build10Stages(
        countryCode = "US",
        countryName = "United States",
        langCode = "en",
        langName = "English",
        capital = "Washington, D.C.",
        greetingInTarget = "Hey, how's it going? Great to meet you!",
        greetingPhonetic = "Upbeat, energetic American rhythm",
        greetingMeaning = "Friendly, informal American greeting",
        foodPhraseInTarget = "Could I get a cheeseburger with sweet potato fries and a lemonade?",
        foodPhrasePhonetic = "Standard diner order",
        foodPhraseMeaning = "Ordering classic American comfort food",
        marketPhraseInTarget = "Hey, do you guys have this jacket in a size medium in the back?",
        marketPhrasePhonetic = "Casual retail shopping question",
        marketPhraseMeaning = "Asking store staff for clothing sizes",
        transitPhraseInTarget = "Excuse me, does this subway train go uptown to Central Park?",
        transitPhrasePhonetic = "Direct transit query",
        transitPhraseMeaning = "Navigating NYC subway lines",
        etiquettePhraseInTarget = "Thanks so much for having me over for dinner, this barbecue is amazing!",
        etiquettePhrasePhonetic = "Enthusiastic guest compliment",
        etiquettePhraseMeaning = "Complimenting American hosts on home-cooked food",
        festivalName = "Independence Day (4th of July)",
        festivalPhraseInTarget = "Happy Fourth of July! Enjoy the fireworks and barbecue!",
        festivalPhrasePhonetic = "Festive summer cheer",
        festivalPhraseMeaning = "Celebrating America's national birthday",
        dialectPhraseA = "You guys should check this out! (Neutral/Northern)",
        dialectPhraseB = "Y'all gotta come over and eat! (Southern)",
        dialectRegionA = "New York / California",
        dialectRegionB = "Texas / Georgia"
      )
    ),

    // 4. CANADA (CA)
    CountryCultureJourney(
      countryCode = "CA",
      countryName = "Canada",
      flagEmoji = "🇨🇦",
      languageCode = "en",
      capitalCity = "Ottawa",
      cefrRange = "A1 – C2 Canadian Immersion",
      passportStampCode = "PASSPORT_CA",
      motto = "Politeness, Diversity, Nature & Community",
      culturalOverview = "Canadian culture is celebrated for welcoming kindness, multicultural inclusivity, deep connection to pristine wilderness, and iconic cultural touchstones from hockey to maple syrup.",
      survivalVocab = listOf(
        CulturalVocabCard("Double-double", "Double-double", "Coffee with two creams and two sugars", "The staple Canadian Tim Hortons coffee order."),
        CulturalVocabCard("Eh?", "Ay", "Conversational affirmation tag seeking agreement", "The quintessential Canadian speech marker: 'Nice day, eh?'"),
        CulturalVocabCard("Toque", "Took", "Warm winter knit beanie hat", "Every Canadian owns several toques for snowy winters.")
      ),
      listeningExercise = CulturalListeningExercise("Vancouver Waterfront Café", "Beauty day out on the seawall today, eh?", "Friendly Canadian lilt", "What does the speaker observe?", listOf("Enjoying the beautiful sunny weather on the seawall", "Complaining of severe traffic", "Asking for train tickets", "Selling maple syrup"), 0, "Canadian friendliness combined with the classic 'eh' tag."),
      speakingSituation = CulturalSpeakingSituation("Ordering at Tim Hortons", "At a lively drive-thru counter in Toronto.", "Welcome to Tim's! What can I get for you today?", "Friendly cashier", "Can I get a large double-double and a box of Timbits, please?", "Warm Canadian tone", "Classic Canadian coffee & doughnut order", "Always say please and thank you warmly."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("Apologizing reflexively", "🍁", "Say 'sorry' politely when crossing paths or asking a favor.", "Don't interpret a Canadian 'sorry' as admitting legal guilt; it is empathy.", "Canadians use apologies as social lubricant to maintain communal harmony."),
        CulturalEtiquetteItem("Embracing Multiculturalism", "🤝", "Celebrate diverse heritage, indigenous cultures, and international languages.", "Avoid stereotypes about winter or lumberjacks.", "Canada prides itself on being a vibrant cultural mosaic.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("ca_banff", "Banff National Park", "Alberta", "🏔️", "Turquoise glacial lakes framed by the soaring peaks of the Canadian Rockies.", "Liam Tremblay", "Parks Canada Ranger", "Welcome to Banff! Breathe in that crisp mountain air.", "Outdoor Canadian warmth", "Mountain welcome", "Keep your eyes peeled for moose!", "Wildlife notice", "Watch carefully for wildlife on the trail"),
        VirtualTravelDestination("ca_cntower", "CN Tower & Waterfront", "Toronto", "🗼", "Soaring over 550 meters above the glittering skyline of Lake Ontario.", "Sarah Chen", "Toronto Urban Architect", "Welcome to Toronto, Canada's multicultural hub!", "Cosmopolitan Canadian tone", "Toronto greeting", "The 6ix", "Toronto nickname", "Affectionate nickname for Toronto popularized by musicians")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("ca_ontario_maritime", "Central Canadian vs. Atlantic Maritimes", "Ontario & West", "🇨🇦", "Going up to the cottage for the weekend, eh?", "Central Canadian English", "Newfoundland & Maritimes", "🎣", "Whaddya at, b'y? Come on in out of the wet!", "Maritime warmth and rhythmic dialect", "Newfoundland English has ancient Celtic and West Country English roots.")
      ),
      missions = listOf(
        CultureMission("MISSION_CA_VOCAB", "Master Canadian Slang & Customs", "Learn words like 'toque', 'loonie', and 'two-four'.", 50, "VOCAB"),
        CultureMission("MISSION_CA_SPEAK", "Order at a Canadian Diner", "Practice ordering poutine and maple treats aloud.", 75, "SPEAKING"),
        CultureMission("MISSION_CA_STAMP", "Earn Your Canadian Visa Stamp", "Pass the Great White North Cultural Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("ca_q1", "What is a 'loonie' in Canada?", listOf("A one-dollar Canadian coin featuring the common loon bird", "A wild winter bird", "A crazy hockey fan", "A type of sweet pastry"), 0, "Introduced in 1987, the one-dollar coin is affectionately called the loonie."),
        CultureQuizQuestion("ca_q2", "What is Canada's official national winter sport?", listOf("Ice Hockey", "Curling", "Snowboarding", "Dog Sledding"), 0, "Ice hockey is Canada's official national winter sport, while lacrosse is the summer sport.")
      ),
      stages = build10Stages(
        countryCode = "CA",
        countryName = "Canada",
        langCode = "en",
        langName = "English",
        capital = "Ottawa",
        greetingInTarget = "Hello! Wonderful to meet you, hope you're having a great day, eh?",
        greetingPhonetic = "Polite and cheerful Canadian cadence",
        greetingMeaning = "Warm, welcoming Canadian greeting",
        foodPhraseInTarget = "Could we share a large order of traditional Quebec poutine, please?",
        foodPhrasePhonetic = "Classic Canadian comfort dish",
        foodPhraseMeaning = "Ordering fries topped with fresh cheese curds and warm gravy",
        marketPhraseInTarget = "Hi there, is this pure Grade A maple syrup harvested locally?",
        marketPhrasePhonetic = "Artisan market question",
        marketPhraseMeaning = "Inquiring about authentic Canadian maple syrup",
        transitPhraseInTarget = "Which bus goes directly from downtown Ottawa to Parliament Hill?",
        transitPhrasePhonetic = "Clear capital city transit query",
        transitPhraseMeaning = "Navigating transit to the iconic parliament buildings",
        etiquettePhraseInTarget = "Oh I'm so sorry, thank you so much for pointing me in the right direction!",
        etiquettePhrasePhonetic = "Classic courteous Canadian apology and thanks",
        etiquettePhraseMeaning = "Polite interaction with a helpful stranger",
        festivalName = "Canada Day & Winterlude",
        festivalPhraseInTarget = "Happy Canada Day! Joyeuse fête du Canada!",
        festivalPhrasePhonetic = "Bilingual celebratory greeting",
        festivalPhraseMeaning = "Celebrating Canada's national birthday on July 1st",
        dialectPhraseA = "Heading to the cottage for the long weekend! (Ontario)",
        dialectPhraseB = "Stay where you're at, I'll come where you're to! (Newfoundland)",
        dialectRegionA = "Toronto & Ontario",
        dialectRegionB = "Newfoundland & Atlantic Coast"
      )
    ),

    // 5. AUSTRALIA (AU)
    CountryCultureJourney(
      countryCode = "AU",
      countryName = "Australia",
      flagEmoji = "🇦🇺",
      languageCode = "en",
      capitalCity = "Canberra",
      cefrRange = "A1 – C2 Australian Immersion",
      passportStampCode = "PASSPORT_AU",
      motto = "Mateship, Egalitarianism, Sunshine & Easygoing Humor",
      culturalOverview = "Aussie culture is built on 'mateship'—unconditional loyalty and equality among people. Australians love the outdoors, beach lifestyle, shorten almost every word, and never take themselves too seriously.",
      survivalVocab = listOf(
        CulturalVocabCard("G'day mate / How ya going?", "G'day mate", "Classic Aussie greeting to anyone and everyone", "Warm, egalitarian opening used from surf beaches to corporate offices."),
        CulturalVocabCard("No worries!", "No worries", "You're welcome / Everything will be fine", "The national philosophical response to almost any situation."),
        CulturalVocabCard("Brekkie & Arvo", "Breakfast & Afternoon", "Aussie slang shortening words with 'ie' or 'o'", "Australians playfully shorten words as a sign of informal friendliness.")
      ),
      listeningExercise = CulturalListeningExercise("Sydney Beach Kiosk", "G'day mate! Grab a flat white and watch the morning surf roll in.", "Relaxed Aussie accent", "What is the café host recommending?", listOf("Ordering an espresso coffee while enjoying the ocean view", "Buying surfboard wax", "Warning of a shark sighting", "Closing the beach"), 0, "Flat white coffee and coastal lifestyle are central to Australian mornings."),
      speakingSituation = CulturalSpeakingSituation("Ordering Coffee in Melbourne", "At a world-renowned laneway specialty roaster in Melbourne.", "What can we brew for you this arvo, mate?", "Friendly barista", "I'd love a flat white with oat milk, thanks mate!", "Warm Aussie cadence", "Ordering Australia's signature espresso creation", "Smile and keep it friendly and relaxed."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("Mateship & Egalitarianism", "🦘", "Treat everyone as an equal; chat comfortably with taxi drivers and CEOs alike.", "Avoid boasting, pretentiousness, or 'tall poppy' arrogance.", "Australians celebrate humility and dislike people acting superior."),
        CulturalEtiquetteItem("Slip, Slop, Slap", "☀️", "Slip on a shirt, slop on sunscreen, slap on a hat under the intense sun.", "Never underestimate the UV radiation on Australian beaches.", "Sun safety is an essential national public health custom.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("au_opera_house", "Sydney Opera House & Harbour", "Sydney", "⛵", "Iconic sail-like white shells overlooking the azure waters of Sydney Harbour.", "Jack Thornton", "Harbour Pilot & Guide", "G'day! Welcome to the best harbour in the world.", "Aussie beach lilt", "Sydney welcome", "Fair dinkum!", "Aussie idiom", "True, genuine, honest to goodness!"),
        VirtualTravelDestination("au_uluru", "Uluru-Kata Tjuta", "Northern Territory", "🏜️", "Sacred sandstone monolith rising ancient and majestic out of the red desert heart.", "Aunty Maya", "Anangu Cultural Educator", "Palya! Welcome to our sacred ancestral lands.", "Reverent First Nations greeting", "Warm indigenous greeting", "Respect the Tjukurpa", "Sacred law & creation stories", "Honoring the ancient spiritual heritage of Uluru")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("au_broad_general", "General Aussie vs. Strine Outback", "City Coastal (General)", "🇦🇺", "How's your day going, mate?", "Melodic city Australian", "Outback / Bush (Broad)", "🤠", "Fair suck of the sav, mate! She'll be right!", "Broad drawl with rich vintage slang", "Broad Aussie uses colorful folk imagery and exaggerated vowels.")
      ),
      missions = listOf(
        CultureMission("MISSION_AU_VOCAB", "Master Aussie Slang & Vocabulary", "Learn terms like 'esky', 'thongs', 'ute', and 'barbie'.", 50, "VOCAB"),
        CultureMission("MISSION_AU_SPEAK", "Converse at an Aussie Barbecue", "Chat with Aussie locals by the grill.", 75, "SPEAKING"),
        CultureMission("MISSION_AU_STAMP", "Earn Your Australian Visa Stamp", "Pass the Down Under Culture & Wildlife Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("au_q1", "What does 'She'll be right' mean in Australia?", listOf("Everything will work out fine in the end; don't stress", "That woman is always correct", "Turn right at the traffic lights", "A female kangaroo is approaching"), 0, "The national motto of Aussie optimism and relaxed resilience."),
        CultureQuizQuestion("au_q2", "What is Canberra's role in Australia?", listOf("The planned federal capital city between Sydney and Melbourne", "Australia's largest surf town", "An island in the Great Barrier Reef", "A desert gold mining camp"), 0, "Canberra was founded as the compromise capital between competing rivals Sydney and Melbourne.")
      ),
      stages = build10Stages(
        countryCode = "AU",
        countryName = "Australia",
        langCode = "en",
        langName = "English",
        capital = "Canberra",
        greetingInTarget = "G'day mate! How ya going? Absolute pleasure to meet you!",
        greetingPhonetic = "Warm, relaxed Australian cadence",
        greetingMeaning = "Friendly, authentic Aussie greeting",
        foodPhraseInTarget = "Could I get a flat white coffee and some avocado toast with Vegemite, please?",
        foodPhrasePhonetic = "Iconic Aussie café order",
        foodPhraseMeaning = "Ordering the quintessential Australian brunch",
        marketPhraseInTarget = "G'day, are these opals mined locally in Coober Pedy?",
        marketPhrasePhonetic = "Inquiring about Australian gems",
        marketPhraseMeaning = "Asking about native Australian opals",
        transitPhraseInTarget = "Which ferry takes me across the harbour to Manly Beach?",
        transitPhrasePhonetic = "Iconic Sydney ferry inquiry",
        transitPhraseMeaning = "Taking the scenic ferry across Sydney Harbour",
        etiquettePhraseInTarget = "Thanks so much for having me at the barbie, you're an absolute legend!",
        etiquettePhrasePhonetic = "Heartfelt Aussie compliment",
        etiquettePhraseMeaning = "Thanking an Australian host for a backyard barbecue",
        festivalName = "Australia Day & Sydney New Year's",
        festivalPhraseInTarget = "Happy New Year from the greatest fireworks in the world!",
        festivalPhrasePhonetic = "Joyful Sydney celebration",
        festivalPhraseMeaning = "Celebrating Sydney Harbour's global New Year's spectacle",
        dialectPhraseA = "Having a flat white this morning! (City)",
        dialectPhraseB = "Throw another shrimp on the barbie, mate! (Classic Outback)",
        dialectRegionA = "Melbourne & Sydney",
        dialectRegionB = "Outback Queensland & Bush"
      )
    ),

    // 6. SPAIN (ES)
    CountryCultureJourney(
      countryCode = "ES",
      countryName = "Spain",
      flagEmoji = "🇪🇸",
      languageCode = "es",
      capitalCity = "Madrid",
      cefrRange = "A1 – C1 Spanish Immersion",
      passportStampCode = "PASSPORT_ES",
      motto = "Sobremesa, Passion, Art & Living in the Moment",
      culturalOverview = "Spanish culture centers around warmth, passionate conversation, late evening dinners, shared tapas, and 'la sobremesa'—lingering around the table chatting long after the meal finishes.",
      survivalVocab = listOf(
        CulturalVocabCard("¡Hola! ¿Qué tal?", "OH-lah keh tahl", "Hello! How are things?", "Universal friendly Spanish greeting."),
        CulturalVocabCard("Por favor / Muchas gracias", "por fah-VOR / MOO-chas GRAH-syas", "Please / Thank you very much", "Polite courtesy everywhere in Spain."),
        CulturalVocabCard("Una ronda de tapas, por favor", "OO-nah RON-dah deh TAH-pas", "A round of tapas, please", "The essence of Spanish social dining.")
      ),
      listeningExercise = CulturalListeningExercise("In a Madrid Tapas Bar", "¡Buenas tardes! ¿Qué os pongo de beber?", "BWEH-nas TAR-des", "What is the camarero asking?", listOf("What would you like to drink?", "Where is your luggage?", "Are you leaving now?", "Do you have tickets?"), 0, "Standard warm tavern greeting across Spain."),
      speakingSituation = CulturalSpeakingSituation("Ordering Tapas in Seville", "At a historic taberna in the Santa Cruz quarter.", "¿Vais a tomar raciones o tapas?", "Tapas or full plates?", "Unas patatas bravas y jamón ibérico, por favor.", "OO-nas pah-TAH-tas BRAH-vas", "Spicy potatoes and Iberian ham, please", "Speak with warmth and enjoy the lively atmosphere."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("La Sobremesa", "🍷", "Linger at the table after dessert to talk and enjoy company.", "Never rush away immediately when the last bite is swallowed.", "Sobremesa is considered the soul of Spanish dining culture."),
        CulturalEtiquetteItem("Greetings (Dos Besos)", "💋", "Two cheek kisses (right then left) when introduced in social settings.", "Never plant wet lips; it is a light cheek-to-cheek touch with a kiss sound.", "Standard greeting between women and between men and women in social gatherings.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("es_sagrada", "Sagrada Família", "Barcelona", "⛪", "Gaudí's forest-like basilica with soaring towers and kaleidoscope stained glass.", "Núria Soler", "Catalan Art Historian", "¡Bienvenidos a la joya de Barcelona!", "Catalan-accented Spanish", "Welcome to Barcelona!", "Poco a poco se va lejos", "Proverb", "Little by little, one goes far")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("es_castellano_andaluz", "Madrid vs. Andalusia", "Madrid (Castellano)", "🇪🇸", "¿Quieres tomar algo ahora?", "Distinction of Z/C and S", "Seville (Andaluz)", "💃", "¿Te apetece una cervecita, miarma?", "Rhythmic dropped consonants and poetic warmth", "Andalusian Spanish is famous for musicality, dropped word endings, and warmth.")
      ),
      missions = listOf(
        CultureMission("MISSION_ES_VOCAB", "Master 5 Spanish Tapas Phrases", "Learn authentic tavern vocabulary.", 50, "VOCAB"),
        CultureMission("MISSION_ES_SPEAK", "Converse in Seville", "Complete the tapas ordering situation.", 75, "SPEAKING"),
        CultureMission("MISSION_ES_STAMP", "Earn Your Spain Visa Stamp", "Achieve top score on the Spain Culture Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("es_q1", "What does 'la sobremesa' refer to in Spanish culture?", listOf("The relaxed conversation around the table after finishing a meal", "Setting the table before guests arrive", "Paying the bill quickly", "A dessert made of custard"), 0, "Sobremesa is sacred time spent talking with loved ones."),
        CultureQuizQuestion("es_q2", "At what time do locals typically eat dinner in Spain?", listOf("Between 9:00 PM and 11:00 PM", "At 5:00 PM", "At 12:00 noon", "Only in the morning"), 0, "Spain operates on late evening dining schedules adapted to long sunny days.")
      ),
      stages = build10Stages(
        countryCode = "ES",
        countryName = "Spain",
        langCode = "es",
        langName = "Spanish",
        capital = "Madrid",
        greetingInTarget = "¡Hola! Mucho gusto en conocerte, ¿cómo estás?",
        greetingPhonetic = "OH-lah! MOO-choh GOOS-toh en koh-noh-SAIR-teh",
        greetingMeaning = "Hello! Pleased to meet you, how are you?",
        foodPhraseInTarget = "¿Cuál es la especialidad de la casa? Una tortilla española, por favor",
        foodPhrasePhonetic = "KWAHL es lah es-peh-sya-lee-DAHD deh lah KAH-sah?",
        foodPhraseMeaning = "What is the house specialty? Spanish omelette, please",
        marketPhraseInTarget = "¿Cuánto cuesta este jamón ibérico de bellota?",
        marketPhrasePhonetic = "KWAHN-toh KWEHS-tah EHS-teh hah-MOHN ee-BEH-ree-koh?",
        marketPhraseMeaning = "How much is this acorn-fed Iberian ham?",
        transitPhraseInTarget = "¿Por dónde se toma el tren de cercanías hacia Toledo?",
        transitPhrasePhonetic = "Por DOHN-deh seh TOH-mah el trehn?",
        transitPhraseMeaning = "Where do I catch the commuter train to Toledo?",
        etiquettePhraseInTarget = "¡Muchas gracias por la invitación, ha sido una velada maravillosa!",
        etiquettePhrasePhonetic = "MOO-chas GRAH-syas por lah een-vee-tah-SYOHN",
        etiquettePhraseMeaning = "Thank you so much for the invitation; it was a wonderful evening!",
        festivalName = "La Tomatina & Feria de Abril",
        festivalPhraseInTarget = "¡Feliz Feria de Abril! ¡A bailar sevillanas!",
        festivalPhrasePhonetic = "Feh-LEES FEH-ryah deh ah-BREEL!",
        festivalPhraseMeaning = "Happy April Fair! Let's dance sevillanas!",
        dialectPhraseA = "¿Queréis tomar algo en el bar? (Castellano)",
        dialectPhraseB = "¿Queréis una tapita, miarma? (Andaluz)",
        dialectRegionA = "Madrid & Castille",
        dialectRegionB = "Seville & Andalusia"
      )
    ),

    // 7. MEXICO (MX)
    CountryCultureJourney(
      countryCode = "MX",
      countryName = "Mexico",
      flagEmoji = "🇲🇽",
      languageCode = "es",
      capitalCity = "Mexico City",
      cefrRange = "A1 – C2 Mexican Spanish Immersion",
      passportStampCode = "PASSPORT_MX",
      motto = "Warmth, Family, Ancestral Heritage & Joy of Life",
      culturalOverview = "Mexican culture is deeply hospitable, expressive, and rooted in rich indigenous civilizations and baroque traditions. Meals are celebratory, language is poetic and polite, and family is the beating heart of life.",
      survivalVocab = listOf(
        CulturalVocabCard("¿Qué onda? / ¡Hola!", "¿Qué onda?", "What's up? / Hello!", "The quintessential Mexican friendly greeting."),
        CulturalVocabCard("Ahorita", "ah-oh-REE-tah", "In a moment / Right now / Later", "Famous Mexican concept of flexible, gentle time."),
        CulturalVocabCard("¡Buen provecho!", "BWEHN proh-VEH-choh", "Enjoy your meal!", "Spoken politely to anyone dining as you pass their table.")
      ),
      listeningExercise = CulturalListeningExercise("Taquería in Mexico City", "¡Pásele marchante! ¿Cuántos de al pastor le preparo?", "Warm Mexican cadence", "What is the taquero asking?", listOf("How many tacos al pastor would you like?", "Asking for your train ticket", "Closing the taco stand", "Asking for directions"), 0, "Warm taquería hospitality in Mexico City."),
      speakingSituation = CulturalSpeakingSituation("Ordering Tacos in Coyoacán", "At a colorful food stall in Frida Kahlo's neighborhood.", "¿Con todo, joven? Cilantro, cebolla y piña?", "With all toppings?", "¡Sí, con todo y salsa verde, por favor!", "SEE, kohn TOH-doh", "Yes, with everything and green salsa please!", "Always say 'provecho' to neighboring tables."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("Polite Diminutives & Warmth", "🌮", "Use polite forms like 'con permiso', 'gracias', and gentle phrasing.", "Avoid blunt commands or cold transactional tones.", "Mexican culture places great value on gentleness and emotional connection."),
        CulturalEtiquetteItem("Dia de los Muertos", "💀", "Treat Day of the Dead with celebration and sacred respect for ancestors.", "Never confuse it with Halloween; it is an intimate family reunion of memories.", "Altars (ofrendas) honor beloved relatives with marigolds and favorite foods.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("mx_zocalo", "Zócalo & Templo Mayor", "Mexico City", "🏛️", "The colossal historic plaza where the Aztec capital meets the Metropolitan Cathedral.", "Mateo Rivera", "CDMX Cultural Historian", "¡Bienvenidos a la Ciudad de México!", "Warm Mexican Spanish", "Welcome to CDMX!", "¡Esta ciudad tiene magia!", "Mexican idiom", "This city has magic!")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("mx_chilango_norteno", "Chilango (CDMX) vs. Norteño", "Mexico City (Chilango)", "🇲🇽", "¡Está bien padre este lugar!", "CDMX warmth with singing intonation", "Northern Mexico", "🤠", "¡Está con madre, compa!", "Direct, resonant Northern cadence", "Northern Mexican Spanish is direct and famous for cowboy culture and ranchero music.")
      ),
      missions = listOf(
        CultureMission("MISSION_MX_VOCAB", "Master Mexican Street Slang", "Learn words like 'chido', 'padre', and 'güey'.", 50, "VOCAB"),
        CultureMission("MISSION_MX_SPEAK", "Order at a Mexico City Taquería", "Converse with the taquero in real time.", 75, "SPEAKING"),
        CultureMission("MISSION_MX_STAMP", "Earn Your Mexico Visa Stamp", "Pass the Mexican Heritage & Gastronomy Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("mx_q1", "What does saying '¡Buen provecho!' mean in Mexico?", listOf("Wishing someone an enjoyable meal, spoken even to strangers", "Complaining about food temperature", "Asking for the check", "Saying goodbye forever"), 0, "'Buen provecho' is courteous dining etiquette universally practiced in Mexico."),
        CultureQuizQuestion("mx_q2", "What is an 'ofrenda' during Día de los Muertos?", listOf("An altar decorated with marigolds, photos, and food to welcome spirits of ancestors", "A sweet candy bread only", "A costume party mask", "A church bell"), 0, "Ofrendas are central to welcoming returning loved ones in Mexican tradition.")
      ),
      stages = build10Stages(
        countryCode = "MX",
        countryName = "Mexico",
        langCode = "es",
        langName = "Spanish",
        capital = "Mexico City",
        greetingInTarget = "¡Hola! ¿Qué tal? Mucho gusto en conocerte.",
        greetingPhonetic = "OH-lah! keh tahl? MOO-choh GOOS-toh",
        greetingMeaning = "Hello! What's up? Wonderful to meet you.",
        foodPhraseInTarget = "¿Me da tres tacos al pastor con todo y agua de horchata, por favor?",
        foodPhrasePhonetic = "Classic taquería order",
        foodPhraseMeaning = "Ordering tacos al pastor with everything and horchata water",
        marketPhraseInTarget = "Disculpe marchante, ¿este alebrije fue tallado en Oaxaca?",
        marketPhrasePhonetic = "Artisan market question",
        marketPhraseMeaning = "Inquiring about colorful handmade Oaxaca folk art",
        transitPhraseInTarget = "¿Cuál estación del metro me deja en el Palacio de Bellas Artes?",
        transitPhrasePhonetic = "CDMX metro inquiry",
        transitPhraseMeaning = "Navigating to the iconic Palace of Fine Arts",
        etiquettePhraseInTarget = "¡Con permiso! Gracias por recibirnos en su casa tan hermosa.",
        etiquettePhrasePhonetic = "Polite home entry and thanks",
        etiquettePhraseMeaning = "Courtesy when entering a Mexican family home",
        festivalName = "Día de los Muertos",
        festivalPhraseInTarget = "¡Feliz Día de Muertos! Honremos la memoria de nuestros seres queridos.",
        festivalPhrasePhonetic = "Reverent and joyful blessing",
        festivalPhraseMeaning = "Celebrating the remembrance of ancestors",
        dialectPhraseA = "¡Está bien chido este lugar! (Chilango)",
        dialectPhraseB = "¡Qué chilo está el rollo! (Norteño)",
        dialectRegionA = "Mexico City & Central",
        dialectRegionB = "Monterrey & North"
      )
    ),

    // 8. FRANCE (FR)
    CountryCultureJourney(
      countryCode = "FR",
      countryName = "France",
      flagEmoji = "🇫🇷",
      languageCode = "fr",
      capitalCity = "Paris",
      cefrRange = "A1 – C1 French Immersion",
      passportStampCode = "PASSPORT_FR",
      motto = "Liberté, Égalité, Fraternité & Art de Vivre",
      culturalOverview = "French culture celebrates the art of conversation, gastronomy, literature, and architectural beauty. Greeting with a polite 'Bonjour Madame/Monsieur' is the indispensable golden rule.",
      survivalVocab = listOf(
        CulturalVocabCard("Bonjour Madame / Monsieur", "bon-ZHOOR mah-DAHM / muh-SYUH", "Good day Madam / Sir", "The mandatory opening greeting before saying anything else in France."),
        CulturalVocabCard("S'il vous plaît / Merci beaucoup", "seel voo PLEH / mair-SEE boh-KOO", "Please / Thank you very much", "Essential courtesy phrases for daily French interactions."),
        CulturalVocabCard("L'addition, s'il vous plaît", "lah-dee-SYOHN seel voo pleh", "The bill, please", "Requested at the end of meals; bills are never brought unasked.")
      ),
      listeningExercise = CulturalListeningExercise("In a Parisian Boulangerie", "Bonjour ! Une baguette tradition et deux croissants, s'il vous plaît.", "Melodic Parisian cadence", "What is the customer ordering?", listOf("A traditional baguette and two croissants", "A bottle of mineral water", "A train ticket to Versailles", "Postage stamps"), 0, "The daily morning ritual of French culinary life."),
      speakingSituation = CulturalSpeakingSituation("Entering a Parisian Boutique", "Walking into an artisanal perfume atelier in Le Marais.", "Bonjour, bienvenue ! Je peux vous renseigner ?", "Can I assist you?", "Bonjour ! Je regarde simplement pour l'instant, merci.", "bon-ZHOOR! Zhuh ruh-GARD sam-pluh-MAHN", "Good day! I am just browsing for now, thank you", "Always look the shopkeeper in the eye and say Bonjour first."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("The Mandatory 'Bonjour'", "🥖", "Always say 'Bonjour' the moment you step into any shop or café.", "Never demand help without greeting first; it is seen as rude.", "Establishes mutual dignity between customer and merchant."),
        CulturalEtiquetteItem("Bread at the Table", "🍞", "Place your bread directly onto the tablecloth beside your plate.", "Do not set bread inside your soup or plate unless wiping sauce.", "French dining considers the clean tablecloth an extension of the breadboard.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("fr_eiffel", "Eiffel Tower & Champ de Mars", "Paris", "🗼", "The world-famous iron lady soaring above the Seine and Paris rooftops.", "Camille Dubois", "Parisian Architecture Guide", "Bonjour et bienvenue au cœur de Paris !", "Refined French cadence", "Welcome to Paris!", "C'est la vie !", "Famous idiom", "That is life!")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("fr_paris_midi", "Paris vs. Southern Provence", "Paris (Standard)", "🇫🇷", "On va prendre un café en terrasse ?", "Crisp urban rhythm", "Provence (Midi)", "🌻", "Té, peuchère ! On prend un pastis au soleil ?", "Warm southern singing vowels", "Southern French is famous for singing vowels and relaxed sunny rhythm.")
      ),
      missions = listOf(
        CultureMission("MISSION_FR_VOCAB", "Master 5 Parisian Courtesy Phrases", "Learn essential French etiquette phrases.", 50, "VOCAB"),
        CultureMission("MISSION_FR_SPEAK", "Order at a French Boulangerie", "Speak your bakery order aloud.", 75, "SPEAKING"),
        CultureMission("MISSION_FR_STAMP", "Earn Your France Visa Stamp", "Achieve top score on the France Culture Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("fr_q1", "What must you ALWAYS say immediately upon entering any shop in France?", listOf("Bonjour (Madame / Monsieur)", "I want to buy something", "Where is the cashier?", "Silent entrance"), 0, "Saying Bonjour recognizes the merchant's humanity before commerce begins."),
        CultureQuizQuestion("fr_q2", "Why does a waiter in France wait for you to ask for the bill (l'addition)?", listOf("It is considered rude to rush guests; the table is yours as long as you wish", "They forgot about your table", "Bills are only paid the next day", "Meals are free after 8 PM"), 0, "A French restaurant table belongs to you for the evening.")
      ),
      stages = build10Stages(
        countryCode = "FR",
        countryName = "France",
        langCode = "fr",
        langName = "French",
        capital = "Paris",
        greetingInTarget = "Bonjour ! Enchanté de faire votre connaissance.",
        greetingPhonetic = "bon-ZHOOR! ahn-shahn-TAY duh fair VO-truh koh-neh-SAHNS",
        greetingMeaning = "Good day! Delighted to make your acquaintance.",
        foodPhraseInTarget = "Quel est le plat du jour aujourd'hui ? Je prendrai le confit de canard.",
        foodPhrasePhonetic = "Classic French bistro order",
        foodPhraseMeaning = "What is the special today? I will have the duck confit.",
        marketPhraseInTarget = "Bonjour, ce fromage de chèvre fermier vient de quelle région ?",
        marketPhrasePhonetic = "Inquiring at a French fromagerie",
        marketPhraseMeaning = "Asking which region the goat cheese is crafted in",
        transitPhraseInTarget = "Pardonnez-moi, quelle ligne de métro va directement au Musée du Louvre ?",
        transitPhrasePhonetic = "Parisian metro query",
        transitPhraseMeaning = "Asking which metro line goes to the Louvre Museum",
        etiquettePhraseInTarget = "Merci infiniment pour cette merveilleuse soirée et ce délicieux dîner !",
        etiquettePhrasePhonetic = "Polite dinner guest thanks",
        etiquettePhraseMeaning = "Thanking hosts for a marvelous dinner evening",
        festivalName = "Fête de la Musique & Bastille Day",
        festivalPhraseInTarget = "Vive la musique et joyeux Quatorze Juillet !",
        festivalPhrasePhonetic = "Celebratory French holiday cheer",
        festivalPhraseMeaning = "Celebrating France's national music and independence holidays",
        dialectPhraseA = "C'est un régal ! (Paris Standard)",
        dialectPhraseB = "Boudiou, que c'est bon ! (Occitan / Midi)",
        dialectRegionA = "Paris & Île-de-France",
        dialectRegionB = "Provence & Marseille"
      )
    ),

    // 9. GERMANY (DE)
    CountryCultureJourney(
      countryCode = "DE",
      countryName = "Germany",
      flagEmoji = "🇩🇪",
      languageCode = "de",
      capitalCity = "Berlin",
      cefrRange = "A1 – C1 German Immersion",
      passportStampCode = "PASSPORT_DE",
      motto = "Punctuality, Precision, Gemütlichkeit & Critical Thinking",
      culturalOverview = "German communication values punctuality, honesty, precision, and thorough preparation, paired with cozy relaxation in beer gardens and rich musical heritage.",
      survivalVocab = listOf(
        CulturalVocabCard("Guten Tag / Hallo!", "GOO-ten TAHK / HAH-lo", "Good day / Hello!", "Standard respectful German greeting."),
        CulturalVocabCard("Bitte / Danke schön", "BIT-teh / DAHN-keh shurn", "Please / Thank you kindly", "The cornerstones of polite German interaction."),
        CulturalVocabCard("Die Rechnung, bitte", "dee REKH-noong, BIT-teh", "The bill, please", "Requested when you are ready to pay at a German restaurant.")
      ),
      listeningExercise = CulturalListeningExercise("In a Berlin Bakery", "Guten Morgen! Was darf es denn sein?", "Clear Berlin German", "What is the baker asking?", listOf("What can I get for you this morning?", "Do you have an appointment?", "Is your bicycle locked?", "Where are you traveling?"), 0, "Daily morning greeting in German bakeries."),
      speakingSituation = CulturalSpeakingSituation("Ordering Pretzels in Munich", "At a bustling beer garden under the chestnut trees.", "Ein Bier und eine Breze?", "Beer and pretzel?", "Ja gerne, ein alkoholfreies Weißbier und eine frische Brezel, bitte.", "yah GAIR-neh", "Yes gladly, an alcohol-free wheat beer and fresh pretzel please", "Make direct eye contact when toasting (Prost!)."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("Punctuality (Pünktlichkeit)", "⏰", "Arrive 5 to 10 minutes early for business or social appointments.", "Never show up 15 minutes late without calling ahead.", "Punctuality demonstrates respect for other people's time."),
        CulturalEtiquetteItem("Toasting (Prost!)", "🍻", "Look each person directly in the eye as glasses clink.", "Never look away or cross arms over other glasses.", "Failing eye contact during a toast is considered bad luck.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("de_brandenburg", "Brandenburg Gate", "Berlin", "🏛️", "The monumental 18th-century neoclassical monument symbolizing German unity.", "Felix Weber", "Berlin Historian", "Willkommen in der Hauptstadt Berlin!", "Standard German", "Welcome to the capital Berlin!", "Alles in Butter", "German idiom", "Everything is in order / fine")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("de_berlin_bavaria", "Berlin vs. Bavaria", "Berlin (Hochdeutsch)", "🇩🇪", "Guten Tag, wie geht es Ihnen?", "Standard German", "Bavaria (Bairisch)", "🥨", "Servus! Grüß Gott beinand!", "Warm Alpine Bavarian dialect", "Bavarian German uses friendly Alpine greetings like 'Grüß Gott' and 'Servus'.")
      ),
      missions = listOf(
        CultureMission("MISSION_DE_VOCAB", "Master German Courtesy Phrases", "Practice German polite forms.", 50, "VOCAB"),
        CultureMission("MISSION_DE_SPEAK", "Converse in Munich", "Complete the Munich beer garden speaking task.", 75, "SPEAKING"),
        CultureMission("MISSION_DE_STAMP", "Earn Your Germany Visa Stamp", "Achieve top score on the Germany Culture Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("de_q1", "What must you do while clinking glasses during a German toast (Prost)?", listOf("Look the other person directly in the eyes", "Look down at the floor", "Close your eyes tightly", "Check your phone"), 0, "Direct eye contact signifies trustworthiness in German culture."),
        CultureQuizQuestion("de_q2", "What is the cultural expectation regarding meeting times in Germany?", listOf("Arriving on time or slightly early shows respect and reliability", "Arriving 30 minutes late is considered fashionable", "Appointments are merely suggestions", "Only morning meetings require punctuality"), 0, "Punctuality is a core expression of respect in Germany.")
      ),
      stages = build10Stages(
        countryCode = "DE",
        countryName = "Germany",
        langCode = "de",
        langName = "German",
        capital = "Berlin",
        greetingInTarget = "Guten Tag! Es freut mich sehr, Sie kennenzulernen.",
        greetingPhonetic = "GOO-ten TAHK! es froyt mikh zair, zee KEN-nen-tsoo-lair-nen",
        greetingMeaning = "Good day! It is a great pleasure to meet you.",
        foodPhraseInTarget = "Was empfehlen Sie als Spezialität des Hauses? Ein Schnitzel, bitte.",
        foodPhrasePhonetic = "German restaurant order",
        foodPhraseMeaning = "What do you recommend as the house specialty? Schnitzel please.",
        marketPhraseInTarget = "Entschuldigung, ist dieses Roggenbrot frisch gebacken?",
        marketPhrasePhonetic = "Bakery market question",
        marketPhraseMeaning = "Excuse me, is this rye bread freshly baked today?",
        transitPhraseInTarget = "Auf welchem Gleis fährt der ICE-Zug nach Hamburg ab?",
        transitPhrasePhonetic = "Deutsche Bahn station inquiry",
        transitPhraseMeaning = "Which platform does the high-speed ICE train to Hamburg depart from?",
        etiquettePhraseInTarget = "Vielen herzlichen Dank für Ihre Gastfreundschaft und die Einladung!",
        etiquettePhrasePhonetic = "Polite German host thanks",
        etiquettePhraseMeaning = "Heartfelt thanks for your wonderful hospitality and invitation!",
        festivalName = "Oktoberfest & Karneval",
        festivalPhraseInTarget = "O'zapft is! Prost auf die Gemütlichkeit!",
        festivalPhrasePhonetic = "Bavarian Oktoberfest cheer",
        festivalPhraseMeaning = "The keg is tapped! Cheers to good cheer and community!",
        dialectPhraseA = "Auf Wiedersehen! (Standard)",
        dialectPhraseB = "Servus und pfiat di! (Bavarian)",
        dialectRegionA = "Berlin & North",
        dialectRegionB = "Munich & Bavaria"
      )
    ),

    // 10. SOUTH KOREA (KR)
    CountryCultureJourney(
      countryCode = "KR",
      countryName = "South Korea",
      flagEmoji = "🇰🇷",
      languageCode = "ko",
      capitalCity = "Seoul",
      cefrRange = "A1 – C1 Korean Immersion",
      passportStampCode = "PASSPORT_KR",
      motto = "Jeong (Heartfelt Bond), Respect & Dynamic Energy",
      culturalOverview = "Korean culture weaves deep Confucian respect for elders and honorific speech (Jondaenmal) with hyper-modern innovation, communal dining, and K-Culture creativity.",
      survivalVocab = listOf(
        CulturalVocabCard("안녕하세요", "Annyeonghaseyo", "Hello / Good day", "The universal respectful Korean greeting, accompanied by a polite bow."),
        CulturalVocabCard("감사합니다", "Gamsahamnida", "Thank you very much", "Formal expression of sincere gratitude used daily across Korea."),
        CulturalVocabCard("잘 먹겠습니다", "Jal meokgesseumnida", "I will eat well / Thank you for this meal", "Said respectfully before eating to thank the provider of the meal.")
      ),
      listeningExercise = CulturalListeningExercise("In a Seoul Convenience Store", "어서오세요! 봉투 필요하세요?", "Eoseo-oseyo! Bongtu piryohaseyo?", "What is the cashier asking?", listOf("Welcome! Do you need a shopping bag?", "Do you need an umbrella?", "Where is the subway?", "Are you returning items?"), 0, "The standard greeting and bag inquiry in South Korea."),
      speakingSituation = CulturalSpeakingSituation("Ordering Street Food in Myeongdong", "At a lively street food cart in Seoul.", "어떤 거 드릴까요? 떡볶이 있어요.", "What would you like?", "떡볶이 1인분하고 김밥 주세요.", "Tteokbokki il-inbun-hago gimbap juseyo", "One serving of tteokbokki and gimbap, please", "Use two hands when receiving food or payment."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("Two-Handed Respect", "🤲", "Give and receive items (cards, drinks, gifts) with both hands or supporting right elbow.", "Never hand something to an elder or colleague with one hand casually.", "Shows complete attentiveness and deep respect in Korean society."),
        CulturalEtiquetteItem("Drinking Etiquette", "🍶", "Turn your head and body slightly away from elders when drinking.", "Never pour your own drink; pour for others and let them pour for you.", "Essential etiquette demonstrating modesty and mutual care (Jeong).")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("kr_gyeongbokgung", "Gyeongbokgung Palace", "Seoul", "🏯", "The grand 14th-century royal palace of the Joseon dynasty.", "Minji Park", "Seoul Heritage Guide", "안녕하세요! 경복궁에 오신 것을 환영합니다.", "Annyeonghaseyo! Gyeongbokgung-e osin geoseul hwanyeonghamnida.", "Welcome to Gyeongbokgung Palace!", "대박!", "Daebak!", "Awesome / Incredible!")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("kr_seoul_busan", "Seoul vs. Busan Dialect (Satoori)", "Seoul (Standard)", "🇰🇷", "밥 먹었어요?", "Bap meogeosseoyo?", "Busan (Gyeongsang-do)", "🌊", "밥 묵었나?", "Bap mugeonna?", "Busan Satoori has a dynamic, strong musical pitch beloved across Korea.")
      ),
      missions = listOf(
        CultureMission("MISSION_KR_VOCAB", "Master 5 Honorific Korean Phrases", "Practice polite Jondaenmal vocabulary.", 50, "VOCAB"),
        CultureMission("MISSION_KR_SPEAK", "Order Food in Seoul", "Complete the Myeongdong street food challenge.", 75, "SPEAKING"),
        CultureMission("MISSION_KR_STAMP", "Earn Your Korea Visa Stamp", "Achieve top score on the Korea Culture Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("kr_q1", "How should you receive a business card, payment, or drink in South Korea?", listOf("With both hands or right hand supported at the forearm", "With your left hand in your pocket", "With your foot", "Throwing it across the table"), 0, "Two-handed reception is universal etiquette of respect in Korea."),
        CultureQuizQuestion("kr_q2", "What is 'Jeong' (정) in Korean culture?", listOf("A deep, warm emotional bond of affection and solidarity that grows between people", "A type of martial arts kick", "A spicy soup seasoning", "A train ticket app"), 0, "Jeong is considered the emotional soul of Korean interpersonal warmth.")
      ),
      stages = build10Stages(
        countryCode = "KR",
        countryName = "South Korea",
        langCode = "ko",
        langName = "Korean",
        capital = "Seoul",
        greetingInTarget = "처음 뵙겠습니다. 잘 부탁드립니다.",
        greetingPhonetic = "Cheoeum boepgesseumnida. Jal butakdeurimnida.",
        greetingMeaning = "Pleased to meet you for the first time. I look forward to your kindness.",
        foodPhraseInTarget = "여기 추천 메뉴가 무엇인가요? 비빔밥 하나 주세요.",
        foodPhrasePhonetic = "Seoul restaurant order",
        foodPhraseMeaning = "What is the recommended dish here? One bibimbap, please.",
        marketPhraseInTarget = "이거 얼마예요? 전통 수공예품인가요?",
        marketPhrasePhonetic = "Insadong market query",
        marketPhraseMeaning = "How much is this? Is it a traditional handmade craft?",
        transitPhraseInTarget = "실례지만, 강남역으로 가려면 몇 호선을 타야 하나요?",
        transitPhrasePhonetic = "Seoul subway inquiry",
        transitPhraseMeaning = "Excuse me, which line do I take to get to Gangnam Station?",
        etiquettePhraseInTarget = "초대해 주셔서 진심으로 감사드립니다.",
        etiquettePhrasePhonetic = "Respectful guest thanks",
        etiquettePhraseMeaning = "Thank you from the bottom of my heart for inviting me.",
        festivalName = "Chuseok & Seollal",
        festivalPhraseInTarget = "풍성하고 행복한 한가위 보내세요!",
        festivalPhrasePhonetic = "Chuseok holiday greeting",
        festivalPhraseMeaning = "Wishing you a bountiful and joyous harvest festival!",
        dialectPhraseA = "정말 맛있어요! (Seoul)",
        dialectPhraseB = "억수로 맛있다 아이가! (Busan Satoori)",
        dialectRegionA = "Seoul & Capital",
        dialectRegionB = "Busan & Gyeongsang"
      )
    ),

    // 11. ITALY (IT)
    CountryCultureJourney(
      countryCode = "IT",
      countryName = "Italy",
      flagEmoji = "🇮🇹",
      languageCode = "it",
      capitalCity = "Rome",
      cefrRange = "A1 – C1 Italian Immersion",
      passportStampCode = "PASSPORT_IT",
      motto = "La Dolce Vita, Craftsmanship, Passion & Family",
      culturalOverview = "Italian culture centers around artistic beauty, warm expressive gesturing, deep love of culinary traditions, and taking time to savor life with family and friends.",
      survivalVocab = listOf(
        CulturalVocabCard("Buongiorno / Ciao!", "bwon-ZHOR-noh / CHOW", "Good morning / Hello!", "The warm, musical Italian greetings used in every piazza."),
        CulturalVocabCard("Per favore / Grazie mille", "pehr fah-VOH-reh / GRAHT-syeh MEEL-leh", "Please / A thousand thanks", "Expressing sincere Italian courtesy."),
        CulturalVocabCard("Il conto, per favore", "eel KOHN-toh pehr fah-VOH-reh", "The bill, please", "Politely requested at the end of a delicious meal.")
      ),
      listeningExercise = CulturalListeningExercise("In a Roman Piazza Café", "Buongiorno! Un espresso e un cornetto alla crema?", "Musical Roman Italian", "What is the barista confirming?", listOf("An espresso and a cream croissant for breakfast", "An afternoon train ticket", "Closing the bar", "Selling luggage"), 0, "The classic Italian morning breakfast ritual."),
      speakingSituation = CulturalSpeakingSituation("Ordering Gelato in Florence", "At an artisanal gelateria near the Duomo.", "¿Quali gusti desidera?", "Which flavors would you like?", "Vorrei pistacchio di Bronte e cioccolato fondente, per favore.", "vor-RAY pee-STAHK-kyoh", "I would like Bronte pistachio and dark chocolate, please", "Compliment the artisanal quality with a warm smile."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("Coffee Rules", "☕", "Drink cappuccino only before 11:00 AM; drink un caffè (espresso) after meals.", "Avoid ordering a milky cappuccino immediately after a large dinner.", "Italians consider milk heavy for digestion following savory meals."),
        CulturalEtiquetteItem("Pasta Integrity", "🍝", "Twirl spaghetti using only your fork against the curve of the bowl.", "Never cut spaghetti with a knife or use a spoon if you can avoid it.", "Shows respect for the pasta texture and Italian culinary tradition.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("it_colosseum", "The Colosseum & Roman Forum", "Rome", "🏛️", "The grand amphitheater of the Roman Empire standing timeless in Rome.", "Marco Rossi", "Roman Antiquities Scholar", "Buongiorno e benvenuti nella Città Eterna!", "Resonant Roman Italian", "Welcome to the Eternal City!", "La dolce vita", "Iconic Italian concept", "The sweet, good life")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("it_rome_naples", "Rome vs. Naples", "Rome (Romano)", "🇮🇹", "Che fai di bello stasera?", "Lively capital Italian", "Naples (Napoletano)", "🍕", "Uè, che se dice a Napule?", "Singing southern rhythm", "Neapolitan Italian is poetic, deeply musical, and UNESCO-recognized.")
      ),
      missions = listOf(
        CultureMission("MISSION_IT_VOCAB", "Master 5 Italian Dining Phrases", "Learn authentic trattoria phrasing.", 50, "VOCAB"),
        CultureMission("MISSION_IT_SPEAK", "Order Gelato in Florence", "Speak with the gelataio aloud.", 75, "SPEAKING"),
        CultureMission("MISSION_IT_STAMP", "Earn Your Italy Visa Stamp", "Achieve top score on the Italy Culture Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("it_q1", "When do Italians customarily drink cappuccino?", listOf("In the morning for breakfast (before 11 AM)", "Exclusively at midnight", "Only while running", "Never in public"), 0, "Cappuccino is viewed as a morning breakfast beverage in Italy."),
        CultureQuizQuestion("it_q2", "How should you properly eat spaghetti in Italy?", listOf("Twirled neatly with a fork against the curve of the plate, without cutting", "Cut into tiny pieces with a steak knife", "Eaten with chopsticks only", "Blended into soup"), 0, "Cutting pasta is considered a faux pas in Italian dining.")
      ),
      stages = build10Stages(
        countryCode = "IT",
        countryName = "Italy",
        langCode = "it",
        langName = "Italian",
        capital = "Rome",
        greetingInTarget = "Buongiorno! È un vero piacere fare la sua conoscenza.",
        greetingPhonetic = "bwon-ZHOR-noh! eh oon VEH-roh pyah-CHEH-reh",
        greetingMeaning = "Good morning! It is a true pleasure to make your acquaintance.",
        foodPhraseInTarget = "Qual è la specialità dello chef? Vorrei gli spaghetti alla carbonara.",
        foodPhrasePhonetic = "Traditional Roman order",
        foodPhraseMeaning = "What is the chef's specialty? I will have spaghetti carbonara.",
        marketPhraseInTarget = "Buongiorno, questo olio extravergine d'oliva è toscano?",
        marketPhrasePhonetic = "Florence market query",
        marketPhraseMeaning = "Good day, is this extra virgin olive oil from Tuscany?",
        transitPhraseInTarget = "Mi scusi, da quale binario parte il Frecciarossa per Venezia?",
        transitPhrasePhonetic = "Train station platform query",
        transitPhraseMeaning = "Excuse me, which track does the high-speed train to Venice depart from?",
        etiquettePhraseInTarget = "Grazie di cuore per la splendida ospitalità e la cena squisita!",
        etiquettePhrasePhonetic = "Heartfelt Italian dinner guest thanks",
        etiquettePhraseMeaning = "Heartfelt thanks for the splendid hospitality and exquisite dinner!",
        festivalName = "Carnevale di Venezia & Palio di Siena",
        festivalPhraseInTarget = "Buon Carnevale a tutti! Viva la festa!",
        festivalPhrasePhonetic = "Festive Venetian cheer",
        festivalPhraseMeaning = "Happy Carnival to everyone! Long live the celebration!",
        dialectPhraseA = "Andiamo a prendere un caffè! (Standard)",
        dialectPhraseB = "Jamme a piglià 'o cafè! (Neapolitan)",
        dialectRegionA = "Rome & Florence",
        dialectRegionB = "Naples & South"
      )
    ),

    // 12. CHINA (CN)
    CountryCultureJourney(
      countryCode = "CN",
      countryName = "China",
      flagEmoji = "🇨🇳",
      languageCode = "zh",
      capitalCity = "Beijing",
      cefrRange = "A1 – C1 Mandarin Immersion",
      passportStampCode = "PASSPORT_CN",
      motto = "Harmony (Hexie), Filial Piety & Shared Prosperity",
      culturalOverview = "Chinese culture is anchored in thousands of years of philosophy, tea ceremonies, banquet hospitality, filial respect, and the dynamic modern pulse of technological hubs like Shanghai and Shenzhen.",
      survivalVocab = listOf(
        CulturalVocabCard("你好！很高兴认识你", "Nǐ hǎo! Hěn gāoxìng rènshi nǐ", "Hello! Very pleased to meet you", "Universal polite opening greeting in Mandarin."),
        CulturalVocabCard("谢谢 / 不客气", "Xièxie / Bù kèqi", "Thank you / You are welcome", "Everyday courtesy phrases across China."),
        CulturalVocabCard("买单，谢谢", "Mǎidān, xièxie", "The check please, thank you", "Used politely to ask for the bill at restaurants.")
      ),
      listeningExercise = CulturalListeningExercise("In a Beijing Teahouse", "您好！几位？请问想喝什么茶？", "Nín hǎo! Jǐ wèi? Qǐngwèn xiǎng hē shénme chá?", "What is the teahouse host asking?", listOf("Greeting politely and asking how many guests and which tea they prefer", "Asking for train tickets", "Closing the tea shop", "Directing to an airport"), 0, "Traditional Chinese tea hospitality greeting."),
      speakingSituation = CulturalSpeakingSituation("Ordering Dim Sum in Shanghai", "At a lively morning tea restaurant near Yu Garden.", "您想点什么点心？有小笼包和虾饺。", "What dim sum would you like?", "请给我一笼小笼包和一壶龙井茶，谢谢。", "Qǐng gěi wǒ yì lóng xiǎolóngbāo", "Please give me a basket of soup dumplings and Longjing tea", "Hold your tea cup with both hands when being served."),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("Tea Pouring & Tapping", "🍵", "Tap your index and middle finger lightly on the table when someone pours tea for you.", "Never pour tea only for yourself without filling others' cups first.", "Finger tapping is the traditional silent thank-you gesture originating from the Qing dynasty."),
        CulturalEtiquetteItem("Banquets & Face (Mianzi)", "🥢", "Praise the hosts and taste dishes from the rotating lazy susan respectfully.", "Never turn the turntable while someone is actively reaching for food.", "Shows attentiveness to dining companions.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination("cn_great_wall", "The Great Wall of China", "Beijing", "🏯", "The majestic ancient stone dragon winding across the mist-shrouded northern mountains.", "Li Wei", "Beijing Historical Curator", "你好！欢迎来到万里的长城！", "Nǐ hǎo! Huānyíng lái dào wànlǐ de chángchéng!", "Welcome to the Great Wall of China!", "不到长城非好汉", "Famous proverb", "He who has not climbed the Great Wall is not a true hero")
      ),
      regionalDialects = listOf(
        RegionalDialectComparison("cn_beijing_shanghai", "Beijing Mandarin vs. Shanghainese", "Beijing (Standard Putonghua)", "🇨🇳", "您吃了吗？", "Nín chī le ma? (With Erhua sound)", "Shanghai (Wu Dialect)", "🌆", "侬饭吃了伐？", "Nong ve chi le va?", "Beijing Mandarin features rich 'Erhua' rhotics, while Shanghainese has soft musical Wu tones.")
      ),
      missions = listOf(
        CultureMission("MISSION_CN_VOCAB", "Master 5 Mandarin Courtesy Phrases", "Practice survival Mandarin expressions.", 50, "VOCAB"),
        CultureMission("MISSION_CN_SPEAK", "Order Tea in Beijing", "Complete the teahouse dialogue aloud.", 75, "SPEAKING"),
        CultureMission("MISSION_CN_STAMP", "Earn Your China Visa Stamp", "Achieve top score on the China Culture Quiz.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion("cn_q1", "What does tapping two fingers on the table mean when someone pours your tea in China?", listOf("A silent, polite gesture of 'thank you'", "An insult to the server", "A request to bring the bill immediately", "A demand to stop talking"), 0, "Tapping fingers mimics a respectful bow of gratitude."),
        CultureQuizQuestion("cn_q2", "What is the significance of giving red envelopes (Hongbao) during Chinese New Year?", listOf("They contain money symbolizing good luck, prosperity, and blessings for the coming year", "They are invitations to a wedding", "They are recipes for dumplings", "They are receipts for taxes"), 0, "Red envelopes bestow luck and blessings upon younger generations.")
      ),
      stages = build10Stages(
        countryCode = "CN",
        countryName = "China",
        langCode = "zh",
        langName = "Chinese",
        capital = "Beijing",
        greetingInTarget = "您好！初次见面，非常高兴认识您。",
        greetingPhonetic = "Nín hǎo! Chūcì jiànmiàn, fēicháng gāoxìng rènshi nín.",
        greetingMeaning = "Hello! Meeting for the first time, very pleased to meet you.",
        foodPhraseInTarget = "请问这里的招牌菜是什么？给我一份北京烤鸭，谢谢。",
        foodPhrasePhonetic = "Beijing banquet order",
        foodPhraseMeaning = "What is the signature dish here? Please bring one Peking duck, thanks.",
        marketPhraseInTarget = "师傅，请问这件景泰蓝工艺品是纯手工制作的吗？",
        marketPhrasePhonetic = "Traditional artisan market query",
        marketPhraseMeaning = "Master, is this cloisonné art piece handcrafted by hand?",
        transitPhraseInTarget = "请问去故宫博物院该坐哪一路地铁？",
        transitPhrasePhonetic = "Beijing transit query",
        transitPhraseMeaning = "Excuse me, which subway line goes to the Palace Museum?",
        etiquettePhraseInTarget = "非常感谢您的热情款待，今天过得非常愉快！",
        etiquettePhrasePhonetic = "Heartfelt banquet host thanks",
        etiquettePhraseMeaning = "Thank you so much for your warm hospitality; today was wonderful!",
        festivalName = "Spring Festival (Chinese New Year)",
        festivalPhraseInTarget = "新年快乐，恭喜发财，万事如意！",
        festivalPhrasePhonetic = "Traditional Spring Festival blessing",
        festivalPhraseMeaning = "Happy New Year, may you enjoy prosperity and fulfill all wishes!",
        dialectPhraseA = "吃了么您内？ (Beijing Putonghua)",
        dialectPhraseB = "侬好呀，进来坐坐！ (Shanghainese)",
        dialectRegionA = "Beijing & North",
        dialectRegionB = "Shanghai & Yangtze Delta"
      )
    )
  )

  fun getAllJourneys(): List<CountryCultureJourney> {
    return curatedJourneys
  }

  fun getJourneyForCountry(countryCode: String): CountryCultureJourney {
    val clean = countryCode.trim().uppercase()
    return curatedJourneys.firstOrNull { it.countryCode.equals(clean, ignoreCase = true) }
      ?: curatedJourneys.firstOrNull { it.languageCode.equals(clean.lowercase(), ignoreCase = true) }
      ?: getJourneyForLanguage(clean.lowercase())
  }

  fun getCountriesForLanguage(languageCode: String): List<CountryCultureJourney> {
    val clean = languageCode.trim().lowercase()
    val matching = curatedJourneys.filter { it.languageCode.equals(clean, ignoreCase = true) }
    return if (matching.isNotEmpty()) matching else listOf(getJourneyForLanguage(clean))
  }

  /**
   * Returns a curated CountryCultureJourney for [languageCode], optionally preferring [preferredCountryCode].
   * If the language is English, supports US, GB, CA, AU.
   * If the language is Spanish, supports ES, MX.
   */
  fun getJourneyForLanguage(languageCode: String, preferredCountryCode: String? = null): CountryCultureJourney {
    val cleanLang = languageCode.lowercase().trim()
    if (!preferredCountryCode.isNullOrBlank()) {
      val match = curatedJourneys.firstOrNull {
        it.countryCode.equals(preferredCountryCode.trim(), ignoreCase = true)
      }
      if (match != null) return match
    }

    // Default primary country when language is selected:
    val primaryCode = when (cleanLang) {
      "en" -> "US"
      "es" -> "ES"
      "ja" -> "JP"
      "fr" -> "FR"
      "de" -> "DE"
      "ko" -> "KR"
      "it" -> "IT"
      "zh" -> "CN"
      else -> null
    }

    if (primaryCode != null) {
      val found = curatedJourneys.firstOrNull { it.countryCode == primaryCode }
      if (found != null) return found
    }

    val existing = curatedJourneys.firstOrNull { it.languageCode.equals(cleanLang, ignoreCase = true) }
    if (existing != null) return existing

    val lang = Language.findByCode(cleanLang)
    val countryName = "${lang.name}-Speaking World"
    val capitalCity = "Cultural Capital"
    val stampCode = "PASSPORT_${cleanLang.uppercase()}"
    val greeting = "Hello! Welcome to ${lang.name} cultural immersion." to "Polite opening greeting"

    return CountryCultureJourney(
      countryCode = cleanLang.uppercase(),
      countryName = countryName,
      flagEmoji = lang.flagEmoji,
      languageCode = lang.code,
      capitalCity = capitalCity,
      cefrRange = "A1 – C2 Global Immersion",
      passportStampCode = stampCode,
      motto = "Hospitality, Heritage & Authentic Dialogue",
      culturalOverview = "Explore the living traditions, hospitality customs, regional expressions, and real-world conversational etiquette of $countryName (${lang.nativeName}).",
      survivalVocab = listOf(
        CulturalVocabCard(greeting.first, greeting.second, "Hello! Pleased to meet you.", "Warm, respectful opening greeting when meeting native speakers."),
        CulturalVocabCard("Thank you very much", "Polite gratitude", "Thank you very much for your kindness", "Expressing sincere gratitude opens doors in every community."),
        CulturalVocabCard("Please / Excuse me", "Polite courtesy", "Pardon me, could you please help?", "Polite consideration in public venues.")
      ),
      listeningExercise = CulturalListeningExercise(
        settingTitle = "Welcome at a Local Venue in $capitalCity",
        audioScriptInTarget = greeting.first,
        phoneticHint = greeting.second,
        question = "What is the cultural purpose of this warm opening greeting?",
        options = listOf("To welcome a guest and express pleasure in meeting them", "To ask for directions to the airport", "To close a shop for the evening", "To negotiate a fare"),
        correctIndex = 0,
        culturalExplanation = "Greeting locals in their native tongue immediately builds trust and cultural rapport."
      ),
      speakingSituation = CulturalSpeakingSituation(
        situationTitle = "Meeting a Local Host in $capitalCity",
        scenarioSetting = "You have just arrived at a cultural exchange meetup in $capitalCity.",
        localSpeakerLine = greeting.first,
        localSpeakerTranslation = "Hello! Welcome—it is wonderful to meet you!",
        suggestedResponseInTarget = greeting.first,
        suggestedPhonetic = greeting.second,
        suggestedMeaning = "Hello! It is a pleasure to meet you too!",
        etiquetteTip = "Speak with a warm smile and match the host's level of formality."
      ),
      etiquetteRules = listOf(
        CulturalEtiquetteItem("Greetings & First Impressions", "🤝", "Greet elders and hosts first using polite expressions.", "Avoid rushing into requests without exchanging pleasantries.", "Personal warmth and mutual respect are foundational."),
        CulturalEtiquetteItem("Dining & Hospitality", "🍲", "Compliment the host on regional dishes and try a little of everything offered.", "Avoid refusing hospitality abruptly without a polite thank-you.", "Sharing food is a primary symbol of friendship.")
      ),
      virtualLandmarks = listOf(
        VirtualTravelDestination(
          id = "${cleanLang}_capital_square",
          landmarkName = "Historic Cultural Quarter",
          city = capitalCity,
          badgeEmoji = lang.flagEmoji,
          sceneDescription = "Bustling cafés, historic architecture, and welcoming locals gathering in the heart of $capitalCity.",
          localPersonaName = "Cultural Ambassador",
          localPersonaRole = "Local Guide",
          openingGreetingInTarget = greeting.first,
          openingGreetingPhonetic = greeting.second,
          openingGreetingTranslation = "Hello and welcome! What brings you to our country today?",
          streetPhraseInTarget = greeting.first,
          streetPhrasePhonetic = greeting.second,
          streetPhraseMeaning = "Welcome friend, pleased to meet you!"
        )
      ),
      regionalDialects = listOf(
        RegionalDialectComparison(
          id = "${cleanLang}_dialect_1",
          conceptTitle = "Standard vs. Regional Expressions",
          regionAName = "$capitalCity (Standard)",
          regionAFlag = lang.flagEmoji,
          regionAPhrase = greeting.first,
          regionAPhonetic = greeting.second,
          regionBName = "Regional Dialect",
          regionBFlag = "🌍",
          regionBPhrase = greeting.first,
          regionBPhonetic = greeting.second,
          culturalNuance = "Standard dialect is used in official media, while regional dialects add authentic warmth."
        )
      ),
      missions = listOf(
        CultureMission("MISSION_${cleanLang.uppercase()}_VOCAB", "Master $countryName Essential Phrases", "Practice the core cultural vocabulary with native audio.", 50, "VOCAB"),
        CultureMission("MISSION_${cleanLang.uppercase()}_SPEAK", "Speak with a Host in $capitalCity", "Complete the live speaking situation aloud.", 75, "SPEAKING"),
        CultureMission("MISSION_${cleanLang.uppercase()}_STAMP", "Earn Your $countryName Visa Stamp", "Complete the Cultural Quiz Battle to stamp your passport.", 100, "QUIZ")
      ),
      quizBattleQuestions = listOf(
        CultureQuizQuestion(
          id = "${cleanLang}_q1",
          question = "What is the best way to begin a conversation with a native speaker in $capitalCity?",
          options = listOf("Start with a warm, polite greeting in their language", "Speak loudly without greeting first", "Avoid eye contact and point silently", "Demand immediate service"),
          correctIndex = 0,
          explanation = "Opening with a genuine greeting shows cultural respect and builds instant connection."
        ),
        CultureQuizQuestion(
          id = "${cleanLang}_q2",
          question = "Why is meal-sharing etiquette important in $countryName?",
          options = listOf("Sharing food is a central expression of hospitality and friendship", "Meals must be eaten in under 3 minutes", "Talking at the table is forbidden", "Guests never thank the host"),
          correctIndex = 0,
          explanation = "Hospitality around the table unites people across communities."
        )
      ),
      stages = build10Stages(
        countryCode = cleanLang.uppercase(),
        countryName = countryName,
        langCode = cleanLang,
        langName = lang.name,
        capital = capitalCity,
        greetingInTarget = greeting.first,
        greetingPhonetic = greeting.second,
        greetingMeaning = "Pleased to meet you; wonderful to connect with you",
        foodPhraseInTarget = "What do you recommend as the regional specialty? I would love to try it.",
        foodPhrasePhonetic = "Polite restaurant inquiry",
        foodPhraseMeaning = "Ordering local culinary specialties",
        marketPhraseInTarget = "Excuse me, was this handcrafted locally in your region?",
        marketPhrasePhonetic = "Market craft query",
        marketPhraseMeaning = "Inquiring about artisan heritage crafts",
        transitPhraseInTarget = "Excuse me, which line or gate takes me to the city center?",
        transitPhrasePhonetic = "Clear transit inquiry",
        transitPhraseMeaning = "Navigating public transit politely",
        etiquettePhraseInTarget = "Thank you so much for your wonderful hospitality and warm welcome!",
        etiquettePhrasePhonetic = "Heartfelt guest gratitude",
        etiquettePhraseMeaning = "Expressing sincere thanks to local hosts",
        festivalName = "Traditional Cultural Festival",
        festivalPhraseInTarget = "Happy festival celebrations to you and your family!",
        festivalPhrasePhonetic = "Festive seasonal greeting",
        festivalPhraseMeaning = "Sharing joy during cultural celebrations",
        dialectPhraseA = "Formal standard expression (Capital)",
        dialectPhraseB = "Warm colloquial expression (Regional)",
        dialectRegionA = "Capital Region",
        dialectRegionB = "Coastal & Southern Regions"
      )
    )
  }
}
