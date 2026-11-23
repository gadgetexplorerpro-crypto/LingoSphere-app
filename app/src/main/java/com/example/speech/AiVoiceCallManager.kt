package com.example.speech

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.ai.GeminiTutorService
import com.example.ai.GroupPartnerAiTurn
import com.example.ai.VoiceCallAiResponse
import com.example.data.model.AiConversationPartner
import com.example.data.model.CallLearningReport
import com.example.data.model.CallMode
import com.example.data.model.CorrectionMode
import com.example.data.model.GrammarCorrection
import com.example.data.model.Language
import com.example.data.model.MessageSender
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.SavedAiConversationRecord
import com.example.data.model.TutorVoice
import com.example.data.model.VoiceCallStatus
import com.example.data.model.VoiceCallTurn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class AiVoiceCallManager(
  private val context: Context,
  private val speechManager: SpeechManager,
  private val coroutineScope: CoroutineScope,
  private val targetLanguage: Language,
  private val nativeLanguage: Language,
  private val proficiencyLevel: String,
  initialMode: CallMode = CallMode.FREE_TALK,
  initialVoice: TutorVoice = TutorVoice.AOEDE,
  initialPartner: AiConversationPartner = AiConversationPartner.ALL_PARTNERS.first(),
  initialGroupPartners: List<AiConversationPartner> = emptyList(),
  initialTextMode: Boolean = false,
  initialCorrectionMode: CorrectionMode = CorrectionMode.IMMEDIATE,
  initialCefrLevel: String = "B1",
  val isPremium: Boolean = false,
  private val pastMistakesMemory: String = "",
  private val onGrammarMistakeDetected: ((MistakeNotebookEntry) -> Unit)? = null,
  private val onConversationRecordSaved: ((SavedAiConversationRecord) -> Unit)? = null,
  private val onCallReportGenerated: (CallLearningReport) -> Unit
) {
  private val tag = "AiVoiceCallManager"
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  private val _callStatus = MutableStateFlow(VoiceCallStatus.CONNECTING)
  val callStatus = _callStatus.asStateFlow()

  private val _callSeconds = MutableStateFlow(0)
  val callSeconds = _callSeconds.asStateFlow()

  val callLimitSeconds: Int
    get() = com.example.data.repository.AccessPolicyManager.tutorUsageState.value.maxSingleSessionSeconds

  private val _remainingSeconds = MutableStateFlow(
    com.example.data.repository.AccessPolicyManager.tutorUsageState.value.remainingSecondsToday
  )
  val remainingSeconds = _remainingSeconds.asStateFlow()

  private val _limitReachedReason = MutableStateFlow<String?>(null)
  val limitReachedReason = _limitReachedReason.asStateFlow()

  private val _userSpokenSeconds = MutableStateFlow(0)
  val userSpokenSeconds = _userSpokenSeconds.asStateFlow()

  private var secondsSinceLastUserSpeech = 0
  private var backgroundTimerJob: Job? = null
  private var activeServerSessionId: String = UUID.randomUUID().toString()

  private val _isQuotaWarning = MutableStateFlow(false)
  val isQuotaWarning = _isQuotaWarning.asStateFlow()

  private val _isMuted = MutableStateFlow(false)
  val isMuted = _isMuted.asStateFlow()

  private val _isSpeakerOn = MutableStateFlow(true)
  val isSpeakerOn = _isSpeakerOn.asStateFlow()

  private val _isTextMode = MutableStateFlow(initialTextMode)
  val isTextMode = _isTextMode.asStateFlow()

  private val _currentMode = MutableStateFlow(initialMode)
  val currentMode = _currentMode.asStateFlow()

  private val _currentPartner = MutableStateFlow(initialPartner)
  val currentPartner = _currentPartner.asStateFlow()

  private val _groupPartners = MutableStateFlow(initialGroupPartners)
  val groupPartners = _groupPartners.asStateFlow()

  private val _activeSpeakingPartner = MutableStateFlow(initialPartner)
  val activeSpeakingPartner = _activeSpeakingPartner.asStateFlow()

  private val _currentVoice = MutableStateFlow(initialVoice)
  val currentVoice = _currentVoice.asStateFlow()

  private val _correctionMode = MutableStateFlow(initialCorrectionMode)
  val correctionMode = _correctionMode.asStateFlow()

  private val _cefrLevel = MutableStateFlow(initialCefrLevel)
  val cefrLevel = _cefrLevel.asStateFlow()

  private val _speakingSpeedMultiplier = MutableStateFlow(1.0f)
  val speakingSpeedMultiplier = _speakingSpeedMultiplier.asStateFlow()

  private val _regionalAccent = MutableStateFlow(initialPartner.accent.ifBlank { "Standard" })
  val regionalAccent = _regionalAccent.asStateFlow()

  private val _selectedTopic = MutableStateFlow(initialMode.title)
  val selectedTopic = _selectedTopic.asStateFlow()

  private val _liveTranscript = MutableStateFlow<List<VoiceCallTurn>>(emptyList())
  val liveTranscript = _liveTranscript.asStateFlow()

  private val _currentPartialSpeech = MutableStateFlow("")
  val currentPartialSpeech = _currentPartialSpeech.asStateFlow()

  private val _silencePromptMessage = MutableStateFlow<String?>(null)
  val silencePromptMessage = _silencePromptMessage.asStateFlow()

  private val _latestTutorResponse = MutableStateFlow<VoiceCallAiResponse?>(null)
  val latestTutorResponse = _latestTutorResponse.asStateFlow()

  private val _sessionCorrections = MutableStateFlow<List<GrammarCorrection>>(emptyList())
  val sessionCorrections = _sessionCorrections.asStateFlow()

  private val _learningReport = MutableStateFlow<CallLearningReport?>(null)
  val learningReport = _learningReport.asStateFlow()

  private val _adaptiveProficiencyLevel = MutableStateFlow(proficiencyLevel)
  val adaptiveProficiencyLevel = _adaptiveProficiencyLevel.asStateFlow()

  private val _aiErrorMessage = MutableStateFlow<String?>(null)
  val aiErrorMessage = _aiErrorMessage.asStateFlow()

  private var lastFailedUserSpeech: String? = null

  private var timerJob: Job? = null
  private var silenceWatcherJob: Job? = null
  private var isCallActive = false
  private var consecutiveCleanTurns = 0
  private var consecutiveCorrectionTurns = 0
  private val inCallMistakesLog = mutableListOf<String>()

  init {
    try {
      @Suppress("DEPRECATION")
      audioManager?.isSpeakerphoneOn = true
    } catch (e: Exception) {
      Log.w(tag, "Audio routing adjustment issue", e)
    }
  }

  private fun isLikelyAudioEcho(partialSpeech: String): Boolean {
    val cleanPartial = partialSpeech.trim().lowercase()
    if (cleanPartial.length < 3) return true
    val currentTutorReply = _latestTutorResponse.value?.replyInTargetLanguage?.trim()?.lowercase() ?: return false
    return currentTutorReply.contains(cleanPartial)
  }

  fun startCall(resumedRecord: SavedAiConversationRecord? = null) {
    isCallActive = true
    _callStatus.value = VoiceCallStatus.CONNECTING
    _silencePromptMessage.value = null

    speechManager.setAudioTurnCallback(null)
    speechManager.setCallSpeechCallbacks(
      onPartial = { partialText ->
        if (!isCallActive || _isMuted.value || _isTextMode.value) return@setCallSpeechCallbacks
        if (_callStatus.value == VoiceCallStatus.TUTOR_SPEAKING) {
          if (isLikelyAudioEcho(partialText)) return@setCallSpeechCallbacks
          interruptTutor()
        }
        _silencePromptMessage.value = null
        _callStatus.value = VoiceCallStatus.USER_SPEAKING
        _currentPartialSpeech.value = partialText
        resetSilenceTimer()
      },
      onResult = { finalSpeech ->
        if (!isCallActive || _isMuted.value) return@setCallSpeechCallbacks
        val clean = finalSpeech.trim()
        if (clean.isNotBlank() &&
          !clean.equals("SILENCE", ignoreCase = true) &&
          !clean.equals("Listening...", ignoreCase = true)
        ) {
          if (_callStatus.value == VoiceCallStatus.TUTOR_SPEAKING && isLikelyAudioEcho(clean)) {
            return@setCallSpeechCallbacks
          }
          _silencePromptMessage.value = null
          onUserFinishedUtterance(clean)
        } else {
          handleLearnerSilenceTimeout()
        }
      },
      onSpeechStarted = {
        if (!isCallActive || _isMuted.value || _isTextMode.value) return@setCallSpeechCallbacks
        if (_callStatus.value != VoiceCallStatus.TUTOR_SPEAKING) {
          _silencePromptMessage.value = null
          _callStatus.value = VoiceCallStatus.USER_SPEAKING
          resetSilenceTimer()
        }
      },
      onIdleTimeout = { _ ->
        if (!isCallActive || _isMuted.value) return@setCallSpeechCallbacks
        handleLearnerSilenceTimeout()
      }
    )

    secondsSinceLastUserSpeech = 0
    _limitReachedReason.value = null
    val initialUsage = com.example.data.repository.AccessPolicyManager.tutorUsageState.value
    _remainingSeconds.value = initialUsage.remainingSecondsToday
    if (initialUsage.remainingSecondsToday <= 0) {
      _limitReachedReason.value = when {
        initialUsage.isPro && initialUsage.isMonthlyExhausted -> "pro_monthly_limit"
        initialUsage.isPro -> "pro_daily_limit"
        else -> "free_daily_limit"
      }
      endCall(limitReached = true)
      return
    }

    startTimer()
    notifyServerTutorSessionStart()

    if (resumedRecord != null && resumedRecord.turns.isNotEmpty()) {
      _liveTranscript.value = resumedRecord.turns
      _sessionCorrections.value = resumedRecord.corrections
      _callSeconds.value = resumedRecord.durationSeconds
      _callStatus.value = if (_isTextMode.value) VoiceCallStatus.IDLE else VoiceCallStatus.LISTENING
      if (!_isTextMode.value && !_isMuted.value) {
        resumeListening()
      }
      return
    }

    // Generate Tutor Opening Greeting once and wait for real user input
    coroutineScope.launch {
      delay(500)
      val partner = _currentPartner.value
      val openingGreeting = getOpeningGreeting(targetLanguage, _currentMode.value, partner)
      deliverTutorResponse(
        response = VoiceCallAiResponse(
          replyInTargetLanguage = openingGreeting.first,
          phoneticGuide = openingGreeting.second,
          translationInNativeLanguage = openingGreeting.third,
          gentleCorrection = null
        ),
        speakingPartner = partner,
        speakAloud = !_isTextMode.value
      )
    }
  }

  private fun handleLearnerSilenceTimeout() {
    if (!isCallActive) return
    _currentPartialSpeech.value = ""
    _callStatus.value = if (_isTextMode.value) VoiceCallStatus.IDLE else VoiceCallStatus.LISTENING
    _silencePromptMessage.value = "Take your time — tap the microphone or start speaking whenever you're ready."
  }

  private fun startTimer() {
    timerJob?.cancel()
    timerJob = coroutineScope.launch {
      while (isActive && isCallActive) {
        delay(1000)
        // Count ONLY connected conversation time (not CONNECTING or ENDED)
        if (_callStatus.value == VoiceCallStatus.CONNECTING || _callStatus.value == VoiceCallStatus.ENDED) {
          continue
        }
        _callSeconds.value += 1
        val elapsed = _callSeconds.value

        if (_callStatus.value == VoiceCallStatus.USER_SPEAKING) {
          _userSpokenSeconds.value += 1
          secondsSinceLastUserSpeech = 0
        } else if (_callStatus.value == VoiceCallStatus.LISTENING ||
          _callStatus.value == VoiceCallStatus.IDLE ||
          _callStatus.value == VoiceCallStatus.MUTED
        ) {
          secondsSinceLastUserSpeech += 1
        }

        // Decrement remaining seconds by 1 connected second
        com.example.data.repository.AccessPolicyManager.recordConnectedTutorSecondsForLocalDisplay(1)
        val liveUsage = com.example.data.repository.AccessPolicyManager.tutorUsageState.value
        val maxSessionSec = com.example.data.repository.AccessPolicyManager.limitsState.value.aiTutorMaxSessionSeconds
        val sessionRemaining = (maxSessionSec - elapsed).coerceAtLeast(0)
        val effectiveRemaining = minOf(liveUsage.remainingSecondsToday, sessionRemaining)
        _remainingSeconds.value = effectiveRemaining

        // Turn warning amber at <= 10 seconds remaining
        _isQuotaWarning.value = effectiveRemaining <= 10

        // Idle timeout enforcement (default 45s with no user speech)
        val idleLimit = com.example.data.repository.AccessPolicyManager.limitsState.value.aiTutorIdleTimeoutSeconds
        if (secondsSinceLastUserSpeech >= idleLimit) {
          _limitReachedReason.value = "idle_timeout"
          endCall(limitReached = false)
          break
        }

        // Send 5-second server heartbeat (capped at 10s delta per beat on server)
        if (elapsed % 5 == 0) {
          com.example.ai.GeminiCostAndQuotaManager.recordConsumedCallSeconds(
            feature = com.example.ai.GeminiFeatureCategory.AI_VOICE_TUTOR,
            secondsConsumed = 5
          )
          sendServerTutorHeartbeat(deltaSeconds = 5, endSession = false)
        }

        // Re-check entitlement every 60 seconds during a session
        if (elapsed % 60 == 0) {
          recheckEntitlementMidSession()
        }

        // Check daily / monthly / max-session exhaustion
        if (liveUsage.remainingSecondsToday <= 0) {
          _limitReachedReason.value = when {
            liveUsage.isPro && liveUsage.isMonthlyExhausted -> "pro_monthly_limit"
            liveUsage.isPro -> "pro_daily_limit"
            else -> "free_daily_limit"
          }
          endCall(limitReached = true)
          break
        } else if (elapsed >= maxSessionSec) {
          _limitReachedReason.value = "max_session_limit"
          endCall(limitReached = true)
          break
        }
      }
    }
  }

  fun onAppBackgrounded() {
    if (!isCallActive) return
    backgroundTimerJob?.cancel()
    backgroundTimerJob = coroutineScope.launch {
      // Auto-end if the app stays in the background for > 30 seconds
      delay(30_000L)
      if (isCallActive) {
        _limitReachedReason.value = "background_timeout"
        endCall(limitReached = false)
      }
    }
  }

  fun onAppForegrounded() {
    backgroundTimerJob?.cancel()
    backgroundTimerJob = null
  }

  private fun notifyServerTutorSessionStart() {
    coroutineScope.launch(Dispatchers.IO) {
      runCatching {
        val db = com.example.data.repository.AccessPolicyManager.lastFirestoreInstance ?: return@launch
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
        db.collection("activeTutorSessions").document(uid).get()
      }
    }
  }

  private fun sendServerTutorHeartbeat(deltaSeconds: Int, endSession: Boolean) {
    val safeDelta = deltaSeconds.coerceIn(0, 10)
    coroutineScope.launch(Dispatchers.IO) {
      runCatching {
        // Sync heartbeat via Firebase Functions if deployed; local AccessPolicyManager listener stays synced with usage/{uid}
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
        val idToken = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
          ?.getIdToken(false)?.let { com.google.android.gms.tasks.Tasks.await(it).token }
        if (idToken.isNullOrBlank()) return@launch
        val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId ?: return@launch
        val url = java.net.URL("https://us-central1-$projectId.cloudfunctions.net/tutorSessionHeartbeat")
        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
          requestMethod = "POST"
          connectTimeout = 4500
          readTimeout = 4500
          doOutput = true
          setRequestProperty("Content-Type", "application/json; charset=UTF-8")
          setRequestProperty("Authorization", "Bearer $idToken")
        }
        val body = org.json.JSONObject().apply {
          put("sessionId", activeServerSessionId)
          put("elapsedDeltaSeconds", safeDelta)
          put("endSession", endSession)
        }
        conn.outputStream.use { os ->
          os.write(body.toString().toByteArray(Charsets.UTF_8))
        }
        if (conn.responseCode in 200..299) {
          val respText = conn.inputStream.bufferedReader().use { it.readText() }
          val json = org.json.JSONObject(respText)
          val serverRemaining = json.optInt("remainingSeconds", _remainingSeconds.value)
          val shouldEnd = json.optBoolean("shouldEnd", false)
          val reason = json.optString("limitReason", "")
          _remainingSeconds.value = minOf(_remainingSeconds.value, serverRemaining.coerceAtLeast(0))
          if (shouldEnd && isCallActive) {
            if (reason.isNotBlank()) _limitReachedReason.value = reason
            kotlinx.coroutines.withContext(Dispatchers.Main) {
              endCall(limitReached = true)
            }
          }
        }
      }
    }
  }

  private fun recheckEntitlementMidSession() {
    val ent = com.example.data.repository.AccessPolicyManager.entitlementState.value
    val liveUsage = com.example.data.repository.AccessPolicyManager.tutorUsageState.value
    if (!ent.hasProEntitlement && liveUsage.remainingSecondsToday <= 0 && isCallActive) {
      _limitReachedReason.value = "free_daily_limit"
      endCall(limitReached = true)
    }
  }

  private fun resetSilenceTimer() {
    silenceWatcherJob?.cancel()
    silenceWatcherJob = coroutineScope.launch {
      delay(1600)
      if (_callStatus.value == VoiceCallStatus.USER_SPEAKING && _currentPartialSpeech.value.isNotBlank()) {
        val speech = _currentPartialSpeech.value.trim()
        _currentPartialSpeech.value = ""
        if (speech.isNotBlank() && !speech.equals("Listening...", ignoreCase = true)) {
          onUserFinishedUtterance(speech)
        }
      }
    }
  }

  private fun onUserFinishedUtterance(
    userText: String,
    isRetry: Boolean = false,
    autoRetryAttempt: Int = 0
  ) {
    val cleanText = userText.trim()
    if (!isCallActive || cleanText.isBlank()) return
    secondsSinceLastUserSpeech = 0
    val estimatedSpokenSec = (cleanText.split(Regex("\\s+")).size / 2).coerceIn(2, 15)
    _userSpokenSeconds.value += estimatedSpokenSec
    silenceWatcherJob?.cancel()
    speechManager.stopListening()
    _silencePromptMessage.value = null
    if (autoRetryAttempt == 0) {
      _aiErrorMessage.value = null
    }

    val lastTurn = _liveTranscript.value.lastOrNull()
    val shouldAppendUserTurn = (!isRetry && autoRetryAttempt == 0) ||
      lastTurn == null ||
      lastTurn.speaker != MessageSender.USER ||
      !lastTurn.text.equals(cleanText, ignoreCase = false)

    if (shouldAppendUserTurn) {
      val turn = VoiceCallTurn(
        id = UUID.randomUUID().toString(),
        speaker = MessageSender.USER,
        text = cleanText
      )
      _liveTranscript.value = _liveTranscript.value + turn
    }
    _currentPartialSpeech.value = ""
    _callStatus.value = VoiceCallStatus.THINKING

    coroutineScope.launch {
      try {
        val combinedMemory = buildList {
          if (pastMistakesMemory.isNotBlank()) add(pastMistakesMemory)
          addAll(inCallMistakesLog.takeLast(4))
        }.joinToString("; ")

        val group = _groupPartners.value
        if (group.size >= 2) {
          val groupTurns = GeminiTutorService.chatInGroupVoiceCall(
            userSpeechInput = cleanText,
            targetLanguage = targetLanguage,
            nativeLanguage = nativeLanguage,
            proficiencyLevel = _adaptiveProficiencyLevel.value,
            cefrLevel = _cefrLevel.value,
            mode = _currentMode.value,
            partners = group,
            history = _liveTranscript.value,
            correctionMode = _correctionMode.value,
            customTopic = _selectedTopic.value
          )
          if (isCallActive) {
            if (groupTurns.isEmpty()) {
              if (autoRetryAttempt < 1) {
                delay(1200L * (autoRetryAttempt + 1))
                if (isCallActive) {
                  onUserFinishedUtterance(cleanText, isRetry = true, autoRetryAttempt = autoRetryAttempt + 1)
                }
              } else {
                lastFailedUserSpeech = cleanText
                _aiErrorMessage.value = "Offline or connection issue — tap Retry."
                _callStatus.value = VoiceCallStatus.IDLE
              }
            } else {
              lastFailedUserSpeech = null
              _aiErrorMessage.value = null
              deliverGroupPartnerResponses(groupTurns, lastUserText = cleanText)
            }
          }
        } else {
          val partner = _currentPartner.value
          val aiResponse = GeminiTutorService.chatInVoiceCall(
            userSpeechInput = cleanText,
            targetLanguage = targetLanguage,
            nativeLanguage = nativeLanguage,
            proficiencyLevel = _adaptiveProficiencyLevel.value,
            mode = _currentMode.value,
            tutorVoice = _currentVoice.value,
            history = _liveTranscript.value,
            regionalAccent = _regionalAccent.value,
            customTopic = _selectedTopic.value,
            pastMistakesMemory = combinedMemory,
            partner = partner,
            correctionMode = _correctionMode.value,
            cefrLevel = _cefrLevel.value
          )
          if (isCallActive) {
            if (aiResponse.isServiceError || aiResponse.replyInTargetLanguage.isBlank()) {
              if (autoRetryAttempt < 1) {
                delay(1200L * (autoRetryAttempt + 1))
                if (isCallActive) {
                  onUserFinishedUtterance(cleanText, isRetry = true, autoRetryAttempt = autoRetryAttempt + 1)
                }
              } else {
                lastFailedUserSpeech = cleanText
                _aiErrorMessage.value = aiResponse.errorMessage
                  ?: "Offline or connection issue — tap Retry."
                _callStatus.value = VoiceCallStatus.IDLE
              }
            } else {
              lastFailedUserSpeech = null
              _aiErrorMessage.value = null
              adaptConversationDifficulty(cleanText, aiResponse.gentleCorrection)
              deliverTutorResponse(
                response = aiResponse,
                lastUserText = cleanText,
                speakingPartner = partner,
                speakAloud = !_isTextMode.value
              )
            }
          }
        }
      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(tag, "Failed to get AI call response", e)
        if (isCallActive) {
          if (autoRetryAttempt < 1) {
            delay(1200L * (autoRetryAttempt + 1))
            if (isCallActive) {
              onUserFinishedUtterance(cleanText, isRetry = true, autoRetryAttempt = autoRetryAttempt + 1)
            }
          } else {
            lastFailedUserSpeech = cleanText
            _aiErrorMessage.value = "Offline or connection issue — tap Retry."
            _callStatus.value = VoiceCallStatus.IDLE
          }
        }
      }
    }
  }

  fun retryLastTurn() {
    val failedSpeech = lastFailedUserSpeech?.takeIf { it.isNotBlank() }
      ?: _liveTranscript.value.lastOrNull { it.speaker == MessageSender.USER }?.text
      ?: return
    if (!isCallActive) return
    _aiErrorMessage.value = null
    onUserFinishedUtterance(failedSpeech, isRetry = true, autoRetryAttempt = 0)
  }

  fun clearAiError() {
    _aiErrorMessage.value = null
  }

  private fun deliverGroupPartnerResponses(
    turns: List<GroupPartnerAiTurn>,
    lastUserText: String
  ) {
    if (!isCallActive || turns.isEmpty()) return

    fun playTurnAtIndex(index: Int) {
      if (!isCallActive) return
      if (index >= turns.size) {
        if (_isTextMode.value) {
          _callStatus.value = VoiceCallStatus.IDLE
        } else if (!_isMuted.value) {
          resumeListening()
        }
        return
      }
      val item = turns[index]
      _activeSpeakingPartner.value = item.partner
      val singleResp = VoiceCallAiResponse(
        replyInTargetLanguage = item.replyInTargetLanguage,
        phoneticGuide = item.phoneticGuide,
        translationInNativeLanguage = item.translationInNativeLanguage,
        gentleCorrection = item.gentleCorrection
      )
      _latestTutorResponse.value = singleResp
      recordCorrectionIfPresent(lastUserText, item.gentleCorrection, null)

      val tutorTurn = VoiceCallTurn(
        id = UUID.randomUUID().toString(),
        speaker = MessageSender.AI_TUTOR,
        text = item.replyInTargetLanguage,
        phoneticGuide = item.phoneticGuide,
        translation = item.translationInNativeLanguage,
        partnerId = item.partner.id,
        partnerName = item.partner.name,
        partnerVoiceId = item.partner.voice.id,
        inlineCorrection = if (_correctionMode.value == CorrectionMode.IMMEDIATE) item.gentleCorrection else null
      )
      _liveTranscript.value = _liveTranscript.value + tutorTurn

      if (_isTextMode.value) {
        playTurnAtIndex(index + 1)
      } else {
        _callStatus.value = VoiceCallStatus.TUTOR_SPEAKING
        speechManager.speakWithTutorVoice(
          text = item.replyInTargetLanguage,
          voice = item.partner.voice,
          languageCode = targetLanguage.code,
          speedMultiplier = _speakingSpeedMultiplier.value,
          persistAsPartnerPreference = false,
          onDone = {
            coroutineScope.launch(Dispatchers.Main) {
              delay(220)
              playTurnAtIndex(index + 1)
            }
          }
        )
      }
    }

    playTurnAtIndex(0)
  }

  private fun recordCorrectionIfPresent(
    lastUserText: String,
    gentleCorrection: String?,
    naturalAlternative: String?
  ) {
    val correction = gentleCorrection?.trim()
    if (!correction.isNullOrBlank() && lastUserText.isNotBlank() && _correctionMode.value != CorrectionMode.CONVERSATION_ONLY) {
      inCallMistakesLog.add("$lastUserText -> $correction")
      val item = GrammarCorrection(
        originalPhrase = lastUserText.take(140),
        correctedPhrase = (naturalAlternative?.takeIf { it.isNotBlank() } ?: correction).take(180),
        explanation = correction.take(220)
      )
      _sessionCorrections.value = (_sessionCorrections.value + item).distinctBy { it.originalPhrase }
      onGrammarMistakeDetected?.invoke(
        MistakeNotebookEntry(
          languageCode = targetLanguage.code,
          originalText = lastUserText.take(140),
          correctedText = item.correctedPhrase,
          category = "Spoken Grammar",
          isGenuineError = true,
          explanationNative = correction,
          practicePrompt = "Say naturally in ${targetLanguage.name}: ${item.correctedPhrase}",
          practiceAnswer = item.correctedPhrase
        )
      )
    }
  }

  private fun adaptConversationDifficulty(userText: String, gentleCorrection: String?) {
    val wordCount = userText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    if (gentleCorrection.isNullOrBlank() && wordCount >= 6) {
      consecutiveCleanTurns++
      consecutiveCorrectionTurns = 0
      if (consecutiveCleanTurns >= 2) {
        consecutiveCleanTurns = 0
        _adaptiveProficiencyLevel.value = when (_adaptiveProficiencyLevel.value) {
          "Beginner" -> "Intermediate"
          "Intermediate" -> "Advanced"
          else -> "Advanced"
        }
      }
    } else if (!gentleCorrection.isNullOrBlank()) {
      consecutiveCorrectionTurns++
      consecutiveCleanTurns = 0
      if (consecutiveCorrectionTurns >= 2 && wordCount <= 4) {
        consecutiveCorrectionTurns = 0
        _adaptiveProficiencyLevel.value = when (_adaptiveProficiencyLevel.value) {
          "Advanced" -> "Intermediate"
          "Intermediate" -> "Beginner"
          else -> "Beginner"
        }
      }
    }
  }

  private fun deliverTutorResponse(
    response: VoiceCallAiResponse,
    lastUserText: String = "",
    speakingPartner: AiConversationPartner = _currentPartner.value,
    speakAloud: Boolean = !_isTextMode.value
  ) {
    if (!isCallActive) return
    _activeSpeakingPartner.value = speakingPartner
    _latestTutorResponse.value = response

    recordCorrectionIfPresent(lastUserText, response.gentleCorrection, response.naturalAlternative)

    val tutorTurn = VoiceCallTurn(
      id = UUID.randomUUID().toString(),
      speaker = MessageSender.AI_TUTOR,
      text = response.replyInTargetLanguage,
      phoneticGuide = response.phoneticGuide,
      translation = response.translationInNativeLanguage,
      partnerId = speakingPartner.id,
      partnerName = speakingPartner.name,
      partnerVoiceId = _currentVoice.value.id,
      inlineCorrection = if (_correctionMode.value == CorrectionMode.IMMEDIATE) response.gentleCorrection else null,
      naturalAlternative = response.naturalAlternative,
      pronunciationScore = response.pronunciationScore
    )
    _liveTranscript.value = _liveTranscript.value + tutorTurn

    if (!speakAloud) {
      _callStatus.value = VoiceCallStatus.IDLE
      return
    }

    _callStatus.value = VoiceCallStatus.TUTOR_SPEAKING
    speechManager.speakWithTutorVoice(
      text = response.replyInTargetLanguage,
      voice = _currentVoice.value,
      languageCode = targetLanguage.code,
      speedMultiplier = _speakingSpeedMultiplier.value,
      onDone = {
        coroutineScope.launch(Dispatchers.Main) {
          if (isCallActive && !_isMuted.value && !_isTextMode.value) {
            resumeListening()
          } else if (isCallActive && _isTextMode.value) {
            _callStatus.value = VoiceCallStatus.IDLE
          }
        }
      }
    )
  }

  fun speakTurnAloud(turn: VoiceCallTurn) {
    val voice = turn.partnerVoiceId?.let { TutorVoice.findById(it) } ?: _currentVoice.value
    speechManager.speakWithTutorVoice(
      text = turn.text,
      voice = voice,
      languageCode = targetLanguage.code,
      speedMultiplier = _speakingSpeedMultiplier.value,
      persistAsPartnerPreference = false
    )
  }

  fun interruptTutor() {
    Log.d(tag, "User interrupted tutor speaking")
    speechManager.stopSpeaking()
    if (isCallActive && !_isMuted.value && !_isTextMode.value) {
      resumeListening()
    } else if (isCallActive && _isTextMode.value) {
      _callStatus.value = VoiceCallStatus.IDLE
    }
  }

  fun submitUserSpeech(speechText: String) {
    val clean = speechText.trim()
    if (!isCallActive || clean.isBlank()) return
    if (_callStatus.value == VoiceCallStatus.TUTOR_SPEAKING) {
      speechManager.stopSpeaking()
    }
    onUserFinishedUtterance(clean)
  }

  fun setTextMode(enabled: Boolean) {
    _isTextMode.value = enabled
    if (enabled) {
      speechManager.stopListening()
      if (_callStatus.value == VoiceCallStatus.LISTENING || _callStatus.value == VoiceCallStatus.USER_SPEAKING) {
        _callStatus.value = VoiceCallStatus.IDLE
      }
    } else if (isCallActive && !_isMuted.value && _callStatus.value != VoiceCallStatus.TUTOR_SPEAKING) {
      resumeListening()
    }
  }

  fun selectPartner(partner: AiConversationPartner) {
    _currentPartner.value = partner
    _activeSpeakingPartner.value = partner
    _currentVoice.value = partner.voice
    _regionalAccent.value = partner.accent
  }

  fun setGroupPartners(partners: List<AiConversationPartner>) {
    _groupPartners.value = partners.take(3)
    if (partners.isNotEmpty()) {
      selectPartner(partners.first())
    }
  }

  fun setCorrectionMode(mode: CorrectionMode) {
    _correctionMode.value = mode
  }

  fun setCefrLevel(level: String) {
    _cefrLevel.value = level
  }

  fun toggleMute() {
    val newMute = !_isMuted.value
    _isMuted.value = newMute
    if (newMute) {
      speechManager.stopListening()
      _callStatus.value = VoiceCallStatus.MUTED
    } else if (!_isTextMode.value) {
      resumeListening()
    } else {
      _callStatus.value = VoiceCallStatus.IDLE
    }
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

  fun switchMode(mode: CallMode) {
    _currentMode.value = mode
    _selectedTopic.value = mode.title
  }

  fun switchVoice(voice: TutorVoice) {
    _currentVoice.value = voice
    speechManager.setPartnerVoice(voice)
  }

  fun setSpeakingSpeed(multiplier: Float) {
    _speakingSpeedMultiplier.value = multiplier.coerceIn(0.65f, 1.35f)
  }

  fun setRegionalAccent(accent: String) {
    _regionalAccent.value = accent
  }

  fun setConversationTopic(topic: String) {
    _selectedTopic.value = topic
  }

  fun resumeListening() {
    if (!isCallActive || _isMuted.value || _isTextMode.value) return
    coroutineScope.launch(Dispatchers.Main) {
      if (!isCallActive || _isMuted.value || _isTextMode.value) return@launch
      _callStatus.value = VoiceCallStatus.LISTENING
      _currentPartialSpeech.value = ""
      speechManager.startListening(targetLanguage.code)
    }
  }

  fun endCall(limitReached: Boolean = false) {
    if (!isCallActive) return
    isCallActive = false
    timerJob?.cancel()
    backgroundTimerJob?.cancel()
    silenceWatcherJob?.cancel()
    speechManager.stopSpeaking()
    speechManager.stopListening()
    _callStatus.value = VoiceCallStatus.ENDED
    sendServerTutorHeartbeat(deltaSeconds = (_callSeconds.value % 5).coerceIn(0, 5), endSession = true)

    try {
      audioManager?.mode = AudioManager.MODE_NORMAL
    } catch (ignored: Exception) {}

    coroutineScope.launch {
      val baseReport = GeminiTutorService.generatePostCallReport(
        language = targetLanguage,
        nativeLanguage = nativeLanguage,
        mode = _currentMode.value,
        durationSeconds = _callSeconds.value,
        turns = _liveTranscript.value
      )
      val mergedCorrections = (_sessionCorrections.value + baseReport.grammarCorrections)
        .distinctBy { it.originalPhrase }
      val recommendations = listOf(
        "Practice 5 minutes of ${_currentMode.value.title} with ${_currentPartner.value.name} at CEFR ${_cefrLevel.value}.",
        "Review ${baseReport.vocabularyLearned.size.coerceAtLeast(3)} new phrases from today's conversation.",
        if (mergedCorrections.isNotEmpty()) {
          "Say the corrected sentences out loud 3 times to build muscle memory."
        } else {
          "Try stepping up to the next CEFR level or Group AI Mode in your next session."
        }
      )
      val enrichedReport = baseReport.copy(
        grammarCorrections = mergedCorrections,
        partnerId = _currentPartner.value.id,
        partnerName = _currentPartner.value.name,
        groupPartnerNames = _groupPartners.value.map { it.name },
        cefrLevel = _cefrLevel.value,
        practiceRecommendations = recommendations
      )
      _learningReport.value = enrichedReport

      val savedRecord = SavedAiConversationRecord(
        id = enrichedReport.sessionId,
        partnerId = _currentPartner.value.id,
        partnerName = _currentPartner.value.name,
        groupPartnerIds = _groupPartners.value.map { it.id },
        groupPartnerNames = _groupPartners.value.map { it.name },
        languageCode = targetLanguage.code,
        languageName = targetLanguage.name,
        topicId = _currentMode.value.id,
        topicTitle = _selectedTopic.value.ifBlank { _currentMode.value.title },
        cefrLevel = _cefrLevel.value,
        correctionModeId = _correctionMode.value.id,
        durationSeconds = _callSeconds.value,
        timestampEpoch = System.currentTimeMillis(),
        turns = _liveTranscript.value,
        corrections = mergedCorrections,
        vocabularyLearned = enrichedReport.vocabularyLearned,
        practiceRecommendations = recommendations,
        summaryHeadline = "Great conversation with ${_currentPartner.value.name}! You practiced ${_currentMode.value.title.lowercase()} for ${(_callSeconds.value / 60).coerceAtLeast(1)} min and learned ${enrichedReport.vocabularyLearned.size} new expressions.",
        audioRecordingConsented = speechManager.voiceRecordingConsent.value
      )
      onConversationRecordSaved?.invoke(savedRecord)
      onCallReportGenerated(enrichedReport)
    }
  }

  fun release() {
    isCallActive = false
    timerJob?.cancel()
    silenceWatcherJob?.cancel()
    speechManager.stopSpeaking()
    speechManager.stopListening()
    speechManager.setCallSpeechCallbacks(null, null, null)
    try {
      audioManager?.mode = AudioManager.MODE_NORMAL
    } catch (ignored: Exception) {}
  }

  private fun getOpeningGreeting(
    language: Language,
    mode: CallMode,
    partner: AiConversationPartner
  ): Triple<String, String, String> {
    return when (language.code.lowercase()) {
      "es" -> when (mode) {
        CallMode.JOB_INTERVIEW -> Triple(
          "¡Hola! Soy ${partner.name}. Bienvenido a la entrevista de hoy. Cuéntame un poco sobre ti.",
          "oh-lah! soy ${partner.name}. byen-beh-nee-doh ah lah en-treh-bees-tah.",
          "Hello! I'm ${partner.name}. Welcome to today's interview. Tell me a little about yourself."
        )
        CallMode.RESTAURANT -> Triple(
          "¡Hola! Soy ${partner.name}. Bienvenido a nuestro restaurante. ¿Qué te gustaría pedir hoy?",
          "oh-lah! soy ${partner.name}. byen-beh-nee-doh ah nwehs-troh res-tow-rahn-teh.",
          "Hello! I'm ${partner.name}. Welcome to our restaurant. What would you like to order today?"
        )
        else -> Triple(
          "¡Hola! Soy ${partner.name}, tu compañero de conversación. ¿Sobre qué te gustaría hablar hoy?",
          "oh-lah! soy ${partner.name}. soh-breh keh teh goos-tah-ree-ah ah-blahr oy?",
          "Hello! I'm ${partner.name}, your conversation partner. What would you like to talk about today?"
        )
      }
      "fr" -> Triple(
        "Bonjour ! Je suis ${partner.name}. C'est un plaisir de discuter avec vous. Comment allez-vous aujourd'hui ?",
        "bohn-zhoor! zhuh swee ${partner.name}. koh-mahn tah-lay vooz oh-zhoor-dwee?",
        "Hello! I am ${partner.name}. It's a pleasure to chat with you. How are you doing today?"
      )
      "de" -> Triple(
        "Hallo! Ich bin ${partner.name}. Schön, mit dir zu sprechen! Worüber möchtest du heute reden?",
        "hah-loh! ikh bin ${partner.name}. shern mit deer tsoo shprekh-en!",
        "Hello! I am ${partner.name}. Great to speak with you! What would you like to talk about today?"
      )
      "ja" -> Triple(
        "こんにちは！${partner.name}です。今日はどんなことについてお話ししましょうか？",
        "kohn-nee-chee-wah! ${partner.name} des. kyoh wah dohn-nah koh-toh ni tsoo-ee-teh?",
        "Hello! I'm ${partner.name}. What would you like to talk about today?"
      )
      else -> Triple(
        partner.sampleGreeting.ifBlank {
          "Hi! I'm ${partner.name}, your ${partner.roleTitle}. Let's practice ${mode.title.lowercase()}—how are you doing today?"
        },
        "Native ${partner.accent} pronunciation (${partner.voice.id})",
        "Hi! I'm ${partner.name}. Let's practice ${mode.title.lowercase()} together!"
      )
    }
  }
}
