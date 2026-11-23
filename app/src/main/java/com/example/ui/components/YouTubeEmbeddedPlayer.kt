package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.Language
import com.example.data.model.LingoVideo
import com.example.data.repository.YouTubeVideoService
import com.example.speech.SpeechManager
import kotlinx.coroutines.delay

private data class VideoLessonScene(
  val index: Int,
  val sceneLabel: String,
  val targetLine: String,
  val coachingNote: String
)

/**
 * Sanitize any legacy or remote creator label so no third-party brand names appear in the UI.
 */
fun sanitizeCreatorName(raw: String): String {
  val cleaned = raw
    .replace(Regex("(?i)\\s*•?\\s*youtube\\s*"), "")
    .replace(Regex("(?i)youtube\\s*•?\\s*"), "")
    .replace(Regex("(?i)pod101"), "Academy")
    .replace(Regex("(?i)cloudflare"), "Global")
    .trim()
    .trim('•', '-', '|', ' ')
  return cleaned.ifBlank { "LingoSphere Studio" }
}

/**
 * Produces an isolated HTML document loading the YouTube player inside an iframe.
 * Using an iframe with explicit permissions and hardware-accelerated rendering enables
 * full video frame rendering (fixing black-screen / audio-only issues) while allowing
 * LingoSphere's transport controls, captions, and playback listeners to operate smoothly.
 */
private fun generateYouTubeEmbedHtml(videoId: String): String {
  return """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body {
          width: 100%;
          height: 100%;
          background: #000000;
          overflow: hidden;
        }
        #player-wrapper {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          background: #000000;
        }
        iframe {
          width: 100%;
          height: 100%;
          border: 0;
          display: block;
        }
      </style>
    </head>
    <body>
      <div id="player-wrapper">
        <iframe id="yt-player"
          src="https://www.youtube-nocookie.com/embed/$videoId?enablejsapi=1&autoplay=1&playsinline=1&controls=1&rel=0&modestbranding=1&fs=1&iv_load_policy=3"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          allowfullscreen>
        </iframe>
      </div>
      <script>
        var player = document.getElementById('yt-player');

        function sendCommand(func, args) {
          try {
            if (player && player.contentWindow) {
              player.contentWindow.postMessage(JSON.stringify({
                event: 'command',
                func: func,
                args: args || []
              }), '*');
            }
          } catch(e) {}
        }

        window.lingoControl = {
          play: function() { sendCommand('playVideo'); },
          pause: function() { sendCommand('pauseVideo'); },
          seekTo: function(sec) { sendCommand('seekTo', [sec, true]); },
          setMuted: function(muted) { if (muted) sendCommand('mute'); else sendCommand('unMute'); },
          setSpeed: function(rate) { sendCommand('setPlaybackRate', [rate]); }
        };

        window.addEventListener('message', function(event) {
          try {
            var msg = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
            if (!msg) return;

            if (msg.event === 'onReady') {
              if (window.LingoBridge) window.LingoBridge.onReady();
              sendCommand('playVideo');
            } else if (msg.event === 'onStateChange') {
              var isPlaying = (msg.info === 1);
              var framesReady = (msg.info === 1 || msg.info === 2);
              if (window.LingoBridge) {
                window.LingoBridge.onProgress(isPlaying, -1, -1, framesReady);
              }
            } else if (msg.info && msg.info.currentTime !== undefined) {
              var cur = Math.floor(msg.info.currentTime);
              var dur = Math.floor(msg.info.duration || 0);
              if (window.LingoBridge) {
                window.LingoBridge.onProgress(true, cur, dur, true);
              }
            } else if (msg.event === 'onError') {
              if (window.LingoBridge) window.LingoBridge.onVideoError('code_' + msg.info);
            }
          } catch (err) {}
        });

        setInterval(function() {
          sendCommand('listening');
        }, 1000);
      </script>
    </body>
    </html>
  """.trimIndent()
}

private class LingoVideoJsBridge(
  private val mainHandler: Handler,
  private val onStateProgress: (Boolean, Long, Long, Boolean) -> Unit,
  private val onErrorFallback: () -> Unit
) {
  @JavascriptInterface
  fun onProgress(isPlaying: Boolean, currentSec: Long, durationSec: Long, hasFrames: Boolean) {
    mainHandler.post {
      onStateProgress(isPlaying, currentSec, durationSec, hasFrames)
    }
  }

  @JavascriptInterface
  fun onReady() {
    mainHandler.post {
      onStateProgress(true, -1L, -1L, true)
    }
  }

  @JavascriptInterface
  fun onVideoError(reason: String) {
    mainHandler.post {
      onErrorFallback()
    }
  }
}

/**
 * Brand-free real video player powered by WebView HTML5 streaming with LingoSphere's custom
 * Compose transport bar, synchronized learning subtitles, speed control, and automatic fallback.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun YouTubeEmbeddedPlayer(
  video: LingoVideo,
  modifier: Modifier = Modifier,
  speechManager: SpeechManager? = null,
  onWatchSecondsUpdated: (Long) -> Unit = {}
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val mainHandler = remember { Handler(Looper.getMainLooper()) }

  val targetLanguage = remember(video.languageCode) {
    Language.findByCode(video.languageCode)
  }

  val fallbackVideoIds = remember(video.videoId, video.languageCode) {
    listOf(video.videoId) + YouTubeVideoService.getFallbackVideoIdsForLanguage(video.languageCode, video.videoId)
  }
  var activeCandidateIndex by remember(video.videoId) { mutableIntStateOf(0) }
  val activeVideoId = fallbackVideoIds.getOrElse(activeCandidateIndex) { video.videoId }

  val parsedDefaultDuration = remember(video.durationText) {
    parseDurationToSeconds(video.durationText).coerceAtLeast(60L)
  }

  val scenes = remember(video.videoId, video.captionSummary, video.description, video.languageCode) {
    buildInteractiveScenes(video, targetLanguage)
  }

  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var isPlaying by remember(video.videoId) { mutableStateOf(true) }
  var hasVideoFrames by remember(activeVideoId) { mutableStateOf(false) }
  var isInitialLoading by remember(activeVideoId) { mutableStateOf(true) }
  var currentPositionSec by remember(video.videoId) { mutableLongStateOf(0L) }
  var totalDurationSec by remember(video.videoId) { mutableLongStateOf(parsedDefaultDuration) }
  var showCaptions by remember { mutableStateOf(false) }
  var showControlsOverlay by remember { mutableStateOf(true) }
  var isAudioMuted by remember { mutableStateOf(false) }
  var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

  // Initial loading auto-fadeout
  LaunchedEffect(activeVideoId) {
    isInitialLoading = true
    delay(2000L)
    isInitialLoading = false
  }

  // Determine current coaching scene from playback position
  val sceneDurationSec = 12L
  val currentSceneIndex = remember(currentPositionSec, scenes.size) {
    if (scenes.isEmpty()) 0
    else ((currentPositionSec / sceneDurationSec).toInt()) % scenes.size
  }
  val activeScene = scenes.getOrElse(currentSceneIndex) {
    VideoLessonScene(
      index = 0,
      sceneLabel = "Lesson Overview",
      targetLine = video.title,
      coachingNote = video.description.ifBlank { "Listen carefully and repeat aloud." }
    )
  }

  // If a specific video ID does not start streaming within 8 seconds, automatically try the next verified video ID
  LaunchedEffect(activeVideoId) {
    delay(8000L)
    if (!hasVideoFrames && activeCandidateIndex + 1 < fallbackVideoIds.size) {
      activeCandidateIndex += 1
    }
  }

  // Fallback timer clock so watch progress and timeline advance even during initial buffering
  LaunchedEffect(isPlaying, playbackSpeed, hasVideoFrames) {
    val tickDelayMs = (1000L / playbackSpeed).toLong().coerceIn(400L, 1600L)
    while (isPlaying && !hasVideoFrames) {
      delay(tickDelayMs)
      currentPositionSec = (currentPositionSec + 1L) % totalDurationSec.coerceAtLeast(60L)
      onWatchSecondsUpdated(currentPositionSec)
    }
  }

  // Auto-hide controls overlay after 4 seconds of active video playback so video view is unobstructed
  LaunchedEffect(showControlsOverlay, isPlaying, hasVideoFrames) {
    if (showControlsOverlay && isPlaying && hasVideoFrames) {
      delay(4000L)
      showControlsOverlay = false
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> {
          webViewRef?.evaluateJavascript("window.lingoControl && window.lingoControl.pause();", null)
          webViewRef?.onPause()
          speechManager?.stopSpeaking()
        }
        Lifecycle.Event.ON_RESUME -> {
          webViewRef?.onResume()
          if (isPlaying) {
            webViewRef?.evaluateJavascript("window.lingoControl && window.lingoControl.play();", null)
          }
        }
        Lifecycle.Event.ON_DESTROY -> {
          speechManager?.stopSpeaking()
          webViewRef?.destroy()
        }
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      speechManager?.stopSpeaking()
      webViewRef?.stopLoading()
      webViewRef?.destroy()
      webViewRef = null
    }
  }

  val cleanCreator = remember(video.channelTitle) {
    sanitizeCreatorName(video.channelTitle)
  }

  Column(modifier = modifier.fillMaxWidth()) {
    // 16:9 Video Stage
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        .background(Color.Black)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) {
          showControlsOverlay = !showControlsOverlay
        }
        .testTag("youtube_embedded_player")
    ) {
      val isRobolectricPlayer = remember {
        android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
      }
      var isPlayerWebViewReady by remember(activeVideoId) {
        mutableStateOf(isRobolectricPlayer)
      }
      LaunchedEffect(activeVideoId) {
        if (!isPlayerWebViewReady) {
          androidx.compose.runtime.withFrameNanos { }
          delay(80L)
          isPlayerWebViewReady = true
        }
      }

      // Layer 1: Hardware-accelerated WebView rendering HTML5 YouTube video surface directly
      if (isPlayerWebViewReady) {
      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(AndroidColor.BLACK)
            // Use LAYER_TYPE_NONE so Chromium's video pipeline composits video frames directly
            setLayerType(View.LAYER_TYPE_NONE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              mediaPlaybackRequiresUserGesture = false
              loadsImagesAutomatically = true
              mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
              cacheMode = WebSettings.LOAD_DEFAULT
              useWideViewPort = true
              loadWithOverviewMode = true
              allowFileAccess = false
              allowContentAccess = false
              userAgentString = WebSettings.getDefaultUserAgent(ctx)
            }

            addJavascriptInterface(
              LingoVideoJsBridge(
                mainHandler = mainHandler,
                onStateProgress = { playingState, curSec, durSec, framesReady ->
                  if (framesReady) {
                    hasVideoFrames = true
                    isInitialLoading = false
                  }
                  if (durSec > 0L) {
                    totalDurationSec = durSec
                  }
                  if (curSec >= 0L) {
                    currentPositionSec = curSec
                    onWatchSecondsUpdated(curSec)
                  }
                  if (playingState != isPlaying && curSec >= 0L) {
                    isPlaying = playingState
                  }
                },
                onErrorFallback = {
                  if (activeCandidateIndex + 1 < fallbackVideoIds.size) {
                    activeCandidateIndex += 1
                  }
                }
              ),
              "LingoBridge"
            )

            webChromeClient = object : WebChromeClient() {
              override fun getDefaultVideoPoster(): Bitmap? {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
              }
            }

            webViewClient = object : WebViewClient() {
              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hasVideoFrames = true
                isInitialLoading = false
                if (isPlaying) {
                  view?.evaluateJavascript("window.lingoControl && window.lingoControl.play();", null)
                }
              }

              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean {
                val targetUrl = request?.url?.toString().orEmpty()
                if (targetUrl.startsWith("https://www.youtube-nocookie.com") ||
                    targetUrl.startsWith("https://www.youtube.com/embed") ||
                    targetUrl.startsWith("about:blank") ||
                    targetUrl.startsWith("data:")
                ) {
                  return false
                }
                try {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                  context.startActivity(intent)
                } catch (_: Exception) {}
                return true
              }

              override fun onRenderProcessGone(
                view: WebView?,
                detail: android.webkit.RenderProcessGoneDetail?
              ): Boolean {
                android.util.Log.w(
                  "YouTubeEmbeddedPlayer",
                  "WebView renderer process terminated (didCrash=${detail?.didCrash()}); detaching safely."
                )
                if (webViewRef === view) webViewRef = null
                isInitialLoading = false
                isPlaying = false
                runCatching {
                  (view?.parent as? ViewGroup)?.removeView(view)
                  view?.destroy()
                }
                return true
              }
            }

            tag = activeVideoId
            loadDataWithBaseURL(
              "https://www.youtube-nocookie.com",
              generateYouTubeEmbedHtml(activeVideoId),
              "text/html",
              "UTF-8",
              null
            )
            webViewRef = this
          }
        },
        update = { webView ->
          if (webView.tag != activeVideoId) {
            webView.tag = activeVideoId
            hasVideoFrames = false
            isInitialLoading = true
            webView.loadDataWithBaseURL(
              "https://www.youtube-nocookie.com",
              generateYouTubeEmbedHtml(activeVideoId),
              "text/html",
              "UTF-8",
              null
            )
          }
        },
        onRelease = { webView ->
          if (webViewRef === webView) webViewRef = null
          runCatching {
            webView.stopLoading()
            webView.removeJavascriptInterface("LingoBridge")
            webView.loadUrl("about:blank")
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
          }
        },
        modifier = Modifier.fillMaxSize()
      )
      }

      // Layer 2: Initial loading spinner or center paused overlay (translucent, never hides video)
      androidx.compose.animation.AnimatedVisibility(
        visible = (isInitialLoading && !hasVideoFrames) || !isPlaying,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(if (!isPlaying) Color(0x66000000) else Color.Transparent),
          contentAlignment = Alignment.Center
        ) {
          if (isInitialLoading && !hasVideoFrames && isPlaying) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CircularProgressIndicator(
                color = Color(0xFF38BDF8),
                strokeWidth = 3.dp,
                modifier = Modifier.size(38.dp)
              )
              Text(
                text = "Starting ${targetLanguage.name} Video...",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          } else if (!isPlaying) {
            Surface(
              shape = CircleShape,
              color = Color(0xFF38BDF8),
              modifier = Modifier
                .size(56.dp)
                .clickable {
                  isPlaying = true
                  showControlsOverlay = true
                  webViewRef?.evaluateJavascript("window.lingoControl && window.lingoControl.play();", null)
                }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = "Resume Video",
                  tint = Color(0xFF040B1A),
                  modifier = Modifier.size(34.dp)
                )
              }
            }
          }
        }
      }

      // Layer 3: Top Status Bar (shown when controls overlay is visible)
      androidx.compose.animation.AnimatedVisibility(
        visible = showControlsOverlay || !isPlaying,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color(0xCC030712), Color.Transparent)
              )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(50),
              color = if (isPlaying) Color(0xFF10B981).copy(alpha = 0.92f) else Color(0xFF334155).copy(alpha = 0.92f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                )
                Text(
                  text = if (isPlaying) "LIVE STREAM • HD" else "PAUSED",
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(50),
              color = Color.Black.copy(alpha = 0.65f)
            ) {
              Text(
                text = "${targetLanguage.flagEmoji} ${targetLanguage.name} • ${video.proficiencyLevel}",
                color = Color(0xFFE2E8F0),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFF1E293B).copy(alpha = 0.88f),
            modifier = Modifier.clickable {
              showCaptions = !showCaptions
            }
          ) {
            Text(
              text = if (showCaptions) "Coaching Notes: ON" else "Coaching Notes: OFF",
              color = Color(0xFF38BDF8),
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }
      }

      // Layer 4: Optional Compact Coaching Subtitle Pill (above bottom controls when toggled on)
      androidx.compose.animation.AnimatedVisibility(
        visible = showCaptions,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = if (showControlsOverlay || !isPlaying) 62.dp else 14.dp, start = 16.dp, end = 16.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xD90B1528))
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable {
              speechManager?.speak(activeScene.targetLine, video.languageCode)
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = activeScene.targetLine,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Layer 5: LingoSphere Custom Transport Controls & Interactive Timeline Scrubber
      androidx.compose.animation.AnimatedVisibility(
        visible = showControlsOverlay || !isPlaying,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xEB030712))
              )
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Slider(
            value = currentPositionSec.toFloat().coerceIn(0f, totalDurationSec.toFloat().coerceAtLeast(1f)),
            onValueChange = { newSec ->
              val targetSec = newSec.toLong().coerceIn(0L, totalDurationSec)
              currentPositionSec = targetSec
              webViewRef?.evaluateJavascript("window.lingoControl && window.lingoControl.seekTo($targetSec);", null)
              onWatchSecondsUpdated(targetSec)
            },
            valueRange = 0f..totalDurationSec.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
              thumbColor = Color(0xFF38BDF8),
              activeTrackColor = Color(0xFF38BDF8),
              inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(20.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Left Transport Controls: Play/Pause, -10s, +10s, Time
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              IconButton(
                onClick = {
                  isPlaying = !isPlaying
                  if (isPlaying) {
                    webViewRef?.evaluateJavascript("window.lingoControl && window.lingoControl.play();", null)
                  } else {
                    webViewRef?.evaluateJavascript("window.lingoControl && window.lingoControl.pause();", null)
                    speechManager?.stopSpeaking()
                  }
                },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("video_play_pause_button")
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = if (isPlaying) "Pause Video" else "Play Video",
                  tint = Color.White,
                  modifier = Modifier.size(22.dp)
                )
              }

              IconButton(
                onClick = {
                  val targetSec = (currentPositionSec - 10L).coerceAtLeast(0L)
                  currentPositionSec = targetSec
                  webViewRef?.evaluateJavascript("window.lingoControl && window.lingoControl.seekTo($targetSec);", null)
                  onWatchSecondsUpdated(targetSec)
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Replay10,
                  contentDescription = "Rewind 10 seconds",
                  tint = Color.White.copy(alpha = 0.9f),
                  modifier = Modifier.size(18.dp)
                )
              }

              IconButton(
                onClick = {
                  val targetSec = (currentPositionSec + 10L).coerceAtMost(totalDurationSec)
                  currentPositionSec = targetSec
                  webViewRef?.evaluateJavascript("window.lingoControl && window.lingoControl.seekTo($targetSec);", null)
                  onWatchSecondsUpdated(targetSec)
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Forward10,
                  contentDescription = "Forward 10 seconds",
                  tint = Color.White.copy(alpha = 0.9f),
                  modifier = Modifier.size(18.dp)
                )
              }

              Spacer(modifier = Modifier.width(4.dp))

              Text(
                text = "${formatSeconds(currentPositionSec)} / ${formatSeconds(totalDurationSec)}",
                color = Color(0xFFE2E8F0),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // Right Controls: Speed, Captions (CC), Mute/Unmute
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.clickable {
                  val nextSpeed = when (playbackSpeed) {
                    0.75f -> 1.0f
                    1.0f -> 1.25f
                    1.25f -> 1.5f
                    else -> 0.75f
                  }
                  playbackSpeed = nextSpeed
                  webViewRef?.evaluateJavascript("window.lingoControl && window.lingoControl.setSpeed($nextSpeed);", null)
                }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Playback Speed",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                  )
                  Text(
                    text = "${playbackSpeed}x",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              IconButton(
                onClick = { showCaptions = !showCaptions },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = if (showCaptions) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                  contentDescription = "Toggle Captions",
                  tint = if (showCaptions) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                  modifier = Modifier.size(19.dp)
                )
              }

              IconButton(
                onClick = {
                  val nextMuted = !isAudioMuted
                  isAudioMuted = nextMuted
                  webViewRef?.evaluateJavascript("window.lingoControl && window.lingoControl.setMuted($nextMuted);", null)
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription = if (isAudioMuted) "Unmute Audio" else "Mute Audio",
                  tint = if (isAudioMuted) Color(0xFFF87171) else Color(0xFF10B981),
                  modifier = Modifier.size(19.dp)
                )
              }
            }
          }
        }
      }
    }

    // LingoSphere Studio Lesson Attribution & Quick Audio Replay Bar
    Surface(
      color = Color(0xFF121824),
      shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "Verified Instructor",
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "LingoSphere Studio • $cleanCreator",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Tap video for player controls • Use AI Study Pack below",
              color = Color(0xFF94A3B8),
              fontSize = 10.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                speechManager?.speak(activeScene.targetLine, video.languageCode)
              }
              .testTag("watch_on_youtube_link")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Speak Key Phrase",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "Speak Line",
                color = Color(0xFFE2E8F0),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                try {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.youtubeWatchUrl))
                  context.startActivity(intent)
                } catch (_: Exception) {}
              }
              .testTag("open_external_video_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open in YouTube App",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(14.dp)
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                currentPositionSec = 0L
                isPlaying = true
                showControlsOverlay = true
                webViewRef?.evaluateJavascript(
                  "window.lingoControl && (window.lingoControl.seekTo(0), window.lingoControl.play());",
                  null
                )
              }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Restart Video",
                tint = Color(0xFFE2E8F0),
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }
    }
  }
}

private fun parseDurationToSeconds(durationText: String): Long {
  val parts = durationText.trim().split(":").mapNotNull { it.trim().toLongOrNull() }
  return when (parts.size) {
    3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
    2 -> parts[0] * 60L + parts[1]
    1 -> parts[0]
    else -> 600L
  }
}

private fun formatSeconds(totalSeconds: Long): String {
  val safe = totalSeconds.coerceAtLeast(0L)
  val hours = safe / 3600L
  val minutes = (safe % 3600L) / 60L
  val seconds = safe % 60L
  return if (hours > 0) {
    String.format("%d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format("%02d:%02d", minutes, seconds)
  }
}

private fun buildInteractiveScenes(
  video: LingoVideo,
  language: Language
): List<VideoLessonScene> {
  val rawSummary = video.captionSummary
    .replace(Regex("(?i)closed captions available[^.]*\\.?"), "")
    .replace(Regex("(?i)context from video description:\\s*"), "")
    .trim()

  val sentences = rawSummary
    .split(Regex("(?<=[.!?¿¡。！？])\\s+"))
    .map { it.trim() }
    .filter { it.length >= 4 }

  val labels = listOf(
    "Opening Greeting",
    "Core Expression",
    "Conversational Rhythm",
    "Real-World Context",
    "Fluency Builder",
    "Active Mastery"
  )

  val coachingTips = listOf(
    "Listen to the native cadence and shadow the phrase aloud • ${video.topicCategory}",
    "Focus on clear vowel pronunciation and natural word linking",
    "Notice how polite register is used in everyday ${language.name} conversation",
    "Tap this subtitle anytime to replay the spoken pronunciation",
    "Practice repeating at 1.0x speed, then try 1.25x for native speed"
  )

  if (sentences.size >= 2) {
    return sentences.take(6).mapIndexed { idx, line ->
      VideoLessonScene(
        index = idx,
        sceneLabel = labels[idx % labels.size],
        targetLine = line,
        coachingNote = coachingTips[idx % coachingTips.size]
      )
    }
  }

  return listOf(
    VideoLessonScene(
      index = 0,
      sceneLabel = "Introduction",
      targetLine = video.title,
      coachingNote = "${language.name} (${language.nativeName}) • ${video.proficiencyLevel} ${video.topicCategory}"
    ),
    VideoLessonScene(
      index = 1,
      sceneLabel = "Key Dialogue",
      targetLine = rawSummary.ifBlank { video.description.ifBlank { video.title } },
      coachingNote = "Tap subtitle to hear native ${language.name} pronunciation"
    ),
    VideoLessonScene(
      index = 2,
      sceneLabel = "Practice & Review",
      targetLine = video.description.ifBlank { "Complete the vocabulary, pronunciation drills, and quiz below!" },
      coachingNote = "Use the AI Study Pack tabs below to practice every phrase"
    )
  )
}
