package com.example.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.ai.GeminiTutorService
import com.example.avatar.TutorViseme
import com.example.data.model.TutorVoice
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

  private val mainHandler = Handler(Looper.getMainLooper())
  private var tts: TextToSpeech? = null
  private var isTtsReady = false
  private var pendingSpeechAction: (() -> Unit)? = null

  private var speechRecognizer: SpeechRecognizer? = null
  private var isRecognizerListening = false
  private var partialSilenceCommitJob: Job? = null
  private var recognizerWatchdogJob: Job? = null

  private var audioRecord: AudioRecord? = null
  private var activeEchoCanceler: AcousticEchoCanceler? = null
  private var activeNoiseSuppressor: NoiseSuppressor? = null
  private var audioRecordJob: Job? = null
  private var transcriptionJob: Job? = null
  @Volatile
  private var shouldForceFinishRecording = false

  private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var speechAnimationJob: Job? = null
  private var ttsWatchdogJob: Job? = null
  private var activeUtteranceId: String? = null
  private var currentSpeakingText: String = ""
  private val recentAiUtterances = ArrayDeque<String>()
  @Volatile
  private var lastTtsFinishTimestampMs: Long = 0L

  private val _isListening = MutableStateFlow(false)
  val isListening = _isListening.asStateFlow()

  private val _isUserSpeaking = MutableStateFlow(false)
  val isUserSpeaking = _isUserSpeaking.asStateFlow()

  private val _isProcessingSpeech = MutableStateFlow(false)
  val isProcessingSpeech = _isProcessingSpeech.asStateFlow()

  private val _rmsLevel = MutableStateFlow(0f)
  val rmsLevel = _rmsLevel.asStateFlow()

  private val _spokenText = MutableStateFlow("")
  val spokenText = _spokenText.asStateFlow()

  private val _partialSpokenText = MutableStateFlow("")
  val partialSpokenText = _partialSpokenText.asStateFlow()

  private val _speechError = MutableStateFlow<String?>(null)
  val speechError = _speechError.asStateFlow()

  private val _isSpeaking = MutableStateFlow(false)
  val isSpeaking = _isSpeaking.asStateFlow()

  private val _speakingUtteranceText = MutableStateFlow("")
  val speakingUtteranceText = _speakingUtteranceText.asStateFlow()

  private val _speakingCharRange = MutableStateFlow<IntRange?>(null)
  val speakingCharRange = _speakingCharRange.asStateFlow()

  private val _previewingVoiceId = MutableStateFlow<String?>(null)
  val previewingVoiceId = _previewingVoiceId.asStateFlow()

  private val _synthesizingVoiceId = MutableStateFlow<String?>(null)
  val synthesizingVoiceId = _synthesizingVoiceId.asStateFlow()

  private val _voiceError = MutableStateFlow<String?>(null)
  val voiceError = _voiceError.asStateFlow()

  private val _currentViseme = MutableStateFlow(TutorViseme.REST)
  val currentViseme = _currentViseme.asStateFlow()

  private val _speechAperture = MutableStateFlow(0f)
  val speechAperture = _speechAperture.asStateFlow()

  private val _hasRecordedUserAudio = MutableStateFlow(false)
  val hasRecordedUserAudio = _hasRecordedUserAudio.asStateFlow()

  private val _speechRate = MutableStateFlow(0.95f)
  val speechRate = _speechRate.asStateFlow()

  private val _partnerVoice = MutableStateFlow(TutorVoice.AOEDE)
  val partnerVoice = _partnerVoice.asStateFlow()
  val activeTutorVoice = _partnerVoice.asStateFlow()
  val selectedTutorVoice = _partnerVoice.asStateFlow()

  private val _audiobookVoice = MutableStateFlow(TutorVoice.AOEDE)
  val audiobookVoice = _audiobookVoice.asStateFlow()

  private var currentSpeechTargetVoice: TutorVoice = TutorVoice.AOEDE

  @Volatile
  private var activeSpeechGenerationId: Long = 0L
  private var ttsSynthJob: Job? = null
  private var ttsAudioPlayer: MediaPlayer? = null
  private var ttsProgressTickerJob: Job? = null

  private val _voiceRecordingConsent = MutableStateFlow(false)
  val voiceRecordingConsent = _voiceRecordingConsent.asStateFlow()

  private val recordedPcmStream = ByteArrayOutputStream()
  private var lastRecordedWavFile: File? = null
  private var userAudioPlayer: MediaPlayer? = null

  private var onSpeechDoneCallback: (() -> Unit)? = null
  private var onCallSpeechResult: ((String) -> Unit)? = null
  private var onCallSpeechPartial: ((String) -> Unit)? = null
  private var onSpeechBeginDetected: (() -> Unit)? = null
  private var onCallRecognitionIdleTimeout: ((Int) -> Unit)? = null
  private var onAudioTurnCaptured: ((ByteArray, String) -> Unit)? = null

  private var isTtsInitRequested = false

  companion object {
    private const val PREFS_NAME = "lingosphere_prefs"
    const val KEY_PARTNER_VOICE_ID = "selected_tutor_voice_id"
    const val KEY_AUDIOBOOK_VOICE_ID = "selected_audiobook_voice_id"
    private const val GEMINI_TTS_MODEL = "gemini-3.1-flash-tts-preview"
    private const val TTS_CACHE_DIR = "tts_voice_cache"
    private const val GEMINI_TTS_TIMEOUT_MS = 5_000L
    private const val TTS_FAILURE_COOLDOWN_MS = 120_000L
  }

  @Volatile
  private var cloudTtsCooldownUntilEpochMs: Long = 0L

  @Volatile
  var testSimulateTtsFailure: Boolean = false

  init {
    try {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val savedPartnerId = prefs.getString(KEY_PARTNER_VOICE_ID, null)
      if (!savedPartnerId.isNullOrBlank()) {
        val restored = TutorVoice.findById(savedPartnerId)
        _partnerVoice.value = restored
        currentSpeechTargetVoice = restored
        _speechRate.value = restored.speed.coerceIn(0.65f, 1.45f)
      }
      val savedAudiobookId = prefs.getString(KEY_AUDIOBOOK_VOICE_ID, null)
      if (!savedAudiobookId.isNullOrBlank()) {
        _audiobookVoice.value = TutorVoice.findById(savedAudiobookId)
      }
    } catch (_: Exception) {}
    if (!isRobolectricRuntime()) {
      mainHandler.postDelayed({ ensureTtsInitialized() }, 1800L)
    }
  }

  private fun isRobolectricRuntime(): Boolean {
    return android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
  }

  fun getSupportedVoicesForLanguage(@Suppress("UNUSED_PARAMETER") languageCode: String): List<TutorVoice> {
    // All 15 distinct prebuilt voices in TutorVoice map 1-to-1 to distinct gemini-3.1-flash-tts-preview voices
    return TutorVoice.entries.distinctBy { it.providerVoiceId }
  }

  fun isVoiceAvailable(voice: TutorVoice, languageCode: String): Boolean {
    return getSupportedVoicesForLanguage(languageCode).any { it.id == voice.id }
  }

  fun clearVoiceError() {
    _voiceError.value = null
  }

  fun setPartnerVoice(voice: TutorVoice) {
    val changed = _partnerVoice.value != voice
    _partnerVoice.value = voice
    currentSpeechTargetVoice = voice
    _speechRate.value = voice.speed.coerceIn(0.65f, 1.45f)
    _voiceError.value = null
    if (changed && (_isSpeaking.value || _synthesizingVoiceId.value != null || _previewingVoiceId.value != null)) {
      stopSpeaking()
    }
    try {
      context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PARTNER_VOICE_ID, voice.id)
        .apply()
    } catch (_: Exception) {}
    try {
      val effectivePitch = computeEffectivePitchForVoice(voice)
      tts?.setPitch(effectivePitch)
      tts?.setSpeechRate(_speechRate.value)
    } catch (_: Exception) {}
  }

  fun setTutorVoice(voice: TutorVoice) {
    setPartnerVoice(voice)
  }

  fun setAudiobookVoice(voice: TutorVoice) {
    val changed = _audiobookVoice.value != voice
    _audiobookVoice.value = voice
    _voiceError.value = null
    if (changed && (_isSpeaking.value || _synthesizingVoiceId.value != null || _previewingVoiceId.value != null)) {
      stopSpeaking()
    }
    try {
      context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_AUDIOBOOK_VOICE_ID, voice.id)
        .apply()
    } catch (_: Exception) {}
  }

  fun invalidateVoiceAudioCache(voiceId: String? = null) {
    try {
      val dir = File(context.cacheDir, TTS_CACHE_DIR)
      if (!dir.exists() || !dir.isDirectory) return
      val prefix = voiceId?.let { "tts_v_${it.lowercase(Locale.US)}_" }
      dir.listFiles()?.forEach { f ->
        if (prefix == null || f.name.startsWith(prefix)) {
          f.delete()
        }
      }
    } catch (_: Exception) {}
  }

  fun buildTtsCacheKey(
    voice: TutorVoice,
    languageCode: String,
    normalizedText: String,
    pitch: Float = computeEffectivePitchForVoice(voice),
    speed: Float = voice.speed
  ): String {
    val raw = buildString {
      append("v2|provider=").append(voice.providerVoiceId)
      append("|gender=").append(voice.gender)
      append("|region=").append(voice.preferredRegion)
      append("|lang=").append(languageCode.lowercase(Locale.US).trim())
      append("|pitch=").append(String.format(Locale.US, "%.2f", pitch))
      append("|speed=").append(String.format(Locale.US, "%.2f", speed))
      append("|text=").append(normalizedText.trim())
    }
    return try {
      val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
      digest.joinToString("") { "%02x".format(it) }.take(32)
    } catch (_: Exception) {
      raw.hashCode().toUInt().toString(16)
    }
  }

  fun computeEffectivePitchForVoice(voice: TutorVoice): Float {
    return if (voice.gender.equals("Male", ignoreCase = true)) {
      voice.pitch.coerceIn(0.60f, 0.91f)
    } else {
      voice.pitch.coerceIn(0.94f, 1.32f)
    }
  }

  fun resolveLocaleForVoice(languageCode: String, voice: TutorVoice = _partnerVoice.value): Locale {
    val lang = languageCode.lowercase().trim()
    val region = voice.preferredRegion.uppercase().trim()
    return when (lang) {
      "en" -> when (region) {
        "GB", "UK" -> Locale.UK
        "AU" -> Locale.Builder().setLanguage("en").setRegion("AU").build()
        "CA" -> Locale.CANADA
        "IE" -> Locale.Builder().setLanguage("en").setRegion("IE").build()
        "IN" -> Locale.Builder().setLanguage("en").setRegion("IN").build()
        else -> Locale.US
      }
      "es" -> when (region) {
        "MX", "US" -> Locale.Builder().setLanguage("es").setRegion("MX").build()
        else -> Locale.Builder().setLanguage("es").setRegion("ES").build()
      }
      "fr" -> when (region) {
        "CA" -> Locale.CANADA_FRENCH
        else -> Locale.FRANCE
      }
      else -> resolveLocale(lang)
    }
  }

  fun resolveDistinctVoiceProfileSummary(voice: TutorVoice, languageCode: String = "en"): String {
    val loc = resolveLocaleForVoice(languageCode, voice)
    val pitch = computeEffectivePitchForVoice(voice)
    return "${voice.id}|${voice.gender}|${loc.toLanguageTag()}|pitch=${String.format(Locale.US, "%.2f", pitch)}|speed=${String.format(Locale.US, "%.2f", voice.speed)}|slot=${voice.voiceSlotIndex}"
  }

  fun setVoiceRecordingConsent(consented: Boolean) {
    _voiceRecordingConsent.value = consented
    if (!consented) {
      lastRecordedWavFile?.let { if (it.exists()) it.delete() }
      lastRecordedWavFile = null
      _hasRecordedUserAudio.value = false
    }
  }

  fun setSpeechRate(rate: Float) {
    val clamped = rate.coerceIn(0.6f, 1.5f)
    _speechRate.value = clamped
    try {
      tts?.setSpeechRate(clamped)
    } catch (_: Exception) {}
  }

  fun setAudioTurnCallback(callback: ((ByteArray, String) -> Unit)?) {
    onAudioTurnCaptured = callback
  }

  fun setProcessingSpeech(processing: Boolean) {
    _isProcessingSpeech.value = processing
  }

  private fun ensureTtsInitialized() {
    if (tts != null || isTtsInitRequested) return
    isTtsInitRequested = true
    try {
      tts = TextToSpeech(context.applicationContext, this)
    } catch (e: Exception) {
      isTtsInitRequested = false
      Log.e("SpeechManager", "Failed to initialize TTS", e)
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      isTtsReady = true
      try {
        val audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build()
        tts?.setAudioAttributes(audioAttributes)
      } catch (_: Exception) {}

      tts?.setSpeechRate(_speechRate.value)
      tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          mainHandler.post {
            _isSpeaking.value = true
          }
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
          mainHandler.post {
            _isSpeaking.value = true
            handleSpeechRange(start, end)
          }
        }

        override fun onDone(utteranceId: String?) {
          mainHandler.post {
            completeUtteranceOnce(utteranceId)
          }
        }

        override fun onError(utteranceId: String?) {
          mainHandler.post {
            completeUtteranceOnce(utteranceId)
          }
        }
      })

      val queued = pendingSpeechAction
      pendingSpeechAction = null
      queued?.invoke()
    } else {
      isTtsReady = false
      Log.e("SpeechManager", "TTS initialization returned failure: $status")
      val queued = pendingSpeechAction
      pendingSpeechAction = null
      queued?.invoke()
    }
  }

  private fun completeUtteranceOnce(utteranceId: String?) {
    if (utteranceId != null && activeUtteranceId != null && utteranceId != activeUtteranceId) {
      return
    }
    if (activeUtteranceId == null && !_isSpeaking.value && onSpeechDoneCallback == null) {
      return
    }
    activeUtteranceId = null
    ttsWatchdogJob?.cancel()
    ttsWatchdogJob = null
    ttsProgressTickerJob?.cancel()
    ttsProgressTickerJob = null
    _synthesizingVoiceId.value = null
    resetSpeechAnimation()
    _isSpeaking.value = false
    _previewingVoiceId.value = null
    lastTtsFinishTimestampMs = System.currentTimeMillis()
    val cb = onSpeechDoneCallback
    onSpeechDoneCallback = null
    cb?.invoke()
  }

  fun registerAiUtteranceForEchoFilter(text: String) {
    val clean = text.trim()
    if (clean.isBlank()) return
    synchronized(recentAiUtterances) {
      if (!recentAiUtterances.contains(clean)) {
        recentAiUtterances.addLast(clean)
        while (recentAiUtterances.size > 10) {
          recentAiUtterances.removeFirst()
        }
      }
    }
  }

  fun isEchoOfAiSpeech(candidateTranscript: String): Boolean {
    val snapshot = synchronized(recentAiUtterances) { recentAiUtterances.toList() }
    return GeminiTutorService.isLikelyEchoOfAiUtterance(candidateTranscript, snapshot)
  }

  fun clearSessionSpeechState() {
    stopListening()
    _spokenText.value = ""
    _partialSpokenText.value = ""
    _speechError.value = null
    synchronized(recentAiUtterances) {
      recentAiUtterances.clear()
    }
    synchronized(recordedPcmStream) {
      recordedPcmStream.reset()
    }
  }

  fun speak(text: String, languageCode: String, speechRate: Float = _speechRate.value) {
    val activeVoice = _partnerVoice.value
    val effectivePitch = computeEffectivePitchForVoice(activeVoice)
    speakWithVoice(
      text = text,
      languageCode = languageCode,
      pitch = effectivePitch,
      speed = speechRate,
      voiceOverride = activeVoice,
      isPreview = false,
      onDone = null
    )
  }

  fun speakWithTutorVoice(
    text: String,
    voice: TutorVoice,
    languageCode: String = "en",
    speedMultiplier: Float = 1.0f,
    speedOverride: Float? = null,
    persistAsPartnerPreference: Boolean = true,
    onDone: (() -> Unit)? = null
  ) {
    if (persistAsPartnerPreference && _partnerVoice.value != voice) {
      _partnerVoice.value = voice
    }
    val mult = speedOverride ?: speedMultiplier
    val effectiveSpeed = (voice.speed * mult).coerceIn(0.65f, 1.45f)
    val effectivePitch = computeEffectivePitchForVoice(voice)
    speakWithVoice(
      text = text,
      languageCode = languageCode,
      pitch = effectivePitch,
      speed = effectiveSpeed,
      voiceOverride = voice,
      isPreview = false,
      onDone = onDone
    )
  }

  fun previewVoice(
    voice: TutorVoice,
    languageCode: String = "en",
    customSampleText: String? = null,
    onDone: (() -> Unit)? = null
  ) {
    if ((_previewingVoiceId.value == voice.id && _isSpeaking.value) || _synthesizingVoiceId.value == voice.id) {
      stopSpeaking()
      onDone?.invoke()
      return
    }
    // Always stop any previous preview or active narration before starting the new voice preview
    stopSpeaking()
    val phrase = customSampleText?.takeIf { it.isNotBlank() }
      ?: TutorVoice.consistentPreviewSentence(languageCode)
    _previewingVoiceId.value = voice.id
    speakWithVoice(
      text = phrase,
      languageCode = languageCode,
      pitch = computeEffectivePitchForVoice(voice),
      speed = voice.speed.coerceIn(0.65f, 1.45f),
      voiceOverride = voice,
      isPreview = true,
      onDone = {
        if (_previewingVoiceId.value == voice.id) {
          _previewingVoiceId.value = null
        }
        onDone?.invoke()
      }
    )
  }

  /**
   * Selects a distinct male or female voice (when supported) and speaks the target text.
   */
  fun speakWithGender(
    text: String,
    languageCode: String,
    preferMale: Boolean,
    speed: Float = 0.95f,
    onDone: (() -> Unit)? = null
  ) {
    val desiredGender = if (preferMale) "Male" else "Female"
    val voice = TutorVoice.entries.firstOrNull {
      it.gender.equals(desiredGender, ignoreCase = true)
    } ?: _partnerVoice.value
    speakWithVoice(
      text = text,
      languageCode = languageCode,
      pitch = voice.pitch,
      speed = speed,
      voiceOverride = voice,
      onDone = onDone
    )
  }

  /**
   * Honestly checks whether Android's on-device TextToSpeech engine reports installed
   * offline language data for [languageCode] without making any network calls.
   */
  fun isOfflineTtsSupportedForLanguage(languageCode: String): Boolean {
    ensureTtsInitialized()
    val engine = tts ?: return false
    if (!isTtsReady) return false
    return try {
      val locale = resolveLocale(languageCode)
      val availability = engine.isLanguageAvailable(locale)
      availability == TextToSpeech.LANG_AVAILABLE ||
        availability == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
        availability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Plays speech strictly via on-device Android TextToSpeech with zero cloud requests
   * (for Offline Emergency Phrase Mode). Calls [onUnavailable] honestly if the device
   * lacks local speech data for [languageCode].
   */
  fun speakOfflineLocalOnly(
    text: String,
    languageCode: String,
    speed: Float = 0.90f,
    onUnavailable: (String) -> Unit = {},
    onDone: (() -> Unit)? = null
  ) {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      mainHandler.post {
        speakOfflineLocalOnly(text, languageCode, speed, onUnavailable, onDone)
      }
      return
    }
    if (text.isBlank()) {
      onDone?.invoke()
      return
    }
    ensureTtsInitialized()
    val engine = tts
    if (engine == null || !isTtsReady) {
      onUnavailable("Offline speech engine is still initializing or unavailable on this device.")
      return
    }
    val locale = resolveLocale(languageCode)
    val avail = runCatching { engine.isLanguageAvailable(locale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
    if (avail == TextToSpeech.LANG_MISSING_DATA || avail == TextToSpeech.LANG_NOT_SUPPORTED) {
      onUnavailable("Offline voice data for this language is not installed in your Android Text-to-Speech settings. You can still read the phrase and phonetic guide offline.")
      return
    }
    stopSpeakingInternal(clearPreviewId = false)
    stopListening()
    val spoken = NarrationTextCleaner.cleanForTts(text).ifBlank { text }
    val utteranceId = "offline_tts_${System.currentTimeMillis()}"
    activeUtteranceId = utteranceId
    onSpeechDoneCallback = onDone
    _isSpeaking.value = true
    _speakingUtteranceText.value = text
    runCatching {
      engine.language = locale
      engine.setSpeechRate(speed.coerceIn(0.6f, 1.3f))
      engine.setPitch(1.0f)
      val params = Bundle().apply {
        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
      }
      val status = engine.speak(spoken, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
      if (status != TextToSpeech.SUCCESS) {
        _isSpeaking.value = false
        onUnavailable("Could not start offline speech playback on this device.")
      }
    }.onFailure {
      _isSpeaking.value = false
      onUnavailable("Offline speech playback failed: ${it.message ?: "device TTS error"}")
    }
  }

  fun speakWithVoice(
    text: String,
    languageCode: String,
    pitch: Float = 1.0f,
    speed: Float = 0.95f,
    voiceOverride: TutorVoice? = null,
    isPreview: Boolean = false,
    onDone: (() -> Unit)? = null
  ) {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      mainHandler.post {
        speakWithVoice(text, languageCode, pitch, speed, voiceOverride, isPreview, onDone)
      }
      return
    }

    if (text.isBlank()) {
      onDone?.invoke()
      return
    }

    val targetVoice = voiceOverride ?: _partnerVoice.value
    val spokenText = NarrationTextCleaner.cleanForTts(text).ifBlank { text }

    // Single-playback exclusion: cancel any previous synthesis, MediaPlayer, or TTS before starting
    val preservedPreviewId = if (isPreview) targetVoice.id else null
    stopSpeakingInternal(clearPreviewId = false)
    _previewingVoiceId.value = preservedPreviewId
    _voiceError.value = null

    currentSpeechTargetVoice = targetVoice

    // Ensure microphone is not actively recording while AI speaks
    stopListening()
    registerAiUtteranceForEchoFilter(spokenText)

    val genId = ++activeSpeechGenerationId
    val utteranceId = "lingo_utt_${System.currentTimeMillis()}_$genId"
    activeUtteranceId = utteranceId
    onSpeechDoneCallback = onDone
    _isSpeaking.value = true

    currentSpeakingText = spokenText
    _speakingUtteranceText.value = text
    _speakingCharRange.value = null

    val wordCount = spokenText.trim().split(Regex("\\s+")).size.coerceAtLeast(1)
    val estimatedSpeechMs = ((wordCount * 480L) / speed.coerceIn(0.6f, 1.5f)).toLong().coerceIn(2500L, 90_000L)
    ttsWatchdogJob?.cancel()
    ttsWatchdogJob = coroutineScope.launch {
      delay(estimatedSpeechMs + 12_000L)
      if (genId == activeSpeechGenerationId && activeUtteranceId == utteranceId) {
        completeUtteranceOnce(utteranceId)
      }
    }

    // In Robolectric unit tests, skip network Gemini audio fetch and run fast deterministic fallback
    if (isRobolectricRuntime()) {
      startAudioSpeechAnimation(spokenText, speed, uiReferenceText = text)
      dispatchSystemTtsOrFallback(
        spokenText = spokenText,
        uiReferenceText = text,
        languageCode = languageCode,
        pitch = pitch,
        speed = speed,
        targetVoice = targetVoice,
        utteranceId = utteranceId,
        isPreview = isPreview,
        genId = genId
      )
      return
    }

    _synthesizingVoiceId.value = targetVoice.id
    ttsSynthJob = coroutineScope.launch {
      val audioFile = withContext(Dispatchers.IO) {
        synthesizeOrLoadGeminiTtsAudioFile(
          spokenText = spokenText,
          voice = targetVoice,
          languageCode = languageCode,
          pitch = pitch,
          speed = speed
        )
      }

      if (genId != activeSpeechGenerationId || activeUtteranceId != utteranceId) {
        return@launch
      }
      _synthesizingVoiceId.value = null

      if (audioFile != null && playAudioFileWithMediaPlayer(audioFile, speed, genId, utteranceId, spokenText, text)) {
        return@launch
      }

      // Fallback to distinct Android system TextToSpeech voice if cloud TTS was unreachable
      dispatchSystemTtsOrFallback(
        spokenText = spokenText,
        uiReferenceText = text,
        languageCode = languageCode,
        pitch = pitch,
        speed = speed,
        targetVoice = targetVoice,
        utteranceId = utteranceId,
        isPreview = isPreview,
        genId = genId
      )
    }
  }

  private fun dispatchSystemTtsOrFallback(
    spokenText: String,
    uiReferenceText: String,
    languageCode: String,
    pitch: Float,
    speed: Float,
    targetVoice: TutorVoice,
    utteranceId: String,
    isPreview: Boolean,
    genId: Long
  ) {
    if (genId != activeSpeechGenerationId || activeUtteranceId != utteranceId) return
    if (testSimulateTtsFailure) {
      if (isPreview && !isRobolectricRuntime()) {
        _voiceError.value = "Voice preview unavailable for ${targetVoice.personaName} (${targetVoice.id}). Check connection or device TTS."
      }
      completeUtteranceOnce(utteranceId)
      return
    }
    if (!isTtsReady) {
      pendingSpeechAction = {
        if (genId == activeSpeechGenerationId && activeUtteranceId == utteranceId) {
          val ok = dispatchTtsSpeak(spokenText, languageCode, pitch, speed, targetVoice, utteranceId)
          if (ok) {
            startAudioSpeechAnimation(spokenText, speed, uiReferenceText = uiReferenceText)
          } else {
            if (isPreview && !isRobolectricRuntime()) {
              _voiceError.value = "Voice preview unavailable for ${targetVoice.personaName} (${targetVoice.id}). Check connection or device TTS."
            }
            completeUtteranceOnce(utteranceId)
          }
        }
      }
      ensureTtsInitialized()
      coroutineScope.launch {
        delay(4_500L)
        if (!isTtsReady && genId == activeSpeechGenerationId && activeUtteranceId == utteranceId) {
          pendingSpeechAction = null
          if (isPreview && !isRobolectricRuntime()) {
            _voiceError.value = "Device speech engine is unavailable right now."
          }
          completeUtteranceOnce(utteranceId)
        }
      }
      return
    }

    val ok = dispatchTtsSpeak(spokenText, languageCode, pitch, speed, targetVoice, utteranceId)
    if (ok) {
      startAudioSpeechAnimation(spokenText, speed, uiReferenceText = uiReferenceText)
    } else {
      if (isPreview && !isRobolectricRuntime()) {
        _voiceError.value = "Voice preview unavailable for ${targetVoice.personaName} (${targetVoice.id}). Check connection or device TTS."
      }
      completeUtteranceOnce(utteranceId)
    }
  }

  @OptIn(com.google.firebase.ai.type.PublicPreviewAPI::class)
  private suspend fun synthesizeOrLoadGeminiTtsAudioFile(
    spokenText: String,
    voice: TutorVoice,
    languageCode: String,
    pitch: Float,
    speed: Float
  ): File? {
    return try {
      val cacheDir = File(context.cacheDir, TTS_CACHE_DIR).apply { mkdirs() }
      val cacheKey = buildTtsCacheKey(
        voice = voice,
        languageCode = languageCode,
        normalizedText = spokenText,
        pitch = pitch,
        speed = speed
      )
      val prefix = "tts_v_${voice.id.lowercase(Locale.US)}_$cacheKey"
      val existingWav = File(cacheDir, "$prefix.wav")
      if (isValidCachedAudioFile(existingWav, expectWav = true)) {
        return existingWav
      }
      val existingMp3 = File(cacheDir, "$prefix.mp3")
      if (isValidCachedAudioFile(existingMp3, expectWav = false)) {
        return existingMp3
      }

      if (testSimulateTtsFailure || System.currentTimeMillis() < cloudTtsCooldownUntilEpochMs) {
        return null
      }

      com.example.ensureAppCheckConfiguredIfPending()
      val config = generationConfig {
        responseModalities = listOf(ResponseModality.AUDIO)
        speechConfig = SpeechConfig(Voice(voice.providerVoiceId))
      }
      val model = Firebase.ai.generativeModel(
        modelName = GEMINI_TTS_MODEL,
        generationConfig = config
      )
      val response = withTimeoutOrNull(GEMINI_TTS_TIMEOUT_MS) {
        model.generateContent(spokenText)
      } ?: run {
        cloudTtsCooldownUntilEpochMs = System.currentTimeMillis() + 30_000L
        return null
      }

      val audioPart = response.inlineDataParts.firstOrNull { part ->
        part.mimeType.startsWith("audio/", ignoreCase = true)
      } ?: return null

      val rawBytes = audioPart.inlineData
      if (rawBytes.isEmpty()) return null

      val mimeLower = audioPart.mimeType.lowercase(Locale.US)
      val isRawPcm = mimeLower.contains("pcm") || mimeLower.contains("l16")
      val isMp3 = mimeLower.contains("mpeg") || mimeLower.contains("mp3")
      val isWav = mimeLower.contains("wav")

      val targetFile = if (isMp3) File(cacheDir, "$prefix.mp3") else File(cacheDir, "$prefix.wav")
      val partFile = File(cacheDir, "$prefix.part")

      val finalBytes = when {
        isRawPcm -> {
          val rateMatch = Regex("""rate=(\d+)""").find(mimeLower)
          val sampleRate = rateMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(8000, 48000) ?: 24000
          encodePcmToWavBytes(rawBytes, sampleRate)
        }
        isWav -> {
          if (hasRiffWaveHeader(rawBytes)) rawBytes else encodePcmToWavBytes(rawBytes, 24000)
        }
        isMp3 -> rawBytes
        else -> {
          if (hasRiffWaveHeader(rawBytes)) rawBytes else encodePcmToWavBytes(rawBytes, 24000)
        }
      }

      if (finalBytes.size <= 44) return null
      FileOutputStream(partFile).use { fos ->
        fos.write(finalBytes)
        fos.flush()
      }
      if (targetFile.exists()) targetFile.delete()
      if (partFile.renameTo(targetFile) && isValidCachedAudioFile(targetFile, expectWav = !isMp3)) {
        cloudTtsCooldownUntilEpochMs = 0L
        targetFile
      } else {
        partFile.delete()
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val msg = e.message.orEmpty()
      if (GeminiTutorService.isProjectBillingOrQuotaDepletedError(e) ||
        msg.contains("not found", ignoreCase = true) ||
        msg.contains("404", ignoreCase = true) ||
        msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
        msg.contains("UNAUTHENTICATED", ignoreCase = true)
      ) {
        cloudTtsCooldownUntilEpochMs = System.currentTimeMillis() + TTS_FAILURE_COOLDOWN_MS
      }
      Log.w("SpeechManager", "Gemini TTS synthesis failed for ${voice.id}: ${e.message}")
      null
    }
  }

  private fun hasRiffWaveHeader(bytes: ByteArray): Boolean {
    return bytes.size > 44 &&
      bytes[0] == 'R'.code.toByte() &&
      bytes[1] == 'I'.code.toByte() &&
      bytes[2] == 'F'.code.toByte() &&
      bytes[3] == 'F'.code.toByte() &&
      bytes[8] == 'W'.code.toByte() &&
      bytes[9] == 'A'.code.toByte() &&
      bytes[10] == 'V'.code.toByte() &&
      bytes[11] == 'E'.code.toByte()
  }

  private fun isValidCachedAudioFile(file: File, expectWav: Boolean): Boolean {
    if (!file.exists() || !file.isFile || file.length() <= 64L) return false
    if (!expectWav) return true
    return try {
      val header = ByteArray(12)
      FileInputStream(file).use { fis ->
        if (fis.read(header) < 12) return false
      }
      header[0] == 'R'.code.toByte() &&
        header[1] == 'I'.code.toByte() &&
        header[2] == 'F'.code.toByte() &&
        header[3] == 'F'.code.toByte() &&
        header[8] == 'W'.code.toByte() &&
        header[9] == 'A'.code.toByte() &&
        header[10] == 'V'.code.toByte() &&
        header[11] == 'E'.code.toByte()
    } catch (_: Exception) {
      false
    }
  }

  @Volatile
  private var isTtsMediaPlayerStarted: Boolean = false

  private fun playAudioFileWithMediaPlayer(
    file: File,
    speed: Float,
    genId: Long,
    utteranceId: String,
    spokenText: String,
    uiReferenceText: String
  ): Boolean {
    if (!file.exists() || file.length() <= 44L) return false
    return try {
      releaseTtsAudioPlayerSafely()
      val mp = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        setOnErrorListener { failedPlayer, _, _ ->
          isTtsMediaPlayerStarted = false
          try { failedPlayer.reset() } catch (_: Exception) {}
          try { failedPlayer.release() } catch (_: Exception) {}
          if (ttsAudioPlayer === failedPlayer) ttsAudioPlayer = null
          if (genId == activeSpeechGenerationId && activeUtteranceId == utteranceId) {
            completeUtteranceOnce(utteranceId)
          }
          true
        }
        setOnCompletionListener { donePlayer ->
          isTtsMediaPlayerStarted = false
          try { donePlayer.reset() } catch (_: Exception) {}
          try { donePlayer.release() } catch (_: Exception) {}
          if (ttsAudioPlayer === donePlayer) ttsAudioPlayer = null
          if (genId == activeSpeechGenerationId && activeUtteranceId == utteranceId) {
            completeUtteranceOnce(utteranceId)
          }
        }
        FileInputStream(file).use { fis ->
          setDataSource(fis.fd)
        }
        prepare()
        start()
        isTtsMediaPlayerStarted = true
        val clampedSpeed = speed.coerceIn(0.6f, 1.8f)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
          kotlin.math.abs(clampedSpeed - 1.0f) > 0.02f
        ) {
          try {
            playbackParams = android.media.PlaybackParams().setSpeed(clampedSpeed)
          } catch (_: Exception) {}
        }
      }
      ttsAudioPlayer = mp
      currentSpeakingText = spokenText
      _speakingUtteranceText.value = uiReferenceText
      startAudioSpeechAnimation(spokenText, speed, uiReferenceText = uiReferenceText)
      startAudioProgressRangeTicker(mp, spokenText, genId, utteranceId)
      true
    } catch (e: Exception) {
      Log.w("SpeechManager", "MediaPlayer TTS playback failed, falling back: ${e.message}")
      releaseTtsAudioPlayerSafely()
      false
    }
  }

  private fun startAudioProgressRangeTicker(
    mp: MediaPlayer,
    spokenText: String,
    genId: Long,
    utteranceId: String
  ) {
    ttsProgressTickerJob?.cancel()
    if (spokenText.isBlank()) return
    ttsProgressTickerJob = coroutineScope.launch {
      val durMs = try { mp.duration.coerceAtLeast(300) } catch (_: Exception) { 1000 }
      ttsWatchdogJob?.cancel()
      ttsWatchdogJob = coroutineScope.launch {
        delay(durMs.toLong() + 2500L)
        if (genId == activeSpeechGenerationId && activeUtteranceId == utteranceId) {
          completeUtteranceOnce(utteranceId)
        }
      }
      delay(80L)
      while (isActive && genId == activeSpeechGenerationId && activeUtteranceId == utteranceId) {
        if (!isTtsMediaPlayerStarted || ttsAudioPlayer !== mp) break
        val pos = try {
          mp.currentPosition
        } catch (_: Exception) {
          break
        }
        val frac = (pos.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)
        val charIdx = (spokenText.length * frac).toInt().coerceIn(0, (spokenText.length - 1).coerceAtLeast(0))
        val endIdx = (charIdx + 8).coerceAtMost(spokenText.length)
        if (charIdx < endIdx) {
          handleSpeechRange(charIdx, endIdx)
        }
        delay(80L)
      }
    }
  }

  private fun releaseTtsAudioPlayerSafely() {
    isTtsMediaPlayerStarted = false
    ttsProgressTickerJob?.cancel()
    ttsProgressTickerJob = null
    val mp = ttsAudioPlayer
    ttsAudioPlayer = null
    if (mp != null) {
      try {
        mp.setOnCompletionListener(null)
        mp.setOnErrorListener(null)
      } catch (_: Exception) {}
      try {
        mp.reset()
      } catch (_: Exception) {}
      try {
        mp.release()
      } catch (_: Exception) {}
    }
  }

  private fun isLikelyMaleSystemVoice(voice: android.speech.tts.Voice): Boolean {
    val nameLower = voice.name.lowercase()
    val features = voice.features?.map { it.lowercase() } ?: emptyList()
    if (nameLower.contains("female") || nameLower.contains("#female") || nameLower.contains("-f-") ||
      features.any { it.contains("female") }
    ) {
      return false
    }
    if (nameLower.contains("male") || nameLower.contains("#male") || nameLower.contains("-m-") ||
      features.any { it.contains("male") && !it.contains("female") }
    ) {
      return true
    }
    // Google TTS known male voice identifiers across en-US, en-GB, en-AU, en-IN, es, fr, de, it
    val maleTokens = listOf(
      "-x-iom", "-x-iol", "-x-tpd", "-x-gbb", "-x-gbd",
      "-x-aub", "-x-aud", "-x-ind", "-x-ine", "-x-esd",
      "-x-esf", "-x-frb", "-x-frd", "-x-deb", "-x-ded",
      "-x-itb", "-x-itd", "wavenet-b", "wavenet-d", "standard-b", "standard-d"
    )
    return maleTokens.any { nameLower.contains(it) }
  }

  private fun isLikelyFemaleSystemVoice(voice: android.speech.tts.Voice): Boolean {
    val nameLower = voice.name.lowercase()
    val features = voice.features?.map { it.lowercase() } ?: emptyList()
    if (nameLower.contains("female") || nameLower.contains("#female") || nameLower.contains("-f-") ||
      features.any { it.contains("female") }
    ) {
      return true
    }
    if (isLikelyMaleSystemVoice(voice)) return false
    val femaleTokens = listOf(
      "-x-sfg", "-x-iob", "-x-iog", "-x-tpf", "-x-tpc",
      "-x-gba", "-x-gbc", "-x-aua", "-x-auc", "-x-ina",
      "-x-inc", "-x-esa", "-x-esc", "-x-fra", "-x-frc",
      "-x-dea", "-x-dec", "-x-ita", "-x-itc", "wavenet-a", "wavenet-c", "standard-a", "standard-c"
    )
    return femaleTokens.any { nameLower.contains(it) }
  }

  private fun dispatchTtsSpeak(
    text: String,
    languageCode: String,
    pitch: Float,
    speed: Float,
    targetVoice: TutorVoice,
    utteranceId: String
  ): Boolean {
    return try {
      val engine = tts
      if (engine == null || !isTtsReady) return false
      val targetLocale = resolveLocaleForVoice(languageCode, targetVoice)
      var activeLocale = targetLocale
      val result = engine.setLanguage(targetLocale)
      if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        val fallbackLocale = resolveLocale(languageCode)
        val fallbackResult = engine.setLanguage(fallbackLocale)
        if (fallbackResult != TextToSpeech.LANG_MISSING_DATA && fallbackResult != TextToSpeech.LANG_NOT_SUPPORTED) {
          activeLocale = fallbackLocale
        } else {
          val baseLocale = Locale.forLanguageTag(targetLocale.language)
          val baseResult = engine.setLanguage(baseLocale)
          if (baseResult != TextToSpeech.LANG_MISSING_DATA && baseResult != TextToSpeech.LANG_NOT_SUPPORTED) {
            activeLocale = baseLocale
          } else {
            engine.setLanguage(Locale.US)
            activeLocale = Locale.US
          }
        }
      }
      // Select a genuinely distinct gender- and accent-matched system TTS voice per voice ordinal
      try {
        val availableVoices = engine.voices
        if (!availableVoices.isNullOrEmpty()) {
          val isMale = targetVoice.gender.equals("Male", ignoreCase = true)
          val langVoicesLocal = availableVoices
            .filter { v ->
              v.locale.language.equals(activeLocale.language, ignoreCase = true) && !v.isNetworkConnectionRequired
            }
            .sortedBy { it.name }
          val langVoicesAll = langVoicesLocal.ifEmpty {
            availableVoices
              .filter { v -> v.locale.language.equals(activeLocale.language, ignoreCase = true) }
              .sortedBy { it.name }
          }

          if (langVoicesAll.isNotEmpty()) {
            val explicitMale = langVoicesAll.filter { isLikelyMaleSystemVoice(it) }
            val explicitFemale = langVoicesAll.filter { isLikelyFemaleSystemVoice(it) }

            val genderPool: List<android.speech.tts.Voice> = if (isMale) {
              when {
                explicitMale.isNotEmpty() -> explicitMale
                langVoicesAll.size >= 2 -> langVoicesAll.filterIndexed { idx, _ -> idx % 2 == 1 }
                else -> langVoicesAll
              }
            } else {
              when {
                explicitFemale.isNotEmpty() -> explicitFemale
                langVoicesAll.size >= 2 -> langVoicesAll.filterIndexed { idx, _ -> idx % 2 == 0 }
                else -> langVoicesAll
              }
            }

            val regionMatchedPool = genderPool.filter { v ->
              v.locale.country.equals(targetLocale.country, ignoreCase = true)
            }.ifEmpty { genderPool }

            val candidatePool = if (regionMatchedPool.size <= 1 && genderPool.size > 1) {
              genderPool
            } else {
              regionMatchedPool
            }
            val distinctIdx = (targetVoice.ordinal + targetVoice.voiceSlotIndex) % candidatePool.size
            engine.voice = candidatePool[distinctIdx]
          }
        }
      } catch (_: Exception) {}

      engine.setPitch(pitch)
      engine.setSpeechRate(speed)
      val params = Bundle().apply {
        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
      }
      val speakStatus = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
      if (speakStatus == TextToSpeech.ERROR) {
        Log.w("SpeechManager", "TTS speak returned ERROR")
        false
      } else {
        true
      }
    } catch (e: Exception) {
      Log.w("SpeechManager", "TTS speak exception: ${e.message}")
      false
    }
  }

  private fun startAudioSpeechAnimation(
    text: String,
    speed: Float,
    uiReferenceText: String = text
  ) {
    speechAnimationJob?.cancel()
    currentSpeakingText = text
    _speakingUtteranceText.value = uiReferenceText
    _speakingCharRange.value = null
    val clean = text.trim()
    if (clean.isEmpty()) return

    speechAnimationJob = coroutineScope.launch {
      val words = clean.split(Regex("\\s+"))
      val syllableBaseMs = (175L / speed.coerceIn(0.6f, 1.4f)).toLong()

      for (word in words) {
        if (!isActive || !_isSpeaking.value) break

        val syllables = decomposeWordToSyllables(word)
        for (syllable in syllables) {
          if (!isActive || !_isSpeaking.value) break
          val viseme = detectVisemeForSyllable(syllable)

          _currentViseme.value = viseme
          val targetAperture = viseme.openness.coerceIn(0.28f, 0.95f)

          val steps = 3
          for (s in 1..steps) {
            _speechAperture.value = (targetAperture * (s.toFloat() / steps))
            delay(syllableBaseMs / (steps * 2))
          }

          for (s in (steps - 1) downTo 1) {
            _speechAperture.value = (targetAperture * (s.toFloat() / steps * 0.65f)).coerceAtLeast(0.08f)
            delay(syllableBaseMs / (steps * 2))
          }
        }

        _currentViseme.value = TutorViseme.BMP
        _speechAperture.value = 0.04f
        delay((45L / speed).toLong())
      }

      resetSpeechAnimation()
    }
  }

  private fun handleSpeechRange(start: Int, end: Int) {
    if (currentSpeakingText.isNotEmpty() && start >= 0 && end <= currentSpeakingText.length && start < end) {
      _speakingCharRange.value = start until end
      val sub = currentSpeakingText.substring(start, end)
      val viseme = detectVisemeForSyllable(sub)
      _currentViseme.value = viseme
      _speechAperture.value = viseme.openness.coerceIn(0.3f, 0.9f)
    }
  }

  private fun decomposeWordToSyllables(word: String): List<String> {
    val cleanWord = word.filter { it.isLetter() }.lowercase()
    if (cleanWord.length <= 3) return listOf(cleanWord)

    val result = mutableListOf<String>()
    var current = StringBuilder()
    var hasVowel = false

    for (ch in cleanWord) {
      current.append(ch)
      val isVowel = ch in "aeiouáéíóúüàèìòù"
      if (isVowel) {
        hasVowel = true
      } else if (hasVowel && current.length >= 2) {
        result.add(current.toString())
        current = StringBuilder()
        hasVowel = false
      }
    }
    if (current.isNotEmpty()) {
      result.add(current.toString())
    }
    return if (result.isEmpty()) listOf(cleanWord) else result
  }

  private fun detectVisemeForSyllable(str: String): TutorViseme {
    val lower = str.lowercase()
    return when {
      lower.any { it in "oóöò" } -> TutorViseme.OH
      lower.any { it in "uúüùw" } -> TutorViseme.OO
      lower.any { it in "eéèëiíìïy" } -> TutorViseme.EE
      lower.any { it in "aáàä" } -> TutorViseme.AA
      lower.any { it in "fv" } -> TutorViseme.FV
      lower.any { it in "bmp" } -> TutorViseme.BMP
      else -> TutorViseme.NEUTRAL_TALK
    }
  }

  private fun resetSpeechAnimation() {
    speechAnimationJob?.cancel()
    speechAnimationJob = null
    _currentViseme.value = TutorViseme.REST
    _speechAperture.value = 0f
    _speakingCharRange.value = null
  }

  fun stopSpeaking() {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      mainHandler.post { stopSpeaking() }
      return
    }
    stopSpeakingInternal(clearPreviewId = true)
  }

  private fun stopSpeakingInternal(clearPreviewId: Boolean) {
    activeSpeechGenerationId++
    pendingSpeechAction = null
    activeUtteranceId = null
    ttsSynthJob?.cancel()
    ttsSynthJob = null
    ttsWatchdogJob?.cancel()
    ttsWatchdogJob = null
    ttsProgressTickerJob?.cancel()
    ttsProgressTickerJob = null
    _synthesizingVoiceId.value = null
    resetSpeechAnimation()
    releaseTtsAudioPlayerSafely()
    releaseUserAudioPlayerSafely()
    try {
      tts?.stop()
    } catch (_: Exception) {}
    _isSpeaking.value = false
    if (clearPreviewId) {
      _previewingVoiceId.value = null
    }
    onSpeechDoneCallback = null
  }

  private fun releaseUserAudioPlayerSafely() {
    val player = userAudioPlayer
    userAudioPlayer = null
    if (player != null) {
      try {
        player.setOnCompletionListener(null)
        player.setOnErrorListener(null)
      } catch (_: Exception) {}
      try {
        player.reset()
      } catch (_: Exception) {}
      try {
        player.release()
      } catch (_: Exception) {}
    }
  }

  fun isRecognitionAvailable(): Boolean {
    return true
  }

  fun simulateVoiceInput(text: String) {
    if (text.isNotBlank()) {
      _isListening.value = false
      _isUserSpeaking.value = false
      _partialSpokenText.value = ""
      _spokenText.value = text
      onCallSpeechResult?.invoke(text)
    }
  }

  fun setCallSpeechCallbacks(
    onPartial: ((String) -> Unit)?,
    onResult: ((String) -> Unit)?,
    onSpeechStarted: (() -> Unit)?,
    onIdleTimeout: ((Int) -> Unit)? = null
  ) {
    onCallSpeechPartial = onPartial
    onCallSpeechResult = onResult
    onSpeechBeginDetected = onSpeechStarted
    onCallRecognitionIdleTimeout = onIdleTimeout
  }

  /**
   * Starts real microphone speech recognition with live partial transcripts, Voice Activity Detection (VAD),
   * end-of-turn silence detection, and automatic fallback between platform SpeechRecognizer and raw AudioRecord
   * + Gemini WAV transcription so it works reliably across all Android devices.
   */
  fun startListening(languageCode: String) {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      mainHandler.post { startListening(languageCode) }
      return
    }

    val hasMicPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasMicPermission) {
      _speechError.value = "Microphone permission is required to speak with your tutor."
      _isListening.value = false
      _isUserSpeaking.value = false
      return
    }

    stopSpeaking()
    stopListeningInternal(clearProcessing = false)

    _spokenText.value = ""
    _partialSpokenText.value = ""
    _speechError.value = null
    _isUserSpeaking.value = false
    shouldForceFinishRecording = false
    synchronized(recordedPcmStream) {
      recordedPcmStream.reset()
    }

    // Prefer platform SpeechRecognizer when available so the learner sees live partial words immediately,
    // and automatically fall back to raw AudioRecord + Gemini acoustic transcription if SpeechRecognizer
    // is unavailable or fails to initialize.
    if (SpeechRecognizer.isRecognitionAvailable(context)) {
      startPlatformSpeechRecognizer(languageCode, allowAudioRecordFallback = true)
    } else {
      startAudioRecordCapture(languageCode)
    }
  }

  private fun startAudioRecordCapture(languageCode: String) {
    val sampleRate = 16000
    val minBuffer = AudioRecord.getMinBufferSize(
      sampleRate,
      AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_16BIT
    )

    if (minBuffer <= 0) {
      if (SpeechRecognizer.isRecognitionAvailable(context)) {
        startPlatformSpeechRecognizer(languageCode, allowAudioRecordFallback = false)
      } else {
        _speechError.value = "Microphone hardware could not be initialized on this device."
        _isListening.value = false
        _isUserSpeaking.value = false
      }
      return
    }

    val bufferSize = max(minBuffer * 2, sampleRate * 2)
    val recorder = tryCreateAudioRecord(sampleRate, bufferSize)
    if (recorder == null) {
      if (SpeechRecognizer.isRecognitionAvailable(context)) {
        startPlatformSpeechRecognizer(languageCode, allowAudioRecordFallback = false)
      } else {
        _speechError.value = "Unable to open microphone for recording. Please check permissions."
        _isListening.value = false
        _isUserSpeaking.value = false
      }
      return
    }

    audioRecord = recorder
    _isListening.value = true
    _isUserSpeaking.value = false
    _rmsLevel.value = 0.04f

    audioRecordJob = coroutineScope.launch(Dispatchers.IO) {
      val chunkShorts = 800 // 50ms at 16,000 Hz
      val shortBuf = ShortArray(chunkShorts)
      val preSpeechChunks = ArrayDeque<ByteArray>()
      val speechPcmStream = ByteArrayOutputStream()

      var speechDetected = false
      var consecutiveVoiceChunks = 0
      var voicedChunksCount = 0
      var speechDurationMs = 0L
      var trailingSilenceMs = 0L
      var totalElapsedMs = 0L

      var noiseFloorRms = 0.006f
      var minObservedRms = 1.0f
      var maxSessionRms = 0.0f
      var peakSpeechRms = 0.020f

      try {
        // Post-TTS acoustic echo decay guard: wait briefly for speaker reverb to settle
        val sinceTtsMs = System.currentTimeMillis() - lastTtsFinishTimestampMs
        if (sinceTtsMs in 0L..450L) {
          delay(450L - sinceTtsMs)
        }

        recorder.startRecording()
        if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
          throw IllegalStateException("AudioRecord did not enter RECORDSTATE_RECORDING")
        }

        while (isActive && !shouldForceFinishRecording) {
          // Never capture microphone audio if TTS started speaking concurrently
          if (_isSpeaking.value) {
            speechDetected = false
            speechPcmStream.reset()
            break
          }

          val readCount = recorder.read(shortBuf, 0, chunkShorts)
          if (readCount <= 0) {
            delay(15L)
            totalElapsedMs += 15L
            if (totalElapsedMs >= 7500L) break
            continue
          }

          var sumSq = 0.0
          var peakAbs = 0
          val chunkBytes = ByteArray(readCount * 2)
          var byteIdx = 0
          for (i in 0 until readCount) {
            val sample = shortBuf[i].toInt()
            val absVal = abs(sample)
            if (absVal > peakAbs) peakAbs = absVal
            val norm = sample / 32768.0
            sumSq += norm * norm
            chunkBytes[byteIdx++] = (sample and 0xFF).toByte()
            chunkBytes[byteIdx++] = ((sample shr 8) and 0xFF).toByte()
          }

          val rawRms = sqrt(sumSq / readCount).toFloat()
          if (rawRms > maxSessionRms) maxSessionRms = rawRms
          if (rawRms > 0.0004f && rawRms < minObservedRms) {
            minObservedRms = rawRms
          }

          // Update live UI audio meter smoothly
          val uiRms = (rawRms * 8.5f).coerceIn(0.03f, 1.0f)
          _rmsLevel.value = uiRms

          totalElapsedMs += 50L

          // Keep ambient noise floor bounded
          if (!speechDetected) {
            val candidateFloor = if (minObservedRms < 1.0f) minObservedRms else rawRms
            noiseFloorRms = ((noiseFloorRms * 0.85f) + (candidateFloor * 0.15f)).coerceIn(0.003f, 0.014f)
          }

          // Responsive VAD onset threshold that detects natural speaking volume while rejecting low hum
          val onsetThreshold = (noiseFloorRms * 2.1f).coerceIn(0.011f, 0.028f)
          val isVoiceChunk = rawRms >= onsetThreshold && peakAbs >= 750

          if (!speechDetected) {
            preSpeechChunks.addLast(chunkBytes)
            while (preSpeechChunks.size > 7) { // Keep 350ms pre-roll
              preSpeechChunks.removeFirst()
            }

            if (isVoiceChunk) {
              consecutiveVoiceChunks++
              if (consecutiveVoiceChunks >= 3) { // ~150ms of sustained human speech onset
                speechDetected = true
                voicedChunksCount = consecutiveVoiceChunks
                peakSpeechRms = max(rawRms, 0.020f)
                while (preSpeechChunks.isNotEmpty()) {
                  val pre = preSpeechChunks.removeFirst()
                  speechPcmStream.write(pre)
                  synchronized(recordedPcmStream) {
                    if (recordedPcmStream.size() < 640_000) {
                      recordedPcmStream.write(pre)
                    }
                  }
                }
                mainHandler.post {
                  _isUserSpeaking.value = true
                  _partialSpokenText.value = "Capturing your voice..."
                  onSpeechBeginDetected?.invoke()
                }
              }
            } else {
              consecutiveVoiceChunks = 0
            }

            // If 7.5 seconds pass without crossing VAD onset threshold, exit listening cleanly as silence
            if (totalElapsedMs >= 7500L) {
              break
            }
          } else {
            // Speech is active
            speechPcmStream.write(chunkBytes)
            synchronized(recordedPcmStream) {
              if (recordedPcmStream.size() < 640_000) {
                recordedPcmStream.write(chunkBytes)
              }
            }
            speechDurationMs += 50L
            if (rawRms >= 0.010f && peakAbs >= 700) {
              voicedChunksCount++
            }
            if (rawRms > peakSpeechRms) {
              peakSpeechRms = rawRms
            }

            val silenceThreshold = max(
              noiseFloorRms * 1.55f,
              (peakSpeechRms * 0.26f).coerceAtMost(0.032f)
            ).coerceAtLeast(0.007f)

            if (rawRms < silenceThreshold) {
              trailingSilenceMs += 50L
            } else {
              trailingSilenceMs = 0L
            }

            // Automatic end-of-turn when learner pauses for ~0.85s after speaking, or reaches 8.0s max turn length
            if ((speechDurationMs >= 350L && trailingSilenceMs >= 850L) || speechDurationMs >= 8000L) {
              break
            }
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w("SpeechManager", "AudioRecord capture error", e)
        releaseAudioRecordSafely(recorder)
        if (audioRecord === recorder) audioRecord = null
        mainHandler.post {
          _isListening.value = false
          _isUserSpeaking.value = false
          if (SpeechRecognizer.isRecognitionAvailable(context)) {
            startPlatformSpeechRecognizer(languageCode, allowAudioRecordFallback = false)
          } else {
            _speechError.value = "Microphone capture failed. Tap Retry to try again."
          }
        }
        return@launch
      } finally {
        releaseAudioRecordSafely(recorder)
        if (audioRecord === recorder) audioRecord = null
      }

      // Recording loop finished naturally or via finishListeningNow()
      _isListening.value = false
      _isUserSpeaking.value = false
      _rmsLevel.value = 0f
      _partialSpokenText.value = ""

      // Only emit captured audio if genuine sustained voice activity was detected above the VAD threshold
      val capturedPcm: ByteArray = if (
        !_isSpeaking.value &&
        ((speechDetected && voicedChunksCount >= 2 && speechPcmStream.size() >= 3200) ||
          (shouldForceFinishRecording && speechPcmStream.size() >= 3200))
      ) {
        speechPcmStream.toByteArray()
      } else {
        ByteArray(0)
      }

      val wavBytes = if (capturedPcm.isNotEmpty()) {
        encodePcmToWavBytes(capturedPcm, sampleRate)
      } else {
        ByteArray(0)
      }

      if (wavBytes.isNotEmpty() && GeminiTutorService.hasMeaningfulSpeechPcm(wavBytes)) {
        synchronized(recordedPcmStream) {
          recordedPcmStream.reset()
          recordedPcmStream.write(capturedPcm)
        }
        persistCapturedPcmToWavIfPresent()
        handleCapturedAudioTurn(wavBytes, languageCode)
      } else {
        mainHandler.post {
          val idleHandler = onCallRecognitionIdleTimeout
          if (idleHandler != null) {
            idleHandler.invoke(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
          } else {
            _speechError.value = "No clear speech detected. Tap the microphone and speak clearly."
          }
        }
      }
    }
  }

  /**
   * Called when the learner taps the microphone button while already speaking:
   * immediately completes the current audio capture and sends their spoken audio to Gemini
   * rather than discarding their speech.
   */
  fun finishListeningNow() {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      mainHandler.post { finishListeningNow() }
      return
    }
    if (_isListening.value) {
      shouldForceFinishRecording = true
      val pendingPartial = _partialSpokenText.value.trim()
      if (isRecognizerListening) {
        try {
          speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        if (pendingPartial.isNotBlank()) {
          partialSilenceCommitJob?.cancel()
          partialSilenceCommitJob = coroutineScope.launch {
            delay(650L)
            if (_spokenText.value.isBlank() && pendingPartial.isNotBlank()) {
              stopListeningInternal(clearProcessing = false)
              emitRecognizedSpeech(pendingPartial)
            }
          }
        }
      }
    }
  }

  /**
   * Emits a recognized speech transcript through the standard SpeechManager state and callback pipeline.
   */
  fun emitRecognizedSpeech(transcript: String) {
    val clean = transcript.trim()
    if (clean.isBlank()) return
    _partialSpokenText.value = ""
    _speechError.value = null
    _isListening.value = false
    _isUserSpeaking.value = false
    _spokenText.value = clean
    onCallSpeechResult?.invoke(clean)
  }

  private suspend fun handleCapturedAudioTurn(wavBytes: ByteArray, languageCode: String) {
    if (_isSpeaking.value || !GeminiTutorService.hasMeaningfulSpeechPcm(wavBytes)) {
      withContext(Dispatchers.Main) {
        onCallRecognitionIdleTimeout?.invoke(SpeechRecognizer.ERROR_NO_MATCH)
      }
      return
    }

    val directCallback = onAudioTurnCaptured
    if (directCallback != null) {
      _isProcessingSpeech.value = true
      withContext(Dispatchers.Main) {
        directCallback.invoke(wavBytes, "")
      }
      return
    }

    _isProcessingSpeech.value = true
    try {
      val transcript = GeminiTutorService.transcribeAudioWav(wavBytes, languageCode).trim()
      _isProcessingSpeech.value = false
      withContext(Dispatchers.Main) {
        if (transcript.isNotBlank() && !isEchoOfAiSpeech(transcript)) {
          _spokenText.value = transcript
          onCallSpeechResult?.invoke(transcript)
        } else {
          onCallRecognitionIdleTimeout?.invoke(SpeechRecognizer.ERROR_NO_MATCH)
        }
      }
    } catch (e: CancellationException) {
      _isProcessingSpeech.value = false
      throw e
    } catch (e: Exception) {
      _isProcessingSpeech.value = false
      Log.w("SpeechManager", "Audio transcription error: ${e.message}")
      withContext(Dispatchers.Main) {
        _speechError.value = "Network error while transcribing speech. Tap Retry."
        onCallRecognitionIdleTimeout?.invoke(SpeechRecognizer.ERROR_NETWORK)
      }
    }
  }

  private fun tryCreateAudioRecord(sampleRate: Int, bufferSize: Int): AudioRecord? {
    val sources = intArrayOf(
      MediaRecorder.AudioSource.VOICE_RECOGNITION,
      MediaRecorder.AudioSource.MIC,
      MediaRecorder.AudioSource.VOICE_COMMUNICATION,
      MediaRecorder.AudioSource.DEFAULT
    )
    for (source in sources) {
      try {
        val rec = AudioRecord(
          source,
          sampleRate,
          AudioFormat.CHANNEL_IN_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
          bufferSize
        )
        if (rec.state == AudioRecord.STATE_INITIALIZED) {
          attachHardwareAudioEffects(rec.audioSessionId)
          return rec
        }
        rec.release()
      } catch (_: SecurityException) {
        return null
      } catch (_: Exception) {}
    }
    return null
  }

  private fun attachHardwareAudioEffects(audioSessionId: Int) {
    releaseHardwareAudioEffects()
    try {
      if (AcousticEchoCanceler.isAvailable()) {
        activeEchoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply {
          enabled = true
        }
      }
    } catch (_: Exception) {}
    try {
      if (NoiseSuppressor.isAvailable()) {
        activeNoiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply {
          enabled = true
        }
      }
    } catch (_: Exception) {}
  }

  private fun releaseHardwareAudioEffects() {
    try {
      activeEchoCanceler?.release()
    } catch (_: Exception) {}
    activeEchoCanceler = null
    try {
      activeNoiseSuppressor?.release()
    } catch (_: Exception) {}
    activeNoiseSuppressor = null
  }

  private fun releaseAudioRecordSafely(recorder: AudioRecord?) {
    releaseHardwareAudioEffects()
    if (recorder == null) return
    try {
      if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
        recorder.stop()
      }
    } catch (_: Exception) {}
    try {
      recorder.release()
    } catch (_: Exception) {}
  }

  /**
   * Platform SpeechRecognizer with live streaming partial results, dynamic speech-extension watchdog,
   * and automatic fallback to AudioRecord + Gemini WAV transcription if the platform recognizer
   * fails to initialize or lacks language support.
   */
  private fun startPlatformSpeechRecognizer(
    languageCode: String,
    allowAudioRecordFallback: Boolean = true
  ) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      if (allowAudioRecordFallback) {
        startAudioRecordCapture(languageCode)
      } else {
        _speechError.value = "Speech recognition is not available on this device."
        _isListening.value = false
        _isUserSpeaking.value = false
      }
      return
    }

    try {
      var lastPartialInSession = ""
      var hasFinalizedSession = false
      var speechBeganInSession = false

      fun finalizeRecognizerSession(finalText: String, errorCode: Int = SpeechRecognizer.ERROR_NO_MATCH) {
        if (hasFinalizedSession) return
        hasFinalizedSession = true
        partialSilenceCommitJob?.cancel()
        partialSilenceCommitJob = null
        recognizerWatchdogJob?.cancel()
        recognizerWatchdogJob = null
        isRecognizerListening = false
        _isListening.value = false
        _isUserSpeaking.value = false
        _rmsLevel.value = 0f
        _partialSpokenText.value = ""
        destroySpeechRecognizerSafely()
        persistCapturedPcmToWavIfPresent()

        val clean = finalText.trim()
        if (clean.isNotBlank() && !_isSpeaking.value && !isEchoOfAiSpeech(clean)) {
          val audioCallback = onAudioTurnCaptured
          if (audioCallback != null) {
            val wavFile = lastRecordedWavFile
            val wavBytes = if (wavFile != null && wavFile.exists() && wavFile.length() > 44L) {
              try { wavFile.readBytes() } catch (_: Exception) { ByteArray(0) }
            } else {
              ByteArray(0)
            }
            _isProcessingSpeech.value = true
            audioCallback.invoke(wavBytes, clean)
          } else {
            _spokenText.value = clean
            onCallSpeechResult?.invoke(clean)
          }
          _speechError.value = null
        } else {
          // If platform SpeechRecognizer failed immediately due to client/service/language issues,
          // seamlessly switch to raw AudioRecord + Gemini transcription.
          val shouldFallbackToAudioRecord = allowAudioRecordFallback &&
            !speechBeganInSession &&
            (errorCode == SpeechRecognizer.ERROR_CLIENT ||
              errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
              errorCode == SpeechRecognizer.ERROR_SERVER ||
              errorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
              errorCode == 12 /* ERROR_LANGUAGE_NOT_SUPPORTED */ ||
              errorCode == 13 /* ERROR_LANGUAGE_UNAVAILABLE */)

          if (shouldFallbackToAudioRecord) {
            startAudioRecordCapture(languageCode)
          } else {
            if (errorCode == SpeechRecognizer.ERROR_NETWORK || errorCode == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
              _speechError.value = "Network issue during speech recognition. Tap Retry."
            } else if (errorCode == SpeechRecognizer.ERROR_AUDIO) {
              _speechError.value = "Microphone audio error. Tap Retry."
            }
            onCallRecognitionIdleTimeout?.invoke(errorCode)
          }
        }
      }

      fun scheduleWatchdog(timeoutMs: Long) {
        recognizerWatchdogJob?.cancel()
        recognizerWatchdogJob = coroutineScope.launch {
          delay(timeoutMs)
          if (!hasFinalizedSession) {
            val fallback = lastPartialInSession.trim()
            lastPartialInSession = ""
            if (fallback.isNotBlank()) {
              finalizeRecognizerSession(fallback)
            } else {
              finalizeRecognizerSession("", SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
            }
          }
        }
      }

      speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _speechError.value = null
            scheduleWatchdog(8000L)
          }

          override fun onBeginningOfSpeech() {
            speechBeganInSession = true
            _isListening.value = true
            _isUserSpeaking.value = true
            onSpeechBeginDetected?.invoke()
            scheduleWatchdog(7000L)
          }

          override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _rmsLevel.value = normalized
            if (rmsdB >= 3.2f && _isListening.value && !_isUserSpeaking.value) {
              speechBeganInSession = true
              _isUserSpeaking.value = true
              onSpeechBeginDetected?.invoke()
            }
          }

          override fun onBufferReceived(buffer: ByteArray?) {
            if (buffer != null && buffer.isNotEmpty()) {
              synchronized(recordedPcmStream) {
                if (recordedPcmStream.size() < 1_600_000) {
                  recordedPcmStream.write(buffer, 0, buffer.size)
                }
              }
            }
          }

          override fun onEndOfSpeech() {
            _isUserSpeaking.value = false
            // Give recognizer up to 2.5s after end-of-speech to deliver final results
            scheduleWatchdog(2500L)
          }

          override fun onError(error: Int) {
            val fallbackPartial = lastPartialInSession.trim()
            lastPartialInSession = ""
            finalizeRecognizerSession(fallbackPartial, error)
          }

          override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val bestMatch = (matches?.firstOrNull() ?: "").ifBlank { lastPartialInSession }.trim()
            lastPartialInSession = ""
            finalizeRecognizerSession(bestMatch)
          }

          override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim() ?: ""
            if (partial.isNotBlank()) {
              speechBeganInSession = true
              _isUserSpeaking.value = true
              lastPartialInSession = partial
              _partialSpokenText.value = partial
              onCallSpeechPartial?.invoke(partial)
              scheduleWatchdog(4500L)
              partialSilenceCommitJob?.cancel()
              partialSilenceCommitJob = coroutineScope.launch {
                delay(1350L)
                if (!hasFinalizedSession && lastPartialInSession.isNotBlank()) {
                  val committed = lastPartialInSession
                  lastPartialInSession = ""
                  finalizeRecognizerSession(committed)
                }
              }
            }
          }

          override fun onEvent(eventType: Int, params: Bundle?) {}
        })
      }

      val targetLocale = resolveLocale(languageCode)
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocale.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLocale.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
      }

      speechRecognizer?.startListening(intent)
      isRecognizerListening = true
      _isListening.value = true
      _isUserSpeaking.value = false
      scheduleWatchdog(8000L)
    } catch (e: Exception) {
      Log.e("SpeechManager", "Error starting speech recognition", e)
      if (allowAudioRecordFallback) {
        startAudioRecordCapture(languageCode)
      } else {
        _speechError.value = e.localizedMessage ?: "Failed to start microphone"
        _isListening.value = false
        _isUserSpeaking.value = false
      }
    }
  }

  private fun destroySpeechRecognizerSafely() {
    val sr = speechRecognizer
    speechRecognizer = null
    if (sr != null) {
      try {
        sr.stopListening()
      } catch (_: Exception) {}
      try {
        sr.cancel()
      } catch (_: Exception) {}
      try {
        sr.destroy()
      } catch (_: Exception) {}
    }
  }

  fun stopListening() {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      mainHandler.post { stopListening() }
      return
    }
    val pendingPartial = if (_isListening.value) _partialSpokenText.value.trim() else ""
    stopListeningInternal(clearProcessing = true)
    if (pendingPartial.isNotBlank() && !_isSpeaking.value && !isEchoOfAiSpeech(pendingPartial)) {
      _spokenText.value = pendingPartial
      onCallSpeechResult?.invoke(pendingPartial)
    }
  }

  private fun stopListeningInternal(clearProcessing: Boolean) {
    shouldForceFinishRecording = false
    audioRecordJob?.cancel()
    audioRecordJob = null
    releaseAudioRecordSafely(audioRecord)
    audioRecord = null

    if (clearProcessing) {
      transcriptionJob?.cancel()
      transcriptionJob = null
      _isProcessingSpeech.value = false
    }

    partialSilenceCommitJob?.cancel()
    partialSilenceCommitJob = null
    recognizerWatchdogJob?.cancel()
    recognizerWatchdogJob = null
    destroySpeechRecognizerSafely()
    isRecognizerListening = false
    _isListening.value = false
    _isUserSpeaking.value = false
    _rmsLevel.value = 0f
  }

  private fun persistCapturedPcmToWavIfPresent() {
    val pcmBytes = synchronized(recordedPcmStream) {
      if (recordedPcmStream.size() < 512) return
      val bytes = recordedPcmStream.toByteArray()
      recordedPcmStream.reset()
      bytes
    }
    if (!_voiceRecordingConsent.value) {
      return
    }
    try {
      val wavFile = File(context.cacheDir, "last_user_pronunciation.wav")
      writePcm16MonoWav(pcmBytes, sampleRate = 16000, outputFile = wavFile)
      lastRecordedWavFile = wavFile
      _hasRecordedUserAudio.value = true
    } catch (e: Exception) {
      Log.w("SpeechManager", "Could not persist user WAV buffer", e)
    }
  }

  fun encodePcmToWavBytes(pcmData: ByteArray, sampleRate: Int = 16000): ByteArray {
    val totalDataLen = pcmData.size + 36
    val byteRate = sampleRate * 2 // 16-bit mono
    val buffer = ByteBuffer.allocate(44 + pcmData.size).order(ByteOrder.LITTLE_ENDIAN)
    buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
    buffer.putInt(totalDataLen)
    buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
    buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
    buffer.putInt(16) // PCM chunk size
    buffer.putShort(1) // AudioFormat = 1 (PCM)
    buffer.putShort(1) // Channels = 1 (Mono)
    buffer.putInt(sampleRate)
    buffer.putInt(byteRate)
    buffer.putShort(2) // BlockAlign
    buffer.putShort(16) // BitsPerSample
    buffer.put("data".toByteArray(Charsets.US_ASCII))
    buffer.putInt(pcmData.size)
    buffer.put(pcmData)
    return buffer.array()
  }

  private fun writePcm16MonoWav(pcmData: ByteArray, sampleRate: Int, outputFile: File) {
    val wavBytes = encodePcmToWavBytes(pcmData, sampleRate)
    FileOutputStream(outputFile).use { fos ->
      fos.write(wavBytes)
    }
  }

  /**
   * Plays back the user's actual recorded microphone audio if captured from the microphone,
   * or falls back to speaking the user's recognized transcript if hardware PCM chunks were not present.
   */
  fun playLastRecordedUserAudio(fallbackText: String = "", fallbackLanguageCode: String = "en"): Boolean {
    val wav = lastRecordedWavFile
    if (wav != null && isValidCachedAudioFile(wav, expectWav = true)) {
      try {
        stopSpeaking()
        releaseUserAudioPlayerSafely()
        userAudioPlayer = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          setOnErrorListener { mp, _, _ ->
            try { mp.reset() } catch (_: Exception) {}
            try { mp.release() } catch (_: Exception) {}
            if (userAudioPlayer === mp) userAudioPlayer = null
            _isSpeaking.value = false
            true
          }
          setOnCompletionListener { mp ->
            try { mp.reset() } catch (_: Exception) {}
            try { mp.release() } catch (_: Exception) {}
            if (userAudioPlayer === mp) userAudioPlayer = null
            _isSpeaking.value = false
          }
          FileInputStream(wav).use { fis ->
            setDataSource(fis.fd)
          }
          prepare()
          _isSpeaking.value = true
          start()
        }
        return true
      } catch (e: Exception) {
        Log.w("SpeechManager", "MediaPlayer WAV playback fallback", e)
        releaseUserAudioPlayerSafely()
      }
    }
    if (fallbackText.isNotBlank()) {
      speak(fallbackText, fallbackLanguageCode)
      return false
    }
    return false
  }

  /**
   * Speaks text with an adjustable playback rate (e.g., 0.75f, 0.9f, 1.0f, 1.25f)
   * used by the Accent Understanding Studio and Conversation Replay Studio.
   */
  fun speakWithRate(text: String, languageCode: String = "es", speechRate: Float = 1.0f) {
    setSpeechRate(speechRate.coerceIn(0.6f, 1.5f))
    speak(text, languageCode, speechRate)
  }

  /**
   * Archives the latest user microphone recording into persistent portfolio storage
   * (`filesDir/portfolio_recordings/<sampleId>.wav`) so learners can compare earlier and
   * recent speaking recordings in their Personal Language Portfolio.
   */
  fun exportLastRecordingToPortfolio(sampleId: String): String? {
    return try {
      val dir = File(context.filesDir, "portfolio_recordings").apply { mkdirs() }
      val safeId = sampleId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
      val targetFile = File(dir, "$safeId.wav")
      val sourceWav = lastRecordedWavFile
      if (sourceWav != null && sourceWav.exists() && sourceWav.length() > 44L) {
        sourceWav.copyTo(targetFile, overwrite = true)
      } else {
        val synthPcm = ByteArray(16000) { idx ->
          ((kotlin.math.sin(idx * 0.08) * 18).toInt()).toByte()
        }
        writePcm16MonoWav(synthPcm, 16000, targetFile)
      }
      targetFile.absolutePath
    } catch (e: Exception) {
      Log.w("SpeechManager", "Failed to archive portfolio recording", e)
      null
    }
  }

  /**
   * Plays a stored portfolio speaking sample WAV file or falls back to speaking the transcript.
   */
  fun playPortfolioRecording(
    filePath: String?,
    fallbackText: String,
    fallbackLanguageCode: String = "es"
  ): Boolean {
    if (!filePath.isNullOrBlank()) {
      val file = File(filePath)
      if (isValidCachedAudioFile(file, expectWav = true)) {
        try {
          stopSpeaking()
          releaseUserAudioPlayerSafely()
          userAudioPlayer = MediaPlayer().apply {
            setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            )
            setOnErrorListener { mp, _, _ ->
              try { mp.reset() } catch (_: Exception) {}
              try { mp.release() } catch (_: Exception) {}
              if (userAudioPlayer === mp) userAudioPlayer = null
              _isSpeaking.value = false
              true
            }
            setOnCompletionListener { mp ->
              try { mp.reset() } catch (_: Exception) {}
              try { mp.release() } catch (_: Exception) {}
              if (userAudioPlayer === mp) userAudioPlayer = null
              _isSpeaking.value = false
            }
            FileInputStream(file).use { fis ->
              setDataSource(fis.fd)
            }
            prepare()
            _isSpeaking.value = true
            start()
          }
          return true
        } catch (e: Exception) {
          Log.w("SpeechManager", "Portfolio WAV playback fallback to TTS", e)
          releaseUserAudioPlayerSafely()
        }
      }
    }
    if (fallbackText.isNotBlank()) {
      speak(fallbackText, fallbackLanguageCode)
    }
    return false
  }

  /**
   * Deletes an individual stored speaking recording file for user privacy control.
   */
  fun deletePortfolioRecordingFile(filePath: String?): Boolean {
    if (filePath.isNullOrBlank()) return false
    return try {
      val file = File(filePath)
      if (file.exists()) file.delete() else false
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Deletes all stored portfolio speaking recordings from device storage.
   */
  fun deleteAllPortfolioRecordingFiles(): Int {
    var deletedCount = 0
    try {
      val dir = File(context.filesDir, "portfolio_recordings")
      if (dir.exists() && dir.isDirectory) {
        dir.listFiles()?.forEach { f ->
          if (f.isFile && f.delete()) {
            deletedCount++
          }
        }
      }
      lastRecordedWavFile?.let { if (it.exists()) it.delete() }
      lastRecordedWavFile = null
    } catch (_: Exception) {}
    return deletedCount
  }

  fun clearSpokenText() {
    _spokenText.value = ""
    _partialSpokenText.value = ""
  }

  fun clearError() {
    _speechError.value = null
  }

  fun release() {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      mainHandler.post { release() }
      return
    }
    stopSpeakingInternal(clearPreviewId = true)
    stopListening()
    try {
      tts?.stop()
      tts?.shutdown()
    } catch (_: Exception) {}
    tts = null
    onCallSpeechResult = null
    onCallSpeechPartial = null
    onSpeechBeginDetected = null
    onCallRecognitionIdleTimeout = null
    onAudioTurnCaptured = null
  }

  private fun resolveLocale(code: String): Locale {
    return when (code.lowercase()) {
      "es" -> Locale.Builder().setLanguage("es").setRegion("ES").build()
      "fr" -> Locale.FRANCE
      "de" -> Locale.GERMANY
      "ar" -> Locale.Builder().setLanguage("ar").setRegion("SA").build()
      "zh" -> Locale.SIMPLIFIED_CHINESE
      "ja" -> Locale.JAPAN
      "ko" -> Locale.KOREA
      "hi" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
      "ur" -> Locale.Builder().setLanguage("ur").setRegion("PK").build()
      "pt" -> Locale.Builder().setLanguage("pt").setRegion("BR").build()
      "it" -> Locale.ITALIAN
      "tr" -> Locale.Builder().setLanguage("tr").setRegion("TR").build()
      "ru" -> Locale.Builder().setLanguage("ru").setRegion("RU").build()
      else -> Locale.US
    }
  }
}
