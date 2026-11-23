package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Language
import com.example.data.model.LingoVideo
import com.example.data.model.VideoAiStudyPack
import com.example.data.model.VideoPronunciationDrill
import com.example.data.model.VideoQuizQuestion
import com.example.data.model.VideoVocabInsight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoAiAssistantService {
  private const val TAG = "VideoAiAssistant"

  /**
   * Generates a complete AI Video Study Pack (Vocabulary, Pronunciation Drills, and Comprehension Quiz)
   * from the video's authorized caption summary, transcript, and lesson metadata using cost-efficient Gemini.
   */
  suspend fun generateVideoStudyPack(
    video: LingoVideo,
    targetLanguage: Language,
    nativeLanguage: Language,
    authorizedCaptionTracks: List<String> = emptyList(),
    customTranscriptExcerpt: String = ""
  ): VideoAiStudyPack = withContext(Dispatchers.IO) {
    val captionStatus = when {
      customTranscriptExcerpt.isNotBlank() -> "Authorized Caption / Transcript Excerpt Provided"
      authorizedCaptionTracks.isNotEmpty() -> "Synchronized Captions (${authorizedCaptionTracks.joinToString(", ")})"
      video.hasCaptions && video.captionSummary.isNotBlank() -> "Interactive Closed Captions & Lesson Script Available"
      else -> "Generated from Lesson Curriculum & Description"
    }

    val contextSource = buildString {
      append("Video Title: ${video.title}\n")
      append("Channel: ${video.channelTitle}\n")
      append("Proficiency Level: ${video.proficiencyLevel}\n")
      append("Category: ${video.topicCategory}\n")
      append("Description: ${video.description}\n")
      if (video.captionSummary.isNotBlank()) {
        append("Authorized Captions / Transcript Summary:\n${video.captionSummary}\n")
      }
      if (customTranscriptExcerpt.isNotBlank()) {
        append("Additional Transcript Excerpt:\n$customTranscriptExcerpt\n")
      }
    }

    val prompt = """
      You are the LingoSphere Videos AI Learning Assistant.
      Target Language being learned: ${targetLanguage.name} (${targetLanguage.code}).
      Learner's Native Language: ${nativeLanguage.name} (${nativeLanguage.code}).
      
      Analyze the following educational YouTube video content and authorized captions/transcript:
      $contextSource
      
      Generate a structured study pack in EXACTLY the following line-by-line format (no markdown code blocks or asterisks):
      SUMMARY: <2 concise sentences in ${nativeLanguage.name} explaining what the learner will master from this video>
      GRAMMAR_NOTE: <1 practical grammar or cultural insight from this video in ${nativeLanguage.name} with an example in ${targetLanguage.name}>
      VOCAB: <Word/Phrase in ${targetLanguage.name}> | <Phonetic/IPA> | <Translation in ${nativeLanguage.name}> | <Part of Speech> | <Example sentence in ${targetLanguage.name}>
      VOCAB: <Word/Phrase 2> | <Phonetic> | <Translation> | <Part of Speech> | <Example sentence>
      VOCAB: <Word/Phrase 3> | <Phonetic> | <Translation> | <Part of Speech> | <Example sentence>
      VOCAB: <Word/Phrase 4> | <Phonetic> | <Translation> | <Part of Speech> | <Example sentence>
      VOCAB: <Word/Phrase 5> | <Phonetic> | <Translation> | <Part of Speech> | <Example sentence>
      DRILL: <Spoken phrase from video in ${targetLanguage.name}> | <Phonetic guide> | <Meaning in ${nativeLanguage.name}> | <Pronunciation mouth/intonation tip in ${nativeLanguage.name}>
      DRILL: <Spoken phrase 2 in ${targetLanguage.name}> | <Phonetic guide> | <Meaning in ${nativeLanguage.name}> | <Pronunciation tip>
      DRILL: <Spoken phrase 3 in ${targetLanguage.name}> | <Phonetic guide> | <Meaning in ${nativeLanguage.name}> | <Pronunciation tip>
      QUIZ: <Question in ${nativeLanguage.name} testing comprehension/vocabulary from this video> | <Option A> | <Option B> | <Option C> | <Option D> | <Correct index 0, 1, 2, or 3> | <Brief explanation in ${nativeLanguage.name}>
      QUIZ: <Question 2> | <Option A> | <Option B> | <Option C> | <Option D> | <Correct index 0, 1, 2, or 3> | <Brief explanation>
      QUIZ: <Question 3> | <Option A> | <Option B> | <Option C> | <Option D> | <Correct index 0, 1, 2, or 3> | <Brief explanation>
    """.trimIndent()

    val hasTestHook = GeminiTutorService.testResponseInterceptor != null ||
      AiProviderManager.openAiBackendTestInterceptor != null ||
      GeminiService.testBypassDailyQuota
    val isPro = com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement ||
      GeminiService.quotaUiState.value.isProUser
    if (!isPro && !hasTestHook) {
      return@withContext buildContextualStudyPackFallback(video, targetLanguage, nativeLanguage, captionStatus)
    }

    try {
      val rawText = GeminiTutorService.generateGeminiText(
        systemInstructionText = "You are the LingoSphere Videos AI Learning Assistant.",
        userPromptText = prompt,
        temperature = 0.4f,
        featureCategory = GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT
      )
      if (!rawText.isNullOrBlank()) {
        parseStudyPackResponse(rawText, video, targetLanguage, nativeLanguage, captionStatus)
      } else {
        buildContextualStudyPackFallback(video, targetLanguage, nativeLanguage, captionStatus)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Study pack generation fallback: ${e.message}")
      buildContextualStudyPackFallback(video, targetLanguage, nativeLanguage, captionStatus)
    }
  }

  /**
   * Answers learner questions about vocabulary, grammar, cultural context, or captions in the active video.
   */
  suspend fun askQuestionAboutVideo(
    video: LingoVideo,
    userQuestion: String,
    targetLanguage: Language,
    nativeLanguage: Language
  ): String = withContext(Dispatchers.IO) {
    val hasTestHook = GeminiTutorService.testResponseInterceptor != null ||
      AiProviderManager.openAiBackendTestInterceptor != null ||
      GeminiService.testBypassDailyQuota
    val isPro = com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement ||
      GeminiService.quotaUiState.value.isProUser
    if (!isPro && !hasTestHook) {
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.AI_UPGRADE,
        featureKey = "ai.video.qa",
        headline = "Unlock AI learning",
        reason = "Interactive AI video Q&A is included with LingoSphere Pro."
      )
      return@withContext "Unlock AI learning with LingoSphere Pro to ask follow-up questions about any video."
    }

    val prompt = """
      You are the LingoSphere Videos AI Learning Tutor helping a student watching a ${targetLanguage.name} educational video lesson.
      Video Title: ${video.title}
      Channel: ${video.channelTitle}
      Level: ${video.proficiencyLevel}
      Video Description & Caption Context: ${video.description} ${video.captionSummary}
      Student's Native Language: ${nativeLanguage.name}
      
      Student Question: "$userQuestion"
      
      Provide a clear, encouraging, and concise answer (3-5 sentences max) in ${nativeLanguage.name}, including ${targetLanguage.name} examples and pronunciation phonetics where helpful. Do not use markdown headers.
    """.trimIndent()

    try {
      GeminiTutorService.generateGeminiText(
        systemInstructionText = "You are the LingoSphere Videos AI Learning Tutor.",
        userPromptText = prompt,
        temperature = 0.5f,
        featureCategory = GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT
      )?.takeIf { it.isNotBlank() }
        ?: "In \"${video.title}\", focus on repeating the key ${targetLanguage.name} phrases aloud along with the speaker. Notice how native speakers connect words smoothly in ${video.topicCategory.lowercase()}."
    } catch (e: Exception) {
      Log.w(TAG, "Video Q&A fallback: ${e.message}")
      AiErrorHandler.handle(throwable = e, contextTag = "VideoQa").message
    }
  }

  private fun parseStudyPackResponse(
    rawText: String,
    video: LingoVideo,
    targetLanguage: Language,
    nativeLanguage: Language,
    captionStatus: String
  ): VideoAiStudyPack {
    var summary = ""
    var grammarNote = ""
    val vocabList = mutableListOf<VideoVocabInsight>()
    val drillList = mutableListOf<VideoPronunciationDrill>()
    val quizList = mutableListOf<VideoQuizQuestion>()

    rawText.lines().forEach { line ->
      val trimmed = line.trim()
      when {
        trimmed.startsWith("SUMMARY:", ignoreCase = true) -> {
          summary = trimmed.substringAfter(":").trim()
        }
        trimmed.startsWith("GRAMMAR_NOTE:", ignoreCase = true) -> {
          grammarNote = trimmed.substringAfter(":").trim()
        }
        trimmed.startsWith("VOCAB:", ignoreCase = true) -> {
          val parts = trimmed.substringAfter(":").split("|").map { it.trim() }
          if (parts.size >= 5) {
            vocabList.add(
              VideoVocabInsight(
                term = parts[0],
                phonetic = parts[1],
                translation = parts[2],
                partOfSpeech = parts[3],
                contextSentence = parts[4]
              )
            )
          }
        }
        trimmed.startsWith("DRILL:", ignoreCase = true) -> {
          val parts = trimmed.substringAfter(":").split("|").map { it.trim() }
          if (parts.size >= 4) {
            drillList.add(
              VideoPronunciationDrill(
                phraseInTarget = parts[0],
                phoneticGuide = parts[1],
                nativeMeaning = parts[2],
                coachingTip = parts[3]
              )
            )
          }
        }
        trimmed.startsWith("QUIZ:", ignoreCase = true) -> {
          val parts = trimmed.substringAfter(":").split("|").map { it.trim() }
          if (parts.size >= 7) {
            val options = listOf(parts[1], parts[2], parts[3], parts[4])
            val correctIdx = parts[5].toIntOrNull()?.coerceIn(0, 3) ?: 0
            quizList.add(
              VideoQuizQuestion(
                question = parts[0],
                options = options,
                correctIndex = correctIdx,
                explanation = parts[6]
              )
            )
          }
        }
      }
    }

    val fallback = buildContextualStudyPackFallback(video, targetLanguage, nativeLanguage, captionStatus)
    return VideoAiStudyPack(
      videoId = video.videoId,
      summaryInNative = summary.ifBlank { fallback.summaryInNative },
      culturalOrGrammarNote = grammarNote.ifBlank { fallback.culturalOrGrammarNote },
      captionSourceStatus = captionStatus,
      vocabulary = vocabList.ifEmpty { fallback.vocabulary },
      pronunciationDrills = drillList.ifEmpty { fallback.pronunciationDrills },
      quizQuestions = quizList.ifEmpty { fallback.quizQuestions }
    )
  }

  private fun buildContextualStudyPackFallback(
    video: LingoVideo,
    targetLanguage: Language,
    nativeLanguage: Language,
    captionStatus: String
  ): VideoAiStudyPack {
    return when (targetLanguage.code.lowercase()) {
      "es" -> VideoAiStudyPack(
        videoId = video.videoId,
        summaryInNative = "This lesson in \"${video.title}\" builds practical Spanish listening comprehension and conversational fluency for ${video.proficiencyLevel} learners.",
        culturalOrGrammarNote = "In Spanish conversation, polite requests use 'Quisiera' (I would like) or '¿Me puede traer...?' rather than direct imperatives.",
        captionSourceStatus = captionStatus,
        vocabulary = listOf(
          VideoVocabInsight("Mucho gusto", "MOO-choh GOOS-toh", "Nice to meet you", "Phrase", "¡Hola! Mucho gusto en conocerte."),
          VideoVocabInsight("Por supuesto", "por soo-PWES-toh", "Of course", "Idiom", "Por supuesto que podemos practicar español hoy."),
          VideoVocabInsight("Madrugar", "mah-droo-GAHR", "To wake up early", "Verb", "Me gusta madrugar para tomar café y estudiar."),
          VideoVocabInsight("Vale la pena", "BAH-leh lah PEH-nah", "It is worth it", "Expression", "Aprender un nuevo idioma siempre vale la pena.")
        ),
        pronunciationDrills = listOf(
          VideoPronunciationDrill(
            phraseInTarget = "Mucho gusto en conocerte, ¿de dónde eres?",
            phoneticGuide = "MOO-choh GOOS-toh en koh-noh-SER-teh, deh DOHN-deh EH-res?",
            nativeMeaning = "Nice to meet you, where are you from?",
            coachingTip = "Keep Spanish vowels crisp and short (a, e, i, o, u) without English diphthongs."
          ),
          VideoPronunciationDrill(
            phraseInTarget = "Quisiera pedir una mesa para dos personas, por favor.",
            phoneticGuide = "kee-SYEH-rah peh-DEER OO-nah MEH-sah PAH-rah dohs per-SOH-nahs",
            nativeMeaning = "I would like to request a table for two people, please.",
            coachingTip = "Tap the single 'r' in 'para' lightly against the roof of your mouth."
          ),
          VideoPronunciationDrill(
            phraseInTarget = "Estudio todos los días para hablar con fluidez.",
            phoneticGuide = "es-TOO-dyoh TOH-dohs lohs DEE-ahs PAH-rah ah-BLAHR kohn floo-ee-DETH",
            nativeMeaning = "I study every day in order to speak fluently.",
            coachingTip = "Remember that 'h' in 'hablar' is completely silent."
          )
        ),
        quizQuestions = listOf(
          VideoQuizQuestion(
            question = "Which Spanish expression from this lesson means 'Nice to meet you'?",
            options = listOf("Mucho gusto", "Hasta luego", "Buen provecho", "Lo siento"),
            correctIndex = 0,
            explanation = "'Mucho gusto' (or 'Encantado/a') is the standard warm way to say 'Nice to meet you'."
          ),
          VideoQuizQuestion(
            question = "When expressing a goal or purpose ('in order to speak fluently'), which preposition is used?",
            options = listOf("Para", "Por", "Sin", "Desde"),
            correctIndex = 0,
            explanation = "'Para' + infinitive expresses purpose or goal ('para hablar con fluidez')."
          ),
          VideoQuizQuestion(
            question = "What does the polite phrase 'Quisiera...' mean when ordering in a restaurant?",
            options = listOf("I would like...", "Where is...", "I don't have...", "How much is..."),
            correctIndex = 0,
            explanation = "'Quisiera' is a polite, natural way to say 'I would like...' in Spanish."
          )
        )
      )

      "fr" -> VideoAiStudyPack(
        videoId = video.videoId,
        summaryInNative = "This lesson in \"${video.title}\" strengthens French listening, liaison awareness, and everyday conversational phrasing.",
        culturalOrGrammarNote = "Always begin interactions in France with 'Bonjour' (or 'Bonsoir' in the evening) before asking a question.",
        captionSourceStatus = captionStatus,
        vocabulary = listOf(
          VideoVocabInsight("Enchanté", "ahn-shahn-TAY", "Delighted to meet you", "Adjective", "Bonjour, je m'appelle Marc, enchanté !"),
          VideoVocabInsight("S'il vous plaît", "seel voo PLEH", "Please (formal)", "Phrase", "Un café et un croissant, s'il vous plaît."),
          VideoVocabInsight("Cependant", "suh-pahn-DAHN", "However / Yet", "Connector", "C'est exigeant, cependant c'est très utile."),
          VideoVocabInsight("Avoir hâte", "ah-vwar AHT", "To look forward to", "Idiom", "J'ai hâte de visiter Paris cet été.")
        ),
        pronunciationDrills = listOf(
          VideoPronunciationDrill(
            phraseInTarget = "Bonjour ! Comment allez-vous aujourd'hui ?",
            phoneticGuide = "bohn-ZHOOR! koh-mahn tah-lay VOO oh-zhoor-DWEE?",
            nativeMeaning = "Hello! How are you doing today?",
            coachingTip = "Link the 't' of 'Comment' into 'allez' (koh-mahn-tah-lay) for smooth French liaison."
          ),
          VideoPronunciationDrill(
            phraseInTarget = "Excusez-moi, pourriez-vous répéter plus lentement ?",
            phoneticGuide = "eks-kew-zay MWAH, poo-ryay voo ray-pay-tay ploo lahnt-MAHN?",
            nativeMeaning = "Excuse me, could you repeat more slowly?",
            coachingTip = "Round your lips tightly for the French 'u' in 'plus'."
          )
        ),
        quizQuestions = listOf(
          VideoQuizQuestion(
            question = "What is the polite French way to say 'Please' to someone you don't know well?",
            options = listOf("S'il vous plaît", "De rien", "À bientôt", "Bien sûr"),
            correctIndex = 0,
            explanation = "'S'il vous plaît' uses the formal pronoun 'vous' to politely say 'Please'."
          ),
          VideoQuizQuestion(
            question = "What happens in French pronunciation between 'Comment' and 'allez-vous'?",
            options = listOf("Liaison links the final 't' sound to the next vowel", "Both words drop their vowels", "The 'v' becomes silent", "The stress moves to the first syllable"),
            correctIndex = 0,
            explanation = "French liaison pronounces the normally silent 't' before the vowel in 'allez'."
          ),
          VideoQuizQuestion(
            question = "What does the French expression 'J'ai hâte de...' mean?",
            options = listOf("I look forward to / can't wait to...", "I forgot to...", "I am afraid of...", "I just finished..."),
            correctIndex = 0,
            explanation = "'Avoir hâte de' expresses excitement and anticipation."
          )
        )
      )

      else -> VideoAiStudyPack(
        videoId = video.videoId,
        summaryInNative = "This ${targetLanguage.name} video lesson (\"${video.title}\") covers key vocabulary, natural pronunciation, and listening comprehension for ${video.proficiencyLevel} learners.",
        culturalOrGrammarNote = "Pay close attention to sentence rhythm and polite register when practicing ${targetLanguage.name} dialogues aloud.",
        captionSourceStatus = captionStatus,
        vocabulary = listOf(
          VideoVocabInsight("${targetLanguage.nativeName} Greeting", "Polite opening", "Hello / Good day", "Phrase", "Used to greet native ${targetLanguage.name} speakers warmly."),
          VideoVocabInsight("Active Listening", "Comprehension", "To understand in context", "Skill", "Shadowing native speakers in ${targetLanguage.name} builds natural fluency."),
          VideoVocabInsight("Polite Request", "Conversational", "Could you help me, please?", "Expression", "Essential for travel and daily conversations in ${targetLanguage.name}."),
          VideoVocabInsight("Daily Routine", "Fluency", "Everyday activities", "Topic", "Describing your day in ${targetLanguage.name} reinforces core verbs.")
        ),
        pronunciationDrills = listOf(
          VideoPronunciationDrill(
            phraseInTarget = video.captionSummary.lines().firstOrNull()?.take(80)?.ifBlank { video.title } ?: video.title,
            phoneticGuide = "Listen to the native speaker's cadence and match the stress pattern",
            nativeMeaning = "Key phrase from ${video.title}",
            coachingTip = "Use the Listen button first, then tap Record to compare your pronunciation."
          )
        ),
        quizQuestions = listOf(
          VideoQuizQuestion(
            question = "What is the primary learning focus of \"${video.title}\"?",
            options = listOf(
              "${targetLanguage.name} ${video.topicCategory} (${video.proficiencyLevel})",
              "Ancient Latin poetry translation",
              "Silent reading without audio",
              "Unrelated technical programming"
            ),
            correctIndex = 0,
            explanation = "This video targets ${targetLanguage.name} ${video.topicCategory} at the ${video.proficiencyLevel} level."
          ),
          VideoQuizQuestion(
            question = "Which technique is most effective when studying with this ${targetLanguage.name} video?",
            options = listOf(
              "Shadowing key phrases aloud and reviewing new vocabulary in context",
              "Watching on mute without captions",
              "Skipping all pronunciation practice",
              "Memorizing isolated words without sentences"
            ),
            correctIndex = 0,
            explanation = "Combining active listening with spoken shadowing and contextual vocabulary accelerates fluency."
          ),
          VideoQuizQuestion(
            question = "How can you retain new ${targetLanguage.name} words from this video long-term?",
            options = listOf(
              "Save them to your LingoSphere Spaced-Repetition Flashcards and practice speaking",
              "Never review them again",
              "Only read them once",
              "Close the video immediately"
            ),
            correctIndex = 0,
            explanation = "Saving video vocabulary to Leitner flashcards ensures spaced-repetition mastery."
          )
        )
      )
    }
  }
}
