package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class UpgradeSheetMode {
  AI_UPGRADE,
  LESSONS,
  ROLEPLAY,
  BOOKS
}

data class UpgradePromptRequest(
  val mode: UpgradeSheetMode = UpgradeSheetMode.AI_UPGRADE,
  val headline: String = "Unlock AI learning",
  val reason: String = "Unlock unlimited AI tutoring, speaking labs, structured lessons, and full books with LingoSphere Pro.",
  val featureKey: String = "ai",
  val itemId: String? = null,
  val onRewardedUnlockGranted: (() -> Unit)? = null
)

sealed class AiUiError(
  open val userMessage: String,
  open val canRetry: Boolean = true
) {
  val message: String
    get() = userMessage

  val isOffline: Boolean
    get() = this is Offline
  data class ProRequired(
    override val userMessage: String = "Unlock AI learning with LingoSphere Pro.",
    val mode: UpgradeSheetMode = UpgradeSheetMode.AI_UPGRADE
  ) : AiUiError(userMessage, canRetry = false)

  data class SignInRequired(
    override val userMessage: String = "Sign in to continue."
  ) : AiUiError(userMessage, canRetry = false)

  data class Offline(
    override val userMessage: String = "You're offline. Check your connection and try again."
  ) : AiUiError(userMessage, canRetry = true)

  data class RateLimited(
    override val userMessage: String = "Taking a brief breather — try again in a few seconds."
  ) : AiUiError(userMessage, canRetry = true)

  data class Transient(
    override val userMessage: String = "Taking a moment — let's try that again."
  ) : AiUiError(userMessage, canRetry = true)
}

/**
 * Central connectivity & calm AI error handler for LingoSphere.
 *
 * Guarantees:
 * - Never exposes raw exceptions, "Gemini", "OpenAI", "Firebase", "App Check", "token",
 *   "API", HTTP codes, or stack traces in user-facing UI.
 * - Logs technical failure details silently via Android Log for diagnostics.
 * - Provides live connectivity monitoring via ConnectivityManager.NetworkCallback.
 * - Routes PRO_REQUIRED responses cleanly to the global "Unlock AI learning" upgrade sheet.
 */
object AiErrorHandler {
  private const val TAG = "AiErrorHandler"

  private val _isOnline = MutableStateFlow(true)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private val _upgradeSheetEvents = MutableSharedFlow<UpgradePromptRequest>(extraBufferCapacity = 8)
  val upgradeSheetEvents: SharedFlow<UpgradePromptRequest> = _upgradeSheetEvents.asSharedFlow()

  @Volatile
  private var appContext: Context? = null

  @Volatile
  private var callbackRegistered = false

  @Volatile
  var testForceOffline: Boolean? = null

  fun startNetworkMonitoring(context: Context) {
    val ctx = context.applicationContext
    appContext = ctx
    updateOnlineFlag(hasActiveInternet(ctx))
    if (callbackRegistered || Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
      return
    }
    runCatching {
      val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
      val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
      cm.registerNetworkCallback(
        request,
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            updateOnlineFlag(true)
          }

          override fun onLost(network: Network) {
            updateOnlineFlag(hasActiveInternet(ctx))
          }

          override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val capable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            updateOnlineFlag(capable)
          }
        }
      )
      callbackRegistered = true
    }.onFailure { err ->
      runCatching { Log.w(TAG, "NetworkCallback registration skipped: ${err.message}") }
    }
  }

  private fun updateOnlineFlag(online: Boolean) {
    val effective = testForceOffline?.let { !it } ?: online
    _isOnline.value = effective
  }

  fun hasActiveInternet(context: Context? = appContext): Boolean {
    testForceOffline?.let { forcedOffline ->
      val effective = !forcedOffline
      _isOnline.value = effective
      return effective
    }
    if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
      val geminiForced = GeminiService.testForceOffline
      val online = geminiForced?.let { !it } ?: true
      _isOnline.value = online
      return online
    }
    val ctx = context ?: return _isOnline.value
    val online = runCatching {
      val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return@runCatching true
      val active = cm.activeNetwork ?: return@runCatching false
      val caps = cm.getNetworkCapabilities(active) ?: return@runCatching false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
          caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
          caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
          caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
    }.getOrDefault(true)
    _isOnline.value = online
    return online
  }

  fun requestUpgrade(
    mode: UpgradeSheetMode = UpgradeSheetMode.AI_UPGRADE,
    featureKey: String = "ai",
    headline: String = when (mode) {
      UpgradeSheetMode.AI_UPGRADE -> "Unlock AI learning"
      UpgradeSheetMode.LESSONS -> "Unlock all structured lessons"
      UpgradeSheetMode.ROLEPLAY -> "Unlock AI Roleplay"
      UpgradeSheetMode.BOOKS -> "Unlock unlimited books"
    },
    reason: String = when (mode) {
      UpgradeSheetMode.AI_UPGRADE ->
        "Upgrade to LingoSphere Pro for AI Tutor conversations, Speaking Labs, Pronunciation & Writing coaching."
      UpgradeSheetMode.LESSONS ->
        "Upgrade to LingoSphere Pro for all 500+ sequential CEFR A1–C2 lessons."
      UpgradeSheetMode.ROLEPLAY ->
        "Upgrade to LingoSphere Pro for unlimited interactive AI Roleplay and real-world missions."
      UpgradeSheetMode.BOOKS ->
        "Free includes 5 books per month. Upgrade to LingoSphere Pro for unlimited full books and audiobooks."
    },
    itemId: String? = null,
    onRewardedUnlockGranted: (() -> Unit)? = null
  ) {
    _upgradeSheetEvents.tryEmit(
      UpgradePromptRequest(
        mode = mode,
        headline = headline,
        reason = reason,
        featureKey = featureKey,
        itemId = itemId,
        onRewardedUnlockGranted = onRewardedUnlockGranted
      )
    )
  }

  fun handle(
    throwable: Throwable?,
    isProUser: Boolean,
    contextTag: String = "AiCall"
  ): AiUiError {
    val classified = handle(throwable = throwable, contextTag = contextTag)
    if (!isProUser && classified is AiUiError.ProRequired) {
      requestUpgrade(mode = UpgradeSheetMode.AI_UPGRADE, reason = classified.userMessage)
    }
    return classified
  }

  fun handle(
    throwable: Throwable? = null,
    httpStatus: Int? = null,
    serverCode: String? = null,
    rawMessage: String? = null,
    serverMessage: String? = null,
    contextTag: String = "AiCall"
  ): AiUiError {
    if (throwable != null || !rawMessage.isNullOrBlank() || !serverMessage.isNullOrBlank() || httpStatus != null || !serverCode.isNullOrBlank()) {
      runCatching {
        Log.e(
          TAG,
          "[$contextTag] status=$httpStatus code=$serverCode msg=${serverMessage ?: rawMessage ?: throwable?.message}",
          throwable
        )
      }
    }

    val combined = buildString {
      serverCode?.let { append(it).append(' ') }
      serverMessage?.let { append(it).append(' ') }
      rawMessage?.let { append(it).append(' ') }
      throwable?.message?.let { append(it) }
    }.lowercase()

    if (
      serverCode.equals("PRO_REQUIRED", ignoreCase = true) ||
      httpStatus == 403 && combined.contains("pro") ||
      combined.contains("pro_required") ||
      combined.contains("requires lingosphere pro")
    ) {
      return AiUiError.ProRequired()
    }

    if (
      serverCode.equals("SIGN_IN_REQUIRED", ignoreCase = true) ||
      combined.contains("sign_in_required") ||
      combined.contains("sign in to continue")
    ) {
      return AiUiError.SignInRequired()
    }

    if (
      !hasActiveInternet() ||
      throwable is UnknownHostException ||
      throwable is ConnectException ||
      combined.contains("unknownhost") ||
      combined.contains("unable to resolve host") ||
      combined.contains("offline") ||
      combined.contains("no internet")
    ) {
      return AiUiError.Offline()
    }

    if (
      httpStatus == 429 ||
      combined.contains("429") ||
      combined.contains("resource_exhausted") ||
      combined.contains("rate limit")
    ) {
      return AiUiError.RateLimited()
    }

    return AiUiError.Transient()
  }

  fun sanitizeUserFacingMessage(
    raw: String?,
    fallback: String = "Taking a moment — let's try that again.",
    isPro: Boolean = false
  ): String {
    val clean = raw?.trim().orEmpty()
    if (clean.isBlank()) return fallback
    val lower = clean.lowercase()
    val bannedTerms = listOf(
      "gemini",
      "openai",
      "firebase",
      "app check",
      "appcheck",
      "token",
      "api key",
      "apikey",
      "http ",
      "exception",
      "stacktrace",
      "401",
      "403",
      "404",
      "429",
      "500",
      "502",
      "503",
      "resource_exhausted",
      "unknownhost"
    )
    if (bannedTerms.any { lower.contains(it) }) {
      return when {
        lower.contains("offline") || lower.contains("internet") || lower.contains("connection") ->
          "You're offline. Check your connection and try again."
        lower.contains("pro") ->
          "Unlock AI learning with LingoSphere Pro."
        else -> fallback
      }
    }
    return clean
  }

  suspend fun <T> executeWithSingleRetry(
    retryDelayMs: Long = 2000L,
    isResultTransientFailure: (T) -> Boolean = { false },
    block: suspend (attempt: Int) -> T
  ): T {
    var lastException: Throwable? = null
    for (attempt in 1..2) {
      try {
        val result = block(attempt)
        if (!isResultTransientFailure(result) || attempt == 2) {
          return result
        }
        delay(retryDelayMs)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        lastException = e
        val classified = handle(throwable = e, contextTag = "SingleRetry_Attempt$attempt")
        if (classified is AiUiError.ProRequired || classified is AiUiError.SignInRequired || classified is AiUiError.Offline) {
          throw e
        }
        if (attempt == 1) {
          delay(retryDelayMs)
        }
      }
    }
    throw (lastException ?: IllegalStateException("Transient failure"))
  }

  fun isTransientNetworkOrServerException(t: Throwable): Boolean {
    return t is SocketTimeoutException ||
      t is TimeoutCancellationException ||
      t is IOException ||
      (t.message?.lowercase()?.let {
        it.contains("timeout") || it.contains("500") || it.contains("502") || it.contains("503") || it.contains("appcheck") || it.contains("401")
      } == true)
  }
}
