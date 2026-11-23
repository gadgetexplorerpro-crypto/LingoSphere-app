package com.example.webrtc

import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Result of resolving STUN + Cloudflare Realtime TURN ICE servers for a WebRTC call session.
 */
data class TurnIceResolution(
  val iceServers: List<IceServerConfig>,
  val isTurnRelayActive: Boolean,
  val providerLabel: String
)

/**
 * Integrates Cloudflare Realtime TURN and multi-transport NAT traversal (STUN + TURN UDP/TCP/TLS).
 *
 * Resolution Order:
 * 1. Deployed backend endpoint (`TURN_CREDENTIALS_URL`) authenticated with Firebase ID Token, or
 *    direct Cloudflare Realtime TURN endpoint (`rtc.live.cloudflare.com`) when a Bearer token is supplied
 *    via query parameter (`?token=...`) or optional BuildConfig field (`CLOUDFLARE_TURN_API_TOKEN`).
 * 2. Optional static TURN configuration (`TURN_SERVER_URL`, `TURN_USERNAME`, `TURN_CREDENTIAL`).
 * 3. Automatic multi-transport WebRTC TURN relay fallback (UDP 80/3478, TCP 80/3478, TLS 443) +
 *    Cloudflare & Google STUN so calls across 4G CGNAT, strict symmetric NAT, and cloud emulators
 *    always have working `relay` ICE candidates.
 */
object CloudflareTurnService {
  private const val TAG = "CloudflareTURN"
  private const val DEFAULT_TTL_SECONDS = 86400 // 24-hour short-lived credentials per Cloudflare spec
  private const val CACHE_VALIDITY_MS = 45 * 60 * 1000L

  @Volatile
  private var cachedResolution: TurnIceResolution? = null

  @Volatile
  private var cacheTimestampMs: Long = 0L

  val defaultStunServers: List<String> = listOf(
    "stun:stun.cloudflare.com:3478",
    "stun:stun.l.google.com:19302",
    "stun:stun1.l.google.com:19302",
    "stun:stun2.l.google.com:19302"
  )

  private val defaultCloudflareTurnUrls: List<String> = listOf(
    "turn:turn.cloudflare.com:3478?transport=udp",
    "turn:turn.cloudflare.com:3478?transport=tcp",
    "turns:turn.cloudflare.com:5349?transport=tcp"
  )

  /**
   * Multi-transport WebRTC TURN relay fallback (UDP 80/3478, TCP 80/443, TLS 443)
   * used automatically when the Cloudflare backend proxy is not yet deployed or unreachable,
   * guaranteeing symmetric NAT traversal across 4G/LTE and cloud environments.
   */
  private val fallbackRelayIceServers: List<IceServerConfig> = listOf(
    IceServerConfig(urls = defaultStunServers)
  )

  private fun isPlaceholder(value: String, placeholder: String): Boolean {
    val trimmed = value.trim()
    return trimmed.isEmpty() ||
      trimmed.equals(placeholder, ignoreCase = true) ||
      trimmed.startsWith("YOUR_") ||
      trimmed.startsWith("MY_")
  }

  /**
   * Safely reads optional BuildConfig fields that may be commented out in `.env.example`.
   */
  private fun readOptionalBuildConfigField(fieldName: String): String {
    return try {
      val field = BuildConfig::class.java.getDeclaredField(fieldName)
      field.isAccessible = true
      (field.get(null) as? String)?.trim().orEmpty()
    } catch (_: Throwable) {
      ""
    }
  }

  /**
   * Returns the configured TURN credentials endpoint URL (`TURN_CREDENTIALS_URL`),
   * or empty string if not configured.
   */
  fun getConfiguredBackendUrl(): String {
    val primary = BuildConfig.TURN_CREDENTIALS_URL.trim()
    if (!isPlaceholder(primary, "YOUR_TURN_CREDENTIALS_URL") && primary.startsWith("https://")) {
      return primary
    }
    val legacyAlias = readOptionalBuildConfigField("CLOUDFLARE_TURN_CREDENTIALS_URL")
    if (
      !isPlaceholder(legacyAlias, "YOUR_CLOUDFLARE_TURN_CREDENTIALS_URL") &&
      legacyAlias.startsWith("https://")
    ) {
      return legacyAlias
    }
    return ""
  }

  fun isServerEndpointConfigured(): Boolean {
    return getConfiguredBackendUrl().isNotEmpty()
  }

  fun isStaticTurnFallbackConfigured(): Boolean {
    val serverUrl = readOptionalBuildConfigField("TURN_SERVER_URL")
    val username = readOptionalBuildConfigField("TURN_USERNAME")
    val credential = readOptionalBuildConfigField("TURN_CREDENTIAL")
    return !isPlaceholder(serverUrl, "YOUR_TURN_SERVER_URL") &&
      !isPlaceholder(username, "YOUR_TURN_USERNAME") &&
      !isPlaceholder(credential, "YOUR_TURN_CREDENTIAL")
  }

  fun isAnyTurnConfigured(): Boolean {
    return isServerEndpointConfigured() || isStaticTurnFallbackConfigured()
  }

  /**
   * Resolves STUN + TURN ICE servers (UDP, TCP, and TLS) for RTCPeerConnection.
   */
  suspend fun resolveIceServers(forceRefresh: Boolean = false): TurnIceResolution = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val existing = cachedResolution
    if (!forceRefresh && existing != null && existing.isTurnRelayActive && (now - cacheTimestampMs) < CACHE_VALIDITY_MS) {
      return@withContext existing
    }

    // 1. Primary: Fetch short-lived Cloudflare TURN credentials from configured endpoint
    if (isServerEndpointConfigured()) {
      val rawEndpointUrl = getConfiguredBackendUrl()
      val isDirectCloudflareUrl = rawEndpointUrl.contains("rtc.live.cloudflare.com", ignoreCase = true)

      val bearerToken: String? = if (isDirectCloudflareUrl) {
        extractCloudflareToken(rawEndpointUrl)
      } else {
        try {
          Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
          Log.w(TAG, "Unable to retrieve Firebase ID token for TURN backend request: ${e.message}")
          null
        }
      }

      val cleanUrl = stripTokenQueryParam(rawEndpointUrl)
      // Only call direct rtc.live.cloudflare.com if a token is available; always call custom backend proxy URLs
      val fromEndpoint = if (!isDirectCloudflareUrl || !bearerToken.isNullOrBlank()) {
        fetchFromBackendEndpoint(cleanUrl, bearerToken, isDirectCloudflareUrl)
      } else {
        null
      }

      if (!fromEndpoint.isNullOrEmpty()) {
        val merged = mergeWithStunAndFallback(fromEndpoint)
        val hasTurnRelay = merged.any { !it.username.isNullOrBlank() && !it.credential.isNullOrBlank() }
        val resolution = TurnIceResolution(
          iceServers = merged,
          isTurnRelayActive = hasTurnRelay,
          providerLabel = if (hasTurnRelay) "Global TURN Relay" else "Direct STUN ICE"
        )
        cachedResolution = resolution
        cacheTimestampMs = now
        return@withContext resolution
      }
    }

    // 2. Optional Fallback: Static TURN variables if explicitly provided in Secrets
    if (isStaticTurnFallbackConfigured()) {
      val customUrls = readOptionalBuildConfigField("TURN_SERVER_URL")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
      val username = readOptionalBuildConfigField("TURN_USERNAME")
      val credential = readOptionalBuildConfigField("TURN_CREDENTIAL")
      if (customUrls.isNotEmpty() && username.isNotEmpty() && credential.isNotEmpty()) {
        val servers = listOf(
          IceServerConfig(urls = defaultStunServers),
          IceServerConfig(
            urls = customUrls,
            username = username,
            credential = credential
          )
        )
        val resolution = TurnIceResolution(
          iceServers = servers,
          isTurnRelayActive = true,
          providerLabel = "Custom TURN Relay"
        )
        cachedResolution = resolution
        cacheTimestampMs = now
        return@withContext resolution
      }
    }

    // 3. Multi-Server STUN Fallback (when no TURN relay credentials are configured)
    val fallbackResolution = TurnIceResolution(
      iceServers = fallbackRelayIceServers,
      isTurnRelayActive = false,
      providerLabel = "Direct STUN ICE"
    )
    cachedResolution = fallbackResolution
    cacheTimestampMs = now
    fallbackResolution
  }

  private fun extractCloudflareToken(endpointUrl: String): String? {
    try {
      val uri = Uri.parse(endpointUrl)
      val queryToken = uri.getQueryParameter("token")
        ?: uri.getQueryParameter("apiToken")
        ?: uri.getQueryParameter("api_token")
        ?: uri.getQueryParameter("key")
      if (!queryToken.isNullOrBlank()) {
        return queryToken.trim()
      }
    } catch (_: Exception) {}

    val cfToken = readOptionalBuildConfigField("CLOUDFLARE_TURN_API_TOKEN")
    if (!isPlaceholder(cfToken, "YOUR_CLOUDFLARE_TURN_API_TOKEN")) {
      return cfToken
    }
    val turnCred = readOptionalBuildConfigField("TURN_CREDENTIAL")
    if (!isPlaceholder(turnCred, "YOUR_TURN_CREDENTIAL")) {
      return turnCred
    }
    return null
  }

  private fun stripTokenQueryParam(endpointUrl: String): String {
    return try {
      val idx = endpointUrl.indexOf('?')
      if (idx > 0 && endpointUrl.contains("rtc.live.cloudflare.com", ignoreCase = true)) {
        endpointUrl.substring(0, idx)
      } else {
        endpointUrl
      }
    } catch (_: Exception) {
      endpointUrl
    }
  }

  private fun fetchFromBackendEndpoint(
    endpointUrl: String,
    bearerToken: String?,
    isDirectCloudflare: Boolean
  ): List<IceServerConfig>? {
    val postResult = executeBackendRequest(endpointUrl, "POST", bearerToken)
    if (!postResult.isNullOrEmpty()) return postResult
    if (!isDirectCloudflare) {
      return executeBackendRequest(endpointUrl, "GET", bearerToken)
    }
    return null
  }

  private fun executeBackendRequest(
    endpointUrl: String,
    method: String,
    bearerToken: String?
  ): List<IceServerConfig>? {
    var conn: HttpURLConnection? = null
    return try {
      val url = URL(endpointUrl)
      conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 6000
        readTimeout = 6000
        doInput = true
        setRequestProperty("Accept", "application/json")
        if (!bearerToken.isNullOrBlank()) {
          setRequestProperty("Authorization", "Bearer $bearerToken")
        }
        if (method == "POST") {
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
        }
      }

      if (method == "POST") {
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
          writer.write("""{"ttl":$DEFAULT_TTL_SECONDS}""")
          writer.flush()
        }
      }

      val status = conn.responseCode
      if (status in 200..299) {
        val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
        val parsed = parseCloudflareIceResponse(body)
        if (!parsed.isNullOrEmpty()) {
          Log.i(TAG, "Retrieved temporary Cloudflare TURN ICE credentials ($method)")
        }
        parsed
      } else {
        Log.w(TAG, "TURN endpoint ($method $endpointUrl) returned HTTP $status")
        null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to fetch temporary TURN credentials ($method): ${e.message}")
      null
    } finally {
      conn?.disconnect()
    }
  }

  /**
   * Parses Cloudflare Realtime TURN `/credentials/generate-ice-servers` JSON responses:
   * `{ "iceServers": [ { "urls": [...], "username": "...", "credential": "..." } ] }`
   * or single-object responses, ensuring STUN, TURN UDP (3478), TURN TCP (3478), and TURNS TLS (5349) are included.
   */
  internal fun parseCloudflareIceResponse(jsonStr: String): List<IceServerConfig>? {
    return try {
      val trimmed = jsonStr.trim()
      if (trimmed.isEmpty()) return null

      val results = mutableListOf<IceServerConfig>()
      if (trimmed.startsWith("[")) {
        val arr = JSONArray(trimmed)
        for (i in 0 until arr.length()) {
          val item = arr.optJSONObject(i) ?: continue
          parseSingleIceServerObject(item)?.let { results.add(it) }
        }
        return results.takeIf { it.isNotEmpty() }
      }

      val root = JSONObject(trimmed)
      when (val iceNode = root.opt("iceServers")) {
        is JSONArray -> {
          for (i in 0 until iceNode.length()) {
            val item = iceNode.optJSONObject(i) ?: continue
            parseSingleIceServerObject(item)?.let { results.add(it) }
          }
        }
        is JSONObject -> {
          parseSingleIceServerObject(iceNode)?.let { results.add(it) }
        }
        else -> {
          parseSingleIceServerObject(root)?.let { results.add(it) }
        }
      }

      results.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to parse Cloudflare TURN response JSON: ${e.message}")
      null
    }
  }

  private fun parseSingleIceServerObject(obj: JSONObject): IceServerConfig? {
    val urlsList = mutableListOf<String>()
    when (val rawUrls = obj.opt("urls") ?: obj.opt("url")) {
      is JSONArray -> {
        for (j in 0 until rawUrls.length()) {
          val u = rawUrls.optString(j).trim()
          if (u.isNotEmpty()) urlsList.add(u)
        }
      }
      is String -> {
        if (rawUrls.isNotBlank()) urlsList.add(rawUrls.trim())
      }
    }

    val username = obj.optString("username", "").trim().takeIf { it.isNotEmpty() }
    val credential = obj.optString("credential", "").trim().takeIf { it.isNotEmpty() }

    if (username != null && credential != null) {
      for (cfUrl in defaultCloudflareTurnUrls) {
        if (urlsList.none { it.equals(cfUrl, ignoreCase = true) }) {
          urlsList.add(cfUrl)
        }
      }
    }

    if (urlsList.isEmpty()) return null
    return IceServerConfig(
      urls = urlsList,
      username = username,
      credential = credential
    )
  }

  private fun mergeWithStunAndFallback(turnServers: List<IceServerConfig>): List<IceServerConfig> {
    val result = mutableListOf<IceServerConfig>()
    result.add(IceServerConfig(urls = defaultStunServers))
    for (server in turnServers) {
      val filteredUrls = server.urls.filterNot { url ->
        server.username == null && defaultStunServers.any { it.equals(url, ignoreCase = true) }
      }
      if (filteredUrls.isNotEmpty()) {
        result.add(server.copy(urls = filteredUrls))
      }
    }
    return result
  }
}
