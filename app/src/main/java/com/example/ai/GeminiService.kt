package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.data.model.CurriculumLesson
import com.example.data.model.Exercise
import com.example.data.model.ExerciseType
import com.example.data.model.Language
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * Learner profile context injected into every Gemini system instruction so all AI features
 * adapt strictly to the user's target language, native language, and CEFR level.
 */
data class GeminiUserContext(
  val targetLanguageCode: String,
  val nativeLanguageCode: String = "en",
  val cefrLevel: String = "A1",
  val learnerMemory: String = "",
  val isPro: Boolean = false
) {
  val targetLanguage: Language get() = Language.findByCode(targetLanguageCode)
  val nativeLanguage: Language get() = Language.findByCode(nativeLanguageCode.ifBlank { "en" })

  fun toSystemPromptHeader(roleInstruction: String): String = buildString {
    appendLine(roleInstruction.trim())
    appendLine("Learner Target Language: ${targetLanguage.name} (${targetLanguage.code}).")
    appendLine("Learner Native Language: ${nativeLanguage.name} (${nativeLanguage.code}).")
    appendLine("Learner CEFR Level: ${cefrLevel.ifBlank { "A1" }}.")
    appendLine("Always adapt vocabulary, sentence length, and explanations to CEFR ${cefrLevel.ifBlank { "A1" }}.")
    if (learnerMemory.isNotBlank()) {
      appendLine("Learner History & Past Mistakes to reinforce: ${learnerMemory.take(400)}")
    }
  }.trim()
}

/**
 * Typed result returned by [GeminiService] calls so screens can render exact loading,
 * cached, offline, quota-exceeded (paywall), or retryable error states.
 */
sealed class GeminiResult<out T> {
  data class Success<T>(val data: T, val fromCache: Boolean = false) : GeminiResult<T>()
  data class Offline(val message: String = "You're offline. Check your connection and try again.") : GeminiResult<Nothing>()
  data class QuotaExceeded(
    val usedToday: Int,
    val dailyLimit: Int,
    val message: String = "Unlock AI learning with LingoSphere Pro."
  ) : GeminiResult<Nothing>()
  data class Error(val message: String, val canRetry: Boolean = true) : GeminiResult<Nothing>()
}

/**
 * Observable global state for daily AI usage, online connectivity, and paywall notifications.
 */
data class GeminiQuotaUiState(
  val isProUser: Boolean = false,
  val dailyCallsUsed: Int = 0,
  val dailyCallsLimit: Int = GeminiService.FREE_DAILY_AI_CALL_LIMIT,
  val isOnline: Boolean = true,
  val lastErrorMessage: String? = null,
  val cachedEntriesCount: Int = 0
) {
  val remainingDailyCalls: Int
    get() = if (isProUser) Int.MAX_VALUE else (dailyCallsLimit - dailyCallsUsed).coerceAtLeast(0)

  val isLimitReached: Boolean
    get() = !isProUser && dailyCallsUsed >= dailyCallsLimit
}

/**
 * Structured post-turn speaking feedback returned by Gemini after every voice or chat turn.
 */
data class StructuredVoiceTurnPayload(
  val reply: String,
  val phonetics: String,
  val translation: String,
  val pronunciationScore: Int,
  val grammarFix: String?,
  val naturalPhrase: String?
)

/**
 * Unified single-entry Gemini service for LingoSphere.
 *
 * Uses Firebase AI Logic (`Firebase.ai`) authenticated securely via Firebase App Check —
 * no client-side `GEMINI_API_KEY` is hardcoded or exposed in the UI.
 *
 * Capabilities:
 * - Smart model routing (`gemini-3.8-flash` for voice/vision/JSON/streaming, `gemini-3.5-flash-lite` for fast high-volume tasks).
 * - Structured JSON outputs via `responseSchema = Schema.obj(...)` + `responseMimeType = "application/json"`.
 * - 20s timeout (25s multimodal) with 1 automatic retry on transient errors, always rethrowing `CancellationException`.
 * - Connectivity checking with clear offline states.
 * - Persistent local disk + memory JSON cache for lessons, stories, listening scripts, grammar, and flashcards.
 * - Daily free AI quota enforcement (Pro = unlimited) with automatic paywall trigger flow.
 * - Token-by-token streaming via `generateContentStream(...)`.
 * - Background pre-generation of the next CEFR lesson on the roadmap.
 */
object GeminiService {
  private const val TAG = "GeminiService"
  const val MODEL_FLASH = "gemini-3.8-flash"
  const val MODEL_FLASH_LITE = "gemini-3.5-flash-lite"
  const val MODEL_TTS = "gemini-3.1-flash-tts-preview"

  const val DEFAULT_TIMEOUT_MS = 20_000L
  const val MULTIMODAL_TIMEOUT_MS = 25_000L
  const val FREE_DAILY_AI_CALL_LIMIT = 0
  private const val RETRY_DELAY_MS = 2_000L
  private const val PREFS_NAME = "lingosphere_gemini_service_prefs"
  private const val KEY_QUOTA_DAY = "quota_utc_day"
  private const val KEY_QUOTA_USED = "quota_calls_used"
  private const val CACHE_DIR_NAME = "gemini_content_cache"

  @Volatile
  private var appContext: Context? = null

  private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val memoryCache = ConcurrentHashMap<String, String>()
  private val inFlightPregenKeys = ConcurrentHashMap.newKeySet<String>()

  private val _quotaUiState = MutableStateFlow(GeminiQuotaUiState())
  val quotaUiState: StateFlow<GeminiQuotaUiState> = _quotaUiState.asStateFlow()

  private val _paywallRequests = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val paywallRequests: SharedFlow<String> = _paywallRequests.asSharedFlow()

  /** Optional test override to simulate offline mode in Robolectric / JVM tests. */
  @Volatile
  var testForceOffline: Boolean? = null

  /** Optional test override to disable quota blocking in existing legacy tests. */
  @Volatile
  var testBypassDailyQuota: Boolean = false

  // Lazily instantiated base Firebase AI Logic models (initialized only after App Check)
  private val defaultFlashModel: GenerativeModel by lazy {
    com.example.ensureAppCheckConfiguredIfPending()
    Firebase.ai.generativeModel(modelName = MODEL_FLASH)
  }

  private val defaultLiteModel: GenerativeModel by lazy {
    com.example.ensureAppCheckConfiguredIfPending()
    Firebase.ai.generativeModel(modelName = MODEL_FLASH_LITE)
  }

  fun initialize(context: Context) {
    appContext = context.applicationContext
    val isRobolectric = Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    if (isRobolectric) {
      refreshQuotaStateFromPrefs()
      refreshConnectivityState()
      countCachedDiskEntries()
    } else {
      backgroundScope.launch {
        refreshQuotaStateFromPrefs()
        refreshConnectivityState()
        countCachedDiskEntries()
      }
    }
  }

  fun syncProStatus(isPro: Boolean) {
    _quotaUiState.value = _quotaUiState.value.copy(isProUser = isPro)
  }

  fun triggerPaywall(reason: String = "Unlock AI learning with LingoSphere Pro.") {
    _paywallRequests.tryEmit(reason)
  }

  fun resetDailyQuotaForTests() {
    val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs?.edit()?.putInt(KEY_QUOTA_USED, 0)?.putString(KEY_QUOTA_DAY, currentUtcDayKey())?.apply()
    _quotaUiState.value = _quotaUiState.value.copy(dailyCallsUsed = 0, lastErrorMessage = null)
  }

  private fun currentUtcDayKey(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
  }

  private fun refreshQuotaStateFromPrefs() {
    val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
    val today = currentUtcDayKey()
    val savedDay = prefs.getString(KEY_QUOTA_DAY, "")
    val used = if (savedDay == today) prefs.getInt(KEY_QUOTA_USED, 0) else {
      prefs.edit().putString(KEY_QUOTA_DAY, today).putInt(KEY_QUOTA_USED, 0).apply()
      0
    }
    _quotaUiState.value = _quotaUiState.value.copy(
      dailyCallsUsed = used,
      dailyCallsLimit = FREE_DAILY_AI_CALL_LIMIT
    )
  }

  private fun isVerifiedProActive(): Boolean {
    return _quotaUiState.value.isProUser ||
      com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement
  }

  private fun consumeDailyQuotaIfAllowed(countTowardQuota: Boolean): GeminiResult.QuotaExceeded? {
    if (testBypassDailyQuota) return null
    refreshQuotaStateFromPrefs()
    val current = _quotaUiState.value
    if (isVerifiedProActive()) return null
    if (!countTowardQuota && current.dailyCallsLimit > 0 && current.dailyCallsUsed < current.dailyCallsLimit) {
      return null
    }
    val exceeded = GeminiResult.QuotaExceeded(
      usedToday = current.dailyCallsUsed,
      dailyLimit = FREE_DAILY_AI_CALL_LIMIT,
      message = "Unlock AI learning with LingoSphere Pro."
    )
    _quotaUiState.value = current.copy(lastErrorMessage = exceeded.message)
    triggerPaywall(exceeded.message)
    AiErrorHandler.requestUpgrade(UpgradeSheetMode.AI_UPGRADE, reason = exceeded.message)
    return exceeded
  }

  private fun recordDailyCallConsumed(countTowardQuota: Boolean) {
    if (!countTowardQuota) return
    val today = currentUtcDayKey()
    val nextUsed = _quotaUiState.value.dailyCallsUsed + 1
    appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()
      ?.putString(KEY_QUOTA_DAY, today)
      ?.putInt(KEY_QUOTA_USED, nextUsed)
      ?.apply()
    _quotaUiState.value = _quotaUiState.value.copy(
      dailyCallsUsed = nextUsed,
      lastErrorMessage = null
    )
  }

  internal fun recordExternalQuotaConsumption() {
    recordDailyCallConsumed(countTowardQuota = true)
  }

  fun isNetworkAvailable(): Boolean {
    testForceOffline?.let { forced ->
      val online = !forced
      _quotaUiState.value = _quotaUiState.value.copy(isOnline = online)
      return online
    }
    val ctx = appContext ?: return true
    if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
      return true
    }
    val online = runCatching {
      val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return@runCatching true
      val network = cm.activeNetwork ?: return@runCatching false
      val caps = cm.getNetworkCapabilities(network) ?: return@runCatching false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
          caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
          caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
          caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
    }.getOrDefault(true)
    _quotaUiState.value = _quotaUiState.value.copy(isOnline = online)
    return online
  }

  fun refreshConnectivityState(): Boolean = isNetworkAvailable()

  // ═══════════════════════════════════════════════════════════════════════════
  // LOCAL PERSISTENT CACHE (Memory LRU + FilesDir JSON Cache)
  // ═══════════════════════════════════════════════════════════════════════════

  private fun cacheKeyHash(rawKey: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }.take(32)
  }

  private fun cacheDir(): File? {
    val ctx = appContext ?: return null
    val dir = File(ctx.filesDir, CACHE_DIR_NAME)
    if (!dir.exists()) dir.mkdirs()
    return dir
  }

  fun readFromCache(cacheKey: String): String? {
    val hashed = cacheKeyHash(cacheKey)
    memoryCache[hashed]?.let { return it }
    val file = cacheDir()?.let { File(it, "$hashed.json") } ?: return null
    if (!file.exists()) return null
    return runCatching {
      file.readText(Charsets.UTF_8).takeIf { it.isNotBlank() }?.also {
        memoryCache[hashed] = it
      }
    }.getOrNull()
  }

  fun writeToCache(cacheKey: String, payload: String) {
    if (payload.isBlank()) return
    val hashed = cacheKeyHash(cacheKey)
    memoryCache[hashed] = payload
    runCatching {
      val dir = cacheDir() ?: return
      File(dir, "$hashed.json").writeText(payload, Charsets.UTF_8)
      countCachedDiskEntries()
    }
  }

  private fun countCachedDiskEntries() {
    val count = runCatching { cacheDir()?.listFiles()?.size ?: 0 }.getOrDefault(memoryCache.size)
    _quotaUiState.value = _quotaUiState.value.copy(cachedEntriesCount = maxOf(count, memoryCache.size))
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STRUCTURED JSON SCHEMAS (Firebase AI Logic `Schema.obj(...)`)
  // ═══════════════════════════════════════════════════════════════════════════

  val voiceTurnFeedbackSchema: Schema by lazy {
    Schema.obj(
      properties = mapOf(
        "reply" to Schema.string("Tutor's natural spoken response in the target language adapted to the learner's CEFR level"),
        "phonetics" to Schema.string("Pronunciation guide / romanization of the reply"),
        "translation" to Schema.string("Accurate translation of the reply in the learner's native language"),
        "pronunciationScore" to Schema.integer("Estimated speaking/pronunciation clarity score from 0 to 100 based on the learner's utterance"),
        "grammarFix" to Schema.string("One concise grammar fix for the learner's utterance, or empty string if no grammar mistake"),
        "naturalPhrase" to Schema.string("One more natural native phrasing of what the learner tried to say, or empty string if already natural")
      )
    )
  }

  val cefrLessonSchema: Schema by lazy {
    Schema.obj(
      properties = mapOf(
        "title" to Schema.string("Concise lesson title"),
        "description" to Schema.string("Short 1-sentence lesson goal"),
        "category" to Schema.string("Skill category: Vocabulary, Speaking, Listening, or Grammar"),
        "estimatedMinutes" to Schema.integer("Estimated minutes (typically 5)"),
        "xpReward" to Schema.integer("XP reward (typically 35 to 50)"),
        "vocabulary" to Schema.array(
          Schema.obj(
            properties = mapOf(
              "word" to Schema.string("Word or phrase in target language"),
              "translation" to Schema.string("Meaning in native language"),
              "phonetic" to Schema.string("Pronunciation guide")
            )
          )
        ),
        "exercises" to Schema.array(
          Schema.obj(
            properties = mapOf(
              "exerciseType" to Schema.string("One of: MULTIPLE_CHOICE, FILL_IN_THE_BLANK, SPEAK_THE_SENTENCE, MATCH"),
              "prompt" to Schema.string("Clear learner prompt in native language"),
              "targetPhrase" to Schema.string("Target language sentence or phrase for audio/speaking"),
              "phonetic" to Schema.string("Pronunciation guide"),
              "correctAnswer" to Schema.string("Exact correct answer"),
              "options" to Schema.array(Schema.string("Choice option (4 items for MULTIPLE_CHOICE/FILL_IN_THE_BLANK, or match pairs formatted as 'Target = Native')")),
              "explanation" to Schema.string("Short helpful explanation in native language")
            )
          )
        )
      )
    )
  }

  val cameraSceneSchema: Schema by lazy {
    Schema.obj(
      properties = mapOf(
        "sceneTitle" to Schema.string("Short descriptive title for the photo scene"),
        "sceneDescription" to Schema.string("1-sentence scene summary in the learner's native language"),
        "items" to Schema.array(
          Schema.obj(
            properties = mapOf(
              "detectedObject" to Schema.string("Object or printed sign name in English"),
              "targetName" to Schema.string("Name in the target language"),
              "phonetic" to Schema.string("Pronunciation guide"),
              "nativeTranslation" to Schema.string("Translation in the learner's native language"),
              "grammarNote" to Schema.string("Part of speech / grammatical gender note"),
              "exampleTarget" to Schema.string("Natural example sentence in the target language"),
              "exampleNative" to Schema.string("Translation of the example sentence in the native language"),
              "speakingPrompt" to Schema.string("A short speaking question prompting the user to describe or order this item")
            )
          )
        )
      )
    )
  }

  val readingStorySchema: Schema by lazy {
    Schema.obj(
      properties = mapOf(
        "title" to Schema.string("Story title in target language"),
        "summary" to Schema.string("Short summary in native language"),
        "paragraphs" to Schema.array(Schema.string("Story paragraph in target language adapted to CEFR level")),
        "wordGlosses" to Schema.array(
          Schema.obj(
            properties = mapOf(
              "word" to Schema.string("Key word from the story"),
              "meaning" to Schema.string("Meaning in native language"),
              "phonetic" to Schema.string("Pronunciation guide"),
              "example" to Schema.string("Short example sentence")
            )
          )
        ),
        "questions" to Schema.array(
          Schema.obj(
            properties = mapOf(
              "question" to Schema.string("Comprehension question"),
              "options" to Schema.array(Schema.string("4 answer options")),
              "correctIndex" to Schema.integer("Zero-based index of the correct option (0..3)"),
              "explanation" to Schema.string("Short explanation")
            )
          )
        )
      )
    )
  }

  val grammarLessonSchema: Schema by lazy {
    Schema.obj(
      properties = mapOf(
        "topicTitle" to Schema.string("Grammar topic title"),
        "ruleExplanation" to Schema.string("Clear, learner-friendly grammar rule explanation in native language"),
        "examples" to Schema.array(
          Schema.obj(
            properties = mapOf(
              "target" to Schema.string("Example sentence in target language"),
              "native" to Schema.string("Translation in native language"),
              "highlight" to Schema.string("Key grammar structure highlighted")
            )
          )
        ),
        "questions" to Schema.array(
          Schema.obj(
            properties = mapOf(
              "prompt" to Schema.string("Practice question prompt"),
              "options" to Schema.array(Schema.string("4 choices")),
              "correctIndex" to Schema.integer("Correct option index 0..3"),
              "explanation" to Schema.string("Why this answer is correct")
            )
          )
        )
      )
    )
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CORE EXECUTION ENGINE (Timeout, 1 Auto-Retry, Offline, Quota, Caching)
  // ═══════════════════════════════════════════════════════════════════════════

  private fun buildConfiguredModel(
    modelName: String,
    systemInstructionText: String?,
    temperature: Float,
    responseSchema: Schema? = null
  ): GenerativeModel {
    com.example.ensureAppCheckConfiguredIfPending()
    val config = generationConfig {
      this.temperature = temperature
      if (responseSchema != null) {
        this.responseMimeType = "application/json"
        this.responseSchema = responseSchema
      }
    }
    return if (!systemInstructionText.isNullOrBlank()) {
      Firebase.ai.generativeModel(
        modelName = modelName,
        generationConfig = config,
        systemInstruction = content { text(systemInstructionText) }
      )
    } else if (responseSchema != null) {
      Firebase.ai.generativeModel(
        modelName = modelName,
        generationConfig = config
      )
    } else {
      if (modelName == MODEL_FLASH_LITE) defaultLiteModel else defaultFlashModel
    }
  }

  /**
   * Executes a structured JSON Gemini call with:
   * - Local disk+memory cache lookup before hitting the network
   * - Offline connectivity detection
   * - Daily free quota enforcement (Pro = unlimited)
   * - System prompt injection with target/native language + CEFR level
   * - 20s timeout + 1 automatic retry on transient failures
   */
  suspend fun generateStructuredJson(
    userContext: GeminiUserContext,
    roleInstruction: String,
    userPrompt: String,
    responseSchema: Schema,
    cacheKey: String? = null,
    useLiteModel: Boolean = false,
    temperature: Float = 0.45f,
    timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    countTowardQuota: Boolean = true,
    featureCategory: GeminiFeatureCategory = GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT
  ): GeminiResult<JSONObject> = withContext(Dispatchers.IO) {
    if (!cacheKey.isNullOrBlank()) {
      readFromCache(cacheKey)?.let { cachedRaw ->
        runCatching { JSONObject(cachedRaw) }.getOrNull()?.let { cachedObj ->
          return@withContext GeminiResult.Success(cachedObj, fromCache = true)
        }
      }
    }

    if (GeminiTutorService.testSimulateGeminiFailure) {
      return@withContext GeminiResult.Error("Taking a moment — let's try that again.", canRetry = true)
    }

    val fullSystemPrompt = userContext.toSystemPromptHeader(roleInstruction)
    val modelName = if (useLiteModel) MODEL_FLASH_LITE else MODEL_FLASH

    // Honor existing Robolectric/JVM test interceptor if installed
    GeminiTutorService.testResponseInterceptor?.let { interceptor ->
      val intercepted = interceptor.invoke("$fullSystemPrompt\n\n$userPrompt", modelName)?.trim()
      if (!intercepted.isNullOrEmpty()) {
        val parsed = extractJsonObject(intercepted)
        if (parsed != null) {
          if (!cacheKey.isNullOrBlank()) writeToCache(cacheKey, parsed.toString())
          return@withContext GeminiResult.Success(parsed, fromCache = false)
        }
      }
    }

    if (!isNetworkAvailable()) {
      return@withContext GeminiResult.Offline()
    }

    consumeDailyQuotaIfAllowed(countTowardQuota)?.let { return@withContext it }

    var lastErrorMessage = "Taking a moment — let's try that again."
    for (attempt in 1..2) {
      try {
        val model = buildConfiguredModel(
          modelName = modelName,
          systemInstructionText = fullSystemPrompt,
          temperature = temperature,
          responseSchema = responseSchema
        )
        val response = withTimeout(timeoutMs) {
          model.generateContent(userPrompt)
        }
        val rawText = response.text?.trim().orEmpty()
        val parsed = runCatching { JSONObject(rawText) }.getOrNull() ?: extractJsonObject(rawText)
        if (parsed != null) {
          if (!cacheKey.isNullOrBlank()) {
            writeToCache(cacheKey, parsed.toString())
          }
          recordDailyCallConsumed(countTowardQuota)
          GeminiCostAndQuotaManager.recordSuccessfulGeminiCall(
            feature = featureCategory,
            modelTier = if (useLiteModel) GeminiModelTier.FLASH_LITE else GeminiModelTier.FLASH_STANDARD,
            promptText = userPrompt,
            responseText = rawText,
            incrementFeatureCounter = countTowardQuota
          )
          return@withContext GeminiResult.Success(parsed, fromCache = false)
        }
        lastErrorMessage = "Received an incomplete AI response. Tap Retry."
      } catch (e: TimeoutCancellationException) {
        lastErrorMessage = "Request timed out after ${timeoutMs / 1000}s. Check your connection and tap Retry."
        Log.w(TAG, "Structured JSON call timed out (attempt $attempt)", e)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        lastErrorMessage = mapFriendlyErrorMessage(e)
        Log.e(TAG, "Structured JSON call failed (attempt $attempt): ${e.message}", e)
        if (attempt == 1) {
          delay(RETRY_DELAY_MS)
        }
      }
    }

    _quotaUiState.value = _quotaUiState.value.copy(lastErrorMessage = lastErrorMessage)
    GeminiResult.Error(lastErrorMessage, canRetry = true)
  }

  /**
   * Executes a plain text Gemini call with system prompt injection, cache support,
   * connectivity detection, daily quota tracking, 20s timeout, and 1 auto-retry.
   */
  suspend fun generateText(
    userContext: GeminiUserContext,
    roleInstruction: String,
    userPrompt: String,
    cacheKey: String? = null,
    useLiteModel: Boolean = false,
    temperature: Float = 0.6f,
    timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    countTowardQuota: Boolean = true,
    featureCategory: GeminiFeatureCategory = GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT
  ): GeminiResult<String> = withContext(Dispatchers.IO) {
    if (!cacheKey.isNullOrBlank()) {
      readFromCache(cacheKey)?.let { cached ->
        return@withContext GeminiResult.Success(cached, fromCache = true)
      }
    }

    if (GeminiTutorService.testSimulateGeminiFailure) {
      return@withContext GeminiResult.Error("Taking a moment — let's try that again.", canRetry = true)
    }

    val fullSystemPrompt = userContext.toSystemPromptHeader(roleInstruction)
    val modelName = if (useLiteModel) MODEL_FLASH_LITE else MODEL_FLASH

    GeminiTutorService.testResponseInterceptor?.let { interceptor ->
      val intercepted = interceptor.invoke("$fullSystemPrompt\n\n$userPrompt", modelName)?.trim()
      if (!intercepted.isNullOrEmpty()) {
        if (!cacheKey.isNullOrBlank()) writeToCache(cacheKey, intercepted)
        return@withContext GeminiResult.Success(intercepted, fromCache = false)
      }
    }

    if (!isNetworkAvailable()) {
      return@withContext GeminiResult.Offline()
    }

    consumeDailyQuotaIfAllowed(countTowardQuota)?.let { return@withContext it }

    var lastErrorMessage = "Taking a moment — let's try that again."
    for (attempt in 1..2) {
      try {
        val model = buildConfiguredModel(
          modelName = modelName,
          systemInstructionText = fullSystemPrompt,
          temperature = temperature
        )
        val response = withTimeout(timeoutMs) {
          model.generateContent(userPrompt)
        }
        val text = response.text?.trim()
        if (!text.isNullOrEmpty()) {
          if (!cacheKey.isNullOrBlank()) writeToCache(cacheKey, text)
          recordDailyCallConsumed(countTowardQuota)
          GeminiCostAndQuotaManager.recordSuccessfulGeminiCall(
            feature = featureCategory,
            modelTier = if (useLiteModel) GeminiModelTier.FLASH_LITE else GeminiModelTier.FLASH_STANDARD,
            promptText = userPrompt,
            responseText = text,
            incrementFeatureCounter = countTowardQuota
          )
          return@withContext GeminiResult.Success(text, fromCache = false)
        }
      } catch (e: TimeoutCancellationException) {
        lastErrorMessage = "Taking a moment — check your connection and try again."
        Log.w(TAG, "Text call timed out (attempt $attempt)", e)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        lastErrorMessage = mapFriendlyErrorMessage(e)
        Log.e(TAG, "Text call failed (attempt $attempt): ${e.message}", e)
        if (attempt == 1) delay(RETRY_DELAY_MS)
      }
    }

    _quotaUiState.value = _quotaUiState.value.copy(lastErrorMessage = lastErrorMessage)
    GeminiResult.Error(lastErrorMessage, canRetry = true)
  }

  /**
   * Streams AI tutor text tokens progressively for chat and long-form tutor responses.
   */
  fun streamText(
    userContext: GeminiUserContext,
    roleInstruction: String,
    userPrompt: String,
    temperature: Float = 0.65f,
    countTowardQuota: Boolean = true
  ): Flow<String> = flow {
    val fullSystemPrompt = userContext.toSystemPromptHeader(roleInstruction)
    GeminiTutorService.testResponseInterceptor?.let { interceptor ->
      val intercepted = interceptor.invoke("$fullSystemPrompt\n\n$userPrompt", MODEL_FLASH)
      if (!intercepted.isNullOrBlank()) {
        emit(intercepted)
        return@flow
      }
    }
    if (!isNetworkAvailable()) {
      throw IllegalStateException("You're offline. Check your connection and try again.")
    }
    consumeDailyQuotaIfAllowed(countTowardQuota)?.let { quota ->
      throw IllegalStateException(quota.message)
    }

    val model = buildConfiguredModel(
      modelName = MODEL_FLASH,
      systemInstructionText = fullSystemPrompt,
      temperature = temperature
    )
    var accumulated = ""
    model.generateContentStream(userPrompt).collect { chunk ->
      val delta = chunk.text.orEmpty()
      if (delta.isNotEmpty()) {
        accumulated += delta
        emit(accumulated)
      }
    }
    if (accumulated.isNotBlank()) {
      recordDailyCallConsumed(countTowardQuota)
    }
  }.flowOn(Dispatchers.IO)

  /**
   * Analyzes an image bitmap using AI Vision with a strict JSON schema.
   * Automatically scales large bitmaps down to <= 1024px to keep payloads fast and well under 20MB.
   */
  suspend fun analyzeImageStructured(
    bitmap: Bitmap,
    userContext: GeminiUserContext,
    roleInstruction: String,
    userPrompt: String,
    responseSchema: Schema = cameraSceneSchema,
    timeoutMs: Long = MULTIMODAL_TIMEOUT_MS,
    countTowardQuota: Boolean = true
  ): GeminiResult<JSONObject> = withContext(Dispatchers.IO) {
    if (GeminiTutorService.testSimulateGeminiFailure) {
      return@withContext GeminiResult.Error("Taking a moment — let's try that again.", canRetry = true)
    }
    if (!isNetworkAvailable()) {
      return@withContext GeminiResult.Offline("You're offline. Reconnect to analyze photos.")
    }
    consumeDailyQuotaIfAllowed(countTowardQuota)?.let { return@withContext it }

    val scaledBitmap = scaleBitmapForVision(bitmap, maxDimension = 1024)
    val fullSystemPrompt = userContext.toSystemPromptHeader(roleInstruction)
    var lastError = "Couldn't analyze that photo right now. Tap Retry."

    for (attempt in 1..2) {
      try {
        val model = buildConfiguredModel(
          modelName = MODEL_FLASH,
          systemInstructionText = fullSystemPrompt,
          temperature = 0.35f,
          responseSchema = responseSchema
        )
        val response = withTimeout(timeoutMs) {
          model.generateContent(
            content {
              image(scaledBitmap)
              text(userPrompt)
            }
          )
        }
        val raw = response.text?.trim().orEmpty()
        val parsed = runCatching { JSONObject(raw) }.getOrNull() ?: extractJsonObject(raw)
        if (parsed != null) {
          recordDailyCallConsumed(countTowardQuota)
          GeminiCostAndQuotaManager.recordSuccessfulGeminiCall(
            feature = GeminiFeatureCategory.INTELLIGENT_STUDIO,
            modelTier = GeminiModelTier.FLASH_STANDARD,
            promptText = userPrompt,
            responseText = raw,
            incrementFeatureCounter = countTowardQuota
          )
          return@withContext GeminiResult.Success(parsed, fromCache = false)
        }
      } catch (e: TimeoutCancellationException) {
        lastError = "Vision analysis timed out after ${timeoutMs / 1000}s. Tap Retry."
        Log.w(TAG, "Vision call timed out (attempt $attempt)", e)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        lastError = mapFriendlyErrorMessage(e)
        Log.e(TAG, "Vision call failed (attempt $attempt): ${e.message}", e)
        if (attempt == 1) delay(RETRY_DELAY_MS)
      }
    }

    _quotaUiState.value = _quotaUiState.value.copy(lastErrorMessage = lastError)
    GeminiResult.Error(lastError, canRetry = true)
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HIGH-LEVEL DOMAIN METHODS (Voice Turn Feedback, CEFR Lessons & Pre-Gen)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Generates a real-time spoken tutor reply AND structured post-turn feedback
   * (`pronunciationScore` 0..100, `grammarFix`, `naturalPhrase`) in one low-latency Gemini call.
   */
  suspend fun generateVoiceTurnWithFeedback(
    userContext: GeminiUserContext,
    partnerName: String,
    partnerPersona: String,
    topic: String,
    userUtterance: String,
    recentHistory: List<Pair<String, String>>,
    regionalAccent: String = "Standard"
  ): GeminiResult<StructuredVoiceTurnPayload> {
    val historyText = recentHistory.takeLast(6).joinToString("\n") { (role, msg) -> "$role: $msg" }
    val roleInstruction = """
      You are $partnerName, a warm, natural ${userContext.targetLanguage.name} AI conversation tutor ($partnerPersona).
      Regional accent / dialect: $regionalAccent.
      Topic / Mode: $topic.
      Keep your spoken reply conversational (1-3 concise sentences) and ask an engaging follow-up question.
      Evaluate the learner's utterance honestly:
      - pronunciationScore: integer 55..100 estimating clarity, fluency, and naturalness of their phrasing (higher for clean natural phrasing, lower for broken grammar or native-language mixing).
      - grammarFix: 1 concise grammar fix explaining what to adjust (or empty string if grammatically correct).
      - naturalPhrase: 1 more natural native-speaker way to say what they meant in ${userContext.targetLanguage.name} (or empty string if already natural).
    """.trimIndent()

    val prompt = buildString {
      if (historyText.isNotBlank()) {
        appendLine("Recent conversation:")
        appendLine(historyText)
        appendLine()
      }
      appendLine("Learner just said: \"$userUtterance\"")
      append("Respond as $partnerName and return the structured JSON feedback.")
    }

    return when (
      val res = generateStructuredJson(
        userContext = userContext,
        roleInstruction = roleInstruction,
        userPrompt = prompt,
        responseSchema = voiceTurnFeedbackSchema,
        useLiteModel = false,
        temperature = 0.65f,
        featureCategory = GeminiFeatureCategory.AI_VOICE_TUTOR
      )
    ) {
      is GeminiResult.Success -> {
        val obj = res.data
        val reply = obj.optString("reply").trim()
        if (reply.isBlank()) {
          GeminiResult.Error("Tutor reply was empty. Tap Retry.", canRetry = true)
        } else {
          GeminiResult.Success(
            StructuredVoiceTurnPayload(
              reply = reply,
              phonetics = obj.optString("phonetics").trim(),
              translation = obj.optString("translation").trim(),
              pronunciationScore = obj.optInt("pronunciationScore", 86).coerceIn(0, 100),
              grammarFix = obj.optString("grammarFix").trim().takeIf { it.isNotBlank() && !it.equals("NONE", true) },
              naturalPhrase = obj.optString("naturalPhrase").trim().takeIf { it.isNotBlank() && !it.equals("NONE", true) }
            ),
            fromCache = res.fromCache
          )
        }
      }
      is GeminiResult.Offline -> res
      is GeminiResult.QuotaExceeded -> res
      is GeminiResult.Error -> res
    }
  }

  /**
   * Generates a complete 5-minute CEFR lesson (multiple choice, fill-in-the-blank, speak-the-sentence,
   * and match exercises) with structured JSON and caches it locally so it works offline and is never billed twice.
   */
  suspend fun generateCefrLessonStructured(
    userContext: GeminiUserContext,
    topic: String,
    lessonIdOverride: String? = null,
    countTowardQuota: Boolean = true
  ): GeminiResult<CurriculumLesson> {
    val cleanTopic = topic.trim().ifBlank { "Everyday Conversation" }
    val cacheKey = "cefr_lesson_${userContext.targetLanguageCode}_${userContext.nativeLanguageCode}_${userContext.cefrLevel}_${cleanTopic.lowercase()}"
    val roleInstruction = """
      You are an expert CEFR curriculum designer creating a 5-minute interactive lesson in ${userContext.targetLanguage.name}.
      Include 4 distinct exercises in this exact order:
      1. MULTIPLE_CHOICE (vocabulary or comprehension with 4 options)
      2. FILL_IN_THE_BLANK (complete the sentence with 4 options)
      3. SPEAK_THE_SENTENCE (learner speaks a practical target sentence aloud)
      4. MATCH (4 target-to-native pairs formatted as 'TargetPhrase = NativeMeaning' inside options)
      Also include 4 key vocabulary items to save to flashcards.
    """.trimIndent()

    val prompt = "Build a CEFR ${userContext.cefrLevel} ${userContext.targetLanguage.name} lesson on topic: \"$cleanTopic\"."

    return when (
      val res = generateStructuredJson(
        userContext = userContext,
        roleInstruction = roleInstruction,
        userPrompt = prompt,
        responseSchema = cefrLessonSchema,
        cacheKey = cacheKey,
        useLiteModel = false,
        temperature = 0.5f,
        timeoutMs = MULTIMODAL_TIMEOUT_MS,
        countTowardQuota = countTowardQuota,
        featureCategory = GeminiFeatureCategory.BASIC_LESSON_QUIZ_CHAT
      )
    ) {
      is GeminiResult.Success -> {
        val lesson = parseCefrLessonJson(
          json = res.data,
          userContext = userContext,
          topic = cleanTopic,
          lessonIdOverride = lessonIdOverride
        )
        if (lesson != null) {
          GeminiResult.Success(lesson, fromCache = res.fromCache)
        } else {
          GeminiResult.Error("Couldn't build the lesson exercises. Tap Retry.")
        }
      }
      is GeminiResult.Offline -> res
      is GeminiResult.QuotaExceeded -> res
      is GeminiResult.Error -> res
    }
  }

  /**
   * Pre-generates and caches the next CEFR lesson in the background so opening it is instant
   * and available offline. Does not count against the user's manual quota or trigger paywall popups.
   */
  fun pregenerateNextLessonInBackground(
    userContext: GeminiUserContext,
    nextLesson: CurriculumLesson
  ) {
    if (!isVerifiedProActive() && !testBypassDailyQuota) return
    val key = "pregen_${nextLesson.id}_${userContext.targetLanguageCode}_${userContext.cefrLevel}"
    if (!inFlightPregenKeys.add(key)) return
    backgroundScope.launch {
      try {
        if (!isNetworkAvailable()) return@launch
        val cacheKey = "cefr_lesson_${userContext.targetLanguageCode}_${userContext.nativeLanguageCode}_${nextLesson.cefrLevel}_${nextLesson.title.lowercase()}"
        if (readFromCache(cacheKey) != null) return@launch
        generateCefrLessonStructured(
          userContext = userContext.copy(cefrLevel = nextLesson.cefrLevel),
          topic = nextLesson.title,
          lessonIdOverride = nextLesson.id,
          countTowardQuota = false
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.d(TAG, "Background lesson pre-generation skipped: ${e.message}")
      } finally {
        inFlightPregenKeys.remove(key)
      }
    }
  }

  private fun parseCefrLessonJson(
    json: JSONObject,
    userContext: GeminiUserContext,
    topic: String,
    lessonIdOverride: String?
  ): CurriculumLesson? {
    val exArray = json.optJSONArray("exercises") ?: JSONArray()
    val exercises = mutableListOf<Exercise>()
    for (i in 0 until exArray.length()) {
      val item = exArray.optJSONObject(i) ?: continue
      val typeStr = item.optString("exerciseType").uppercase(Locale.US)
      val mappedType = when {
        typeStr.contains("SPEAK") -> ExerciseType.SPEAKING
        typeStr.contains("FILL") -> ExerciseType.GRAMMAR
        typeStr.contains("MATCH") -> ExerciseType.VOCABULARY
        typeStr.contains("LISTEN") -> ExerciseType.LISTENING
        typeStr.contains("WRITE") -> ExerciseType.WRITING
        else -> ExerciseType.VOCABULARY
      }
      val rawOptions = mutableListOf<String>()
      val optArr = item.optJSONArray("options")
      if (optArr != null) {
        for (j in 0 until optArr.length()) {
          optArr.optString(j).trim().takeIf { it.isNotBlank() }?.let { rawOptions.add(it) }
        }
      }
      val correctAnswer = item.optString("correctAnswer").trim()
      val options = when {
        rawOptions.isEmpty() && correctAnswer.isNotBlank() -> listOf(correctAnswer)
        correctAnswer.isNotBlank() && rawOptions.none { it.equals(correctAnswer, ignoreCase = true) } ->
          (listOf(correctAnswer) + rawOptions.take(3))
        else -> rawOptions
      }
      val correctIdx = options.indexOfFirst { it.equals(correctAnswer, ignoreCase = true) }.coerceAtLeast(0)
      exercises.add(
        Exercise(
          id = "gemini_ex_${lessonIdOverride ?: topic.hashCode()}_$i",
          type = mappedType,
          prompt = item.optString("prompt").ifBlank { "Practice $topic (${userContext.cefrLevel})" },
          explanationNative = item.optString("explanation").ifBlank { "Great practice for CEFR ${userContext.cefrLevel}." },
          targetAudioText = item.optString("targetPhrase").ifBlank { correctAnswer },
          phoneticGuide = item.optString("phonetic"),
          options = options,
          correctOptionIndex = correctIdx,
          acceptableAnswers = listOfNotNull(correctAnswer.takeIf { it.isNotBlank() })
        )
      )
    }

    if (exercises.isEmpty()) return null

    val profMapped = when (userContext.cefrLevel.uppercase(Locale.US)) {
      "A1", "A2" -> "Beginner"
      "B1", "B2" -> "Intermediate"
      else -> "Advanced"
    }

    return CurriculumLesson(
      id = lessonIdOverride ?: "ai_cefr_${userContext.cefrLevel.lowercase()}_${System.currentTimeMillis()}",
      title = json.optString("title").ifBlank { topic },
      description = json.optString("description").ifBlank { "Interactive CEFR ${userContext.cefrLevel} ${userContext.targetLanguage.name} lesson" },
      category = json.optString("category").ifBlank { "Speaking" },
      proficiencyLevel = profMapped,
      cefrLevel = userContext.cefrLevel.uppercase(Locale.US),
      targetLanguageCode = userContext.targetLanguageCode,
      estimatedMinutes = json.optInt("estimatedMinutes", 5).coerceIn(3, 15),
      xpReward = json.optInt("xpReward", 40).coerceIn(20, 100),
      exercises = exercises
    )
  }

  private fun scaleBitmapForVision(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap
    val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
    val newWidth = (width * ratio).toInt().coerceAtLeast(1)
    val newHeight = (height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
  }

  internal fun extractJsonObject(raw: String): JSONObject? {
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    if (start == -1 || end <= start) return null
    return runCatching { JSONObject(raw.substring(start, end + 1)) }.getOrNull()
  }

  private fun mapFriendlyErrorMessage(e: Throwable): String {
    return AiErrorHandler.handle(throwable = e, contextTag = "GeminiService").userMessage
  }
}
