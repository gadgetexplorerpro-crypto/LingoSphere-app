package com.example.speech

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class VoiceRecordingMode {
  IDLE,
  HOLD_RECORDING,
  LOCKED_RECORDING,
  PAUSED,
  PREVIEW_READY
}

data class RecordedVoiceDraft(
  val audioFile: File,
  val audioBase64: String,
  val durationMs: Long,
  val mimeType: String,
  val waveform: List<Int>
)

data class VoicePlaybackState(
  val activeMessageId: String? = null,
  val isPlaying: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val playbackSpeed: Float = 1.0f
)

/**
 * Real hardware microphone recorder and audio player for Language Exchange voice messages.
 *
 * - Captures real microphone audio into compressed AAC (.m4a) via MediaRecorder (24 kbps, 16 kHz mono)
 *   with automatic fallback to 16 kHz PCM (.wav) if AAC encoder is unavailable on a virtual device.
 * - Samples real microphone amplitude every 80ms to generate an authentic waveform (0..100).
 * - Supports Hold-to-Record (release to send, slide left to cancel, slide up to lock) and
 *   Tap-to-Record (Pause, Resume, Preview before sending, Delete, Send).
 * - Enforces single-instance playback so playing one voice message automatically pauses any other.
 * - Supports 1.0x, 1.5x, and 2.0x playback speeds and interactive waveform seeking.
 */
class VoiceMessageController(private val context: Context) {
  companion object {
    private const val TAG = "VoiceMsgController"
    private const val SAMPLE_RATE = 16000
    private const val AAC_BITRATE = 24000
    private const val MIN_VALID_DURATION_MS = 600L
    const val PREVIEW_DRAFT_ID = "__voice_preview_draft__"
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val _recordingMode = MutableStateFlow(VoiceRecordingMode.IDLE)
  val recordingMode: StateFlow<VoiceRecordingMode> = _recordingMode.asStateFlow()

  private val _recordingDurationMs = MutableStateFlow(0L)
  val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

  private val _liveWaveform = MutableStateFlow<List<Int>>(emptyList())
  val liveWaveform: StateFlow<List<Int>> = _liveWaveform.asStateFlow()

  private val _currentAmplitude = MutableStateFlow(0)
  val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

  private val _previewDraft = MutableStateFlow<RecordedVoiceDraft?>(null)
  val previewDraft: StateFlow<RecordedVoiceDraft?> = _previewDraft.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _playbackState = MutableStateFlow(VoicePlaybackState())
  val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

  private var mediaRecorder: MediaRecorder? = null
  private var currentOutputFile: File? = null
  private var currentMimeType: String = "audio/mp4"
  private var recordingTimerJob: Job? = null
  private var accumulatedRecordedMs: Long = 0L
  private var lastResumeTimestampMs: Long = 0L
  private val recordedAmplitudes = mutableListOf<Int>()

  // Fallback PCM AudioRecord when MediaRecorder AAC is unavailable
  private var fallbackAudioRecord: AudioRecord? = null
  private var fallbackJob: Job? = null
  private val fallbackPcmStream = ByteArrayOutputStream()
  @Volatile private var isFallbackActive = false
  @Volatile private var isFallbackPaused = false

  // MediaPlayer for voice message bubbles & preview
  private var mediaPlayer: MediaPlayer? = null
  private var playbackProgressJob: Job? = null

  fun hasRecordPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun clearError() {
    _errorMessage.value = null
  }

  /**
   * Starts real microphone recording.
   * @param lockImmediately true for Tap-to-Record mode; false for Hold-to-Record mode.
   */
  @SuppressLint("MissingPermission")
  fun startRecording(lockImmediately: Boolean = false): Boolean {
    if (!hasRecordPermission()) {
      _errorMessage.value = "Microphone permission is required to record voice messages."
      return false
    }

    // Pause any active voice message playback before recording
    stopPlayback()
    discardPreviewDraft()
    stopInternalRecorders()

    recordedAmplitudes.clear()
    accumulatedRecordedMs = 0L
    _recordingDurationMs.value = 0L
    _liveWaveform.value = emptyList()
    _currentAmplitude.value = 0
    _errorMessage.value = null

    val cacheDir = File(context.cacheDir, "voice_messages").apply { mkdirs() }
    val outFile = File(cacheDir, "rec_${System.currentTimeMillis()}.m4a")
    currentOutputFile = outFile
    currentMimeType = "audio/mp4"

    var startedWithMediaRecorder = false
    try {
      val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }
      recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
      recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      recorder.setAudioSamplingRate(SAMPLE_RATE)
      recorder.setAudioChannels(1)
      recorder.setAudioEncodingBitRate(AAC_BITRATE)
      recorder.setOutputFile(outFile.absolutePath)
      recorder.prepare()
      recorder.start()
      mediaRecorder = recorder
      startedWithMediaRecorder = true
    } catch (e: Exception) {
      Log.w(TAG, "MediaRecorder AAC unavailable, falling back to AudioRecord PCM: ${e.message}")
      try {
        mediaRecorder?.release()
      } catch (_: Exception) {}
      mediaRecorder = null
      outFile.delete()
    }

    if (!startedWithMediaRecorder) {
      val wavFile = File(cacheDir, "rec_${System.currentTimeMillis()}.wav")
      currentOutputFile = wavFile
      currentMimeType = "audio/wav"
      if (!startFallbackPcmRecorder()) {
        _errorMessage.value = "Unable to initialize microphone. Please check audio permissions."
        _recordingMode.value = VoiceRecordingMode.IDLE
        return false
      }
    }

    lastResumeTimestampMs = System.currentTimeMillis()
    _recordingMode.value = if (lockImmediately) {
      VoiceRecordingMode.LOCKED_RECORDING
    } else {
      VoiceRecordingMode.HOLD_RECORDING
    }

    startAmplitudeAndTimerLoop()
    return true
  }

  @SuppressLint("MissingPermission")
  private fun startFallbackPcmRecorder(): Boolean {
    return try {
      val minBuf = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      )
      if (minBuf <= 0) return false
      val recorder = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        minBuf * 2
      )
      if (recorder.state != AudioRecord.STATE_INITIALIZED) {
        recorder.release()
        return false
      }
      synchronized(fallbackPcmStream) {
        fallbackPcmStream.reset()
      }
      isFallbackActive = true
      isFallbackPaused = false
      recorder.startRecording()
      fallbackAudioRecord = recorder

      fallbackJob = scope.launch(Dispatchers.IO) {
        val shortBuf = ShortArray(1024)
        while (isActive && isFallbackActive) {
          val read = try {
            recorder.read(shortBuf, 0, shortBuf.size)
          } catch (_: Exception) {
            -1
          }
          if (read > 0 && !isFallbackPaused) {
            var sumSq = 0.0
            val byteBuf = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until read) {
              val s = shortBuf[i]
              val norm = s / 32768.0
              sumSq += norm * norm
              byteBuf.putShort(s)
            }
            synchronized(fallbackPcmStream) {
              if (fallbackPcmStream.size() < SAMPLE_RATE * 2 * 90) {
                fallbackPcmStream.write(byteBuf.array())
              }
            }
            val rms = sqrt(sumSq / read)
            val level = (rms * 380.0).roundToInt().coerceIn(4, 100)
            _currentAmplitude.value = level
          } else {
            delay(20L)
          }
        }
      }
      true
    } catch (e: Exception) {
      Log.e(TAG, "Fallback PCM recorder error: ${e.message}")
      false
    }
  }

  private fun startAmplitudeAndTimerLoop() {
    recordingTimerJob?.cancel()
    recordingTimerJob = scope.launch {
      while (isActive &&
        (_recordingMode.value == VoiceRecordingMode.HOLD_RECORDING ||
          _recordingMode.value == VoiceRecordingMode.LOCKED_RECORDING)
      ) {
        val now = System.currentTimeMillis()
        val totalMs = accumulatedRecordedMs + (now - lastResumeTimestampMs).coerceAtLeast(0L)
        _recordingDurationMs.value = totalMs

        val amp = if (mediaRecorder != null) {
          val rawMax = try {
            mediaRecorder?.maxAmplitude ?: 0
          } catch (_: Exception) {
            0
          }
          // Convert 0..32767 to a natural 6..100 visual bar height
          if (rawMax <= 80) {
            6
          } else {
            ((sqrt(rawMax / 32768.0) * 115.0).roundToInt()).coerceIn(8, 100)
          }
        } else {
          _currentAmplitude.value.coerceIn(6, 100)
        }

        _currentAmplitude.value = amp
        recordedAmplitudes.add(amp)
        _liveWaveform.value = recordedAmplitudes.takeLast(36)

        // Auto-stop and move to preview at 60 seconds so Firestore payload stays compact
        if (totalMs >= 60_000L) {
          stopRecordingToPreview()
          break
        }
        delay(80L)
      }
    }
  }

  fun lockRecording() {
    if (_recordingMode.value == VoiceRecordingMode.HOLD_RECORDING) {
      _recordingMode.value = VoiceRecordingMode.LOCKED_RECORDING
    }
  }

  fun pauseRecording() {
    if (_recordingMode.value != VoiceRecordingMode.LOCKED_RECORDING &&
      _recordingMode.value != VoiceRecordingMode.HOLD_RECORDING
    ) return

    val now = System.currentTimeMillis()
    accumulatedRecordedMs += (now - lastResumeTimestampMs).coerceAtLeast(0L)
    _recordingDurationMs.value = accumulatedRecordedMs
    recordingTimerJob?.cancel()

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        mediaRecorder?.pause()
      }
    } catch (_: Exception) {}
    isFallbackPaused = true
    _recordingMode.value = VoiceRecordingMode.PAUSED
  }

  fun resumeRecording() {
    if (_recordingMode.value != VoiceRecordingMode.PAUSED) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        mediaRecorder?.resume()
      }
    } catch (_: Exception) {}
    isFallbackPaused = false
    lastResumeTimestampMs = System.currentTimeMillis()
    _recordingMode.value = VoiceRecordingMode.LOCKED_RECORDING
    startAmplitudeAndTimerLoop()
  }

  /**
   * Stops active recording and prepares a preview draft so the user can listen before sending.
   */
  fun stopRecordingToPreview(): RecordedVoiceDraft? {
    val draft = finalizeRecordingInternal()
    if (draft != null) {
      _previewDraft.value = draft
      _recordingMode.value = VoiceRecordingMode.PREVIEW_READY
    } else {
      _recordingMode.value = VoiceRecordingMode.IDLE
    }
    return draft
  }

  /**
   * Stops active recording (or consumes the preview draft) and returns the finalized RecordedVoiceDraft
   * ready to send immediately.
   */
  fun finishRecordingForSend(): RecordedVoiceDraft? {
    val existingPreview = _previewDraft.value
    if (existingPreview != null && _recordingMode.value == VoiceRecordingMode.PREVIEW_READY) {
      stopPlayback()
      _previewDraft.value = null
      _recordingMode.value = VoiceRecordingMode.IDLE
      _recordingDurationMs.value = 0L
      _liveWaveform.value = emptyList()
      return existingPreview
    }

    val draft = finalizeRecordingInternal()
    _previewDraft.value = null
    _recordingMode.value = VoiceRecordingMode.IDLE
    _recordingDurationMs.value = 0L
    _liveWaveform.value = emptyList()
    return draft
  }

  fun cancelRecording() {
    stopPlayback()
    recordingTimerJob?.cancel()
    recordingTimerJob = null
    stopInternalRecorders()
    try {
      currentOutputFile?.delete()
    } catch (_: Exception) {}
    currentOutputFile = null
    _previewDraft.value?.audioFile?.delete()
    _previewDraft.value = null
    recordedAmplitudes.clear()
    accumulatedRecordedMs = 0L
    _recordingDurationMs.value = 0L
    _liveWaveform.value = emptyList()
    _currentAmplitude.value = 0
    _recordingMode.value = VoiceRecordingMode.IDLE
  }

  fun discardPreviewDraft() {
    if (_playbackState.value.activeMessageId == PREVIEW_DRAFT_ID) {
      stopPlayback()
    }
    try {
      _previewDraft.value?.audioFile?.delete()
    } catch (_: Exception) {}
    _previewDraft.value = null
    if (_recordingMode.value == VoiceRecordingMode.PREVIEW_READY) {
      _recordingMode.value = VoiceRecordingMode.IDLE
      _recordingDurationMs.value = 0L
      _liveWaveform.value = emptyList()
    }
  }

  private fun finalizeRecordingInternal(): RecordedVoiceDraft? {
    val now = System.currentTimeMillis()
    if (_recordingMode.value == VoiceRecordingMode.HOLD_RECORDING ||
      _recordingMode.value == VoiceRecordingMode.LOCKED_RECORDING
    ) {
      accumulatedRecordedMs += (now - lastResumeTimestampMs).coerceAtLeast(0L)
    }
    val totalDurationMs = accumulatedRecordedMs.coerceAtLeast(_recordingDurationMs.value)
    recordingTimerJob?.cancel()
    recordingTimerJob = null

    val outFile = currentOutputFile
    stopInternalRecorders()

    if (outFile == null || totalDurationMs < MIN_VALID_DURATION_MS) {
      try {
        outFile?.delete()
      } catch (_: Exception) {}
      _errorMessage.value = "Hold or tap the microphone a little longer to record a voice message."
      return null
    }

    if (currentMimeType == "audio/wav") {
      val pcmBytes = synchronized(fallbackPcmStream) { fallbackPcmStream.toByteArray() }
      if (pcmBytes.size < 1600) {
        _errorMessage.value = "No microphone audio detected. Please try speaking closer to the microphone."
        return null
      }
      writeWavFile(outFile, pcmBytes, SAMPLE_RATE)
    }

    if (!outFile.exists() || outFile.length() < 256L) {
      _errorMessage.value = "Voice recording was empty and was not sent."
      return null
    }

    val bytes = try {
      outFile.readBytes()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to read recorded voice file: ${e.message}")
      return null
    }

    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    val normalizedBars = normalizeWaveformBars(recordedAmplitudes, targetCount = 32)

    return RecordedVoiceDraft(
      audioFile = outFile,
      audioBase64 = base64,
      durationMs = totalDurationMs,
      mimeType = currentMimeType,
      waveform = normalizedBars
    )
  }

  private fun stopInternalRecorders() {
    val rec = mediaRecorder
    mediaRecorder = null
    if (rec != null) {
      try {
        rec.stop()
      } catch (_: Exception) {}
      try {
        rec.reset()
        rec.release()
      } catch (_: Exception) {}
    }

    isFallbackActive = false
    isFallbackPaused = false
    fallbackJob?.cancel()
    fallbackJob = null
    val fallbackRec = fallbackAudioRecord
    fallbackAudioRecord = null
    if (fallbackRec != null) {
      try {
        fallbackRec.stop()
      } catch (_: Exception) {}
      try {
        fallbackRec.release()
      } catch (_: Exception) {}
    }
  }

  private fun normalizeWaveformBars(raw: List<Int>, targetCount: Int): List<Int> {
    if (raw.isEmpty()) {
      return List(targetCount) { idx -> 18 + ((idx * 13) % 45) }
    }
    if (raw.size <= targetCount) {
      val padded = raw.toMutableList()
      while (padded.size < targetCount) {
        padded.add(raw[padded.size % raw.size].coerceIn(10, 100))
      }
      return padded.map { it.coerceIn(10, 100) }
    }
    val step = raw.size.toFloat() / targetCount.toFloat()
    return List(targetCount) { i ->
      val start = (i * step).toInt().coerceIn(0, raw.lastIndex)
      val end = ((i + 1) * step).toInt().coerceIn(start + 1, raw.size)
      val slice = raw.subList(start, end)
      (slice.average().roundToInt()).coerceIn(10, 100)
    }
  }

  private fun writeWavFile(file: File, pcmData: ByteArray, sampleRate: Int) {
    val totalDataLen = pcmData.size + 36
    val byteRate = sampleRate * 2
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(totalDataLen)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16)
    header.putShort(1)
    header.putShort(1)
    header.putInt(sampleRate)
    header.putInt(byteRate)
    header.putShort(2)
    header.putShort(16)
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(pcmData.size)

    FileOutputStream(file).use { fos ->
      fos.write(header.array())
      fos.write(pcmData)
    }
  }

  // ---------------------------------------------------------------------------
  // Voice Message Playback (Single Active Instance + 1x / 1.5x / 2x Speed + Seek)
  // ---------------------------------------------------------------------------

  fun togglePlayPreviewDraft() {
    val draft = _previewDraft.value ?: return
    togglePlayFile(
      messageId = PREVIEW_DRAFT_ID,
      file = draft.audioFile,
      fallbackDurationMs = draft.durationMs
    )
  }

  @Volatile
  private var isVoicePlayerStarted: Boolean = false

  fun togglePlayVoiceMessage(
    messageId: String,
    audioBase64: String,
    mimeType: String,
    fallbackDurationMs: Long
  ) {
    if (messageId.isBlank() || audioBase64.isBlank()) return
    val current = _playbackState.value
    if (current.activeMessageId == messageId && mediaPlayer != null) {
      val player = mediaPlayer!!
      try {
        if (isVoicePlayerStarted && current.isPlaying) {
          isVoicePlayerStarted = false
          player.pause()
          _playbackState.value = current.copy(
            isPlaying = false,
            currentPositionMs = player.currentPosition.toLong()
          )
        } else {
          player.start()
          isVoicePlayerStarted = true
          if (abs(current.playbackSpeed - 1.0f) > 0.02f) {
            applyPlaybackSpeed(player, current.playbackSpeed)
          }
          _playbackState.value = current.copy(isPlaying = true)
          startPlaybackProgressLoop(messageId, fallbackDurationMs)
        }
        return
      } catch (_: Exception) {
        stopPlayback()
      }
    }

    val cachedFile = resolveCachedAudioFile(messageId, audioBase64, mimeType) ?: run {
      _errorMessage.value = "Unable to play voice message audio."
      return
    }
    togglePlayFile(
      messageId = messageId,
      file = cachedFile,
      fallbackDurationMs = fallbackDurationMs
    )
  }

  private fun togglePlayFile(
    messageId: String,
    file: File,
    fallbackDurationMs: Long
  ) {
    val current = _playbackState.value
    if (current.activeMessageId == messageId && mediaPlayer != null) {
      val player = mediaPlayer!!
      try {
        if (isVoicePlayerStarted && current.isPlaying) {
          isVoicePlayerStarted = false
          player.pause()
          _playbackState.value = current.copy(
            isPlaying = false,
            currentPositionMs = player.currentPosition.toLong()
          )
        } else {
          player.start()
          isVoicePlayerStarted = true
          if (abs(current.playbackSpeed - 1.0f) > 0.02f) {
            applyPlaybackSpeed(player, current.playbackSpeed)
          }
          _playbackState.value = current.copy(isPlaying = true)
          startPlaybackProgressLoop(messageId, fallbackDurationMs)
        }
        return
      } catch (_: Exception) {
        stopPlayback()
      }
    }

    // Stop any other playing voice message first
    stopPlayback()

    if (!file.exists() || file.length() <= 44L) {
      _errorMessage.value = "Audio file is unavailable or empty."
      return
    }

    try {
      val speed = _playbackState.value.playbackSpeed
      val player = MediaPlayer()
      player.setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .build()
      )
      player.setOnErrorListener { _, _, _ ->
        isVoicePlayerStarted = false
        _errorMessage.value = "Could not play audio recording."
        stopPlayback()
        true
      }
      java.io.FileInputStream(file).use { fis ->
        player.setDataSource(fis.fd)
      }
      player.prepare()
      val resolvedDuration = player.duration.toLong().takeIf { it > 0L } ?: fallbackDurationMs
      player.setOnCompletionListener {
        isVoicePlayerStarted = false
        playbackProgressJob?.cancel()
        _playbackState.value = _playbackState.value.copy(
          isPlaying = false,
          currentPositionMs = 0L,
          durationMs = resolvedDuration
        )
      }
      player.start()
      isVoicePlayerStarted = true
      if (abs(speed - 1.0f) > 0.02f) {
        applyPlaybackSpeed(player, speed)
      }
      mediaPlayer = player
      _playbackState.value = VoicePlaybackState(
        activeMessageId = messageId,
        isPlaying = true,
        currentPositionMs = 0L,
        durationMs = resolvedDuration,
        playbackSpeed = speed
      )
      startPlaybackProgressLoop(messageId, resolvedDuration)
    } catch (e: Exception) {
      Log.e(TAG, "Voice playback failed: ${e.message}", e)
      _errorMessage.value = "Could not play audio recording."
      stopPlayback()
    }
  }

  fun seekToFraction(messageId: String, fraction: Float) {
    val current = _playbackState.value
    if (current.activeMessageId != messageId) return
    val player = mediaPlayer ?: return
    try {
      val total = current.durationMs.takeIf { it > 0L } ?: player.duration.toLong()
      if (total > 0L) {
        val targetMs = (total * fraction.coerceIn(0f, 1f)).toInt()
        player.seekTo(targetMs)
        _playbackState.value = current.copy(currentPositionMs = targetMs.toLong())
      }
    } catch (_: Exception) {}
  }

  fun cyclePlaybackSpeed(): Float {
    val currentSpeed = _playbackState.value.playbackSpeed
    val nextSpeed = when {
      abs(currentSpeed - 1.0f) < 0.05f -> 1.5f
      abs(currentSpeed - 1.5f) < 0.05f -> 2.0f
      else -> 1.0f
    }
    _playbackState.value = _playbackState.value.copy(playbackSpeed = nextSpeed)
    mediaPlayer?.let { player ->
      if (isVoicePlayerStarted && _playbackState.value.isPlaying) {
        applyPlaybackSpeed(player, nextSpeed)
      }
    }
    return nextSpeed
  }

  private fun applyPlaybackSpeed(player: MediaPlayer, speed: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      try {
        player.playbackParams = PlaybackParams().setSpeed(speed.coerceIn(0.75f, 2.0f))
      } catch (_: Exception) {}
    }
  }

  private fun startPlaybackProgressLoop(messageId: String, fallbackDurationMs: Long) {
    playbackProgressJob?.cancel()
    playbackProgressJob = scope.launch {
      delay(60L)
      while (isActive) {
        val player = mediaPlayer ?: break
        if (!isVoicePlayerStarted || !_playbackState.value.isPlaying) break
        val pos = try {
          player.currentPosition.toLong().coerceAtLeast(0L)
        } catch (_: Exception) {
          0L
        }
        val dur = try {
          player.duration.toLong().takeIf { it > 0L } ?: fallbackDurationMs
        } catch (_: Exception) {
          fallbackDurationMs
        }
        _playbackState.value = _playbackState.value.copy(
          activeMessageId = messageId,
          isPlaying = true,
          currentPositionMs = pos.coerceAtMost(dur.coerceAtLeast(pos)),
          durationMs = dur
        )
        delay(60L)
      }
    }
  }

  private fun resolveCachedAudioFile(
    messageId: String,
    audioBase64: String,
    mimeType: String
  ): File? {
    return try {
      val ext = if (mimeType.contains("wav", ignoreCase = true)) "wav" else "m4a"
      val safeId = messageId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
      val cacheDir = File(context.cacheDir, "voice_messages").apply { mkdirs() }
      val targetFile = File(cacheDir, "msg_${safeId}.$ext")
      if (!targetFile.exists() || targetFile.length() < 128L) {
        val decoded = Base64.decode(audioBase64, Base64.DEFAULT)
        FileOutputStream(targetFile).use { it.write(decoded) }
      }
      targetFile
    } catch (e: Exception) {
      Log.e(TAG, "Failed to decode cached voice message: ${e.message}", e)
      null
    }
  }

  fun stopPlayback() {
    isVoicePlayerStarted = false
    playbackProgressJob?.cancel()
    playbackProgressJob = null
    val player = mediaPlayer
    mediaPlayer = null
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
    _playbackState.value = _playbackState.value.copy(
      activeMessageId = null,
      isPlaying = false,
      currentPositionMs = 0L
    )
  }

  fun release() {
    cancelRecording()
    stopPlayback()
    scope.cancel()
  }
}
