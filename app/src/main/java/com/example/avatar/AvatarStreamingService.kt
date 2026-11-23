package com.example.avatar

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object AvatarStreamingService {
  private const val TAG = "AvatarStreaming"
  private const val HEYGEN_API_BASE = "https://api.heygen.com/v1"
  private const val TAVUS_API_BASE = "https://tavusapi.com/v1"

  private const val PREFS_NAME = "lingo_avatar_credentials"
  private const val KEY_HEYGEN = "custom_heygen_key"
  private const val KEY_TAVUS = "custom_tavus_key"

  private val okHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(25, TimeUnit.SECONDS)
      .readTimeout(25, TimeUnit.SECONDS)
      .writeTimeout(25, TimeUnit.SECONDS)
      .build()
  }

  private val moshi by lazy {
    Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()
  }

  fun getHeyGenApiKey(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val customKey = prefs.getString(KEY_HEYGEN, "")?.trim() ?: ""
    if (customKey.isNotBlank()) return customKey

    return try {
      val buildConfigKey = BuildConfig::class.java.getField("HEYGEN_API_KEY").get(null) as? String
      if (!buildConfigKey.isNullOrBlank() && !buildConfigKey.startsWith("YOUR_")) {
        buildConfigKey.trim()
      } else ""
    } catch (_: Exception) {
      ""
    }
  }

  fun getTavusApiKey(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val customKey = prefs.getString(KEY_TAVUS, "")?.trim() ?: ""
    if (customKey.isNotBlank()) return customKey

    return try {
      val buildConfigKey = BuildConfig::class.java.getField("TAVUS_API_KEY").get(null) as? String
      if (!buildConfigKey.isNullOrBlank() && !buildConfigKey.startsWith("YOUR_")) {
        buildConfigKey.trim()
      } else ""
    } catch (_: Exception) {
      ""
    }
  }

  fun saveCustomHeyGenApiKey(context: Context, key: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_HEYGEN, key.trim())
      .apply()
  }

  fun saveCustomTavusApiKey(context: Context, key: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_TAVUS, key.trim())
      .apply()
  }

  fun isHeyGenConfigured(context: Context): Boolean {
    return getHeyGenApiKey(context).isNotBlank()
  }

  fun isTavusConfigured(context: Context): Boolean {
    return getTavusApiKey(context).isNotBlank()
  }

  /**
   * Initializes a real-time streaming avatar session with HeyGen.
   */
  suspend fun createHeyGenStreamingSession(
    apiKey: String,
    avatarName: String,
    voiceId: String
  ): Result<HeyGenSessionData> = withContext(Dispatchers.IO) {
    try {
      if (apiKey.isBlank()) {
        return@withContext Result.failure(IllegalStateException("HeyGen API key is missing"))
      }

      val reqAdapter = moshi.adapter(HeyGenNewSessionRequest::class.java)
      val jsonBody = reqAdapter.toJson(
        HeyGenNewSessionRequest(
          quality = "medium",
          avatarName = avatarName,
          voice = HeyGenVoiceConfig(voiceId = voiceId, rate = 1.0f),
          version = "v2"
        )
      )

      val request = Request.Builder()
        .url("$HEYGEN_API_BASE/streaming.new")
        .addHeader("X-Api-Key", apiKey)
        .addHeader("Content-Type", "application/json")
        .post(jsonBody.toRequestBody("application/json".toMediaType()))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseText = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        Log.w(TAG, "HeyGen streaming.new returned status (${response.code}), using Local High-Fidelity Avatar")
        return@withContext Result.failure(Exception("HeyGen error ${response.code}: $responseText"))
      }

      val resAdapter = moshi.adapter(HeyGenNewSessionResponse::class.java)
      val parsed = resAdapter.fromJson(responseText)
      val sessionData = parsed?.data

      if (sessionData != null) {
        Result.success(sessionData)
      } else {
        Result.failure(Exception(parsed?.message ?: "Empty session data from HeyGen"))
      }
    } catch (e: Exception) {
      Log.w(TAG, "createHeyGenStreamingSession error (falling back to local avatar): ${e.localizedMessage}")
      Result.failure(e)
    }
  }

  /**
   * Starts video streaming for the established session.
   */
  suspend fun startHeyGenSession(
    apiKey: String,
    sessionId: String,
    sdpAnswer: HeyGenSdpData
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val reqAdapter = moshi.adapter(HeyGenStartSessionRequest::class.java)
      val jsonBody = reqAdapter.toJson(
        HeyGenStartSessionRequest(
          sessionId = sessionId,
          sdp = sdpAnswer
        )
      )

      val request = Request.Builder()
        .url("$HEYGEN_API_BASE/streaming.start")
        .addHeader("X-Api-Key", apiKey)
        .addHeader("Content-Type", "application/json")
        .post(jsonBody.toRequestBody("application/json".toMediaType()))
        .build()

      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful) {
        Result.success(true)
      } else {
        val err = response.body?.string() ?: ""
        Result.failure(Exception("HeyGen start error ${response.code}: $err"))
      }
    } catch (e: Exception) {
      Log.e(TAG, "startHeyGenSession error", e)
      Result.failure(e)
    }
  }

  /**
   * Sends text to the avatar for live speech and synchronized lip-sync and facial expressions.
   */
  suspend fun sendHeyGenSpeakTask(
    apiKey: String,
    sessionId: String,
    text: String
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val reqAdapter = moshi.adapter(HeyGenTaskRequest::class.java)
      val jsonBody = reqAdapter.toJson(
        HeyGenTaskRequest(
          sessionId = sessionId,
          text = text,
          taskType = "repeat"
        )
      )

      val request = Request.Builder()
        .url("$HEYGEN_API_BASE/streaming.task")
        .addHeader("X-Api-Key", apiKey)
        .addHeader("Content-Type", "application/json")
        .post(jsonBody.toRequestBody("application/json".toMediaType()))
        .build()

      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful) {
        Result.success(true)
      } else {
        val err = response.body?.string() ?: ""
        Result.failure(Exception("HeyGen task error ${response.code}: $err"))
      }
    } catch (e: Exception) {
      Log.e(TAG, "sendHeyGenSpeakTask error", e)
      Result.failure(e)
    }
  }

  /**
   * Stops and closes the HeyGen streaming avatar session.
   */
  suspend fun stopHeyGenSession(
    apiKey: String,
    sessionId: String
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val reqAdapter = moshi.adapter(HeyGenStopSessionRequest::class.java)
      val jsonBody = reqAdapter.toJson(HeyGenStopSessionRequest(sessionId = sessionId))

      val request = Request.Builder()
        .url("$HEYGEN_API_BASE/streaming.stop")
        .addHeader("X-Api-Key", apiKey)
        .addHeader("Content-Type", "application/json")
        .post(jsonBody.toRequestBody("application/json".toMediaType()))
        .build()

      val response = okHttpClient.newCall(request).execute()
      Result.success(response.isSuccessful)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Creates a Tavus conversational video session.
   */
  suspend fun createTavusConversation(
    apiKey: String,
    replicaId: String,
    personaId: String? = null,
    conversationalContext: String? = null
  ): Result<TavusConversationResponse> = withContext(Dispatchers.IO) {
    try {
      if (apiKey.isBlank()) {
        return@withContext Result.failure(IllegalStateException("Tavus API key is missing"))
      }

      val reqAdapter = moshi.adapter(TavusConversationRequest::class.java)
      val jsonBody = reqAdapter.toJson(
        TavusConversationRequest(
          replicaId = replicaId,
          personaId = personaId,
          conversationalContext = conversationalContext
        )
      )

      val request = Request.Builder()
        .url("$TAVUS_API_BASE/conversations")
        .addHeader("x-api-key", apiKey)
        .addHeader("Content-Type", "application/json")
        .post(jsonBody.toRequestBody("application/json".toMediaType()))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseText = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("Tavus error ${response.code}: $responseText"))
      }

      val resAdapter = moshi.adapter(TavusConversationResponse::class.java)
      val parsed = resAdapter.fromJson(responseText)
      if (parsed?.conversationUrl != null) {
        Result.success(parsed)
      } else {
        Result.failure(Exception(parsed?.message ?: "Tavus returned empty conversation URL"))
      }
    } catch (e: Exception) {
      Log.e(TAG, "createTavusConversation error", e)
      Result.failure(e)
    }
  }
}
