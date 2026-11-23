package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class ChatMessage(
  val id: String,
  val sender: MessageSender,
  val text: String,
  val translation: String? = null,
  val phoneticGuide: String? = null,
  val grammarFeedback: String? = null,
  val timestampEpoch: Long = System.currentTimeMillis()
)

enum class MessageSender {
  USER,
  AI_TUTOR
}

data class RoleplayScenario(
  val id: String,
  val title: String,
  val roleName: String,
  val description: String,
  val iconEmoji: String,
  val systemPromptInstruction: String,
  val openingGreetingTemplate: String,
  val taskObjective: String = "Complete a natural 4-turn conversation and achieve your communication goal.",
  val keyVocabulary: List<String> = listOf("Polite greeting", "Clarifying question", "Confirmation")
) {
  companion object {
    val SCENARIOS = listOf(
      RoleplayScenario(
        id = "restaurant_waiter",
        title = "Ordering Food at a Restaurant",
        roleName = "Waiter",
        description = "Order delicious regional dishes, ask about specials or allergies, and request the check.",
        iconEmoji = "🍽️",
        systemPromptInstruction = "You are an enthusiastic restaurant server. Recommend the chef's special, take their food and drink orders, and inquire about any dietary restrictions.",
        openingGreetingTemplate = "Good evening! Here are your menus. Would you like to start with some sparkling water or an appetizer?",
        taskObjective = "Ask about the daily special, order a main course and drink, mention a dietary preference, and ask for the bill.",
        keyVocabulary = listOf("I would like to order...", "What do you recommend?", "Does it contain...?", "The bill, please")
      ),
      RoleplayScenario(
        id = "airport_employee",
        title = "Airport Check-in & Customs",
        roleName = "Airport Staff",
        description = "Navigate gate departures, baggage drop-off, seat selection, and passport control.",
        iconEmoji = "✈️",
        systemPromptInstruction = "You are an airport gate and check-in agent. Check their boarding pass, ask for passport identification, inquire about checked luggage, and direct them to their departure gate.",
        openingGreetingTemplate = "Hello! May I please see your passport and flight booking confirmation?",
        taskObjective = "Check in for your flight, request a window or aisle seat, check one suitcase, and confirm your boarding gate.",
        keyVocabulary = listOf("Boarding pass", "Window / aisle seat", "Checked luggage", "Which gate?")
      ),
      RoleplayScenario(
        id = "hotel_receptionist",
        title = "Hotel Reservations & Check-in",
        roleName = "Hotel Receptionist",
        description = "Check into your boutique room, request extra amenities, and ask about breakfast and local attractions.",
        iconEmoji = "🏨",
        systemPromptInstruction = "You are a warm, courteous hotel receptionist welcoming a guest. Help them check in, ask for their reservation details, and answer questions about breakfast hours and WiFi.",
        openingGreetingTemplate = "Welcome! Good day. How may I assist you with your reservation today?",
        taskObjective = "Confirm your reservation under your name, ask what time breakfast is served, and request the Wi-Fi password.",
        keyVocabulary = listOf("I have a reservation", "What time is breakfast?", "Late check-out", "Wi-Fi password")
      ),
      RoleplayScenario(
        id = "job_interviewer",
        title = "Job Interview",
        roleName = "Hiring Manager",
        description = "Practice introducing your professional background, strengths, and solving workplace questions.",
        iconEmoji = "💼",
        systemPromptInstruction = "You are an encouraging and professional hiring manager conducting a first interview. Ask the learner about their experience, problem solving, and career goals.",
        openingGreetingTemplate = "Welcome to our interview. Please tell me a little about yourself and what interests you about this role.",
        taskObjective = "Introduce your professional background, share one key strength with an example, and ask a question about the team.",
        keyVocabulary = listOf("Years of experience", "My main strength is...", "Team collaboration", "Professional growth")
      ),
      RoleplayScenario(
        id = "university_admission",
        title = "University Admission Interview",
        roleName = "Admissions Dean",
        description = "Discuss your academic goals, chosen degree program, scholarship application, and campus life.",
        iconEmoji = "🎓",
        systemPromptInstruction = "You are a welcoming University Admissions Dean interviewing a prospective international student. Ask why they chose this major, their academic interests, and how they plan to contribute to campus life.",
        openingGreetingTemplate = "Welcome to the university admissions interview! Tell me, what inspired you to apply for this degree program?",
        taskObjective = "Explain why you chose your field of study, highlight an academic achievement, and ask about research or campus opportunities.",
        keyVocabulary = listOf("Academic background", "Research interest", "Scholarship program", "Campus community")
      ),
      RoleplayScenario(
        id = "customer_service",
        title = "Shopping & Customer Service",
        roleName = "Store Specialist",
        description = "Ask for sizes, colors, return policies, or exchange a defective item at customer service.",
        iconEmoji = "🛍️",
        systemPromptInstruction = "You are an attentive store associate and customer service specialist. Help the customer find sizes/colors or process an exchange/refund with a receipt.",
        openingGreetingTemplate = "Hello! Are you looking for anything in particular today, or can I help you with an exchange?",
        taskObjective = "Ask for a different size or color, inquire about the store return policy, and complete your purchase or exchange.",
        keyVocabulary = listOf("Do you have this in medium?", "Can I try it on?", "Return receipt", "Refund or exchange")
      ),
      RoleplayScenario(
        id = "doctor_consultation",
        title = "Doctor Appointment",
        roleName = "Doctor",
        description = "Explain common health symptoms, ask about treatment options, and understand medical advice.",
        iconEmoji = "🩺",
        systemPromptInstruction = "You are a gentle, attentive doctor listening to a patient's symptoms. Ask when the symptoms started, check for allergies, and advise on care and rest.",
        openingGreetingTemplate = "Hello, please have a seat. What brings you to the clinic today?",
        taskObjective = "Describe two symptoms and when they started, mention whether you have any allergies, and confirm how to take the prescribed medicine.",
        keyVocabulary = listOf("I have had a fever since...", "Sore throat / headache", "Allergic to...", "Twice a day after meals")
      ),
      RoleplayScenario(
        id = "friendly_coffee",
        title = "Meeting New Friends",
        roleName = "Local Friend",
        description = "Break the ice at a café, talk about hobbies, weekend plans, favorite movies, and travel dreams.",
        iconEmoji = "☕",
        systemPromptInstruction = "You are a fun, friendly local peer meeting a new international friend for coffee. Talk casually, use natural everyday expressions, and ask about their hobbies and hometown.",
        openingGreetingTemplate = "Hey there! It is so great to meet you. What do you enjoy doing in your free time?",
        taskObjective = "Share two of your hobbies, ask your new friend about their favorite local hangout spot, and suggest meeting again this weekend.",
        keyVocabulary = listOf("In my free time...", "What kind of music/food...?", "Have you ever been to...?", "Let's hang out this weekend")
      ),
      RoleplayScenario(
        id = "workplace_discussion",
        title = "Workplace Discussions",
        roleName = "Project Colleague",
        description = "Coordinate project deadlines, share status updates in a standup meeting, and resolve blockers.",
        iconEmoji = "📊",
        systemPromptInstruction = "You are a collaborative workplace colleague in a weekly project sync. Discuss current tasks, timeline adjustments, and how to divide responsibilities fairly.",
        openingGreetingTemplate = "Good morning! Before our team meeting starts, how is your part of the project coming along?",
        taskObjective = "Give a brief status update on your current task, mention one challenge, and agree on a deadline for the next milestone.",
        keyVocabulary = listOf("Current progress", "Ahead of / behind schedule", "Let's prioritize...", "By Friday afternoon")
      ),
      RoleplayScenario(
        id = "asking_directions",
        title = "Asking for Directions",
        roleName = "Helpful Local",
        description = "Find your way to the historic museum, nearest metro station, or pharmacy using city landmarks.",
        iconEmoji = "🗺️",
        systemPromptInstruction = "You are a polite local resident on a busy street. Give clear step-by-step directions using landmarks (turn left at the square, go straight two blocks, next to the bank).",
        openingGreetingTemplate = "Excuse me! You look a little lost. Are you looking for a landmark or metro station nearby?",
        taskObjective = "Ask politely how to get to the central museum, confirm whether it is within walking distance, and repeat the directions back to be sure.",
        keyVocabulary = listOf("Excuse me, how do I get to...?", "Turn left / right", "Go straight ahead", "How many minutes on foot?")
      ),
      RoleplayScenario(
        id = "train_station_transit",
        title = "Public Transportation & Trains",
        roleName = "Station Agent",
        description = "Buy a transit card, change a missed train ticket, find platform numbers, and navigate transfers.",
        iconEmoji = "🚄",
        systemPromptInstruction = "You are a helpful rail and metro station agent assisting a traveler who needs to buy a ticket, check transfer lines, and find the right platform.",
        openingGreetingTemplate = "Good morning! Next departure to the capital is in twenty minutes. Where are you traveling today?",
        taskObjective = "Purchase a round-trip ticket, ask which platform the train departs from, and check if a transfer is required.",
        keyVocabulary = listOf("Round-trip / one-way ticket", "Which platform?", "Do I need to transfer?", "Next departure")
      ),
      RoleplayScenario(
        id = "street_market_bargaining",
        title = "Street Market Bargaining",
        roleName = "Artisan Vendor",
        description = "Negotiate prices politely, ask about handmade crafts, and learn local market etiquette.",
        iconEmoji = "🏮",
        systemPromptInstruction = "You are a cheerful local market vendor selling handcrafted souvenirs and spices. Negotiate prices in a friendly cultural way and explain what makes your crafts special.",
        openingGreetingTemplate = "Welcome to my stall! Everything here is handmade locally. Which piece caught your eye today?",
        taskObjective = "Ask about the material of a handmade item, inquire about the price, and politely negotiate a bundle discount.",
        keyVocabulary = listOf("How much is this?", "Is it handmade?", "Could you give me a better price?", "I'll take two")
      ),
      RoleplayScenario(
        id = "apartment_rental",
        title = "Apartment Viewing & Lease",
        roleName = "Property Manager",
        description = "Inquire about monthly rent, utilities, neighborhood safety, and lease terms.",
        iconEmoji = "🔑",
        systemPromptInstruction = "You are a helpful property manager showing an apartment in the city center. Answer questions about rent, utilities, public transit, and lease duration.",
        openingGreetingTemplate = "Hello! Welcome to the apartment. As you can see, the living room gets plenty of natural light. What questions do you have about the lease?",
        taskObjective = "Ask whether utilities are included in the monthly rent, check the security deposit amount, and ask about nearby transit.",
        keyVocabulary = listOf("Monthly rent", "Are utilities included?", "Security deposit", "Minimum lease term")
      ),
      RoleplayScenario(
        id = "business_negotiation",
        title = "Executive Business Pitch",
        roleName = "Client Director",
        description = "Present a project proposal, handle timeline questions, and close an international partnership.",
        iconEmoji = "🤝",
        systemPromptInstruction = "You are an international client director listening to a partnership pitch. Ask smart questions about deliverables, timelines, and collaboration.",
        openingGreetingTemplate = "Thank you for joining our executive briefing today. Could you walk us through the key highlights of your proposal?",
        taskObjective = "Summarize your proposal's value, address a budget or timeline question, and propose a follow-up contract call.",
        keyVocabulary = listOf("Key deliverables", "Return on investment", "Implementation timeline", "Mutual partnership")
      ),
      RoleplayScenario(
        id = "cultural_festival",
        title = "Local Cultural Festival",
        roleName = "Festival Host",
        description = "Experience traditional music, regional holiday customs, and local street food specialties.",
        iconEmoji = "🎭",
        systemPromptInstruction = "You are an enthusiastic local host at a traditional cultural festival. Share the history of the celebration, local idioms, and traditional dishes.",
        openingGreetingTemplate = "Happy festival day! Have you tried our traditional holiday pastries yet, or is this your first time celebrating with us?",
        taskObjective = "Ask about the historical meaning of the festival, try a traditional dish, and learn a festive greeting.",
        keyVocabulary = listOf("What does this tradition celebrate?", "Traditional dish", "Local custom", "Happy holidays!")
      ),
      RoleplayScenario(
        id = "pharmacy_urgent_care",
        title = "Pharmacy & First Aid",
        roleName = "Pharmacist",
        description = "Describe allergies or minor ailments, ask for over-the-counter remedies, and dosage rules.",
        iconEmoji = "💊",
        systemPromptInstruction = "You are a knowledgeable pharmacist. Ask about symptoms and any allergies, and clearly explain how often to take the recommended remedy.",
        openingGreetingTemplate = "Hello! How can I help you feel better today? Do you have a prescription or are you looking for over-the-counter relief?",
        taskObjective = "Ask for an over-the-counter remedy, check possible side effects, and confirm the daily dosage.",
        keyVocabulary = listOf("Over-the-counter", "Side effects", "Every six hours", "With food or water")
      ),
      RoleplayScenario(
        id = "bank_and_sim_setup",
        title = "Bank & Mobile SIM Setup",
        roleName = "Account Specialist",
        description = "Open a local bank account or activate an international eSIM and mobile data plan.",
        iconEmoji = "📱",
        systemPromptInstruction = "You are a courteous telecom and banking specialist helping an international newcomer set up a local SIM card and mobile banking account.",
        openingGreetingTemplate = "Welcome! Are you looking to set up a local prepaid data plan or open a checking account today?",
        taskObjective = "Compare two mobile data plans or checking account options, provide identification details, and activate your service.",
        keyVocabulary = listOf("Unlimited data plan", "Monthly fee", "Passport identification", "Activate SIM card")
      ),
      RoleplayScenario(
        id = "university_seminar",
        title = "University Seminar Debate",
        roleName = "Professor",
        description = "Express complex opinions on technology, environment, and global culture at B2–C2 level.",
        iconEmoji = "🏛️",
        systemPromptInstruction = "You are a university seminar professor leading a thoughtful discussion on technology and cultural exchange. Challenge the student to articulate nuanced opinions.",
        openingGreetingTemplate = "Welcome to today's seminar. In your view, how does learning a second language transform the way we perceive the world?",
        taskObjective = "Present a structured argument with a concrete example, respond to a counterargument, and synthesize your conclusion.",
        keyVocabulary = listOf("From my perspective...", "On the other hand...", "Empirical evidence suggests...", "In conclusion")
      ),
      RoleplayScenario(
        id = "teacher_mentor",
        title = "Language Classroom",
        roleName = "Language Teacher",
        description = "Discuss grammar nuances, clarify homework doubts, and practice cultural idioms.",
        iconEmoji = "📚",
        systemPromptInstruction = "You are an inspiring, friendly language teacher. Gently correct pronunciation and encourage the student to practice speaking in full sentences.",
        openingGreetingTemplate = "Hello! Today we are practicing daily conversations. What is your favorite hobby to do on weekends?",
        taskObjective = "Practice speaking in complete sentences, ask one grammar question, and use a new idiom.",
        keyVocabulary = listOf("How do you say...?", "Could you explain...?", "On weekends I enjoy...", "For example")
      ),
      RoleplayScenario(
        id = "hospital_appointment",
        title = "Hospital Appointments & Triage",
        roleName = "Hospital Triage Nurse",
        description = "Check in at the hospital clinic, explain symptoms, verify health insurance, and schedule an urgent specialist consultation.",
        iconEmoji = "🏥",
        systemPromptInstruction = "You are a professional hospital triage nurse. Inquire about the patient's symptoms, severity, and existing conditions, and direct them to the appropriate medical department.",
        openingGreetingTemplate = "Hello, welcome to Central Clinic. Please take a number. What symptoms are you experiencing today?",
        taskObjective = "Explain what pain or symptoms you have, state how long it has persisted, and provide identification to schedule a consultation.",
        keyVocabulary = listOf("Emergency room", "I have sharp pain in...", "Health insurance card", "Specialist doctor")
      ),
      RoleplayScenario(
        id = "telephone_conversation",
        title = "Telephone Conversations & Bookings",
        roleName = "Customer Support Agent",
        description = "Handle telephone calls, request information, clarify audio misunderstandings, and confirm an appointment over the phone.",
        iconEmoji = "📞",
        systemPromptInstruction = "You are a telephone customer support specialist. Speak with professional phone etiquette, verify the caller's details, and repeat information clearly.",
        openingGreetingTemplate = "Thank you for calling Customer Support. My name is Alex. How may I direct your call today?",
        taskObjective = "State the reason for your call, ask to speak to the booking department, clarify a misunderstood sentence, and confirm the reservation.",
        keyVocabulary = listOf("Could you repeat that?", "I am calling regarding...", "May I speak with...?", "Please hold the line")
      ),
      RoleplayScenario(
        id = "emergency_services",
        title = "Emergency Situations & Dispatch",
        roleName = "Emergency Dispatcher",
        description = "Call emergency services, report an accident, provide your exact location, and follow safety instructions calmly.",
        iconEmoji = "🚨",
        systemPromptInstruction = "You are a calm, decisive emergency dispatch operator. Ask the caller for their emergency type (police, fire, medical), their location, and keep them calm while dispatching help.",
        openingGreetingTemplate = "Emergency Services, what is the address and nature of your emergency?",
        taskObjective = "Clearly state whether you need medical, police, or fire assistance, give your location landmark, and describe the situation.",
        keyVocabulary = listOf("I need an ambulance", "There has been an accident", "The address is...", "Please hurry!")
      )
    )

    fun findById(id: String): RoleplayScenario {
      return SCENARIOS.firstOrNull { it.id == id } ?: SCENARIOS.first()
    }
  }
}

data class ExamStrengthQuote(
  val point: String,
  val quote: String
)

data class ExamMistakeQuote(
  val said: String,
  val better: String,
  val reason: String
)

data class ExamNextStepLab(
  val text: String,
  val labId: String
)

data class ExamPromptComparison(
  val promptIndex: Int,
  val prompt: String,
  val userAnswer: String,
  val improvedAnswer: String,
  val wordCount: Int,
  val relevanceScore: Int,
  val isVoice: Boolean
)

data class SpeakingAssessmentResult(
  val cefrBand: String,
  val overallScore: Int,
  val pronunciationScore: Int,
  val accentIntonationScore: Int,
  val grammarAccuracyScore: Int,
  val vocabularyRichnessScore: Int,
  val correctedTranscript: String,
  val pronunciationFeedback: String,
  val grammarFeedback: String,
  val personalizedNextStep: String,
  val isScored: Boolean = true,
  val isPartialUnrated: Boolean = false,
  val unscoredReason: String? = null,
  val relevanceScore: Int = 85,
  val fluencyScore: Int = 85,
  val coherenceScore: Int = 85,
  val isVoiceResponse: Boolean = false,
  val showPronunciationScore: Boolean = true,
  val confidenceLabel: String = "Medium",
  val totalWordsCount: Int = 0,
  val strengths: List<ExamStrengthQuote> = emptyList(),
  val mistakes: List<ExamMistakeQuote> = emptyList(),
  val improvedAnswer: String = correctedTranscript,
  val nextSteps: List<ExamNextStepLab> = emptyList(),
  val promptComparisons: List<ExamPromptComparison> = emptyList()
)

object SpeakingExamPromptBank {
  fun minWordsForLevel(level: String): Int = when (level.trim().uppercase()) {
    "A1" -> 8
    "A2" -> 15
    "B1" -> 30
    "B2" -> 50
    "C1" -> 70
    "C2" -> 90
    else -> 15 // Placement default
  }

  fun allPromptsForLevel(level: String, languageName: String): List<Pair<String, String>> {
    val cleanLevel = level.trim().uppercase()
    return when (cleanLevel) {
      "A1" -> listOf(
        "a1_1" to "Introduce yourself in $languageName: say your name, where you live, and what you like to eat.",
        "a1_2" to "Describe your morning routine in $languageName using simple sentences.",
        "a1_3" to "Talk about your family or friends and what you do together on weekends in $languageName.",
        "a1_4" to "Order your favorite drink and snack at a café in $languageName.",
        "a1_5" to "Describe your home or bedroom and list three objects inside it in $languageName.",
        "a1_6" to "Say what the weather is like today and what clothes you are wearing in $languageName.",
        "a1_7" to "Explain what hobbies or sports you enjoy in your free time in $languageName.",
        "a1_8" to "Ask for directions to a train station or supermarket and thank the person in $languageName."
      )
      "A2" -> listOf(
        "a2_1" to "Describe your last vacation or weekend trip in $languageName, including where you went and with whom.",
        "a2_2" to "Explain what you usually do at work or school and what part of the day you enjoy most in $languageName.",
        "a2_3" to "Tell a friend about a favorite meal you cooked or ate at a restaurant in $languageName.",
        "a2_4" to "Explain a small problem at a hotel room (such as no hot water or Wi-Fi) and ask for help in $languageName.",
        "a2_5" to "Describe your hometown and recommend two places a visitor should see in $languageName.",
        "a2_6" to "Talk about your plans for next month and one goal you want to achieve in $languageName.",
        "a2_7" to "Describe a movie, book, or show you watched recently and why you liked it in $languageName.",
        "a2_8" to "Explain how you travel to work or school and compare taking the bus with walking in $languageName."
      )
      "B1" -> listOf(
        "b1_1" to "Describe a memorable travel experience in $languageName and explain how you handled an unexpected delay or challenge.",
        "b1_2" to "Compare learning a language through real conversations versus studying grammar books in $languageName.",
        "b1_3" to "Explain a healthy habit you started recently and how it has changed your daily energy in $languageName.",
        "b1_4" to "Describe a cultural festival or tradition from your country to someone visiting for the first time in $languageName.",
        "b1_5" to "Discuss the pros and cons of living in a big city compared to a small town in $languageName.",
        "b1_6" to "Tell a story about a time you helped a colleague or friend solve a difficult problem in $languageName.",
        "b1_7" to "Explain how you would prepare for an important job interview in $languageName.",
        "b1_8" to "Discuss how smartphones and social media affect friendships and daily communication in $languageName."
      )
      "B2" -> listOf(
        "b2_1" to "Discuss the advantages and drawbacks of remote and hybrid work for both employees and companies in $languageName.",
        "b2_2" to "Argue whether cities should ban private cars from downtown centers to reduce pollution in $languageName.",
        "b2_3" to "Explain how learning a foreign language changes the way people perceive other cultures and perspectives in $languageName.",
        "b2_4" to "Analyze a significant decision you made in your career or education and what you would advise others in $languageName.",
        "b2_5" to "Discuss how artificial intelligence tools are transforming education and what skills remain uniquely human in $languageName.",
        "b2_6" to "Evaluate the balance between tourism growth and preserving local neighborhoods in popular destinations in $languageName.",
        "b2_7" to "Describe a complex conflict in a team project and explain how transparent communication resolved it in $languageName.",
        "b2_8" to "Discuss whether financial literacy and practical life skills should replace traditional subjects in high schools in $languageName."
      )
      "C1" -> listOf(
        "c1_1" to "Critically evaluate the ethical implications of generative AI in journalism, creative industries, and public discourse in $languageName.",
        "c1_2" to "Analyze how globalization shapes linguistic diversity and whether minority languages can thrive in a digital era in $languageName.",
        "c1_3" to "Discuss the tension between individual privacy rights and predictive data analytics in modern healthcare and governance in $languageName.",
        "c1_4" to "Examine how urban architecture and public spaces influence civic trust, social cohesion, and mental well-being in $languageName.",
        "c1_5" to "Evaluate whether four-day workweeks can sustain economic productivity across diverse service and manufacturing sectors in $languageName.",
        "c1_6" to "Discuss the psychological factors behind misinformation spread and propose structural solutions in $languageName."
      )
      "C2" -> listOf(
        "c2_1" to "Synthesize the philosophical and socio-economic arguments surrounding universal basic income in an automated post-industrial economy in $languageName.",
        "c2_2" to "Deconstruct the relationship between linguistic relativity, cognitive framing, and cross-cultural diplomatic negotiation in $languageName.",
        "c2_3" to "Evaluate how algorithmic curation alters epistemic authority, democratic deliberation, and cultural memory in $languageName.",
        "c2_4" to "Critique the paradox of sustainable growth within global supply chains and propose an actionable policy framework in $languageName.",
        "c2_5" to "Analyze the evolution of rhetoric and persuasion from classical oratory to contemporary digital ecosystems in $languageName.",
        "c2_6" to "Discuss the long-term bioethical trade-offs of gene editing and longevity technologies on intergenerational equity in $languageName."
      )
      else -> listOf(
        "pl_1" to "Introduce yourself, your daily routine, and what motivates you to learn $languageName.",
        "pl_2" to "Describe a memorable travel journey, cultural tradition, or favorite regional dish in $languageName.",
        "pl_3" to "Explain how you would handle an unexpected travel delay or hotel reservation issue in $languageName.",
        "pl_4" to "Discuss the advantages and disadvantages of remote work and artificial intelligence in modern careers in $languageName.",
        "pl_5" to "Compare living in a lively metropolitan city with living near nature in a quiet town in $languageName.",
        "pl_6" to "Describe a skill or project you are proud of and explain how you overcame obstacles while learning it in $languageName.",
        "pl_7" to "Explain how technology has changed the way people learn languages and connect across countries in $languageName.",
        "pl_8" to "Discuss a public issue in your community and propose two practical solutions in $languageName."
      )
    }
  }

  /**
   * Selects 4 exam prompts for the given level, avoiding the last 10 used prompt IDs when possible.
   */
  fun selectFourPrompts(
    level: String,
    languageName: String,
    recentPromptIds: List<String>
  ): Pair<List<String>, List<String>> {
    val pool = allPromptsForLevel(level, languageName)
    val fresh = pool.filterNot { it.first in recentPromptIds }
    val chosen = if (fresh.size >= 4) {
      fresh.take(4)
    } else {
      (fresh + pool.filter { it.first in recentPromptIds }).distinctBy { it.first }.take(4)
    }
    return chosen.map { it.first } to chosen.map { it.second }
  }
}

data class WordPronunciationItem(
  val targetWord: String,
  val spokenWord: String,
  val status: String, // "MATCH", "CLOSE", "NEEDS_WORK"
  val phonemeTip: String = ""
)

data class PronunciationDiagnosticResult(
  val targetPhrase: String,
  val recognizedTranscript: String,
  val accuracyPercent: Int,
  val ipaTranscription: String,
  val stressedSyllablesGuide: String,
  val wordBreakdown: List<WordPronunciationItem>,
  val problematicSounds: List<String>,
  val tongueAndMouthTip: String,
  val intonationAdvice: String,
  val regionalAccentNote: String,
  val timestampEpoch: Long = System.currentTimeMillis()
)

data class WritingCorrectionItem(
  val originalSegment: String,
  val correctedSegment: String,
  val category: String, // "Grammar", "Spelling", "Word Choice", "Style & Register"
  val isGenuineError: Boolean, // true = Grammar/Spelling error; false = Optional style improvement
  val explanationNative: String
)

data class WritingCoachResult(
  val originalText: String,
  val correctedText: String,
  val writingFormat: String, // "Sentence", "Message", "Paragraph", "Essay"
  val cefrEstimate: String,
  val overallScore: Int,
  val summaryFeedbackNative: String,
  val corrections: List<WritingCorrectionItem>,
  val naturalExpressions: List<String>,
  val targetedExercisePrompt: String,
  val targetedExerciseAnswer: String
)

data class MistakeNotebookEntry(
  val id: String = "mistake_${System.currentTimeMillis()}",
  val languageCode: String,
  val originalText: String,
  val correctedText: String,
  val category: String,
  val isGenuineError: Boolean = true,
  val explanationNative: String,
  val practicePrompt: String = "",
  val practiceAnswer: String = "",
  val resolvedCount: Int = 0,
  val timestampEpoch: Long = System.currentTimeMillis()
)

data class RoleplayEvaluationReport(
  val scenarioId: String,
  val scenarioTitle: String,
  val objectiveAchieved: Boolean,
  val scorePercent: Int,
  val taskFeedback: String,
  val grammarAndStyleNotes: List<String>,
  val suggestedUpgrades: List<String>
)

@IgnoreExtraProperties
data class SpeakingSession(
  @DocumentId val id: String = "",
  val userId: String = "",
  val languageCode: String = "",
  val mode: String = "Tutor", // Tutor, Roleplay
  val scenarioTitle: String = "",
  val proficiencyLevel: String = "Beginner",
  val turnsCount: Long = 1L,
  val fluencyScore: Long = 85L,
  val grammarFeedbackSummary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CameraDetectedItem(
  val detectedObjectOrText: String,
  val nameInTargetLanguage: String,
  val phoneticGuide: String,
  val nativeTranslation: String,
  val grammarNote: String,
  val exampleSentenceTarget: String,
  val exampleSentenceNative: String
)

data class CameraSceneResult(
  val sceneTitle: String,
  val sceneDescription: String,
  val items: List<CameraDetectedItem>
)

data class InterviewQuestionItem(
  val id: String,
  val profession: String,
  val roleTitle: String,
  val questionPrompt: String,
  val questionTranslation: String,
  val hintKeywords: List<String>
)

data class InterviewEvaluationResult(
  val scorePercent: Int,
  val professionalRating: String, // "Strong Hire", "Promising Candidate", "Needs Practice"
  val spokenTranscript: String,
  val grammarCorrections: List<String>,
  val enhancedProfessionalVersion: String,
  val vocabularyFeedback: String,
  val clarityAdvice: String
)


