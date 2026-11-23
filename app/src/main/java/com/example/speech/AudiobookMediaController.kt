package com.example.speech

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ai.AudiobookLearningService
import com.example.data.model.AudiobookBook
import com.example.data.model.TutorVoice
import com.example.data.model.UserAudiobookProgress
import com.example.data.model.VerifiedTrackAnnouncementBoundaries
import com.example.data.repository.GutendexAndLibriVoxService
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Stateful audiobook playback engine supporting:
 * 1. Real MP3 streaming for LibriVox human-narrated chapters
 * 2. Chunked long-form TTS synthesis for complete Gutenberg chapters & educational summaries
 * 3. Background playback, Lock-Screen / Notification media controls, Sleep Timer, and Mini Player state
 */
object AudiobookMediaController {

  private const val CHANNEL_ID = "lingosphere_audiobook_playback_channel"
  private const val NOTIFICATION_ID = 44091
  private const val ACTION_PLAY_PAUSE = "com.example.audiobook.ACTION_PLAY_PAUSE"
  private const val ACTION_REWIND_10 = "com.example.audiobook.ACTION_REWIND_10"
  private const val ACTION_FORWARD_10 = "com.example.audiobook.ACTION_FORWARD_10"
  private const val ACTION_NEXT_CHAPTER = "com.example.audiobook.ACTION_NEXT_CHAPTER"

  private const val AUDIOBOOK_AUDIO_CACHE_DIR = "audiobook_audio_cache"

  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var mediaPlayer: MediaPlayer? = null
  private var mp3DownloadJob: Job? = null
  private var progressPollerJob: Job? = null
  private var sleepTimerJob: Job? = null
  private var isReceiverRegistered = false

  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .followRedirects(true)
      .followSslRedirects(true)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(45, TimeUnit.SECONDS)
      .callTimeout(60, TimeUnit.SECONDS)
      .build()
  }

  private var appContext: Context? = null
  private var boundSpeechManager: SpeechManager? = null
  private var currentUserId: String = "default_user"

  private val _activeBook = MutableStateFlow<AudiobookBook?>(null)
  val activeBook = _activeBook.asStateFlow()

  private val _activeChapterIndex = MutableStateFlow(0)
  val activeChapterIndex = _activeChapterIndex.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying = _isPlaying.asStateFlow()

  private val _isBuffering = MutableStateFlow(false)
  val isBuffering = _isBuffering.asStateFlow()

  private val _currentSentenceIndex = MutableStateFlow(0)
  val currentSentenceIndex = _currentSentenceIndex.asStateFlow()

  private val _currentPositionMs = MutableStateFlow(0L)
  val currentPositionMs = _currentPositionMs.asStateFlow()

  private val _durationMs = MutableStateFlow(0L)
  val durationMs = _durationMs.asStateFlow()

  private val _playbackSpeed = MutableStateFlow(1.0f)
  val playbackSpeed = _playbackSpeed.asStateFlow()

  private val _selectedVoice = MutableStateFlow(TutorVoice.AOEDE)
  val selectedVoice = _selectedVoice.asStateFlow()

  private val _useHumanNarrator = MutableStateFlow(false)
  val useHumanNarrator = _useHumanNarrator.asStateFlow()

  private val _selectedEditionIndex = MutableStateFlow(0)
  val selectedEditionIndex = _selectedEditionIndex.asStateFlow()

  private val _sleepTimerMinutes = MutableStateFlow(0)
  val sleepTimerMinutes = _sleepTimerMinutes.asStateFlow()

  private val _autoPlayNextChapter = MutableStateFlow(true)
  val autoPlayNextChapter = _autoPlayNextChapter.asStateFlow()

  private val _skipRecordingAnnouncements = MutableStateFlow(true)
  val skipRecordingAnnouncements = _skipRecordingAnnouncements.asStateFlow()

  private val _activeTrackBoundaries = MutableStateFlow<VerifiedTrackAnnouncementBoundaries?>(null)
  val activeTrackBoundaries = _activeTrackBoundaries.asStateFlow()

  private val _introAnnouncementSkippedForChapter = MutableStateFlow(false)
  val introAnnouncementSkippedForChapter = _introAnnouncementSkippedForChapter.asStateFlow()

  private val _humanNarrationStatusMessage = MutableStateFlow<String?>(null)
  val humanNarrationStatusMessage = _humanNarrationStatusMessage.asStateFlow()

  // Shared player state across Listen and Read & Listen modes
  private val _selectedBookId = MutableStateFlow("")
  val selectedBookId = _selectedBookId.asStateFlow()

  private val _selectedChapterId = MutableStateFlow("")
  val selectedChapterId = _selectedChapterId.asStateFlow()

  private val _selectedChapterNumber = MutableStateFlow(1)
  val selectedChapterNumber = _selectedChapterNumber.asStateFlow()

  private val _selectedNarratorType = MutableStateFlow("ai_voice")
  val selectedNarratorType = _selectedNarratorType.asStateFlow()

  private val _currentAudioUrl = MutableStateFlow<String?>(null)
  val currentAudioUrl = _currentAudioUrl.asStateFlow()

  @Volatile
  private var playbackGenerationId: Long = 0L
  @Volatile
  private var isMpPrepared: Boolean = false
  @Volatile
  private var isMpStarted: Boolean = false
  private var currentStreamMp3Url: String? = null
  private var currentStreamBookId: String? = null
  private var currentStreamChapterIndex: Int = -1

  private fun syncSharedSelectionState() {
    val book = _activeBook.value
    val chIdx = _activeChapterIndex.value
    if (book == null) {
      _selectedBookId.value = ""
      _selectedChapterId.value = ""
      _selectedChapterNumber.value = 1
      _selectedNarratorType.value = if (_useHumanNarrator.value) "human_librivox" else "ai_voice"
      _currentAudioUrl.value = null
      return
    }
    val parsedCh = book.parsedChapters.getOrNull(chIdx)
    val chNum = parsedCh?.chapterNumber?.takeIf { it > 0 } ?: (chIdx + 1)
    val chId = parsedCh?.let { "${book.id}_ch_${it.chapterNumber}_idx_${it.chapterIndex}" }
      ?: "${book.id}_ch_$chNum"
    val mp3Url = getChapterMp3Url(book, chIdx)

    _selectedBookId.value = book.id
    _selectedChapterId.value = chId
    _selectedChapterNumber.value = chNum
    _selectedNarratorType.value = if (_useHumanNarrator.value) "human_librivox" else "ai_voice"
    _currentAudioUrl.value = if (_useHumanNarrator.value) mp3Url else null
    _activeTrackBoundaries.value = if (_useHumanNarrator.value && !mp3Url.isNullOrBlank()) {
      LibriVoxAnnouncementDetector.resolveBoundariesForChapter(
        book = book,
        chapterIndex = chIdx,
        trackUrlOverride = mp3Url,
        knownDurationMsOverride = _durationMs.value,
        context = appContext
      )
    } else {
      null
    }
  }

  /**
   * Computes the exact starting playback position (in ms) for [chapterIndex] of [book].
   * - If [savedPosMs] represents mid-chapter user progress past the intro, resumes at [savedPosMs].
   * - Otherwise, if Human Narrator is active, "Skip Recording Announcements" is enabled, and the track
   *   has verified announcement boundaries, starts directly at `boundaries.contentStartMs`
   *   (right after the LibriVox disclaimer and BEFORE the chapter title & story content).
   * - If boundaries are unavailable/unverified or skipping is disabled, starts at `0L` (never guesses).
   */
  fun resolveEffectiveStartPositionMs(
    book: AudiobookBook,
    chapterIndex: Int,
    savedPosMs: Long = 0L
  ): Long {
    val mp3Url = getChapterMp3Url(book, chapterIndex)
    if (!_useHumanNarrator.value || mp3Url.isNullOrBlank()) {
      _activeTrackBoundaries.value = null
      _introAnnouncementSkippedForChapter.value = false
      return if (savedPosMs > 1500L) savedPosMs else 0L
    }

    val boundaries = LibriVoxAnnouncementDetector.resolveBoundariesForChapter(
      book = book,
      chapterIndex = chapterIndex,
      trackUrlOverride = mp3Url,
      knownDurationMsOverride = _durationMs.value,
      context = appContext
    )
    _activeTrackBoundaries.value = boundaries

    if (_skipRecordingAnnouncements.value && boundaries.isVerified && boundaries.contentStartMs > 0L) {
      _introAnnouncementSkippedForChapter.value = true
      return if (savedPosMs > boundaries.contentStartMs + 1000L) {
        savedPosMs
      } else {
        boundaries.contentStartMs
      }
    }

    _introAnnouncementSkippedForChapter.value = false
    return if (savedPosMs > 1500L) savedPosMs else 0L
  }

  private val mediaActionReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      when (intent?.action) {
        ACTION_PLAY_PAUSE -> togglePlayPause()
        ACTION_REWIND_10 -> skipSeconds(forward = false)
        ACTION_FORWARD_10 -> skipSeconds(forward = true)
        ACTION_NEXT_CHAPTER -> nextChapter()
      }
    }
  }

  fun attach(context: Context, speechManager: SpeechManager, userId: String) {
    appContext = context.applicationContext
    boundSpeechManager = speechManager
    currentUserId = userId
    _selectedVoice.value = speechManager.audiobookVoice.value
    _skipRecordingAnnouncements.value = AudiobookLearningService.isSkipRecordingAnnouncementsEnabled(
      context.applicationContext,
      userId
    )
    ensureNotificationChannelAndReceiver(context.applicationContext)
  }

  private fun ensureNotificationChannelAndReceiver(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Audiobook Playback Controls",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Media playback controls for LingoSphere Audiobooks"
        setShowBadge(false)
      }
      mgr?.createNotificationChannel(channel)
    }

    if (!isReceiverRegistered) {
      try {
        val filter = IntentFilter().apply {
          addAction(ACTION_PLAY_PAUSE)
          addAction(ACTION_REWIND_10)
          addAction(ACTION_FORWARD_10)
          addAction(ACTION_NEXT_CHAPTER)
        }
        ContextCompat.registerReceiver(
          context,
          mediaActionReceiver,
          filter,
          ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isReceiverRegistered = true
      } catch (_: Exception) {}
    }
  }

  /**
   * Immediately stops any previous book's audio and resets chapter/playback state when a user
   * opens a different book. Never resets the chapter if the same book is already active unless
   * an explicit new initialChapterIndex is requested.
   */
  fun onBookOpened(book: AudiobookBook, initialChapterIndex: Int? = null) {
    val prevBookId = _activeBook.value?.id
    if (prevBookId != null && prevBookId != book.id) {
      stopInternalPlayback()
      _currentPositionMs.value = 0L
      _durationMs.value = 0L
      _currentSentenceIndex.value = 0
      _humanNarrationStatusMessage.value = null
      val totalCh = getTotalChaptersCount(book)
      val targetCh = (initialChapterIndex ?: 0).coerceIn(0, (totalCh - 1).coerceAtLeast(0))
      _activeBook.value = book
      _activeChapterIndex.value = targetCh
      val hasMp3 = getChapterMp3Url(book, targetCh) != null
      _useHumanNarrator.value = hasMp3
      syncSharedSelectionState()
      cancelMediaNotification()
    } else if (prevBookId == null) {
      val totalCh = getTotalChaptersCount(book)
      val targetCh = (initialChapterIndex ?: 0).coerceIn(0, (totalCh - 1).coerceAtLeast(0))
      _activeBook.value = book
      _activeChapterIndex.value = targetCh
      _humanNarrationStatusMessage.value = null
      syncSharedSelectionState()
    } else {
      _activeBook.value = book
      if (initialChapterIndex != null && initialChapterIndex != _activeChapterIndex.value) {
        stopInternalPlayback()
        val totalCh = getTotalChaptersCount(book)
        _activeChapterIndex.value = initialChapterIndex.coerceIn(0, (totalCh - 1).coerceAtLeast(0))
        _currentSentenceIndex.value = 0
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        _humanNarrationStatusMessage.value = null
      }
      syncSharedSelectionState()
    }
  }

  /**
   * Updates the active book's enriched chapters/audio metadata if and only if the active book ID matches,
   * preserving the user's currently selected chapter and narrator mode.
   */
  fun updateActiveBookMetadata(book: AudiobookBook) {
    if (_activeBook.value?.id == book.id) {
      val currentCh = _activeChapterIndex.value
      val totalCh = getTotalChaptersCount(book)
      val safeCh = currentCh.coerceIn(0, (totalCh - 1).coerceAtLeast(0))
      _activeBook.value = book
      if (safeCh != currentCh) {
        _activeChapterIndex.value = safeCh
      }
      syncSharedSelectionState()
    }
  }

  fun loadBookAndChapter(
    book: AudiobookBook,
    chapterIndex: Int,
    sentenceIndex: Int = 0,
    autoPlay: Boolean = false,
    preferHumanAudio: Boolean? = null
  ) {
    val prevBookId = _activeBook.value?.id
    val prevChapter = _activeChapterIndex.value

    val totalCh = getTotalChaptersCount(book)
    val clampedCh = chapterIndex.coerceIn(0, (totalCh - 1).coerceAtLeast(0))

    val isSwitchingBookOrChapter = prevBookId != book.id ||
      prevChapter != clampedCh ||
      (currentStreamBookId != null && currentStreamBookId != book.id) ||
      (currentStreamChapterIndex != -1 && currentStreamChapterIndex != clampedCh)

    if (isSwitchingBookOrChapter) {
      stopInternalPlayback()
    }

    _activeBook.value = book
    _activeChapterIndex.value = clampedCh
    _humanNarrationStatusMessage.value = null

    val savedProg = appContext?.let { AudiobookLearningService.loadUserProgress(it, currentUserId, book.id) }
    if (savedProg != null && prevBookId != book.id) {
      _autoPlayNextChapter.value = savedProg.autoPlayNextChapter
      _skipRecordingAnnouncements.value = savedProg.skipRecordingAnnouncements
    }

    val mp3Url = getChapterMp3Url(book, clampedCh)
    val hasMp3 = !mp3Url.isNullOrBlank()
    if (preferHumanAudio != null) {
      if (preferHumanAudio && !hasMp3) {
        _useHumanNarrator.value = false
        _humanNarrationStatusMessage.value = "Human narration unavailable"
      } else {
        _useHumanNarrator.value = preferHumanAudio && hasMp3
      }
    } else if (prevBookId != book.id) {
      // Default to LibriVox human narration whenever available unless the user explicitly selected AI TTS
      _useHumanNarrator.value = hasMp3 && savedProg?.preferredEditionId != "tts"
    } else if (_useHumanNarrator.value && !hasMp3) {
      _useHumanNarrator.value = false
      _humanNarrationStatusMessage.value = "Human narration unavailable"
    }

    if (isSwitchingBookOrChapter) {
      val savedChSentence = if (sentenceIndex > 0) {
        sentenceIndex
      } else {
        savedProg?.chapterSentenceIndexMap?.get(clampedCh)
          ?: if (savedProg != null && savedProg.lastEpisodeIndex == clampedCh) savedProg.lastSentenceIndex else 0
      }
      _currentSentenceIndex.value = savedChSentence.coerceAtLeast(0)

      val savedChPosMs = savedProg?.chapterAudioPositionMsMap?.get(clampedCh)
        ?: if (savedProg != null && savedProg.lastEpisodeIndex == clampedCh) savedProg.lastPlaybackPositionMs else 0L
      val rawSavedMs = if (sentenceIndex == 0 && savedChPosMs > 1500L) savedChPosMs else 0L
      _durationMs.value = 0L
      _currentPositionMs.value = resolveEffectiveStartPositionMs(book, clampedCh, rawSavedMs)
    } else if (sentenceIndex > 0) {
      _currentSentenceIndex.value = sentenceIndex.coerceAtLeast(0)
    } else if (_useHumanNarrator.value) {
      _currentPositionMs.value = resolveEffectiveStartPositionMs(book, clampedCh, _currentPositionMs.value)
    }

    syncSharedSelectionState()
    persistCurrentProgress()

    if (autoPlay) {
      startPlayback()
    } else {
      updateMediaNotification()
    }
  }

  fun setAutoPlayNextChapter(enabled: Boolean) {
    _autoPlayNextChapter.value = enabled
    persistCurrentProgress()
  }

  /**
   * Enables or disables skipping verified LibriVox recording announcements at the beginning and end of tracks.
   * Enabled by default and persisted across listening sessions.
   */
  fun setSkipRecordingAnnouncements(enabled: Boolean) {
    val prev = _skipRecordingAnnouncements.value
    _skipRecordingAnnouncements.value = enabled
    appContext?.let { ctx ->
      AudiobookLearningService.setSkipRecordingAnnouncementsEnabled(ctx, currentUserId, enabled)
    }

    val book = _activeBook.value
    val chIdx = _activeChapterIndex.value
    if (book != null && _useHumanNarrator.value) {
      val bounds = LibriVoxAnnouncementDetector.resolveBoundariesForChapter(
        book = book,
        chapterIndex = chIdx,
        trackUrlOverride = getChapterMp3Url(book, chIdx),
        knownDurationMsOverride = _durationMs.value,
        context = appContext
      )
      _activeTrackBoundaries.value = bounds

      if (enabled && bounds.isVerified && bounds.contentStartMs > 0L) {
        _introAnnouncementSkippedForChapter.value = true
        if (_currentPositionMs.value < bounds.contentStartMs) {
          _currentPositionMs.value = bounds.contentStartMs
          if (isMpPrepared) {
            try {
              mediaPlayer?.seekTo(bounds.contentStartMs.toInt())
            } catch (_: Exception) {}
          }
        }
      } else if (!enabled && prev) {
        _introAnnouncementSkippedForChapter.value = false
        if (!_isPlaying.value && bounds.isVerified && _currentPositionMs.value == bounds.contentStartMs) {
          _currentPositionMs.value = 0L
          if (isMpPrepared) {
            try {
              mediaPlayer?.seekTo(0)
            } catch (_: Exception) {}
          }
        }
      }
    } else {
      _introAnnouncementSkippedForChapter.value = false
    }

    persistCurrentProgress()
  }

  /**
   * Switches between Human Narrator (LibriVox) and AI Voice while keeping the currently selected
   * book and currently selected chapter unchanged. Never resets to Chapter 1.
   */
  fun setNarrationEditionMode(
    useHumanLibriVox: Boolean,
    editionIndex: Int = 0,
    autoStartPlayback: Boolean = _isPlaying.value
  ) {
    val book = _activeBook.value ?: return
    val currentCh = _activeChapterIndex.value
    stopInternalPlayback()
    _selectedEditionIndex.value = editionIndex

    val savedProg = appContext?.let { AudiobookLearningService.loadUserProgress(it, currentUserId, book.id) }

    if (useHumanLibriVox) {
      val mp3Url = getChapterMp3Url(book, currentCh)
      if (mp3Url.isNullOrBlank()) {
        // Never substitute Chapter 1 or another book's recording when human narration is unavailable
        _useHumanNarrator.value = false
        _humanNarrationStatusMessage.value = "Human narration unavailable"
        _activeTrackBoundaries.value = null
        _introAnnouncementSkippedForChapter.value = false
        syncSharedSelectionState()
        updateMediaNotification()
        return
      } else {
        _humanNarrationStatusMessage.value = null
        _useHumanNarrator.value = true
        val savedPos = savedProg?.chapterAudioPositionMsMap?.get(currentCh) ?: 0L
        _durationMs.value = 0L
        _currentPositionMs.value = resolveEffectiveStartPositionMs(book, currentCh, savedPos)
      }
    } else {
      _humanNarrationStatusMessage.value = null
      _useHumanNarrator.value = false
      _activeTrackBoundaries.value = null
      _introAnnouncementSkippedForChapter.value = false
      val savedSent = savedProg?.chapterSentenceIndexMap?.get(currentCh) ?: _currentSentenceIndex.value
      _currentSentenceIndex.value = savedSent.coerceAtLeast(0)
      _currentPositionMs.value = 0L
      _durationMs.value = 0L
    }

    syncSharedSelectionState()
    persistCurrentProgress()
    if (autoStartPlayback) {
      startPlayback()
    } else {
      updateMediaNotification()
    }
  }

  fun setTutorVoice(voice: TutorVoice) {
    val changed = _selectedVoice.value != voice
    _selectedVoice.value = voice
    boundSpeechManager?.setAudiobookVoice(voice)
    if (changed && _isPlaying.value && !_useHumanNarrator.value) {
      boundSpeechManager?.stopSpeaking()
      playCurrentTtsChunk(_currentSentenceIndex.value)
    }
  }

  fun setPlaybackSpeed(speed: Float) {
    val clamped = speed.coerceIn(0.75f, 2.0f)
    _playbackSpeed.value = clamped
    boundSpeechManager?.setSpeechRate(clamped)

    mediaPlayer?.let { mp ->
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isMpPrepared && isMpStarted) {
        try {
          mp.playbackParams = PlaybackParams().setSpeed(clamped)
        } catch (_: Exception) {}
      }
    }

    if (_isPlaying.value && !_useHumanNarrator.value) {
      playCurrentTtsChunk(_currentSentenceIndex.value)
    }
  }

  fun setSleepTimer(minutes: Int) {
    sleepTimerJob?.cancel()
    _sleepTimerMinutes.value = minutes.coerceAtLeast(0)
    if (minutes > 0) {
      sleepTimerJob = scope.launch {
        while (isActive && _sleepTimerMinutes.value > 0) {
          delay(60_000L)
          val next = _sleepTimerMinutes.value - 1
          _sleepTimerMinutes.value = next
          if (next <= 0) {
            pausePlayback()
          }
        }
      }
    }
  }

  fun togglePlayPause() {
    if (_isPlaying.value) {
      pausePlayback()
    } else {
      startPlayback()
    }
  }

  fun startPlayback() {
    val book = _activeBook.value ?: return
    val chIdx = _activeChapterIndex.value
    val mp3Url = getChapterMp3Url(book, chIdx)
    syncSharedSelectionState()

    if (_useHumanNarrator.value) {
      if (!mp3Url.isNullOrBlank()) {
        _humanNarrationStatusMessage.value = null
        startMp3Streaming(mp3Url)
      } else {
        // Human narrator selected, but no verified recording exists for this book/chapter
        stopInternalPlayback()
        _useHumanNarrator.value = false
        _humanNarrationStatusMessage.value = "Human narration unavailable"
        syncSharedSelectionState()
        updateMediaNotification()
      }
    } else {
      _humanNarrationStatusMessage.value = null
      playCurrentTtsChunk(_currentSentenceIndex.value)
    }
  }

  fun pausePlayback() {
    playbackGenerationId++
    _isPlaying.value = false
    _isBuffering.value = false
    mp3DownloadJob?.cancel()
    mp3DownloadJob = null
    progressPollerJob?.cancel()
    try {
      if (isMpPrepared && isMpStarted) {
        isMpStarted = false
        mediaPlayer?.pause()
      }
    } catch (_: Exception) {}
    boundSpeechManager?.stopSpeaking()
    persistCurrentProgress()
    updateMediaNotification()
  }

  fun stopAndClearMiniPlayer() {
    stopInternalPlayback()
    _activeBook.value = null
    _activeChapterIndex.value = 0
    _currentSentenceIndex.value = 0
    _currentPositionMs.value = 0L
    _durationMs.value = 0L
    _humanNarrationStatusMessage.value = null
    syncSharedSelectionState()
    cancelMediaNotification()
  }

  private fun stopInternalPlayback() {
    playbackGenerationId++
    isMpPrepared = false
    isMpStarted = false
    _isPlaying.value = false
    _isBuffering.value = false
    mp3DownloadJob?.cancel()
    mp3DownloadJob = null
    progressPollerJob?.cancel()
    currentStreamMp3Url = null
    currentStreamBookId = null
    currentStreamChapterIndex = -1
    val mp = mediaPlayer
    mediaPlayer = null
    if (mp != null) {
      try {
        mp.setOnPreparedListener(null)
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
    boundSpeechManager?.stopSpeaking()
  }

  fun skipSeconds(forward: Boolean) {
    val book = _activeBook.value ?: return
    if (_useHumanNarrator.value && mediaPlayer != null && isMpPrepared) {
      try {
        val mp = mediaPlayer ?: return
        val delta = if (forward) 10_000 else -10_000
        val target = (mp.currentPosition + delta).coerceIn(0, mp.duration.coerceAtLeast(1))
        mp.seekTo(target)
        _currentPositionMs.value = target.toLong()
        persistCurrentProgress()
      } catch (_: Exception) {}
    } else {
      val sentences = getChapterSentences(book, _activeChapterIndex.value)
      if (sentences.isEmpty()) return
      val step = if (forward) 1 else -1
      val nextIdx = (_currentSentenceIndex.value + step).coerceIn(0, sentences.size - 1)
      _currentSentenceIndex.value = nextIdx
      persistCurrentProgress()
      if (_isPlaying.value) {
        playCurrentTtsChunk(nextIdx)
      }
    }
  }

  fun seekToFraction(fraction: Float) {
    val book = _activeBook.value ?: return
    val clamped = fraction.coerceIn(0f, 1f)
    if (_useHumanNarrator.value && mediaPlayer != null && isMpPrepared) {
      try {
        val mp = mediaPlayer ?: return
        val target = (mp.duration * clamped).toInt().coerceIn(0, mp.duration.coerceAtLeast(1))
        mp.seekTo(target)
        _currentPositionMs.value = target.toLong()
        persistCurrentProgress()
      } catch (_: Exception) {}
    } else {
      val sentences = getChapterSentences(book, _activeChapterIndex.value)
      if (sentences.isEmpty()) return
      val targetIdx = ((sentences.size - 1) * clamped).toInt().coerceIn(0, sentences.size - 1)
      _currentSentenceIndex.value = targetIdx
      persistCurrentProgress()
      if (_isPlaying.value) {
        playCurrentTtsChunk(targetIdx)
      }
    }
  }

  fun seekToPositionMs(positionMs: Long) {
    val safeMs = positionMs.coerceAtLeast(0L)
    _currentPositionMs.value = safeMs
    if (_useHumanNarrator.value && mediaPlayer != null && isMpPrepared) {
      try {
        val mp = mediaPlayer ?: return
        val maxDur = mp.duration.takeIf { it > 0 } ?: Int.MAX_VALUE
        val target = safeMs.coerceAtMost(maxDur.toLong()).toInt()
        mp.seekTo(target)
        _currentPositionMs.value = target.toLong()
      } catch (_: Exception) {}
    }
    persistCurrentProgress()
  }

  fun jumpToSentence(sentenceIndex: Int, playImmediately: Boolean = true) {
    val book = _activeBook.value ?: return
    val sentences = getChapterSentences(book, _activeChapterIndex.value)
    if (sentences.isEmpty()) return
    val clamped = sentenceIndex.coerceIn(0, sentences.size - 1)
    _currentSentenceIndex.value = clamped
    if (_useHumanNarrator.value && playImmediately) {
      // Switch to TTS mode when tapping a specific sentence for immediate sentence-synced playback
      stopInternalPlayback()
      _useHumanNarrator.value = false
      _humanNarrationStatusMessage.value = null
      syncSharedSelectionState()
    }
    persistCurrentProgress()
    if (playImmediately) {
      playCurrentTtsChunk(clamped)
    }
  }

  fun nextChapter() {
    val book = _activeBook.value ?: return
    val total = getTotalChaptersCount(book)
    if (_activeChapterIndex.value + 1 < total) {
      val wasPlaying = _isPlaying.value
      stopInternalPlayback()
      val nextCh = _activeChapterIndex.value + 1
      _activeChapterIndex.value = nextCh
      val savedProg = appContext?.let { AudiobookLearningService.loadUserProgress(it, currentUserId, book.id) }
      _currentSentenceIndex.value = (savedProg?.chapterSentenceIndexMap?.get(nextCh) ?: 0).coerceAtLeast(0)
      val savedPos = savedProg?.chapterAudioPositionMsMap?.get(nextCh) ?: 0L
      _durationMs.value = 0L
      _humanNarrationStatusMessage.value = null
      if (_useHumanNarrator.value && getChapterMp3Url(book, nextCh) == null) {
        _useHumanNarrator.value = false
        _humanNarrationStatusMessage.value = "Human narration unavailable"
      }
      _currentPositionMs.value = resolveEffectiveStartPositionMs(book, nextCh, savedPos)
      syncSharedSelectionState()
      persistCurrentProgress()
      if (wasPlaying) startPlayback() else updateMediaNotification()
    }
  }

  fun previousChapter() {
    val book = _activeBook.value ?: return
    if (_activeChapterIndex.value > 0) {
      val wasPlaying = _isPlaying.value
      stopInternalPlayback()
      val prevCh = _activeChapterIndex.value - 1
      _activeChapterIndex.value = prevCh
      val savedProg = appContext?.let { AudiobookLearningService.loadUserProgress(it, currentUserId, book.id) }
      _currentSentenceIndex.value = (savedProg?.chapterSentenceIndexMap?.get(prevCh) ?: 0).coerceAtLeast(0)
      val savedPos = savedProg?.chapterAudioPositionMsMap?.get(prevCh) ?: 0L
      _durationMs.value = 0L
      _humanNarrationStatusMessage.value = null
      if (_useHumanNarrator.value && getChapterMp3Url(book, prevCh) == null) {
        _useHumanNarrator.value = false
        _humanNarrationStatusMessage.value = "Human narration unavailable"
      }
      _currentPositionMs.value = resolveEffectiveStartPositionMs(book, prevCh, savedPos)
      syncSharedSelectionState()
      persistCurrentProgress()
      if (wasPlaying) startPlayback() else updateMediaNotification()
    }
  }

  /**
   * Advances sequentially from the completed chapter to the next chapter (e.g., Chapter 2 -> 3 -> 4)
   * when autoplay is enabled, stopping cleanly after the final chapter without ever restarting Chapter 1.
   */
  fun advanceToNextChapterOnCompletion(expectedBookId: String? = _activeBook.value?.id): Boolean {
    val book = _activeBook.value ?: return false
    if (expectedBookId != null && book.id != expectedBookId) return false
    val completedCh = _activeChapterIndex.value
    val totalCh = getTotalChaptersCount(book)

    if (completedCh + 1 < totalCh) {
      persistCurrentProgress(markChapterDone = true)
      if (_autoPlayNextChapter.value) {
        stopInternalPlayback()
        val nextCh = completedCh + 1
        _activeChapterIndex.value = nextCh
        _currentSentenceIndex.value = 0
        _durationMs.value = 0L
        _humanNarrationStatusMessage.value = null
        if (_useHumanNarrator.value && getChapterMp3Url(book, nextCh) == null) {
          _useHumanNarrator.value = false
          _humanNarrationStatusMessage.value = "Human narration unavailable"
        }
        _currentPositionMs.value = resolveEffectiveStartPositionMs(book, nextCh, 0L)
        syncSharedSelectionState()
        persistCurrentProgress()
        startPlayback()
        return true
      } else {
        _isPlaying.value = false
        syncSharedSelectionState()
        updateMediaNotification()
        return false
      }
    } else {
      // Final chapter completed: stop cleanly and never return to Chapter 1
      _isPlaying.value = false
      _currentSentenceIndex.value = 0
      persistCurrentProgress(markCompleted = true, markChapterDone = true)
      syncSharedSelectionState()
      updateMediaNotification()
      return false
    }
  }

  // =========================================================================
  // INTERNAL: LIBRIVOX HUMAN MP3 STREAMING
  // =========================================================================

  private fun startMp3Streaming(mp3Url: String) {
    val expectedBook = _activeBook.value ?: return
    val expectedBookId = expectedBook.id
    val expectedChapterIndex = _activeChapterIndex.value

    val initialBounds = LibriVoxAnnouncementDetector.resolveBoundariesForChapter(
      book = expectedBook,
      chapterIndex = expectedChapterIndex,
      trackUrlOverride = mp3Url,
      knownDurationMsOverride = _durationMs.value,
      context = appContext
    )
    _activeTrackBoundaries.value = initialBounds
    if (_skipRecordingAnnouncements.value && initialBounds.isVerified && initialBounds.contentStartMs > 0L) {
      _introAnnouncementSkippedForChapter.value = true
      if (_currentPositionMs.value < initialBounds.contentStartMs) {
        _currentPositionMs.value = initialBounds.contentStartMs
      }
    } else {
      _introAnnouncementSkippedForChapter.value = false
    }

    val existing = mediaPlayer
    if (
      existing != null &&
      isMpPrepared &&
      currentStreamMp3Url == mp3Url &&
      currentStreamBookId == expectedBookId &&
      currentStreamChapterIndex == expectedChapterIndex
    ) {
      try {
        if (_skipRecordingAnnouncements.value && initialBounds.isVerified && existing.currentPosition < initialBounds.contentStartMs) {
          existing.seekTo(initialBounds.contentStartMs.toInt())
          _currentPositionMs.value = initialBounds.contentStartMs
        }
        existing.start()
        isMpStarted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && kotlin.math.abs(_playbackSpeed.value - 1.0f) > 0.01f) {
          try {
            existing.playbackParams = PlaybackParams().setSpeed(_playbackSpeed.value)
          } catch (_: Exception) {}
        }
        _isPlaying.value = true
        _isBuffering.value = false
        startMp3Poller()
        updateMediaNotification()
        return
      } catch (_: Exception) {
        stopInternalPlayback()
      }
    } else if (existing != null) {
      // Different book, chapter, or MP3 URL: release previous MediaPlayer immediately
      stopInternalPlayback()
    }

    val genId = ++playbackGenerationId
    currentStreamMp3Url = mp3Url
    currentStreamBookId = expectedBookId
    currentStreamChapterIndex = expectedChapterIndex
    _currentAudioUrl.value = mp3Url
    _isBuffering.value = true
    _isPlaying.value = true
    updateMediaNotification()

    // If boundaries are not yet verified in the local catalog, launch non-blocking acoustic frame inspection
    if (!initialBounds.isVerified) {
      val chNum = expectedBook.parsedChapters.getOrNull(expectedChapterIndex)?.chapterNumber ?: (expectedChapterIndex + 1)
      val chTitle = getChapterTitle(expectedBook, expectedChapterIndex)
      scope.launch {
        val detected = LibriVoxAnnouncementDetector.detectTrackBoundariesAsync(
          context = appContext,
          trackUrl = mp3Url,
          chapterNumber = chNum,
          totalChaptersInBook = getTotalChaptersCount(expectedBook),
          knownDurationMs = _durationMs.value,
          chapterTitle = chTitle
        )
        if (
          detected.isVerified &&
          genId == playbackGenerationId &&
          _activeBook.value?.id == expectedBookId &&
          _activeChapterIndex.value == expectedChapterIndex
        ) {
          _activeTrackBoundaries.value = detected
          if (_skipRecordingAnnouncements.value && _currentPositionMs.value < detected.contentStartMs) {
            _introAnnouncementSkippedForChapter.value = true
            _currentPositionMs.value = detected.contentStartMs
            if (isMpPrepared) {
              try {
                mediaPlayer?.seekTo(detected.contentStartMs.toInt())
              } catch (_: Exception) {}
            }
          }
        }
      }
    }

    mp3DownloadJob?.cancel()
    mp3DownloadJob = scope.launch {
      val cachedFile = if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
        null
      } else {
        withContext(Dispatchers.IO) {
          downloadOrGetCachedMp3File(mp3Url)
        }
      }

      if (genId != playbackGenerationId || _activeBook.value?.id != expectedBookId || _activeChapterIndex.value != expectedChapterIndex) {
        return@launch
      }

      if (cachedFile == null && !Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
        _isBuffering.value = false
        _isPlaying.value = false
        _humanNarrationStatusMessage.value = "Human narration unavailable"
        stopInternalPlayback()
        syncSharedSelectionState()
        updateMediaNotification()
        return@launch
      }

      try {
        val mp = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          if (cachedFile != null) {
            FileInputStream(cachedFile).use { fis ->
              setDataSource(fis.fd)
            }
          } else {
            setDataSource(GutendexAndLibriVoxService.normalizeSecureBookUrl(mp3Url))
          }
          setOnPreparedListener { player ->
            if (genId != playbackGenerationId || _activeBook.value?.id != expectedBookId || _activeChapterIndex.value != expectedChapterIndex) {
              isMpPrepared = false
              isMpStarted = false
              try { player.reset() } catch (_: Exception) {}
              try { player.release() } catch (_: Exception) {}
              return@setOnPreparedListener
            }
            isMpPrepared = true
            _isBuffering.value = false
            val actualDurMs = player.duration.toLong().coerceAtLeast(1000L)
            _durationMs.value = actualDurMs
            val durSecs = (actualDurMs / 1000L).toInt()
            if (durSecs > 0) {
              _activeBook.value?.let { currBook ->
                val chs = currBook.parsedChapters
                if (expectedChapterIndex in chs.indices) {
                  val targetCh = chs[expectedChapterIndex]
                  if (!targetCh.isAudioDurationVerified || targetCh.librivoxDurationSeconds != durSecs) {
                    val updatedChs = chs.toMutableList()
                    updatedChs[expectedChapterIndex] = targetCh.copy(
                      librivoxDurationSeconds = durSecs,
                      isAudioDurationVerified = true
                    )
                    _activeBook.value = currBook.copy(parsedChapters = updatedChs)
                  }
                }
              }
            }

            val refinedBounds = LibriVoxAnnouncementDetector.resolveBoundariesForChapter(
              book = expectedBook,
              chapterIndex = expectedChapterIndex,
              trackUrlOverride = mp3Url,
              knownDurationMsOverride = actualDurMs,
              context = appContext
            )
            _activeTrackBoundaries.value = refinedBounds

            val desiredStartMs = if (_skipRecordingAnnouncements.value && refinedBounds.isVerified && refinedBounds.contentStartMs > 0L) {
              _introAnnouncementSkippedForChapter.value = true
              maxOf(_currentPositionMs.value, refinedBounds.contentStartMs)
            } else {
              _introAnnouncementSkippedForChapter.value = false
              _currentPositionMs.value
            }

            if (desiredStartMs in 500L until actualDurMs) {
              player.seekTo(desiredStartMs.toInt())
              _currentPositionMs.value = desiredStartMs
            }
            player.start()
            isMpStarted = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && kotlin.math.abs(_playbackSpeed.value - 1.0f) > 0.01f) {
              try {
                player.playbackParams = PlaybackParams().setSpeed(_playbackSpeed.value)
              } catch (_: Exception) {}
            }
            _isPlaying.value = true
            startMp3Poller()
            updateMediaNotification()
          }
          setOnCompletionListener {
            isMpStarted = false
            if (genId != playbackGenerationId || _activeBook.value?.id != expectedBookId) {
              return@setOnCompletionListener
            }
            _isPlaying.value = false
            progressPollerJob?.cancel()
            advanceToNextChapterOnCompletion(expectedBookId)
          }
          setOnErrorListener { failedPlayer, _, _ ->
            isMpPrepared = false
            isMpStarted = false
            try { failedPlayer.reset() } catch (_: Exception) {}
            try { failedPlayer.release() } catch (_: Exception) {}
            if (genId != playbackGenerationId) return@setOnErrorListener true
            _isBuffering.value = false
            _isPlaying.value = false
            _humanNarrationStatusMessage.value = "Human narration unavailable"
            stopInternalPlayback()
            syncSharedSelectionState()
            updateMediaNotification()
            true
          }
          isMpPrepared = false
          isMpStarted = false
          prepareAsync()
        }
        mediaPlayer = mp
      } catch (_: Exception) {
        _isBuffering.value = false
        _isPlaying.value = false
        _humanNarrationStatusMessage.value = "Human narration unavailable"
        stopInternalPlayback()
        syncSharedSelectionState()
        updateMediaNotification()
      }
    }
  }

  private fun downloadOrGetCachedMp3File(mp3Url: String): File? {
    val ctx = appContext ?: return null
    val cacheDir = File(ctx.cacheDir, AUDIOBOOK_AUDIO_CACHE_DIR).apply { mkdirs() }
    val key = try {
      val digest = MessageDigest.getInstance("SHA-256").digest(mp3Url.toByteArray(Charsets.UTF_8))
      digest.joinToString("") { "%02x".format(it) }.take(32)
    } catch (_: Exception) {
      mp3Url.hashCode().toUInt().toString(16)
    }
    val cachedFile = File(cacheDir, "lv_$key.mp3")
    if (cachedFile.exists() && cachedFile.isFile && cachedFile.length() > 1024L) {
      return cachedFile
    }
    val partFile = File(cacheDir, "lv_$key.part")
    return try {
      val request = Request.Builder()
        .url(mp3Url)
        .header("User-Agent", "LingoSphereAndroid/1.0")
        .build()
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val contentType = response.header("Content-Type").orEmpty().lowercase()
        if (contentType.contains("text/html") || contentType.contains("application/xhtml")) {
          return null
        }
        val body = response.body ?: return null
        body.byteStream().use { input ->
          FileOutputStream(partFile).use { output ->
            val buf = ByteArray(16 * 1024)
            var totalBytes = 0L
            while (true) {
              val read = input.read(buf)
              if (read <= 0) break
              output.write(buf, 0, read)
              totalBytes += read
            }
            output.flush()
            if (totalBytes <= 1024L) {
              partFile.delete()
              return null
            }
          }
        }
        if (cachedFile.exists()) cachedFile.delete()
        if (partFile.renameTo(cachedFile) && cachedFile.length() > 1024L) {
          cachedFile
        } else {
          partFile.delete()
          null
        }
      }
    } catch (_: Exception) {
      try { partFile.delete() } catch (_: Exception) {}
      null
    }
  }

  val isExactSentenceSyncAvailable: Boolean
    get() = !_useHumanNarrator.value

  /**
   * Evaluates whether the current playback position has reached the verified closing announcement
   * boundary (`contentEndMs`) of the active LibriVox track. When "Skip Recording Announcements" is
   * enabled and `positionMs >= contentEndMs`, cleanly finishes the chapter and advances to the next
   * chapter without speaking the closing "End of Chapter..." LibriVox credits.
   */
  fun onTrackPositionReached(positionMs: Long, durationMs: Long = _durationMs.value): Boolean {
    val book = _activeBook.value ?: return false
    if (durationMs > 0L) {
      _durationMs.value = durationMs
    }
    _currentPositionMs.value = positionMs

    if (!_useHumanNarrator.value || !_skipRecordingAnnouncements.value) {
      return false
    }

    val bounds = _activeTrackBoundaries.value ?: LibriVoxAnnouncementDetector.resolveBoundariesForChapter(
      book = book,
      chapterIndex = _activeChapterIndex.value,
      trackUrlOverride = _currentAudioUrl.value,
      knownDurationMsOverride = _durationMs.value,
      context = appContext
    ).also { _activeTrackBoundaries.value = it }

    val endMs = bounds.contentEndMs
    if (bounds.isVerified && endMs != null && endMs > bounds.contentStartMs && positionMs >= endMs) {
      progressPollerJob?.cancel()
      try {
        if (isMpPrepared && isMpStarted) {
          isMpStarted = false
          mediaPlayer?.pause()
        }
      } catch (_: Exception) {}
      return advanceToNextChapterOnCompletion(book.id)
    }
    return false
  }

  private fun startMp3Poller() {
    progressPollerJob?.cancel()
    progressPollerJob = scope.launch {
      delay(250L)
      var tick = 0
      while (isActive && _isPlaying.value && _useHumanNarrator.value) {
        try {
          val mp = mediaPlayer
          if (mp != null && isMpPrepared && isMpStarted) {
            val pos = mp.currentPosition.toLong()
            val dur = mp.duration.toLong().coerceAtLeast(1L)
            if (onTrackPositionReached(pos, dur)) {
              break
            }

            // Human LibriVox MP3 lacks sentence-level timestamps; never fabricate fake sentence indices.
            tick++
            if (tick % 10 == 0) {
              persistCurrentProgress()
            }
          }
        } catch (_: Exception) {}
        delay(500L)
      }
    }
  }

  // =========================================================================
  // INTERNAL: CHUNKED LONG-FORM TTS NARRATION
  // =========================================================================

  private fun playCurrentTtsChunk(sentenceIndex: Int) {
    val book = _activeBook.value ?: return
    val expectedBookId = book.id
    val expectedChapterIndex = _activeChapterIndex.value
    val sm = boundSpeechManager
    val sentences = getChapterSentences(book, expectedChapterIndex)
    if (sentences.isEmpty()) return

    if (sentenceIndex >= sentences.size) {
      advanceToNextChapterOnCompletion(expectedBookId)
      return
    }

    val genId = ++playbackGenerationId
    val clampedIdx = sentenceIndex.coerceIn(0, sentences.size - 1)
    _currentSentenceIndex.value = clampedIdx
    _isPlaying.value = true
    _isBuffering.value = false
    syncSharedSelectionState()

    val estTotalMs = (sentences.size * 4500L)
    _durationMs.value = estTotalMs
    _currentPositionMs.value = (clampedIdx * 4500L).coerceAtMost(estTotalMs)

    val textToSpeak = sentences[clampedIdx]
    val langCode = book.targetLanguageCode.ifBlank { "en" }

    sm?.speakWithTutorVoice(
      text = textToSpeak,
      voice = _selectedVoice.value,
      languageCode = langCode,
      speedMultiplier = _playbackSpeed.value,
      speedOverride = _playbackSpeed.value,
      persistAsPartnerPreference = false,
      onDone = {
        if (
          genId == playbackGenerationId &&
          _isPlaying.value &&
          !_useHumanNarrator.value &&
          _activeBook.value?.id == expectedBookId &&
          _activeChapterIndex.value == expectedChapterIndex
        ) {
          scope.launch {
            delay(220L)
            if (
              genId == playbackGenerationId &&
              _isPlaying.value &&
              !_useHumanNarrator.value &&
              _activeBook.value?.id == expectedBookId &&
              _activeChapterIndex.value == expectedChapterIndex
            ) {
              playCurrentTtsChunk(clampedIdx + 1)
            }
          }
        }
      }
    )

    persistCurrentProgress()
    updateMediaNotification()
  }

  // =========================================================================
  // HELPERS: CHAPTER RESOLUTION, PERSISTENCE & SYSTEM MEDIA NOTIFICATION
  // =========================================================================

  fun getTotalChaptersCount(book: AudiobookBook): Int {
    return book.totalChapterCount.coerceAtLeast(1)
  }

  fun getChapterTitle(book: AudiobookBook, chapterIndex: Int): String {
    book.parsedChapters.getOrNull(chapterIndex)?.title?.takeIf { it.isNotBlank() }?.let { return it }
    val edition = book.librivoxEditions.getOrNull(_selectedEditionIndex.value)
      ?: book.librivoxEditions.firstOrNull()
    edition?.sections?.getOrNull(chapterIndex)?.title?.takeIf { it.isNotBlank() }?.let { return it }
    book.episodes.getOrNull(chapterIndex)?.title?.takeIf { it.isNotBlank() }?.let { return it }
    return "Chapter ${chapterIndex + 1}"
  }

  fun getChapterSentences(book: AudiobookBook, chapterIndex: Int): List<String> {
    if (book.parsedChapters.isNotEmpty()) {
      val ch = book.parsedChapters.getOrNull(chapterIndex)
      if (ch != null) {
        return ch.sentences.ifEmpty { ch.paragraphs }
      }
    }
    val ep = book.episodes.getOrNull(chapterIndex)
    if (ep != null && ep.narrationLines.isNotEmpty()) {
      return ep.narrationLines.map { it.targetText }
    }
    val edition = book.librivoxEditions.getOrNull(_selectedEditionIndex.value)
      ?: book.librivoxEditions.firstOrNull()
    val sec = edition?.sections?.getOrNull(chapterIndex)
    if (sec != null) {
      return listOf(sec.title, "Human narration by ${sec.readerName}.")
    }
    return emptyList()
  }

  fun getChapterMp3Url(book: AudiobookBook, chapterIndex: Int): String? {
    // 1. When parsedChapters exist, check the verified aligned chapter MP3 URL first
    val parsedCh = book.parsedChapters.getOrNull(chapterIndex)
    if (parsedCh != null) {
      parsedCh.librivoxAudioUrl?.takeIf { it.isNotBlank() }?.let { return it }
    }

    // 2. Strictly match against the selected edition by subtitle, section type, and chapter number
    //    (NEVER fall back to Chapter 1 / index 0 when Chapter 2 or another chapter is selected)
    val edition = book.librivoxEditions.getOrNull(_selectedEditionIndex.value)
      ?: book.librivoxEditions.firstOrNull()
    if (edition != null && edition.sections.isNotEmpty()) {
      if (parsedCh != null) {
        val normTitle = parsedCh.title.lowercase().trim()
        val chSubtitle = normTitle.substringAfter(":", "").substringAfter(" - ", "").trim()
        val targetType = parsedCh.sectionType.uppercase().ifBlank { "CHAPTER" }
        val targetChNumber = parsedCh.chapterNumber.takeIf { it > 0 } ?: (chapterIndex + 1)

        if (chSubtitle.length >= 5) {
          edition.sections.firstOrNull { sec ->
            sec.listenUrl.isNotBlank() && sec.title.lowercase().contains(chSubtitle)
          }?.let { return it.listenUrl }
        }

        val matchedSection = when (targetType) {
          "PREFACE", "INTRODUCTION", "PROLOGUE", "EPILOGUE", "APPENDIX" -> {
            val kw = targetType.lowercase()
            edition.sections.firstOrNull { sec ->
              val t = sec.title.lowercase()
              sec.listenUrl.isNotBlank() && (t.contains(kw) || (targetType in setOf("PREFACE", "INTRODUCTION") && sec.sectionNumber == 0))
            }
          }
          "LETTER" -> {
            edition.sections.firstOrNull { sec ->
              val t = sec.title.lowercase()
              sec.listenUrl.isNotBlank() && t.contains("letter") &&
                (sec.sectionNumber == targetChNumber || t.contains("letter $targetChNumber"))
            }
          }
          else -> {
            edition.sections.firstOrNull { sec ->
              val t = sec.title.lowercase()
              val isSpecialFront = t.startsWith("preface") || t.startsWith("introduction") || t.startsWith("letter ")
              sec.listenUrl.isNotBlank() && !isSpecialFront && sec.sectionNumber == targetChNumber
            }
          }
        }
        if (matchedSection != null && matchedSection.listenUrl.isNotBlank()) {
          return matchedSection.listenUrl
        }
      } else if (book.parsedChapters.isEmpty()) {
        edition.sections.getOrNull(chapterIndex)?.listenUrl?.takeIf { it.isNotBlank() }?.let { return it }
      }
    }

    // 3. Check episode stream URL only when no parsedChapters mismatch exists
    if (book.parsedChapters.isEmpty()) {
      book.episodes.getOrNull(chapterIndex)?.audioStreamUrl?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return null
  }

  fun getActiveNarratorDisplay(book: AudiobookBook): String {
    if (_useHumanNarrator.value) {
      val chReader = book.parsedChapters.getOrNull(_activeChapterIndex.value)?.librivoxReaderName
      if (!chReader.isNullOrBlank()) return "$chReader (Human Narrator)"
      val edition = book.librivoxEditions.getOrNull(_selectedEditionIndex.value)
        ?: book.librivoxEditions.firstOrNull()
      if (edition != null) return edition.narratorName
    }
    return "${_selectedVoice.value.displayName} (AI Narrator)"
  }

  private fun persistCurrentProgress(markCompleted: Boolean = false, markChapterDone: Boolean = false) {
    val ctx = appContext ?: return
    val book = _activeBook.value ?: return
    val existing = AudiobookLearningService.loadUserProgress(ctx, currentUserId, book.id)
    val completedSet = existing.completedChapterIndices.toMutableSet()
    val chIdx = _activeChapterIndex.value
    if (markCompleted || markChapterDone) {
      completedSet.add(chIdx)
    }

    val frac = if (_useHumanNarrator.value && _durationMs.value > 0L) {
      (_currentPositionMs.value.toFloat() / _durationMs.value.toFloat()).coerceIn(0f, 1f)
    } else {
      val sentences = getChapterSentences(book, chIdx)
      if (sentences.size > 1) {
        (_currentSentenceIndex.value.toFloat() / (sentences.size - 1).toFloat()).coerceIn(0f, 1f)
      } else 0f
    }

    val updatedMap = existing.chapterProgressMap.toMutableMap()
    if (markChapterDone || markCompleted) {
      updatedMap[chIdx] = 1f
    } else if (frac > (updatedMap[chIdx] ?: 0f)) {
      updatedMap[chIdx] = frac
    }

    val updatedAudioPosMap = existing.chapterAudioPositionMsMap.toMutableMap()
    val updatedSentenceMap = existing.chapterSentenceIndexMap.toMutableMap()
    if (markChapterDone || markCompleted) {
      updatedAudioPosMap[chIdx] = 0L
      updatedSentenceMap[chIdx] = 0
    } else if (_useHumanNarrator.value) {
      updatedAudioPosMap[chIdx] = _currentPositionMs.value
    } else {
      updatedSentenceMap[chIdx] = _currentSentenceIndex.value
    }

    val totalChapters = book.totalChapterCount.coerceAtLeast(1)
    val overallListeningPercent = (
      (completedSet.size.toFloat() + (if (chIdx in completedSet) 0f else frac)) / totalChapters.toFloat()
      ).coerceIn(0f, 1f)

    AudiobookLearningService.saveListeningProgress(
      context = ctx,
      userId = currentUserId,
      bookId = book.id,
      bookTitle = book.title,
      chapterIndex = chIdx,
      sentenceIndex = _currentSentenceIndex.value,
      positionMs = _currentPositionMs.value,
      progressPercent = overallListeningPercent,
      preferredEditionId = if (_useHumanNarrator.value) "librivox" else "tts",
      autoPlayNextChapter = _autoPlayNextChapter.value,
      skipRecordingAnnouncements = _skipRecordingAnnouncements.value,
      completedChapterIndices = completedSet,
      chapterProgressMap = updatedMap,
      chapterAudioPositionMsMap = updatedAudioPosMap,
      chapterSentenceIndexMap = updatedSentenceMap,
      isCompleted = existing.isCompleted || markCompleted
    )
  }

  private fun updateMediaNotification() {
    val ctx = appContext ?: return
    val book = _activeBook.value ?: return
    val chTitle = getChapterTitle(book, _activeChapterIndex.value)
    val narrator = getActiveNarratorDisplay(book)

    try {
      val openIntent = Intent(ctx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
      val contentPendingIntent = PendingIntent.getActivity(
        ctx, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val rewindPending = PendingIntent.getBroadcast(
        ctx, 1, Intent(ACTION_REWIND_10).setPackage(ctx.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val playPausePending = PendingIntent.getBroadcast(
        ctx, 2, Intent(ACTION_PLAY_PAUSE).setPackage(ctx.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val forwardPending = PendingIntent.getBroadcast(
        ctx, 3, Intent(ACTION_FORWARD_10).setPackage(ctx.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val nextChPending = PendingIntent.getBroadcast(
        ctx, 4, Intent(ACTION_NEXT_CHAPTER).setPackage(ctx.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val playPauseLabel = if (_isPlaying.value) "Pause" else "Play"
      val statusPrefix = if (_isPlaying.value) "Playing" else "Paused"

      val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("${book.title} • $chTitle")
        .setContentText("$statusPrefix • $narrator")
        .setSubText(book.author)
        .setContentIntent(contentPendingIntent)
        .setOngoing(_isPlaying.value)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .addAction(0, "-10s", rewindPending)
        .addAction(0, playPauseLabel, playPausePending)
        .addAction(0, "+10s", forwardPending)
        .addAction(0, "Next Ch", nextChPending)
        .build()

      val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      mgr?.notify(NOTIFICATION_ID, notification)
    } catch (_: Exception) {}
  }

  private fun cancelMediaNotification() {
    val ctx = appContext ?: return
    try {
      val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      mgr?.cancel(NOTIFICATION_ID)
    } catch (_: Exception) {}
  }
}
