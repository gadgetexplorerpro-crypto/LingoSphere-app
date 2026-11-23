package com.example.ai

import com.example.data.model.ConversationRescueResult
import com.example.data.model.ConversationRescueSuggestion
import com.example.data.model.ConversationRepairScenario
import com.example.data.model.ConversationRepairTurnResult
import com.example.data.model.DebateTopic
import com.example.data.model.DebateTurnEvaluation
import com.example.data.model.EmergencyPhraseItem
import com.example.data.model.FutureMeComparisonResult
import com.example.data.model.FutureMePracticeDrill
import com.example.data.model.Language
import com.example.data.model.LevelSpecificExplanation
import com.example.data.model.LivingWorldCharacterProgress
import com.example.data.model.LivingWorldLocation
import com.example.data.model.LivingWorldTurnOutcome
import com.example.data.model.RealLifeObjectRecognition
import com.example.data.model.SpeakWhatYouSeeEvaluation
import com.example.data.model.SpeakWhatYouSeeQuestion
import com.example.data.model.TeachAiTopicOption
import com.example.data.model.TeachAiTurnEvaluation
import com.example.data.model.ThreeWayComprehensionCheck
import com.example.data.model.ThreeWayExplanationResult
import org.json.JSONArray
import org.json.JSONObject

/**
 * AI service powering the 12 differentiated practical language-learning features.
 * Uses Firebase AI Logic (`GeminiTutorService.generateGeminiText`) with structured JSON
 * responses and deterministic linguistic analysis of actual learner speech/text.
 */
object PracticalFeaturesAiService {

    private fun extractJsonObject(raw: String): JSONObject? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end <= start) return null
        return runCatching { JSONObject(raw.substring(start, end + 1)) }.getOrNull()
    }

    private suspend fun callGemini(prompt: String, temperature: Float): String? {
        val hasTestHook = GeminiTutorService.testResponseInterceptor != null ||
            AiProviderManager.openAiBackendTestInterceptor != null ||
            GeminiService.testBypassDailyQuota
        val isPro = com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement ||
            GeminiService.quotaUiState.value.isProUser
        if (!isPro && !hasTestHook) {
            AiErrorHandler.requestUpgrade(
                mode = UpgradeSheetMode.AI_UPGRADE,
                featureKey = "ai.labs",
                headline = "Unlock AI learning",
                reason = "Practical AI Labs are included with LingoSphere Pro."
            )
            return null
        }
        return GeminiTutorService.generateGeminiText(
            systemInstructionText = "You are an expert multilingual language coach. Return ONLY valid JSON.",
            userPromptText = prompt,
            temperature = temperature
        )
    }

    private fun currentAiFailureReason(): String {
        return AiErrorHandler.sanitizeUserFacingMessage(
            raw = GeminiTutorService.apiHealthStatus.value.lastErrorMessage,
            fallback = "Taking a moment — let's try that again."
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. SPEAK WHAT YOU SEE: Natural Contextual Questions & Answer Correction
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun generateSpeakWhatYouSeeQuestionsFromCameraScene(
        scene: com.example.data.model.CameraSceneResult,
        languageCode: String,
        nativeLanguage: String = "English"
    ): List<SpeakWhatYouSeeQuestion> {
        val converted = RealLifeObjectRecognition(
            sceneTitle = scene.sceneTitle,
            sceneDescription = scene.sceneDescription,
            targetLanguage = Language.findByCode(languageCode).name,
            detectedItems = scene.items.map { item ->
                com.example.data.model.RealLifeDetectedItem(
                    targetWord = item.nameInTargetLanguage,
                    nativeWord = item.nativeTranslation,
                    pronunciationGuide = item.phoneticGuide,
                    exampleSentence = item.exampleSentenceTarget,
                    exampleTranslation = item.exampleSentenceNative,
                    contextualExplanation = item.detectedObjectOrText
                )
            }
        )
        return generateSpeakWhatYouSeeQuestions(converted, languageCode, nativeLanguage)
    }

    suspend fun generateSpeakWhatYouSeeQuestions(
        scene: RealLifeObjectRecognition,
        languageCode: String,
        nativeLanguage: String = "English"
    ): List<SpeakWhatYouSeeQuestion> {
        val langName = Language.findByCode(languageCode).name
        val objectSummary = scene.detectedItems.joinToString("; ") {
            "${it.targetWord} (${it.nativeWord})"
        }
        val prompt = """
            You are a contextual $langName speaking coach for "Speak What You See".
            The user captured a photo of: "${scene.sceneTitle}" (${scene.sceneDescription}).
            Detected objects: $objectSummary.
            Generate 3 natural, open-ended speaking questions in $langName about these surroundings so the learner practices describing and interacting with what they see.
            Return ONLY valid JSON:
            {
              "questions": [
                {
                  "detectedObject": "word in $langName",
                  "detectedObjectTranslation": "word in $nativeLanguage",
                  "questionTargetLanguage": "Natural open question in $langName",
                  "questionNativeTranslation": "Translation in $nativeLanguage",
                  "suggestedVocabulary": ["word1", "word2", "word3"],
                  "sampleAnswerTargetLanguage": "Complete natural model answer in $langName",
                  "sampleAnswerRomanization": "Pronunciation guide"
                }
              ]
            }
        """.trimIndent()

        val raw = callGemini(prompt, temperature = 0.35f)
        val json = raw?.let { extractJsonObject(it) }
        val arr = json?.optJSONArray("questions")
        if (arr != null && arr.length() > 0) {
            val parsed = mutableListOf<SpeakWhatYouSeeQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val vocabArr = obj.optJSONArray("suggestedVocabulary") ?: JSONArray()
                val vocab = (0 until vocabArr.length()).mapNotNull { vocabArr.optString(it).takeIf(String::isNotBlank) }
                parsed.add(
                    SpeakWhatYouSeeQuestion(
                        detectedObject = obj.optString("detectedObject").ifBlank { scene.detectedItems.firstOrNull()?.targetWord ?: scene.sceneTitle },
                        detectedObjectTranslation = obj.optString("detectedObjectTranslation").ifBlank { scene.detectedItems.firstOrNull()?.nativeWord ?: "Surroundings" },
                        questionTargetLanguage = obj.optString("questionTargetLanguage"),
                        questionNativeTranslation = obj.optString("questionNativeTranslation"),
                        suggestedVocabulary = vocab.ifEmpty { scene.detectedItems.map { it.targetWord }.take(3) },
                        sampleAnswerTargetLanguage = obj.optString("sampleAnswerTargetLanguage"),
                        sampleAnswerRomanization = obj.optString("sampleAnswerRomanization")
                    )
                )
            }
            if (parsed.isNotEmpty()) return parsed
        }

        // Grounded fallback derived directly from the detected scene objects
        return scene.detectedItems.take(3).mapIndexed { index, item ->
            when (index) {
                0 -> SpeakWhatYouSeeQuestion(
                    detectedObject = item.targetWord,
                    detectedObjectTranslation = item.nativeWord,
                    questionTargetLanguage = when (languageCode.lowercase()) {
                        "es" -> "¿Dónde está ${item.targetWord} en esta escena y para qué lo usas hoy?"
                        "fr" -> "Où se trouve ${item.targetWord} autour de vous et comment l'utilisez-vous ?"
                        "ja" -> "この場所にある「${item.targetWord}」について、どんなものか説明してくれますか？"
                        else -> "How would you describe ${item.targetWord} and how you use it right now?"
                    },
                    questionNativeTranslation = "Where is ${item.nativeWord} in this scene and how are you using it today?",
                    suggestedVocabulary = listOf(item.targetWord) + scene.detectedItems.drop(1).take(2).map { it.targetWord },
                    sampleAnswerTargetLanguage = item.exampleSentence,
                    sampleAnswerRomanization = item.pronunciationGuide
                )
                1 -> SpeakWhatYouSeeQuestion(
                    detectedObject = item.targetWord,
                    detectedObjectTranslation = item.nativeWord,
                    questionTargetLanguage = when (languageCode.lowercase()) {
                        "es" -> "Describe cómo es ${item.targetWord} y qué estás haciendo en este lugar."
                        "fr" -> "Décrivez ${item.targetWord} et ce que vous faites en ce moment."
                        "ja" -> "「${item.targetWord}」を使ってこれから何をしますか？"
                        else -> "Describe ${item.targetWord} and what you are doing in this place."
                    },
                    questionNativeTranslation = "Describe ${item.nativeWord} and what you are doing right now.",
                    suggestedVocabulary = scene.detectedItems.map { it.targetWord }.take(3),
                    sampleAnswerTargetLanguage = item.exampleSentence,
                    sampleAnswerRomanization = item.pronunciationGuide
                )
                else -> SpeakWhatYouSeeQuestion(
                    detectedObject = item.targetWord,
                    detectedObjectTranslation = item.nativeWord,
                    questionTargetLanguage = when (languageCode.lowercase()) {
                        "es" -> "¿Qué pedirías o preguntarías sobre ${item.targetWord} si hablaras con alguien aquí?"
                        "fr" -> "Que demanderiez-vous à propos de ${item.targetWord} ici ?"
                        "ja" -> "この「${item.targetWord}」について誰かに質問するなら、何と聞きますか？"
                        else -> "What question would you ask someone here about ${item.targetWord}?"
                    },
                    questionNativeTranslation = "What would you ask or say about ${item.nativeWord} if speaking to someone here?",
                    suggestedVocabulary = scene.detectedItems.map { it.targetWord }.take(3),
                    sampleAnswerTargetLanguage = item.exampleSentence,
                    sampleAnswerRomanization = item.pronunciationGuide
                )
            }
        }
    }

    suspend fun evaluateSpeakWhatYouSeeAnswer(
        question: SpeakWhatYouSeeQuestion,
        spokenAnswer: String,
        languageCode: String,
        nativeLanguage: String = "English"
    ): SpeakWhatYouSeeEvaluation {
        val cleaned = spokenAnswer.trim()
        val langName = Language.findByCode(languageCode).name
        if (cleaned.isBlank()) {
            return SpeakWhatYouSeeEvaluation(
                questionId = question.id,
                learnerSpokenAnswer = "",
                answeredQuestionDirectly = false,
                correctedSentenceTargetLanguage = question.sampleAnswerTargetLanguage,
                grammarAndWordChoiceFeedback = "We didn't catch any spoken words yet. Try using '${question.detectedObject}' in a short sentence.",
                naturalFollowUpPrompt = question.questionTargetLanguage,
                detectedStrengths = emptyList(),
                retryRecommended = true
            )
        }

        val prompt = """
            You are evaluating a learner's spoken answer in $langName for "Speak What You See".
            Question asked ($langName): "${question.questionTargetLanguage}" (${question.questionNativeTranslation})
            Target object: "${question.detectedObject}" (${question.detectedObjectTranslation})
            Suggested vocabulary: ${question.suggestedVocabulary.joinToString(", ")}
            Learner's spoken answer: "$cleaned"
            
            Provide concrete, honest grammar and vocabulary feedback based strictly on what the learner said. Do not invent numeric scores.
            Return ONLY valid JSON:
            {
              "answeredQuestionDirectly": true,
              "correctedSentenceTargetLanguage": "Polished natural version of the learner's sentence in $langName",
              "grammarAndWordChoiceFeedback": "Clear explanation in $nativeLanguage of any grammar/word fixes or why their phrasing worked well",
              "naturalFollowUpPrompt": "A short natural follow-up question in $langName",
              "detectedStrengths": ["Specific strength 1", "Specific strength 2"],
              "retryRecommended": false
            }
        """.trimIndent()

        val raw = callGemini(prompt, temperature = 0.25f)
        val json = raw?.let { extractJsonObject(it) }
        if (json != null && json.optString("correctedSentenceTargetLanguage").isNotBlank()) {
            val strengthsArr = json.optJSONArray("detectedStrengths") ?: JSONArray()
            val strengths = (0 until strengthsArr.length()).mapNotNull { strengthsArr.optString(it).takeIf(String::isNotBlank) }
            return SpeakWhatYouSeeEvaluation(
                questionId = question.id,
                learnerSpokenAnswer = cleaned,
                answeredQuestionDirectly = json.optBoolean("answeredQuestionDirectly", true),
                correctedSentenceTargetLanguage = json.optString("correctedSentenceTargetLanguage"),
                grammarAndWordChoiceFeedback = json.optString("grammarAndWordChoiceFeedback"),
                naturalFollowUpPrompt = json.optString("naturalFollowUpPrompt").ifBlank { question.questionTargetLanguage },
                detectedStrengths = strengths,
                retryRecommended = json.optBoolean("retryRecommended", false)
            )
        }

        // Deterministic linguistic analysis based on actual spoken tokens
        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val usedTargetObject = cleaned.contains(question.detectedObject, ignoreCase = true) ||
            question.suggestedVocabulary.any { cleaned.contains(it, ignoreCase = true) }
        val strengths = buildList {
            if (usedTargetObject) add("Incorporated scene vocabulary (${question.detectedObject}) naturally")
            if (words.size >= 5) add("Formed a complete multi-word clause (${words.size} words)")
        }
        val needsRetry = !usedTargetObject || words.size < 3
        val feedback = when {
            words.size < 3 ->
                "Your response ('$cleaned') was very brief (${words.size} words). Expand it into a full clause mentioning '${question.detectedObject}'."
            !usedTargetObject ->
                "You spoke a clear phrase ('$cleaned'), but try including '${question.detectedObject}' (${question.detectedObjectTranslation}) to answer the scene question directly."
            else ->
                "Well done! You referenced '${question.detectedObject}' in context. Compare your sentence with the natural model phrasing below to refine verb agreement and connectors."
        }

        return SpeakWhatYouSeeEvaluation(
            questionId = question.id,
            learnerSpokenAnswer = cleaned,
            answeredQuestionDirectly = !needsRetry,
            correctedSentenceTargetLanguage = question.sampleAnswerTargetLanguage,
            grammarAndWordChoiceFeedback = feedback,
            naturalFollowUpPrompt = when (languageCode.lowercase()) {
                "es" -> "¿Y qué más notas cerca de ${question.detectedObject}?"
                "fr" -> "Et que remarquez-vous d'autre près de ${question.detectedObject} ?"
                "ja" -> "そのほかに、近くに何が見えますか？"
                else -> "What else do you notice near ${question.detectedObject}?"
            },
            detectedStrengths = strengths.ifEmpty { listOf("Attempted spoken response in $langName") },
            retryRecommended = needsRetry,
            aiErrorMessage = if (raw == null) currentAiFailureReason() else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. CONVERSATION RESCUE: Context-Aware Phrase Suggestions & In-Place Retry
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun generateConversationRescue(
        languageCode: String,
        lastPartnerPrompt: String,
        learnerPartialOrStruggledDraft: String = "",
        sessionTopic: String = "Live AI Speaking Session",
        nativeLanguage: String = "English"
    ): ConversationRescueResult {
        val langName = Language.findByCode(languageCode).name
        val partnerLine = lastPartnerPrompt.ifBlank {
            when (languageCode.lowercase()) {
                "es" -> "¿Qué opinas sobre esto o qué te gustaría hacer ahora?"
                "fr" -> "Qu'en pensez-vous et que souhaitez-vous faire maintenant ?"
                "ja" -> "これについてどう思いますか？次に何をしたいですか？"
                else -> "What do you think about this, and what would you like to say next?"
            }
        }

        val prompt = """
            You are "Conversation Rescue" inside a live $langName speaking session ($sessionTopic).
            The AI partner just said: "$partnerLine"
            The learner is stuck or struggled with: "$learnerPartialOrStruggledDraft"
            
            Help the learner continue the conversation WITHOUT restarting. Provide 3 contextually appropriate $langName responses (1 simple/direct, 1 natural everyday, 1 expressive/polite), with $nativeLanguage explanations.
            Return ONLY valid JSON:
            {
              "conversationContextSummary": "1-sentence summary in $nativeLanguage of what the partner is asking",
              "whyThisMomentIsTricky": "Brief explanation in $nativeLanguage of the grammar or vocabulary hurdle",
              "grammarBridgeTip": "Quick actionable grammar pattern in $nativeLanguage",
              "retryPromptInPlace": "Encouraging instruction to try speaking one of the phrases right now",
              "suggestions": [
                {
                  "targetPhrase": "Natural response in $langName",
                  "romanization": "Pronunciation guide",
                  "nativeMeaning": "Meaning in $nativeLanguage",
                  "registerAndNuanceNote": "Simple / Natural / Polite",
                  "whenToUseExplanation": "Why this fits the partner's prompt"
                }
              ]
            }
        """.trimIndent()

        val raw = callGemini(prompt, temperature = 0.3f)
        val json = raw?.let { extractJsonObject(it) }
        val arr = json?.optJSONArray("suggestions")
        if (json != null && arr != null && arr.length() > 0) {
            val suggestions = mutableListOf<ConversationRescueSuggestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val target = obj.optString("targetPhrase")
                if (target.isNotBlank()) {
                    suggestions.add(
                        ConversationRescueSuggestion(
                            targetPhrase = target,
                            romanization = obj.optString("romanization"),
                            nativeMeaning = obj.optString("nativeMeaning"),
                            RegisterAndNuanceNote = obj.optString("registerAndNuanceNote").ifBlank { "Natural" },
                            whenToUseExplanation = obj.optString("whenToUseExplanation")
                        )
                    )
                }
            }
            if (suggestions.isNotEmpty()) {
                return ConversationRescueResult(
                    languageCode = languageCode,
                    conversationContextSummary = json.optString("conversationContextSummary").ifBlank { "Responding to: \"$partnerLine\"" },
                    whyThisMomentIsTricky = json.optString("whyThisMomentIsTricky").ifBlank { "Choosing the right opening verb and connector under real-time speaking pressure." },
                    suggestions = suggestions,
                    grammarBridgeTip = json.optString("grammarBridgeTip").ifBlank { "Start with a short discourse marker to buy thinking time while staying in $langName." },
                    retryPromptInPlace = json.optString("retryPromptInPlace").ifBlank { "Tap any phrase to hear it or retry speaking right where you left off—your conversation stays active." }
                )
            }
        }

        // Contextual fallback tailored to target language
        val fallbackSuggestions = when (languageCode.lowercase()) {
            "es" -> listOf(
                ConversationRescueSuggestion(
                    targetPhrase = "Buena pregunta. Creo que prefiero la primera opción porque es más práctica.",
                    romanization = "BWEH-nah preh-GOON-tah. KREH-oh keh preh-FYEH-roh lah pree-MEH-rah op-SYON POR-keh es mahs PRAK-tee-kah.",
                    nativeMeaning = "Good question. I think I prefer the first option because it's more practical.",
                    RegisterAndNuanceNote = "Natural & Versatile",
                    whenToUseExplanation = "Uses 'Creo que...' + 'porque...' to state a clear preference in response to your partner."
                ),
                ConversationRescueSuggestion(
                    targetPhrase = "Déjame pensarlo un segundo... Lo que quiero decir es que me interesa mucho.",
                    romanization = "DEH-hah-meh pen-SAR-loh oon seh-GOON-doh... Loh keh KYEH-roh deh-SEER es keh meh een-teh-REH-sah MOO-choh.",
                    nativeMeaning = "Let me think for a second... What I mean to say is that it interests me a lot.",
                    RegisterAndNuanceNote = "Conversational Time-Buyer",
                    whenToUseExplanation = "Helps you pause naturally in Spanish without switching back to English."
                ),
                ConversationRescueSuggestion(
                    targetPhrase = "Perdona, ¿podrías explicarme esa última parte con otras palabras?",
                    romanization = "Per-DOH-nah, poh-DREE-ahs ex-plee-KAR-meh EH-sah OOL-tee-mah PAR-teh kohn OH-trahs pah-LAH-brahs?",
                    nativeMeaning = "Sorry, could you explain that last part to me in other words?",
                    RegisterAndNuanceNote = "Polite Clarification",
                    whenToUseExplanation = "Use when you understood part of what the AI said but need a simpler rephrasing."
                )
            )
            "fr" -> listOf(
                ConversationRescueSuggestion(
                    targetPhrase = "C'est une bonne question. À mon avis, cela dépend de la situation.",
                    romanization = "Seh tewn bon kes-TYON. Ah mohn ah-VEE, suh-lah day-PAHN duh lah see-tew-ah-SYON.",
                    nativeMeaning = "That's a good question. In my opinion, it depends on the situation.",
                    RegisterAndNuanceNote = "Natural Everyday",
                    whenToUseExplanation = "Gives you a smooth transition ('À mon avis...') to share your thought."
                ),
                ConversationRescueSuggestion(
                    targetPhrase = "Comment dire... Ce que je voudrais expliquer, c'est que j'aime beaucoup cette idée.",
                    romanization = "Koh-mahn DEER... Suh kuh zhuh voo-DRAY ex-plee-KAY, seh kuh zhem boh-KOO set ee-DAY.",
                    nativeMeaning = "How should I put it... What I'd like to explain is that I really like this idea.",
                    RegisterAndNuanceNote = "Fluency Bridge",
                    whenToUseExplanation = "Uses 'Comment dire...' to buy thinking time like a native speaker."
                ),
                ConversationRescueSuggestion(
                    targetPhrase = "Excusez-moi, pourriez-vous reformuler votre question plus simplement ?",
                    romanization = "Ex-kew-zay MWAH, poo-ryay VOO ruh-for-mew-LAY vo-truh kes-TYON plew sahn-pluh-MAHN?",
                    nativeMeaning = "Excuse me, could you rephrase your question more simply?",
                    RegisterAndNuanceNote = "Polite Repair",
                    whenToUseExplanation = "Keeps the conversation going when a word in the prompt was unfamiliar."
                )
            )
            "ja" -> listOf(
                ConversationRescueSuggestion(
                    targetPhrase = "なるほど、いい質問ですね。私はこれが一番いいと思います。",
                    romanization = "Naruhodo, ii shitsumon desu ne. Watashi wa kore ga ichiban ii to omoimasu.",
                    nativeMeaning = "I see, that's a good question. I think this one is the best.",
                    RegisterAndNuanceNote = "Polite & Natural",
                    whenToUseExplanation = "Uses '...to omoimasu' (I think...) to respond smoothly to any question."
                ),
                ConversationRescueSuggestion(
                    targetPhrase = "えっと、うまく言えないのですが、とても興味があります。",
                    romanization = "Etto, umaku ienai no desu ga, totemo kyoumi ga arimasu.",
                    nativeMeaning = "Um, I can't phrase it perfectly yet, but I'm very interested.",
                    RegisterAndNuanceNote = "Humble Bridge Phrase",
                    whenToUseExplanation = "Natural Japanese softener when searching for the right words."
                ),
                ConversationRescueSuggestion(
                    targetPhrase = "すみません、もう少し簡単な言葉で説明してもらえますか？",
                    romanization = "Sumimasen, mou sukoshi kantan na kotoba de setsumei shite moraemasu ka?",
                    nativeMeaning = "Excuse me, could you explain that in slightly simpler words?",
                    RegisterAndNuanceNote = "Polite Clarification",
                    whenToUseExplanation = "Lets you ask for a simpler explanation without ending the call."
                )
            )
            else -> listOf(
                ConversationRescueSuggestion(
                    targetPhrase = "That's a great point—I'd like to try explaining my perspective step by step.",
                    romanization = "Clear conversational bridge in $langName",
                    nativeMeaning = "That's a great point—I'd like to try explaining my perspective step by step.",
                    RegisterAndNuanceNote = "Direct & Clear",
                    whenToUseExplanation = "Bridges directly from the partner's prompt into your own statement."
                ),
                ConversationRescueSuggestion(
                    targetPhrase = "Let me think for a moment about how to say that clearly.",
                    romanization = "Natural thinking pause in $langName",
                    nativeMeaning = "Let me think for a moment about how to say that clearly.",
                    RegisterAndNuanceNote = "Conversational Pause",
                    whenToUseExplanation = "Keeps the turn active while you organize your sentence."
                ),
                ConversationRescueSuggestion(
                    targetPhrase = "Could you rephrase that last question with simpler words?",
                    romanization = "Polite clarification request in $langName",
                    nativeMeaning = "Could you rephrase that last question with simpler words?",
                    RegisterAndNuanceNote = "Polite Repair",
                    whenToUseExplanation = "Requests clarification without resetting the session."
                )
            )
        }

        return ConversationRescueResult(
            languageCode = languageCode,
            conversationContextSummary = "Partner said: \"$partnerLine\"",
            whyThisMomentIsTricky = if (learnerPartialOrStruggledDraft.isNotBlank()) {
                "You started with \"$learnerPartialOrStruggledDraft\"—linking your thought with a transition verb makes it much easier to finish."
            } else {
                "Responding spontaneously requires choosing a natural sentence starter before building the rest of your thought."
            },
            suggestions = fallbackSuggestions,
            grammarBridgeTip = "Pick a 2-3 word opener first in $langName so your sentence structure locks into place.",
            retryPromptInPlace = "Tap 'Use & Retry Now' on any suggestion below to speak or send it without restarting your conversation.",
            aiErrorMessage = if (raw == null) currentAiFailureReason() else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. LIVING LANGUAGE WORLD: Persistent Evolving AI Character & Spoken Tasks
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun advanceLivingWorldTurn(
        location: LivingWorldLocation,
        progress: LivingWorldCharacterProgress,
        userSpokenMessage: String,
        nativeLanguage: String = "English"
    ): LivingWorldTurnOutcome {
        val cleaned = userSpokenMessage.trim()
        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val langName = Language.findByCode(location.languageCode).name

        // Check if any uncompleted task's communicative intent was satisfied by spoken input
        val pendingTasks = location.tasks.filter { it.id !in progress.completedTaskIds }
        val matchedTask = pendingTasks.firstOrNull { task ->
            words.size >= task.minSpokenWordsRequired &&
                (task.requiredSpokenIntentKeywords.any { kw -> cleaned.contains(kw, ignoreCase = true) } || words.size >= 7)
        }

        val rememberedContext = if (progress.rememberedLearnerFacts.isEmpty()) {
            "No prior personal facts recorded yet."
        } else {
            progress.rememberedLearnerFacts.joinToString("; ")
        }

        val prompt = """
            You are roleplaying as ${location.characterName} (${location.characterRole}) at ${location.nameTargetLanguage} in "Living Language World".
            Personality: ${location.characterPersonality}
            Learner's current trust level with you: ${progress.trustScore}/100.
            Facts you already remember about this learner from past conversations: $rememberedContext
            Pending communication task for the learner: ${pendingTasks.firstOrNull()?.communicativeGoal ?: "Free neighborhood conversation"}
            Learner's spoken message ($langName): "$cleaned"
            
            Respond naturally in $langName as ${location.characterName}, referencing your evolving relationship. Also extract 1 short personal preference/fact if the learner shared one, and give constructive $nativeLanguage coaching feedback.
            Return ONLY valid JSON:
            {
              "characterReplyTarget": "Natural in-character response in $langName",
              "characterReplyTranslation": "Translation in $nativeLanguage",
              "newlyRememberedFact": "Short fact in $nativeLanguage about the learner (or empty string)",
              "coachingFeedback": "Specific feedback in $nativeLanguage on how well their spoken message achieved the communicative goal",
              "suggestedCorrectionIfNeeded": "Refined phrasing in $langName if they had a grammar slip"
            }
        """.trimIndent()

        val raw = callGemini(prompt, temperature = 0.4f)
        val json = raw?.let { extractJsonObject(it) }

        val factCandidate = json?.optString("newlyRememberedFact")?.takeIf { it.isNotBlank() && it != "null" }
            ?: if (words.size >= 5) "Spoke with ${location.characterName} about: \"${cleaned.take(45)}\"" else null

        val trustDelta = when {
            matchedTask != null -> 15
            words.size >= 5 -> 5
            else -> 2
        }

        if (json != null && json.optString("characterReplyTarget").isNotBlank()) {
            return LivingWorldTurnOutcome(
                characterReplyTarget = json.optString("characterReplyTarget"),
                characterReplyTranslation = json.optString("characterReplyTranslation"),
                taskCompletedThisTurn = matchedTask,
                newlyRememberedFact = factCandidate,
                trustDelta = trustDelta,
                coachingFeedback = json.optString("coachingFeedback").ifBlank {
                    if (matchedTask != null) "You completed the spoken task '${matchedTask.title}' through real communication!"
                    else "Good conversational turn (${words.size} words). Keep addressing the current task goal to unlock the next milestone."
                },
                suggestedCorrectionIfNeeded = json.optString("suggestedCorrectionIfNeeded")
            )
        }

        val fallbackReplyTarget = when (location.languageCode.lowercase()) {
            "es" -> if (matchedTask != null) {
                "¡Qué bien te has explicado! Tomo nota de lo que me dices. Me encanta charlar contigo en el barrio."
            } else {
                "Te escucho con atención. Cuéntame un poquito más de detalle para poder ayudarte mejor."
            }
            "fr" -> if (matchedTask != null) {
                "C'est très clair, merci ! Je retiens ça pour votre prochaine visite au quartier."
            } else {
                "Je vois ! Pouvez-vous me donner un peu plus de détails ?"
            }
            "ja" -> if (matchedTask != null) {
                "なるほど、よく分かりました！教えてくれてありがとうございます。覚えておきますね。"
            } else {
                "そうなんですね。もう少し詳しく教えてもらえますか？"
            }
            else -> if (matchedTask != null) {
                "Thank you for explaining that so clearly! I'll remember that for your next visit."
            } else {
                "I see! Tell me a little more detail so we can complete this together."
            }
        }

        return LivingWorldTurnOutcome(
            characterReplyTarget = fallbackReplyTarget,
            characterReplyTranslation = if (matchedTask != null) {
                "You explained that so well! I'll remember what you told me for next time."
            } else {
                "I'm listening closely. Tell me a bit more detail so I can help you best."
            },
            taskCompletedThisTurn = matchedTask,
            newlyRememberedFact = factCandidate,
            trustDelta = trustDelta,
            coachingFeedback = if (matchedTask != null) {
                "Task completed ('${matchedTask.title}'): you communicated your intent clearly in ${words.size} spoken words."
            } else {
                "Spoke ${words.size} words. To complete '${pendingTasks.firstOrNull()?.title ?: "current task"}', include details like: ${pendingTasks.firstOrNull()?.requiredSpokenIntentKeywords?.take(3)?.joinToString(", ").orEmpty()}."
            },
            suggestedCorrectionIfNeeded = "",
            aiErrorMessage = if (raw == null) currentAiFailureReason() else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. FUTURE ME SPEAKING: Consent-Gated Synthetic Fluent Comparison
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun generateFutureMeComparison(
        languageCode: String,
        learnerTranscript: String,
        consentVerified: Boolean,
        nativeLanguage: String = "English"
    ): FutureMeComparisonResult {
        val cleaned = learnerTranscript.trim()
        val langName = Language.findByCode(languageCode).name

        val prompt = """
            You are generating a "Future Me Speaking" comparison in $langName.
            IMPORTANT ETHICAL RULE: Never claim voice cloning. Clearly present the target sentence as a "Synthetic Fluent Reference — Not Voice Cloning" showing how a more fluent speaker expresses the learner's exact idea.
            Learner's current recorded transcript: "$cleaned"
            
            Provide:
            1. A natural, more fluent $langName version of the learner's exact message (preserving their personal meaning).
            2. Romanization / pronunciation rhythm guide.
            3. Honest pacing, linking, and phrasing comparison in $nativeLanguage.
            4. 2 practical improvement drills.
            Return ONLY valid JSON:
            {
              "syntheticFluentReferenceText": "Upgraded fluent version in $langName",
              "syntheticFluentRomanization": "Rhythm & stress guide with syllable grouping",
              "pacingAndFlowComparison": "Specific comparison of rhythm, pauses, and sentence flow in $nativeLanguage",
              "phrasingUpgradeExplanation": "Why the fluent reference phrasing sounds more natural in $nativeLanguage",
              "drills": [
                {
                  "focusArea": "Linking & Rhythm",
                  "drillInstruction": "Concrete practice instruction",
                  "targetPhrase": "Phrase to repeat in $langName",
                  "romanization": "Phonetic guide"
                }
              ]
            }
        """.trimIndent()

        val raw = callGemini(prompt, temperature = 0.3f)
        val json = raw?.let { extractJsonObject(it) }
        if (json != null && json.optString("syntheticFluentReferenceText").isNotBlank()) {
            val drillsArr = json.optJSONArray("drills") ?: JSONArray()
            val drills = mutableListOf<FutureMePracticeDrill>()
            for (i in 0 until drillsArr.length()) {
                val d = drillsArr.optJSONObject(i) ?: continue
                drills.add(
                    FutureMePracticeDrill(
                        focusArea = d.optString("focusArea").ifBlank { "Fluency & Rhythm" },
                        drillInstruction = d.optString("drillInstruction"),
                        targetPhrase = d.optString("targetPhrase"),
                        romanization = d.optString("romanization")
                    )
                )
            }
            return FutureMeComparisonResult(
                languageCode = languageCode,
                learnerTranscript = cleaned,
                syntheticFluentReferenceText = json.optString("syntheticFluentReferenceText"),
                syntheticFluentRomanization = json.optString("syntheticFluentRomanization"),
                explicitSyntheticLabel = "Synthetic Fluent Reference — Not Voice Cloning",
                consentVerified = consentVerified,
                pacingAndFlowComparison = json.optString("pacingAndFlowComparison"),
                phrasingUpgradeExplanation = json.optString("phrasingUpgradeExplanation"),
                actionableDrills = drills.ifEmpty { defaultFutureMeDrills(languageCode, cleaned) }
            )
        }

        val upgradedText = when (languageCode.lowercase()) {
            "es" -> "La verdad es que $cleaned, y cada día me siento más cómodo expresándolo con fluidez."
            "fr" -> "En fait, $cleaned, et je me sens de plus en plus à l'aise pour l'exprimer naturellement."
            "ja" -> "実は、${cleaned}。毎日少しずつ自然に話せるようになってきています。"
            else -> "$cleaned — expressed with smooth linking and natural discourse connectors."
        }

        return FutureMeComparisonResult(
            languageCode = languageCode,
            learnerTranscript = cleaned,
            syntheticFluentReferenceText = upgradedText,
            syntheticFluentRomanization = "Group words into 3-4 word breath phrases without pausing after every noun.",
            explicitSyntheticLabel = "Synthetic Fluent Reference — Not Voice Cloning",
            consentVerified = consentVerified,
            pacingAndFlowComparison = "Your recording captured ${cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }.size} words. Comparing it with the synthetic fluent reference highlights how connecting introductory discourse markers reduces mid-sentence hesitation.",
            phrasingUpgradeExplanation = "Adding a natural discourse opener and linking clauses smoothly helps your speech sound cohesive rather than word-by-word.",
            actionableDrills = defaultFutureMeDrills(languageCode, upgradedText),
            aiErrorMessage = if (raw == null) currentAiFailureReason() else null
        )
    }

    private fun defaultFutureMeDrills(languageCode: String, fluentTarget: String): List<FutureMePracticeDrill> {
        return listOf(
            FutureMePracticeDrill(
                focusArea = "Breath-Group Linking",
                drillInstruction = "Speak the first half of the sentence in a single continuous breath without pausing between prepositions and nouns.",
                targetPhrase = fluentTarget,
                romanization = "Smooth connected phrasing (${Language.findByCode(languageCode).name})"
            ),
            FutureMePracticeDrill(
                focusArea = "Sentence-Final Intonation",
                drillInstruction = "Keep your pitch steady through the middle connector and relax naturally on the final stressed syllable.",
                targetPhrase = fluentTarget,
                romanization = "Natural cadence drill"
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. LANGUAGE EMERGENCY MODE: Practice Evaluation
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun evaluateEmergencySpokenResponse(
        item: EmergencyPhraseItem,
        userSpokenText: String
    ): String {
        val cleaned = userSpokenText.trim()
        if (cleaned.isBlank()) {
            return "No speech detected yet. In an emergency, focus on the key words in: \"${item.targetPhrase}\"."
        }
        val targetTokens = item.targetPhrase.lowercase().split(Regex("[^\\p{L}0-9]+")).filter { it.length > 2 }
        val spokenTokens = cleaned.lowercase().split(Regex("[^\\p{L}0-9]+")).filter { it.isNotBlank() }
        val matched = targetTokens.count { t -> spokenTokens.any { it.contains(t) || t.contains(it) } }

        return if (matched >= 2 || spokenTokens.size >= 4) {
            "Clear emergency communication! You conveyed the urgent details (${spokenTokens.size} words). Now try answering the local's follow-up: \"${item.expectedLocalReplyTarget}\" (${item.expectedLocalReplyMeaning})."
        } else {
            "You said \"$cleaned\". To make sure a local helper immediately understands the situation, include key terms from: \"${item.targetPhrase}\"."
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. AI DEBATE PARTNER: Neutral Spoken Debate & Constructive Assessment
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun conductDebateTurn(
        topic: DebateTopic,
        languageCode: String,
        learnerArgument: String,
        nativeLanguage: String = "English"
    ): DebateTurnEvaluation {
        val cleaned = learnerArgument.trim()
        val langName = Language.findByCode(languageCode).name
        val suggestedVocab = topic.suggestedVocabularyByLanguage[languageCode.lowercase()]
            ?: listOf("however", "on the other hand", "for example")

        val prompt = """
            You are an "AI Debate Partner" practicing $langName with a learner on the topic: "${topic.title}" (${topic.neutralPromptQuestion}).
            STRICT ETHICAL RULE: Never judge the learner's personal, political, or social opinions. Evaluate ONLY their language clarity, grammar, vocabulary range, listening comprehension, and ability to respond to arguments.
            Learner's spoken argument ($langName): "$cleaned"
            
            Provide a thoughtful, respectful counter-perspective in $langName to keep the debate stimulating, plus constructive linguistic feedback in $nativeLanguage.
            Return ONLY valid JSON:
            {
              "aiCounterArgumentTarget": "Respectful counter-argument + follow-up question in $langName",
              "aiCounterArgumentTranslation": "Translation in $nativeLanguage",
              "clarityAssessment": "Honest assessment of how clear and structured the argument was",
              "grammarAssessment": "Specific grammar note or correction based on their actual sentence",
              "vocabularyAssessment": "Feedback on connector/vocabulary usage",
              "listeningAndRebuttalAssessment": "How directly they addressed the debate question",
              "upgradedExpressionSuggestion": "A more persuasive way to phrase their point in $langName"
            }
        """.trimIndent()

        val raw = callGemini(prompt, temperature = 0.35f)
        val json = raw?.let { extractJsonObject(it) }
        if (json != null && json.optString("aiCounterArgumentTarget").isNotBlank()) {
            return DebateTurnEvaluation(
                aiCounterArgumentTarget = json.optString("aiCounterArgumentTarget"),
                aiCounterArgumentTranslation = json.optString("aiCounterArgumentTranslation"),
                clarityAssessment = json.optString("clarityAssessment"),
                grammarAssessment = json.optString("grammarAssessment"),
                vocabularyAssessment = json.optString("vocabularyAssessment"),
                listeningAndRebuttalAssessment = json.optString("listeningAndRebuttalAssessment"),
                upgradedExpressionSuggestion = json.optString("upgradedExpressionSuggestion")
            )
        }

        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val usedConnectors = suggestedVocab.filter { cleaned.contains(it, ignoreCase = true) }

        val counterTarget = when (languageCode.lowercase()) {
            "es" -> "Entiendo tu punto de vista. Sin embargo, ¿qué pasa con las personas que necesitan otra alternativa en su día a día? ¿Cómo resolverías ese reto?"
            "fr" -> "Je comprends votre argument. En revanche, comment prendre en compte ceux qui ont des besoins différents au quotidien ?"
            "ja" -> "なるほど、その視点はよく分かります。一方で、反対の立場の人々の課題についてはどう考えますか？"
            else -> "I understand your perspective. On the other hand, how would you address the practical challenges faced by people in the opposite situation?"
        }

        return DebateTurnEvaluation(
            aiCounterArgumentTarget = counterTarget,
            aiCounterArgumentTranslation = "I understand your perspective. However, how would you address the practical challenges faced by people in the opposite situation?",
            clarityAssessment = if (words.size >= 8) {
                "Your argument was well-developed (${words.size} words) with a clear main claim."
            } else {
                "Your statement (${words.size} words) stated a position briefly; adding a concrete example ('for instance...') will make your reasoning clearer."
            },
            grammarAssessment = "Check subordinate clause connectors (such as 'because' / 'although') so your reason links smoothly to your main claim.",
            vocabularyAssessment = if (usedConnectors.isNotEmpty()) {
                "Strong use of debate vocabulary: ${usedConnectors.joinToString(", ")}."
            } else {
                "Try weaving in debate connectors like: ${suggestedVocab.take(3).joinToString(", ")}."
            },
            listeningAndRebuttalAssessment = "Addressed '${topic.title}'. Next, respond directly to the counter-question above.",
            upgradedExpressionSuggestion = suggestedVocab.firstOrNull()?.let { "$it, $cleaned" } ?: cleaned,
            aiErrorMessage = if (raw == null) currentAiFailureReason() else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. EXPLAIN IT THREE WAYS: Simultaneous Beginner, Intermediate, Advanced
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun explainItThreeWays(
        queryConcept: String,
        languageCode: String,
        nativeLanguage: String = "English"
    ): ThreeWayExplanationResult {
        val langName = Language.findByCode(languageCode).name
        val concept = queryConcept.trim().ifBlank {
            if (languageCode.equals("es", ignoreCase = true)) "Por vs. Para / Polite Requests"
            else "Polite Requests & Connectors ($langName)"
        }

        val prompt = """
            You are powering "Explain It Three Ways" for $langName learners (native language: $nativeLanguage).
            Concept requested by learner: "$concept"
            
            Explain "$concept" in $langName at THREE levels simultaneously:
            1. Beginner (simple rule of thumb, everyday survival example)
            2. Intermediate (structural pattern, common contrast, conversational example)
            3. Advanced (subtle nuance, register/idiomatic usage, native-level example)
            Plus 1 quick multiple-choice comprehension check with 3 options.
            Return ONLY valid JSON:
            {
              "beginner": {
                "coreExplanation": "Simple explanation in $nativeLanguage",
                "exampleTargetLanguage": "Beginner example in $langName",
                "exampleRomanization": "Pronunciation guide",
                "exampleNativeTranslation": "Translation in $nativeLanguage",
                "keyTakeaway": "1-line rule of thumb"
              },
              "intermediate": {
                "coreExplanation": "Intermediate pattern explanation in $nativeLanguage",
                "exampleTargetLanguage": "Intermediate example in $langName",
                "exampleRomanization": "Pronunciation guide",
                "exampleNativeTranslation": "Translation in $nativeLanguage",
                "keyTakeaway": "1-line structural takeaway"
              },
              "advanced": {
                "coreExplanation": "Advanced nuance & register explanation in $nativeLanguage",
                "exampleTargetLanguage": "Advanced example in $langName",
                "exampleRomanization": "Pronunciation guide",
                "exampleNativeTranslation": "Translation in $nativeLanguage",
                "keyTakeaway": "1-line mastery insight"
              },
              "comprehensionCheck": {
                "questionPrompt": "Quick question testing understanding of $concept",
                "options": ["Option A", "Option B", "Option C"],
                "correctOptionIndex": 0,
                "explanationOfAnswer": "Why this option is right"
              }
            }
        """.trimIndent()

        val raw = callGemini(prompt, temperature = 0.25f)
        val json = raw?.let { extractJsonObject(it) }
        val beg = json?.optJSONObject("beginner")
        val inter = json?.optJSONObject("intermediate")
        val adv = json?.optJSONObject("advanced")
        val check = json?.optJSONObject("comprehensionCheck")

        if (beg != null && inter != null && adv != null && check != null) {
            val optsArr = check.optJSONArray("options") ?: JSONArray()
            val opts = (0 until optsArr.length()).mapNotNull { optsArr.optString(it).takeIf(String::isNotBlank) }
            if (opts.size >= 2) {
                return ThreeWayExplanationResult(
                    queryConcept = concept,
                    languageCode = languageCode,
                    beginnerLevel = LevelSpecificExplanation(
                        levelName = "Beginner",
                        coreExplanation = beg.optString("coreExplanation"),
                        exampleTargetLanguage = beg.optString("exampleTargetLanguage"),
                        exampleRomanization = beg.optString("exampleRomanization"),
                        exampleNativeTranslation = beg.optString("exampleNativeTranslation"),
                        keyTakeaway = beg.optString("keyTakeaway")
                    ),
                    intermediateLevel = LevelSpecificExplanation(
                        levelName = "Intermediate",
                        coreExplanation = inter.optString("coreExplanation"),
                        exampleTargetLanguage = inter.optString("exampleTargetLanguage"),
                        exampleRomanization = inter.optString("exampleRomanization"),
                        exampleNativeTranslation = inter.optString("exampleNativeTranslation"),
                        keyTakeaway = inter.optString("keyTakeaway")
                    ),
                    advancedLevel = LevelSpecificExplanation(
                        levelName = "Advanced",
                        coreExplanation = adv.optString("coreExplanation"),
                        exampleTargetLanguage = adv.optString("exampleTargetLanguage"),
                        exampleRomanization = adv.optString("exampleRomanization"),
                        exampleNativeTranslation = adv.optString("exampleNativeTranslation"),
                        keyTakeaway = adv.optString("keyTakeaway")
                    ),
                    comprehensionCheck = ThreeWayComprehensionCheck(
                        questionPrompt = check.optString("questionPrompt"),
                        options = opts,
                        correctOptionIndex = check.optInt("correctOptionIndex", 0).coerceIn(0, opts.lastIndex),
                        explanationOfAnswer = check.optString("explanationOfAnswer")
                    )
                )
            }
        }

        // Structured 3-level fallback tailored to concept & language
        return ThreeWayExplanationResult(
            queryConcept = concept,
            languageCode = languageCode,
            beginnerLevel = LevelSpecificExplanation(
                levelName = "Beginner",
                coreExplanation = "Focus on the core everyday meaning of '$concept' in short, direct sentences you can use immediately in conversation.",
                exampleTargetLanguage = when (languageCode.lowercase()) {
                    "es" -> "Este regalo es para ti, muchas gracias por tu ayuda."
                    "fr" -> "C'est pour vous, merci beaucoup pour votre aide."
                    "ja" -> "これはあなたのためです。手伝ってくれてありがとう。"
                    else -> "Practical everyday example applying '$concept' in $langName."
                },
                exampleRomanization = "Clear slow-paced everyday pronunciation",
                exampleNativeTranslation = "This gift is for you; thank you very much for your help.",
                keyTakeaway = "Start with 1 fixed high-frequency frame you can swap nouns into."
            ),
            intermediateLevel = LevelSpecificExplanation(
                levelName = "Intermediate",
                coreExplanation = "At the intermediate level, notice how '$concept' changes depending on speaker intent (cause/motivation vs. destination/goal or politeness register).",
                exampleTargetLanguage = when (languageCode.lowercase()) {
                    "es" -> "Pasé por el centro para recoger los documentos antes de que cerraran."
                    "fr" -> "Je suis passé par le centre pour récupérer les documents avant la fermeture."
                    "ja" -> "閉まる前に書類を受け取るために、駅の前を通りました。"
                    else -> "Connected multi-clause sentence contrasting cause and goal with '$concept'."
                },
                exampleRomanization = "Connected clause rhythm",
                exampleNativeTranslation = "I passed through downtown in order to pick up the documents before they closed.",
                keyTakeaway = "Pair cause/route markers with goal/purpose markers in the same sentence."
            ),
            advancedLevel = LevelSpecificExplanation(
                levelName = "Advanced",
                coreExplanation = "In idiomatic and professional $langName, '$concept' appears in fixed collocations and nuanced register shifts that signal native-like pragmatic awareness.",
                exampleTargetLanguage = when (languageCode.lowercase()) {
                    "es" -> "Por mucho que insistamos, para entonces ya habrán tomado una decisión definitiva."
                    "fr" -> "Pour autant que je sache, cette nuance change subtilement le ton de la phrase."
                    "ja" -> "時と場合によっては、あえて遠回しに表現することで相手への配慮を示します。"
                    else -> "Idiomatic expression demonstrating subtle pragmatic nuance of '$concept'."
                },
                exampleRomanization = "Natural prosody with idiomatic stress",
                exampleNativeTranslation = "No matter how much we insist, by then they will already have made a final decision.",
                keyTakeaway = "Master fixed idiomatic chunks to sound effortless in formal and informal contexts."
            ),
            comprehensionCheck = ThreeWayComprehensionCheck(
                questionPrompt = "When moving from Beginner to Intermediate usage of '$concept', what is the most helpful focus?",
                options = listOf(
                    "Distinguishing speaker intent (cause/route vs. goal/purpose) in connected clauses",
                    "Memorizing isolated word lists without context",
                    "Avoiding the structure completely in conversation"
                ),
                correctOptionIndex = 0,
                explanationOfAnswer = "Understanding speaker intent and clause relationships lets you use '$concept' accurately across real conversations."
            ),
            aiErrorMessage = if (raw == null) currentAiFailureReason() else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 10. TEACH THE AI: Curious AI Student Persona
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun interactWithAiStudent(
        topic: TeachAiTopicOption,
        languageCode: String,
        learnerExplanation: String,
        nativeLanguage: String = "English"
    ): TeachAiTurnEvaluation {
        val cleaned = learnerExplanation.trim()
        val langName = Language.findByCode(languageCode).name

        val prompt = """
            You are playing a curious, enthusiastic "AI Student" learning about "${topic.title}" from the user in $langName.
            The learner just explained to you ($langName): "$cleaned"
            
            Your job:
            1. Paraphrase what you understood from their explanation in $nativeLanguage.
            2. Ask 1 genuine, curious follow-up question in $langName requesting clarification or the next step.
            3. Evaluate how clearly they explained the concept in $langName and offer a smoother phrasing in $langName.
            Return ONLY valid JSON:
            {
              "aiStudentFollowUpQuestionTarget": "Curious follow-up question in $langName",
              "aiStudentFollowUpTranslation": "Translation in $nativeLanguage",
              "whatAiStudentUnderstoodSummary": "What you understood from the learner's explanation ($nativeLanguage)",
              "clarityAndCoherenceFeedback": "Constructive feedback on explanation clarity and sequencing ($nativeLanguage)",
              "suggestedClearerPhrasingTarget": "Refined way to explain that step in $langName",
              "conceptClarityReached": true
            }
        """.trimIndent()

        val raw = callGemini(prompt, temperature = 0.35f)
        val json = raw?.let { extractJsonObject(it) }
        if (json != null && json.optString("aiStudentFollowUpQuestionTarget").isNotBlank()) {
            return TeachAiTurnEvaluation(
                aiStudentFollowUpQuestionTarget = json.optString("aiStudentFollowUpQuestionTarget"),
                aiStudentFollowUpTranslation = json.optString("aiStudentFollowUpTranslation"),
                whatAiStudentUnderstoodSummary = json.optString("whatAiStudentUnderstoodSummary"),
                clarityAndCoherenceFeedback = json.optString("clarityAndCoherenceFeedback"),
                suggestedClearerPhrasingTarget = json.optString("suggestedClearerPhrasingTarget"),
                conceptClarityReached = json.optBoolean("conceptClarityReached", true)
            )
        }

        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val clearEnough = words.size >= 6
        val followUpTarget = when (languageCode.lowercase()) {
            "es" -> "¡Ah, ya entiendo mejor! Pero tengo una duda: ¿qué es lo más importante que debo evitar para no equivocarme en ese paso?"
            "fr" -> "Ah, je comprends mieux ! Mais j'ai une question : quelle est l'erreur la plus fréquente à éviter à cette étape ?"
            "ja" -> "なるほど、分かってきました！でも質問があります。そのステップで失敗しないために一番大切なコツは何ですか？"
            else -> "Aha, I'm following you! One quick question: what is the most common mistake a beginner should avoid in that step?"
        }

        return TeachAiTurnEvaluation(
            aiStudentFollowUpQuestionTarget = followUpTarget,
            aiStudentFollowUpTranslation = "Aha, I understand better now! But I have a question: what is the most important thing to avoid so I don't make a mistake in that step?",
            whatAiStudentUnderstoodSummary = if (clearEnough) {
                "The AI student understood your explanation (${words.size} words): \"$cleaned\"."
            } else {
                "The AI student caught the topic keyword ('$cleaned'), but needs a fuller step-by-step sentence to follow the process."
            },
            clarityAndCoherenceFeedback = if (clearEnough) {
                "Clear step-by-step explanation! Using sequencing words ('first', 'then', 'because') helps your student follow complex ideas effortlessly."
            } else {
                "Your explanation was only ${words.size} words long. Try explaining *how* or *why* using at least one complete sentence."
            },
            suggestedClearerPhrasingTarget = when (languageCode.lowercase()) {
                "es" -> "Primero debes preparar bien la base, porque así el resultado sale mucho mejor."
                "fr" -> "D'abord, il faut bien préparer la base, parce que cela rend la suite beaucoup plus simple."
                "ja" -> "まず最初に基本をしっかり準備することが大切です。そうすると上手くいきますよ。"
                "de" -> "Zuerst sollte man die Grundlagen sorgfältig vorbereiten, damit der nächste Schritt leicht fällt."
                "ur" -> "سب سے پہلے بنیادی چیزوں کو اچھی طرح تیار کریں تاکہ اگلا مرحلہ آسان ہو جائے۔"
                "ar" -> "أولاً، يجب تحضير الأساسيات بعناية حتى تكون الخطوة التالية أسهل."
                else -> "First, make sure to prepare the basics carefully so the next step goes smoothly."
            },
            conceptClarityReached = clearEnough,
            aiErrorMessage = if (raw == null) currentAiFailureReason() else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 11. CONVERSATION REPAIR LAB: Misunderstanding & Recovery Evaluation
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun evaluateConversationRepairTurn(
        scenario: ConversationRepairScenario,
        learnerRepairAttempt: String,
        nativeLanguage: String = "English"
    ): ConversationRepairTurnResult {
        val cleaned = learnerRepairAttempt.trim()
        val langName = Language.findByCode(scenario.languageCode).name
        if (cleaned.isBlank()) {
            return ConversationRepairTurnResult(
                scenarioId = scenario.id,
                learnerRepairAttempt = "",
                repairSucceeded = false,
                partnerResolvedReplyTarget = scenario.partnerConfusingLineTarget,
                partnerResolvedReplyMeaning = scenario.partnerConfusingLineMeaning,
                strategyFeedback = "Try speaking or typing a repair phrase to resolve the misunderstanding.",
                recommendedPoliteRepairPhrase = scenario.modelRepairPhrasesTarget.firstOrNull().orEmpty()
            )
        }

        val prompt = """
            You are evaluating a learner in the "Conversation Repair Lab" ($langName).
            Scenario: ${scenario.title} (${scenario.breakdownContext})
            Repair strategy practiced: ${scenario.strategyType.label}
            Partner's confusing line ($langName): "${scenario.partnerConfusingLineTarget}" (${scenario.partnerConfusingLineMeaning})
            Repair goal: ${scenario.repairGoalDescription}
            Learner's repair attempt ($langName): "$cleaned"
            
            Evaluate whether the learner successfully repaired the communication breakdown in $langName.
            Return ONLY valid JSON:
            {
              "repairSucceeded": true,
              "partnerResolvedReplyTarget": "Partner's helpful clarified response in $langName",
              "partnerResolvedReplyMeaning": "Meaning in $nativeLanguage",
              "strategyFeedback": "Specific feedback in $nativeLanguage on their politeness, clarity, and repair strategy",
              "recommendedPoliteRepairPhrase": "Model repair phrase in $langName"
            }
        """.trimIndent()

        val raw = callGemini(prompt, temperature = 0.25f)
        val json = raw?.let { extractJsonObject(it) }
        if (json != null && json.optString("partnerResolvedReplyTarget").isNotBlank()) {
            return ConversationRepairTurnResult(
                scenarioId = scenario.id,
                learnerRepairAttempt = cleaned,
                repairSucceeded = json.optBoolean("repairSucceeded", true),
                partnerResolvedReplyTarget = json.optString("partnerResolvedReplyTarget"),
                partnerResolvedReplyMeaning = json.optString("partnerResolvedReplyMeaning"),
                strategyFeedback = json.optString("strategyFeedback"),
                recommendedPoliteRepairPhrase = json.optString("recommendedPoliteRepairPhrase")
                    .ifBlank { scenario.modelRepairPhrasesTarget.firstOrNull().orEmpty() }
            )
        }

        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val succeeded = words.size >= 4
        return ConversationRepairTurnResult(
            scenarioId = scenario.id,
            learnerRepairAttempt = cleaned,
            repairSucceeded = succeeded,
            partnerResolvedReplyTarget = when (scenario.languageCode.lowercase()) {
                "es" -> "¡Ah, claro! Te lo digo más despacio: es en el andén número cuatro, justo al lado de la taquilla."
                "fr" -> "Bien sûr ! Je répète plus lentement : c'est au rez-de-chaussée, chez la gardienne."
                else -> "Of course! Let me say that more clearly so you have the exact detail you need."
            },
            partnerResolvedReplyMeaning = "Of course! Let me state that clearly and slowly so you have the exact detail.",
            strategyFeedback = if (succeeded) {
                "Effective repair (${words.size} words)! You used '${scenario.strategyType.label}' to get a clear answer without losing conversational momentum."
            } else {
                "Your repair attempt ('$cleaned') was very short. Try a complete polite repair sentence like the model below."
            },
            recommendedPoliteRepairPhrase = scenario.modelRepairPhrasesTarget.firstOrNull().orEmpty(),
            aiErrorMessage = if (raw == null) currentAiFailureReason() else null
        )
    }
}
