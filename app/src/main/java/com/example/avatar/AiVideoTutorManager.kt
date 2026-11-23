package com.example.avatar

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.ai.GeminiTutorService
import com.example.ai.VideoTutorAiResponse
import com.example.data.model.CallLearningReport
import com.example.data.model.CallMode
import com.example.data.model.Language
import com.example.data.model.MessageSender
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.SpeakingSession
import com.example.data.model.VoiceCallTurn
import com.example.speech.SpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class AiVideoTutorManager(
  private val context: Context,
  private val speechManager: SpeechManager,
  private val coroutineScope: CoroutineScope,
  val targetLanguage: Language,
  val nativeLanguage: Language,
  initialCefrLevel: CefrLevel = CefrLevel.B1,
  initialTutor: TutorPersona = TutorPersona.findTutorForLanguage(targetLanguage.code, preferFemale = true),
  val isPremium: Boolean = false,
  private val pastMistakesMemory: String = "",
  private val onGrammarMistakeDetected: ((MistakeNotebookEntry) -> Unit)? = null,
  private val onCallReportGenerated: (CallLearningReport) -> Unit
) {
  private val tag = "AiVideoTutorManager"
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  private val _callStatus = MutableStateFlow(VideoCallStatus.IDLE)
  val callStatus = _callStatus.asStateFlow()

  private val _callSeconds = MutableStateFlow(0)
  val callSeconds = _callSeconds.asStateFlow()

  val callLimitSeconds = if (isPremium) 3600 else 600 // 60 mins vs 10 mins

  private val _isMuted = MutableStateFlow(false)
  val isMuted = _isMuted.asStateFlow()

  private val _isCameraOn = MutableStateFlow(true)
  val isCameraOn = _isCameraOn.asStateFlow()

  private val _isSpeakerOn = MutableStateFlow(true)
  val isSpeakerOn = _isSpeakerOn.asStateFlow()

  private val _currentTutor = MutableStateFlow(initialTutor)
  val currentTutor = _currentTutor.asStateFlow()

  private val _currentCefrLevel = MutableStateFlow(initialCefrLevel)
  val currentCefrLevel = _currentCefrLevel.asStateFlow()

  private val _speakingSpeedMultiplier = MutableStateFlow(initialCefrLevel.speed)
  val speakingSpeedMultiplier = _speakingSpeedMultiplier.asStateFlow()

  private val _selectedTopic = MutableStateFlow("Daily Life & Real-world Scenarios")
  val selectedTopic = _selectedTopic.asStateFlow()

  private val _liveTranscript = MutableStateFlow<List<VoiceCallTurn>>(emptyList())
  val liveTranscript = _liveTranscript.asStateFlow()

  private val _currentPartialSpeech = MutableStateFlow("")
  val currentPartialSpeech = _currentPartialSpeech.asStateFlow()

  private val _latestTutorResponse = MutableStateFlow<VideoTutorAiResponse?>(null)
  val latestTutorResponse = _latestTutorResponse.asStateFlow()

  private val _latestGrammarCorrection = MutableStateFlow<Pair<String, String>?>(null)
  val latestGrammarCorrection = _latestGrammarCorrection.asStateFlow()

  private val _livePronunciationScore = MutableStateFlow(92)
  val livePronunciationScore = _livePronunciationScore.asStateFlow()

  private val _savedVocabularyTips = MutableStateFlow<List<String>>(emptyList())
  val savedVocabularyTips = _savedVocabularyTips.asStateFlow()

  private val _learningReport = MutableStateFlow<CallLearningReport?>(null)
  val learningReport = _learningReport.asStateFlow()

  // Avatar Provider & Session Data
  private val _avatarProvider = MutableStateFlow(AvatarProvider.HEYGEN)
  val avatarProvider = _avatarProvider.asStateFlow()

  private val _heygenSession = MutableStateFlow<HeyGenSessionData?>(null)
  val heygenSession = _heygenSession.asStateFlow()

  private val _tavusConversation = MutableStateFlow<TavusConversationResponse?>(null)
  val tavusConversation = _tavusConversation.asStateFlow()

  private val _isLiveAvatarStreaming = MutableStateFlow(false)
  val isLiveAvatarStreaming = _isLiveAvatarStreaming.asStateFlow()

  val currentViseme = speechManager.currentViseme
  val speechAperture = speechManager.speechAperture
  val isTutorSpeakingAudio = speechManager.isSpeaking

  private val _credentialsNotice = MutableStateFlow<String?>(null)
  val credentialsNotice = _credentialsNotice.asStateFlow()

  private var timerJob: Job? = null
  private var silenceWatcherJob: Job? = null
  private var isCallActive = false

  init {
    try {
      @Suppress("DEPRECATION")
      audioManager?.isSpeakerphoneOn = true
    } catch (e: Exception) {
      Log.w(tag, "Audio routing adjustment issue", e)
    }
  }

  fun startCall() {
    isCallActive = true
    _callStatus.value = VideoCallStatus.CONNECTING_AVATAR

    // Check credentials for live avatar stream
    val heygenKey = AvatarStreamingService.getHeyGenApiKey(context)
    val tavusKey = AvatarStreamingService.getTavusApiKey(context)

    coroutineScope.launch {
      if (heygenKey.isNotBlank()) {
        try {
          val tutor = _currentTutor.value
          val res = AvatarStreamingService.createHeyGenStreamingSession(
            apiKey = heygenKey,
            avatarName = tutor.heygenAvatarId,
            voiceId = tutor.heygenVoiceId
          )
          if (res.isSuccess) {
            _heygenSession.value = res.getOrNull()
            _isLiveAvatarStreaming.value = true
            _credentialsNotice.value = null
          } else {
            _isLiveAvatarStreaming.value = false
            Log.w(tag, "HeyGen streaming unavailable, using High-Fidelity Local Talking Avatar: ${res.exceptionOrNull()?.message}")
            _credentialsNotice.value = "Using High-Fidelity Local Talking Avatar Engine"
          }
        } catch (e: Exception) {
          _isLiveAvatarStreaming.value = false
          Log.w(tag, "Primary session connection issue, using Local Talking Avatar", e)
          _credentialsNotice.value = "Using High-Fidelity Local Talking Avatar Engine"
        }
      } else if (tavusKey.isNotBlank()) {
        try {
          val tutor = _currentTutor.value
          val res = AvatarStreamingService.createTavusConversation(
            apiKey = tavusKey,
            replicaId = tutor.tavusReplicaId,
            conversationalContext = "Language tutor for ${targetLanguage.name}"
          )
          if (res.isSuccess) {
            _tavusConversation.value = res.getOrNull()
            _isLiveAvatarStreaming.value = true
            _credentialsNotice.value = null
          } else {
            _isLiveAvatarStreaming.value = false
            Log.w(tag, "Tavus streaming unavailable, using Local Talking Avatar: ${res.exceptionOrNull()?.message}")
            _credentialsNotice.value = "Using High-Fidelity Local Talking Avatar Engine"
          }
        } catch (e: Exception) {
          _isLiveAvatarStreaming.value = false
          Log.w(tag, "Secondary session connection issue, using Local Talking Avatar", e)
          _credentialsNotice.value = "Using High-Fidelity Local Talking Avatar Engine"
        }
      } else {
        // Neither key is configured: use local RealisticTalkingAvatar engine
        _isLiveAvatarStreaming.value = false
        _credentialsNotice.value = "High-Fidelity Local Talking Avatar Engine active."
      }

      // Configure Speech Recognition callbacks with echo prevention
      speechManager.setCallSpeechCallbacks(
        onPartial = { partialText ->
          if (!isCallActive || _isMuted.value) return@setCallSpeechCallbacks
          val lastTutorText = _latestTutorResponse.value?.replyInTargetLanguage.orEmpty()
          if (_callStatus.value == VideoCallStatus.TUTOR_SPEAKING) {
            if (isLikelyAudioEcho(partialText, lastTutorText)) {
              return@setCallSpeechCallbacks
            }
            interruptTutor()
          }
          _callStatus.value = VideoCallStatus.LISTENING
          _currentPartialSpeech.value = partialText
          resetSilenceTimer()
        },
        onResult = { finalSpeech ->
          if (!isCallActive || _isMuted.value) return@setCallSpeechCallbacks
          val lastTutorText = _latestTutorResponse.value?.replyInTargetLanguage.orEmpty()
          if (isLikelyAudioEcho(finalSpeech, lastTutorText)) {
            resumeListening()
            return@setCallSpeechCallbacks
          }
          if (finalSpeech.isNotBlank()) {
            onUserFinishedUtterance(finalSpeech)
          } else {
            resumeListening()
          }
        },
        onSpeechStarted = {
          if (!isCallActive || _isMuted.value) return@setCallSpeechCallbacks
          resetSilenceTimer()
        }
      )

      startTimer()

      // Deliver Tutor Opening Greeting
      delay(600)
      val tutor = _currentTutor.value
      val openingResponse = VideoTutorAiResponse(
        replyInTargetLanguage = tutor.defaultGreeting,
        phoneticGuide = tutor.defaultGreetingPhonetics,
        translationInNativeLanguage = tutor.defaultGreetingTranslation,
        grammarFeedback = null,
        suggestedCorrection = null,
        pronunciationScore = 95,
        vocabularyTip = null,
        facialExpression = "warm_smile",
        cefrLevelEvaluated = _currentCefrLevel.value.code
      )
      deliverTutorResponse(openingResponse)
    }
  }

  private fun startTimer() {
    timerJob?.cancel()
    timerJob = coroutineScope.launch {
      while (isActive && isCallActive) {
        delay(1000)
        if (_callStatus.value == VideoCallStatus.IDLE ||
          _callStatus.value == VideoCallStatus.CONNECTING_AVATAR ||
          _callStatus.value == VideoCallStatus.ENDED
        ) {
          continue
        }
        _callSeconds.value += 1
        val elapsed = _callSeconds.value
        if (elapsed % 5 == 0) {
          com.example.ai.GeminiCostAndQuotaManager.recordConsumedCallSeconds(
            feature = com.example.ai.GeminiFeatureCategory.AI_VIDEO_TUTOR,
            secondsConsumed = 5
          )
        }
        val monthlyRemainingSec = com.example.ai.GeminiCostAndQuotaManager.getRemainingAiVideoSeconds()
        if (elapsed >= callLimitSeconds || (monthlyRemainingSec <= 0 && elapsed >= 60)) {
          endCall()
          break
        }
      }
    }
  }

  private fun resetSilenceTimer() {
    silenceWatcherJob?.cancel()
    silenceWatcherJob = coroutineScope.launch {
      delay(1600)
      if (_callStatus.value == VideoCallStatus.LISTENING && _currentPartialSpeech.value.isNotBlank()) {
        val speech = _currentPartialSpeech.value
        _currentPartialSpeech.value = ""
        onUserFinishedUtterance(speech)
      }
    }
  }

  private fun isLikelyAudioEcho(userSpeech: String, tutorSpeech: String): Boolean {
    if (tutorSpeech.isBlank() || userSpeech.isBlank()) return false
    val cleanUser = userSpeech.lowercase().trim()
    val cleanTutor = tutorSpeech.lowercase().trim()
    if (cleanTutor.contains(cleanUser) && cleanUser.length >= 8) return true
    val userWords = cleanUser.split(Regex("\\s+")).filter { it.length > 2 }
    val tutorWords = cleanTutor.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
    if (userWords.isNotEmpty() && userWords.size >= 2 && userWords.all { tutorWords.contains(it) }) return true
    return false
  }

  fun submitUserSpeech(speechText: String) {
    if (!isCallActive || speechText.isBlank()) return
    if (_callStatus.value == VideoCallStatus.TUTOR_SPEAKING) {
      interruptTutor()
    }
    onUserFinishedUtterance(speechText.trim())
  }

  private var consecutiveHighMasteryTurns = 0
  private var consecutiveStruggleTurns = 0
  private val inCallMistakesLog = mutableListOf<String>()

  private fun onUserFinishedUtterance(userText: String) {
    if (!isCallActive) return
    silenceWatcherJob?.cancel()
    speechManager.stopListening()

    val turn = VoiceCallTurn(
      id = UUID.randomUUID().toString(),
      speaker = MessageSender.USER,
      text = userText
    )
    _liveTranscript.value = _liveTranscript.value + turn
    _currentPartialSpeech.value = ""
    _callStatus.value = VideoCallStatus.THINKING

    coroutineScope.launch {
      try {
        val tutor = _currentTutor.value
        val combinedMemory = buildList {
          if (pastMistakesMemory.isNotBlank()) add(pastMistakesMemory)
          addAll(inCallMistakesLog.takeLast(4))
        }.joinToString("; ")

        val aiResponse = GeminiTutorService.chatInVideoTutorCall(
          userSpeechInput = userText,
          targetLanguage = targetLanguage,
          nativeLanguage = nativeLanguage,
          tutorName = tutor.name,
          tutorStyle = tutor.teachingStyle,
          cefrLevel = _currentCefrLevel.value.code,
          accentVariety = tutor.accentDescription,
          history = _liveTranscript.value,
          customTopic = _selectedTopic.value,
          pastMistakesMemory = combinedMemory
        )
        if (isCallActive) {
          adaptCefrDifficultyBasedOnTurn(userText, aiResponse)
          deliverTutorResponse(aiResponse, lastUserText = userText)
        }
      } catch (e: Exception) {
        Log.e(tag, "Failed to get AI video call response", e)
        if (isCallActive) {
          resumeListening()
        }
      }
    }
  }

  private fun adaptCefrDifficultyBasedOnTurn(userText: String, response: VideoTutorAiResponse) {
    val wordCount = userText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    if (response.suggestedCorrection.isNullOrBlank() && response.pronunciationScore >= 90 && wordCount >= 6) {
      consecutiveHighMasteryTurns++
      consecutiveStruggleTurns = 0
      if (consecutiveHighMasteryTurns >= 2) {
        consecutiveHighMasteryTurns = 0
        val levels = CefrLevel.entries
        val idx = levels.indexOf(_currentCefrLevel.value)
        if (idx in 0 until levels.lastIndex) {
          switchCefrLevel(levels[idx + 1])
        }
      }
    } else if (!response.suggestedCorrection.isNullOrBlank() && response.pronunciationScore < 72) {
      consecutiveStruggleTurns++
      consecutiveHighMasteryTurns = 0
      if (consecutiveStruggleTurns >= 2) {
        consecutiveStruggleTurns = 0
        val levels = CefrLevel.entries
        val idx = levels.indexOf(_currentCefrLevel.value)
        if (idx > 0) {
          switchCefrLevel(levels[idx - 1])
        }
      }
    }
  }

  private fun deliverTutorResponse(response: VideoTutorAiResponse, lastUserText: String = "") {
    if (!isCallActive) return
    _latestTutorResponse.value = response
    _livePronunciationScore.value = response.pronunciationScore

    if (response.suggestedCorrection != null && response.grammarFeedback != null) {
      _latestGrammarCorrection.value = response.suggestedCorrection to response.grammarFeedback
      if (lastUserText.isNotBlank()) {
        inCallMistakesLog.add("$lastUserText -> ${response.suggestedCorrection}")
        onGrammarMistakeDetected?.invoke(
          MistakeNotebookEntry(
            languageCode = targetLanguage.code,
            originalText = lastUserText.take(140),
            correctedText = response.suggestedCorrection.take(180),
            category = "Video Tutor Grammar",
            isGenuineError = true,
            explanationNative = response.grammarFeedback,
            practicePrompt = "Say naturally in ${targetLanguage.name}: ${response.suggestedCorrection}",
            practiceAnswer = response.suggestedCorrection
          )
        )
      }
    }

    if (!response.vocabularyTip.isNullOrBlank() && !_savedVocabularyTips.value.contains(response.vocabularyTip)) {
      _savedVocabularyTips.value = (_savedVocabularyTips.value + response.vocabularyTip).takeLast(10)
    }

    val tutorTurn = VoiceCallTurn(
      id = UUID.randomUUID().toString(),
      speaker = MessageSender.AI_TUTOR,
      text = response.replyInTargetLanguage,
      phoneticGuide = response.phoneticGuide,
      translation = response.translationInNativeLanguage
    )
    _liveTranscript.value = _liveTranscript.value + tutorTurn
    _callStatus.value = VideoCallStatus.TUTOR_SPEAKING

    // Trigger HeyGen video task if active
    val session = _heygenSession.value
    val heygenKey = AvatarStreamingService.getHeyGenApiKey(context)
    if (session != null && heygenKey.isNotBlank()) {
      coroutineScope.launch {
        try {
          AvatarStreamingService.sendHeyGenSpeakTask(
            apiKey = heygenKey,
            sessionId = session.sessionId,
            text = response.replyInTargetLanguage
          )
        } catch (e: Exception) {
          Log.w(tag, "Error triggering HeyGen speak task", e)
        }
      }
    }

    // Stop speech recognizer while tutor speaks to prevent acoustic echo feedback
    speechManager.stopListening()

    // Audio TTS speech
    val effectiveSpeed = (_speakingSpeedMultiplier.value).coerceIn(0.6f, 1.4f)
    speechManager.speakWithVoice(
      text = response.replyInTargetLanguage,
      languageCode = targetLanguage.code,
      pitch = if (_currentTutor.value.gender == "Female") 1.1f else 0.95f,
      speed = effectiveSpeed,
      onDone = {
        coroutineScope.launch(Dispatchers.Main) {
          kotlinx.coroutines.delay(250L)
          if (isCallActive && !_isMuted.value) {
            resumeListening()
          }
        }
      }
    )
  }

  fun interruptTutor() {
    Log.d(tag, "User interrupted tutor speaking")
    speechManager.stopSpeaking()
    if (isCallActive && !_isMuted.value) {
      resumeListening()
    }
  }

  fun toggleMute() {
    val newMute = !_isMuted.value
    _isMuted.value = newMute
    if (newMute) {
      speechManager.stopListening()
      _callStatus.value = VideoCallStatus.MUTED
    } else {
      resumeListening()
    }
  }

  fun toggleCamera() {
    _isCameraOn.value = !_isCameraOn.value
  }

  fun toggleSpeaker() {
    val newSpeaker = !_isSpeakerOn.value
    _isSpeakerOn.value = newSpeaker
    try {
      audioManager?.isSpeakerphoneOn = newSpeaker
    } catch (e: Exception) {
      Log.w(tag, "Failed to toggle speakerphone", e)
    }
  }

  fun switchTutor(tutor: TutorPersona) {
    _currentTutor.value = tutor
  }

  fun switchCefrLevel(level: CefrLevel) {
    _currentCefrLevel.value = level
    _speakingSpeedMultiplier.value = level.speed
  }

  fun setSpeakingSpeed(speed: Float) {
    _speakingSpeedMultiplier.value = speed.coerceIn(0.65f, 1.35f)
  }

  fun setTopic(topic: String) {
    _selectedTopic.value = topic
  }

  fun dismissGrammarCorrection() {
    _latestGrammarCorrection.value = null
  }

  fun requestSlowDown() {
    val newSpeed = (_speakingSpeedMultiplier.value * 0.85f).coerceAtLeast(0.65f)
    _speakingSpeedMultiplier.value = newSpeed
    coroutineScope.launch {
      val (targetText, phoneticText) = when (targetLanguage.code.lowercase()) {
        "en" -> "Understood, I will speak a bit slower. Let's continue." to "un-der-stood, ai wil speek uh bit sloh-er."
        "fr" -> "Compris, je vais parler un peu plus lentement. Continuons." to "kohn-pree, zhuh veh par-leh uhn puh ploo lahnt-mahn."
        "de" -> "Verstanden, ich werde etwas langsamer sprechen. Machen wir weiter." to "fehr-shtahn-den, ikh vehr-deh et-vahs lahng-zah-mer shpre-khen."
        "it" -> "Capito, parlerò un po' più lentamente. Continuiamo." to "kah-pee-toh, par-leh-roh oon poh pyoo len-tah-men-teh."
        "pt" -> "Entendido, vou falar um pouco mais devagar. Vamos continuar." to "en-ten-jee-doo, voh fah-lar oom poh-koo mais deh-vah-gar."
        "es" -> "Entendido, hablaré un poco más despacio. Continuemos." to "en-ten-dee-doh, ah-blah-reh oon poh-koh mahs dehs-pah-syoh."
        else -> "Understood, I will speak a bit slower. Let's continue." to "(Speaking slower)"
      }
      val slowResp = VideoTutorAiResponse(
        replyInTargetLanguage = targetText,
        phoneticGuide = phoneticText,
        translationInNativeLanguage = "Understood, I will speak a bit slower. Let's continue.",
        grammarFeedback = null,
        suggestedCorrection = null,
        pronunciationScore = 95,
        vocabularyTip = null,
        facialExpression = "warm_smile",
        cefrLevelEvaluated = _currentCefrLevel.value.code
      )
      deliverTutorResponse(slowResp)
    }
  }

  fun requestRepeat() {
    val lastTutorTurn = _liveTranscript.value.lastOrNull { it.speaker == MessageSender.AI_TUTOR }
    if (lastTutorTurn != null) {
      val effectiveSpeed = (_speakingSpeedMultiplier.value).coerceIn(0.6f, 1.4f)
      speechManager.stopListening()
      _callStatus.value = VideoCallStatus.TUTOR_SPEAKING
      speechManager.speakWithVoice(
        text = lastTutorTurn.text,
        languageCode = targetLanguage.code,
        pitch = if (_currentTutor.value.gender == "Female") 1.1f else 0.95f,
        speed = effectiveSpeed,
        onDone = {
          coroutineScope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(250L)
            if (isCallActive && !_isMuted.value) {
              resumeListening()
            }
          }
        }
      )
    }
  }

  fun requestGrammarExplanation() {
    val lastGrammar = _latestGrammarCorrection.value
    if (lastGrammar != null) {
      coroutineScope.launch {
        val replyText = when (targetLanguage.code.lowercase()) {
          "es" -> "Recuerda: ${lastGrammar.second}. La forma correcta es: ${lastGrammar.first}"
          "fr" -> "Rappel : ${lastGrammar.second}. La forme correcte est : ${lastGrammar.first}"
          "de" -> "Hinweis: ${lastGrammar.second}. Die richtige Form lautet: ${lastGrammar.first}"
          "it" -> "Ricorda: ${lastGrammar.second}. La forma corretta è: ${lastGrammar.first}"
          "pt" -> "Lembre-se: ${lastGrammar.second}. A forma correta é: ${lastGrammar.first}"
          else -> "Remember: ${lastGrammar.second}. The natural phrasing is: ${lastGrammar.first}"
        }
        val explanation = VideoTutorAiResponse(
          replyInTargetLanguage = replyText,
          phoneticGuide = "(Grammar coaching)",
          translationInNativeLanguage = "Grammar note: ${lastGrammar.second}",
          grammarFeedback = lastGrammar.second,
          suggestedCorrection = lastGrammar.first,
          pronunciationScore = 92,
          vocabularyTip = null,
          facialExpression = "thoughtful",
          cefrLevelEvaluated = _currentCefrLevel.value.code
        )
        deliverTutorResponse(explanation)
      }
    }
  }

  private fun resumeListening() {
    if (!isCallActive || _isMuted.value) return
    coroutineScope.launch(Dispatchers.Main) {
      if (!isCallActive || _isMuted.value) return@launch
      _callStatus.value = VideoCallStatus.LISTENING
      _currentPartialSpeech.value = ""
      speechManager.startListening(targetLanguage.code)
    }
  }

  fun endCall(): SpeakingSession {
    if (!isCallActive) {
      return buildSpeakingSession()
    }
    isCallActive = false
    timerJob?.cancel()
    silenceWatcherJob?.cancel()
    speechManager.stopSpeaking()
    speechManager.stopListening()
    _callStatus.value = VideoCallStatus.ENDED

    // Clean up HeyGen session if running
    val session = _heygenSession.value
    val heygenKey = AvatarStreamingService.getHeyGenApiKey(context)
    if (session != null && heygenKey.isNotBlank()) {
      coroutineScope.launch {
        AvatarStreamingService.stopHeyGenSession(heygenKey, session.sessionId)
      }
    }

    try {
      audioManager?.mode = AudioManager.MODE_NORMAL
    } catch (_: Exception) {}

    // Generate comprehensive report
    coroutineScope.launch {
      val report = GeminiTutorService.generatePostCallReport(
        language = targetLanguage,
        nativeLanguage = nativeLanguage,
        mode = CallMode.FREE_TALK,
        durationSeconds = _callSeconds.value,
        turns = _liveTranscript.value
      )
      _learningReport.value = report
      onCallReportGenerated(report)
    }

    return buildSpeakingSession()
  }

  fun buildSpeakingSession(): SpeakingSession {
    val tutor = _currentTutor.value
    val userTurnsCount = _liveTranscript.value.count { it.speaker == MessageSender.USER }
    val grammarNotes = _liveTranscript.value
      .filter { it.speaker == MessageSender.AI_TUTOR }
      .mapNotNull { it.translation }
      .take(3)
      .joinToString(" | ")

    val mappedProficiency = when (_currentCefrLevel.value) {
      CefrLevel.A1, CefrLevel.A2 -> "Beginner"
      CefrLevel.B1, CefrLevel.B2 -> "Intermediate"
      CefrLevel.C1, CefrLevel.C2 -> "Advanced"
    }

    return SpeakingSession(
      id = UUID.randomUUID().toString(),
      languageCode = targetLanguage.code,
      mode = "Tutor",
      scenarioTitle = "[VideoTutor] Face-to-Face with ${tutor.name} (${_currentCefrLevel.value.code})",
      proficiencyLevel = mappedProficiency,
      turnsCount = maxOf(1L, userTurnsCount.toLong()),
      fluencyScore = _livePronunciationScore.value.toLong(),
      grammarFeedbackSummary = if (grammarNotes.isNotBlank()) grammarNotes else "Completed interactive AI video tutor session."
    )
  }

  fun release() {
    isCallActive = false
    timerJob?.cancel()
    silenceWatcherJob?.cancel()
    speechManager.stopSpeaking()
    speechManager.stopListening()
    speechManager.setCallSpeechCallbacks(null, null, null)

    val session = _heygenSession.value
    val heygenKey = AvatarStreamingService.getHeyGenApiKey(context)
    if (session != null && heygenKey.isNotBlank()) {
      coroutineScope.launch {
        AvatarStreamingService.stopHeyGenSession(heygenKey, session.sessionId)
      }
    }

    try {
      audioManager?.mode = AudioManager.MODE_NORMAL
    } catch (_: Exception) {}
  }
}
