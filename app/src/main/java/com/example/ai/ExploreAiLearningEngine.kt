package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.R
import com.example.data.model.Language
import com.example.data.model.RepeatedMistakeSummary
import com.example.data.model.UserProfile
import com.example.data.repository.OfflineEmergencyPhrase
import com.example.data.repository.OfflineEmergencyPhrasePack
import com.example.data.repository.OfflineEmergencyPhraseRepository
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

// =============================================================================
// SHARED EXPLORE AI RESULT & LEARNER CONTEXT
// =============================================================================

sealed class ExploreAiResult<out T> {
  data class Success<T>(val data: T) : ExploreAiResult<T>()
  data class Error(val message: String, val canRetry: Boolean = true) : ExploreAiResult<Nothing>()
  data object Offline : ExploreAiResult<Nothing>()
  data class QuotaExceeded(val message: String) : ExploreAiResult<Nothing>()
}

data class ExploreLearnerContext(
  val targetLanguageCode: String,
  val targetLanguageName: String,
  val nativeLanguageCode: String,
  val nativeLanguageName: String,
  val cefrLevel: String,
  val learningGoals: List<String> = emptyList(),
  val recentMistakePatterns: List<String> = emptyList(),
  val completedLessonsCount: Int = 0,
  val isPro: Boolean = false
) {
  companion object {
    fun fromProfile(
      profile: UserProfile,
      learningGoals: List<String> = emptyList(),
      recentMistakes: List<RepeatedMistakeSummary> = emptyList()
    ): ExploreLearnerContext {
      val target = Language.findByCode(profile.targetLanguageCode)
      val native = Language.findByCode(profile.nativeLanguageCode.ifBlank { "en" })
      val cefr = when {
        profile.proficiencyLevel.contains("C2", true) -> "C2"
        profile.proficiencyLevel.contains("C1", true) -> "C1"
        profile.proficiencyLevel.contains("B2", true) -> "B2"
        profile.proficiencyLevel.contains("B1", true) -> "B1"
        profile.proficiencyLevel.contains("A2", true) -> "A2"
        profile.proficiencyLevel.contains("A1", true) -> "A1"
        profile.proficiencyLevel.equals("Advanced", true) -> "C1"
        profile.proficiencyLevel.equals("Intermediate", true) -> "B1"
        else -> "A2"
      }
      return ExploreLearnerContext(
        targetLanguageCode = target.code,
        targetLanguageName = target.name,
        nativeLanguageCode = native.code,
        nativeLanguageName = native.name,
        cefrLevel = cefr,
        learningGoals = learningGoals.take(4),
        recentMistakePatterns = recentMistakes.take(5).map {
          "${it.category}: '${it.exampleWrong}' -> '${it.exampleCorrected}'"
        },
        completedLessonsCount = profile.completedLessonIds.size,
        isPro = profile.isEntitlementVerified
      )
    }
  }
}

data class CapturedMistakeInfo(
  val category: String,
  val originalText: String,
  val correctedText: String,
  val explanation: String
)

// =============================================================================
// FEATURE DATA MODELS (DYNAMIC AI-GENERATED, STRUCTURED & VALIDATED)
// =============================================================================

data class DynamicImmersionMission(
  val id: String,
  val title: String,
  val objectiveDescription: String,
  val completed: Boolean = false
)

data class DynamicImmersionScene(
  val sceneId: String,
  val locationType: String,
  val placeNameTarget: String,
  val placeNameNative: String,
  val cityAndNeighborhood: String,
  val characterName: String,
  val characterRole: String,
  val characterPersonality: String,
  val preferredVoiceGender: String, // "female" | "male"
  val cefrLevel: String,
  val situationBriefNative: String,
  val openingGreetingTarget: String,
  val openingGreetingNative: String,
  val missions: List<DynamicImmersionMission>,
  val starterHintsTarget: List<String>
)

data class DynamicImmersionTurnOutcome(
  val characterReplyTarget: String,
  val characterReplyNative: String,
  val naturalnessScore: Int,
  val trustDelta: Int,
  val coachingFeedback: String,
  val grammarCorrection: String,
  val vocabularyTip: String,
  val completedMissionId: String?,
  val newlyRememberedFact: String?,
  val capturedMistake: CapturedMistakeInfo?
)

data class CameraLensVocabEntry(
  val wordTarget: String,
  val phonetic: String,
  val meaningNative: String,
  val exampleSentenceTarget: String,
  val exampleSentenceNative: String
)

data class CameraLensSceneAnalysis(
  val sceneTitle: String,
  val visibleObjects: List<String>,
  val sceneSummaryNative: String,
  val vocabulary: List<CameraLensVocabEntry>,
  val usefulPhrasesTarget: List<Pair<String, String>>,
  val describePromptNative: String,
  val starterQuestionTarget: String,
  val starterQuestionNative: String
)

data class CameraDescriptionFeedback(
  val relevanceScore: Int,
  val grammarScore: Int,
  val groundedAssessment: String,
  val improvedDescriptionTarget: String,
  val followUpQuestionTarget: String,
  val followUpQuestionNative: String,
  val capturedMistake: CapturedMistakeInfo?
)

data class DynamicDebateTopic(
  val id: String,
  val titleTarget: String,
  val titleNative: String,
  val contextBriefNative: String,
  val proPositionNative: String,
  val conPositionNative: String,
  val cefrLevel: String,
  val suggestedConnectorsTarget: List<String>
)

data class DebateTurnEntry(
  val id: String,
  val speaker: String, // "learner" | "ai"
  val textTarget: String,
  val translationNative: String = "",
  val spokenViaMic: Boolean = false,
  val roundNumber: Int = 1
)

data class DebateTurnResponse(
  val counterArgumentTarget: String,
  val counterArgumentNative: String,
  val quickCoachingNote: String,
  val followUpChallengeTarget: String,
  val capturedMistake: CapturedMistakeInfo?
)

data class DebateRubricEvaluation(
  val overallScore: Int,
  val grammarScore: Int,
  val vocabularyScore: Int,
  val clarityScore: Int,
  val argumentStructureScore: Int,
  val connectorsUsed: List<String>,
  val counterargumentHandlingNote: String,
  val fluencyScoreIfSpoken: Int?, // null if typed-only — honest evaluation
  val fluencyHonestyNote: String,
  val strengths: List<String>,
  val improvementTips: List<String>,
  val upgradedExampleTarget: String,
  val capturedMistakes: List<CapturedMistakeInfo>
)

data class DynamicRepairScenario(
  val id: String,
  val breakdownType: String, // Fast speech, Unfamiliar vocabulary, Regional accent, Misunderstood instructions, Asking to repeat, Politely correcting, Confirming information
  val settingTitle: String,
  val speakerName: String,
  val speakerRole: String,
  val preferredVoiceGender: String,
  val cefrLevel: String,
  val breakdownPromptTarget: String,
  val breakdownPromptNative: String,
  val challengeNoteNative: String,
  val repairGoalNative: String,
  val suggestedRepairStartersTarget: List<String>
)

data class DynamicRepairTurnEvaluation(
  val repairedSuccessfully: Boolean,
  val score: Int,
  val nextDifficultyHint: String,
  val speakerFollowUpTarget: String,
  val speakerFollowUpNative: String,
  val feedbackNative: String,
  val naturalRepairModelTarget: String,
  val capturedMistake: CapturedMistakeInfo?
)

data class ThreeWayExplanationResult(
  val conceptTitle: String,
  val categoryLabel: String,
  val beginnerExplanationNative: String,
  val beginnerExampleTarget: String,
  val beginnerExampleNative: String,
  val intermediateExplanationNative: String,
  val intermediateExampleTarget: String,
  val intermediateExampleNative: String,
  val advancedExplanationNative: String,
  val advancedExampleTarget: String,
  val advancedExampleNative: String,
  val commonPitfallNative: String,
  val miniPracticePromptNative: String,
  val miniPracticeExpectedTarget: String,
  val miniPracticeHintNative: String
)

data class ExplanationMiniPracticeEvaluation(
  val isCorrect: Boolean,
  val score: Int,
  val feedbackNative: String,
  val modelAnswerTarget: String,
  val capturedMistake: CapturedMistakeInfo?
)

data class DynamicDetectiveCase(
  val id: String,
  val pragmaticType: String, // Sarcasm, Politeness, Hesitation, Indirect refusal, Uncertainty, Humor, Context-dependent
  val settingTitle: String,
  val speakerName: String,
  val preferredVoiceGender: String,
  val contextBriefNative: String,
  val literalPhraseTarget: String,
  val literalTranslationNative: String,
  val interpretationOptions: List<String>,
  val recommendedOptionIndex: Int,
  val nuancedCulturalNote: String
)

data class DetectiveResponseEvaluation(
  val understoodNuance: Boolean,
  val score: Int,
  val feedbackNative: String,
  val pragmaticExplanationNative: String,
  val naturalResponseModelTarget: String,
  val speakerContinuationTarget: String,
  val speakerContinuationNative: String,
  val capturedMistake: CapturedMistakeInfo?
)

data class DynamicMistakeDrill(
  val id: String,
  val mistakeId: String,
  val category: String,
  val promptNative: String,
  val flawedSentenceTarget: String,
  val expectedCorrectedTarget: String,
  val ruleExplanationNative: String
)

data class MistakeDrillEvaluation(
  val passed: Boolean,
  val score: Int,
  val feedbackNative: String,
  val correctedModelTarget: String
)

data class ExploreSessionRecord(
  val sessionId: String,
  val featureType: String,
  val languageCode: String,
  val title: String,
  val summary: String,
  val cefrLevel: String,
  val turnsCount: Int,
  val scorePercent: Int,
  val payloadJson: String = "",
  val updatedAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Shared global AI Learning Engine for all LingoSphere Explore features.
 * Powered exclusively by Firebase AI Logic (`Firebase.ai` / `gemini-3.8-flash`) with
 * strict structured JSON schemas, learner context personalization, honest error handling
 * (no silent mock fallbacks on failure), and user-isolated Firestore persistence.
 */
object ExploreAiLearningEngine {

  private const val TAG = "ExploreAiLearningEngine"
  private const val MODEL_NAME = "gemini-3.8-flash"
  private const val TIMEOUT_MS = 45_000L
  private const val LOCAL_SESSIONS_PREFS = "lingosphere_explore_sessions_v1"

  private val IMMERSION_LOCATION_POOL = listOf(
    "Neighborhood Bakery & Café",
    "International Airport Check-In & Gate",
    "Boutique Hotel Front Desk",
    "Clinic / Hospital Reception & Doctor Visit",
    "Local Electronics & Clothing Shop",
    "Modern Coworking Office & Team Standup",
    "City Train Station Ticket & Platform Desk",
    "Local Pharmacy & Health Consultation",
    "Neighborhood Farmers Market",
    "Apartment Rental & Neighborhood Introduction"
  )

  private val REPAIR_BREAKDOWN_POOL = listOf(
    "Fast natural speech",
    "Unfamiliar vocabulary or idiom",
    "Regional accent & connected speech",
    "Misunderstood multi-step instructions",
    "Asking the speaker to repeat or slow down",
    "Politely correcting a misunderstanding",
    "Confirming key reservation or appointment details"
  )

  private val DETECTIVE_PRAGMATIC_POOL = listOf(
    "Polite indirect refusal",
    "Playful sarcasm or understatement",
    "Hesitation and diplomatic softening",
    "Uncertain or hedged agreement",
    "Light conversational humor",
    "Polite boundary setting",
    "Context-dependent everyday expression"
  )

  // ===========================================================================
  // CORE STRUCTURED AI INVOCATION (NO HARDCODED KEYS, NO MOCK FALLBACKS)
  // ===========================================================================

  fun isNetworkConnected(context: Context): Boolean {
    return try {
      val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
      val network = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
      true
    }
  }

  private fun learnerSystemInstruction(ctx: ExploreLearnerContext, roleSpecific: String): String {
    val goalsStr = if (ctx.learningGoals.isEmpty()) "general everyday fluency"
    else ctx.learningGoals.joinToString("; ")
    val mistakesStr = if (ctx.recentMistakePatterns.isEmpty()) "none logged yet"
    else ctx.recentMistakePatterns.joinToString("; ")
    return """
      You are LingoSphere's expert language-learning engine.
      Learner profile:
      - Target language: ${ctx.targetLanguageName} (${ctx.targetLanguageCode})
      - Native / explanation language: ${ctx.nativeLanguageName} (${ctx.nativeLanguageCode})
      - Proficiency: CEFR ${ctx.cefrLevel} (completed lessons: ${ctx.completedLessonsCount})
      - Learner goals: $goalsStr
      - Recent recurring mistakes to reinforce when relevant: $mistakesStr
      Strict pedagogical rules:
      - All target-language dialogue, phrases, and examples MUST be genuinely written in ${ctx.targetLanguageName} (accurate script, grammar, and natural usage).
      - All explanations, instructions, and translations MUST be written in ${ctx.nativeLanguageName}.
      - Match vocabulary and sentence complexity to CEFR ${ctx.cefrLevel}.
      - Never fabricate scores or claim to hear audio acoustics when only a transcript is provided.
      - Return ONLY valid JSON matching the provided schema.
      $roleSpecific
    """.trimIndent()
  }

  private suspend fun runStructuredJsonRequest(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    systemInstruction: String,
    prompt: String,
    schema: Schema,
    temperature: Float = 0.65f,
    bitmap: Bitmap? = null
  ): ExploreAiResult<JSONObject> = withContext(Dispatchers.IO) {
    if (!isNetworkConnected(context)) {
      return@withContext ExploreAiResult.Offline
    }
    if (!learnerCtx.isPro && !GeminiService.testBypassDailyQuota) {
      val msg = "Unlock AI learning with LingoSphere Pro."
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.AI_UPGRADE,
        featureKey = "ai.labs",
        headline = "Unlock AI learning",
        reason = msg
      )
      return@withContext ExploreAiResult.QuotaExceeded(msg)
    }

    var lastErrorMessage = "Taking a moment — let's try that again."
    for (attempt in 0..1) {
      try {
        com.example.ensureAppCheckConfiguredIfPending()
        val model = Firebase.ai.generativeModel(
          modelName = MODEL_NAME,
          generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = schema
            this.temperature = temperature
          },
          systemInstruction = content { text(systemInstruction) }
        )

        val response = withTimeout(TIMEOUT_MS) {
          if (bitmap != null) {
            model.generateContent(
              content {
                image(bitmap)
                text(prompt)
              }
            )
          } else {
            model.generateContent(prompt)
          }
        }

        val rawText = response.text?.trim().orEmpty()
        if (rawText.isBlank()) {
          lastErrorMessage = "Taking a moment — let's try that again."
        } else {
          val parsed = parseStrictJsonObject(rawText)
          if (parsed != null) {
            return@withContext ExploreAiResult.Success(parsed)
          } else {
            lastErrorMessage = "Taking a moment — let's try that again."
          }
        }
      } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        lastErrorMessage = AiErrorHandler.handle(throwable = e, contextTag = "ExploreAi").message
        Log.w(TAG, "Attempt ${attempt + 1} timed out", e)
      } catch (e: Exception) {
        val msg = e.message.orEmpty()
        Log.w(TAG, "Attempt ${attempt + 1} failed: $msg", e)
        val uiErr = AiErrorHandler.handle(throwable = e, contextTag = "ExploreAi")
        if (uiErr.isOffline) {
          return@withContext ExploreAiResult.Offline
        }
        lastErrorMessage = uiErr.message
      }
      if (attempt == 0) delay(900L)
    }
    ExploreAiResult.Error(lastErrorMessage, canRetry = true)
  }

  internal fun parseStrictJsonObject(raw: String): JSONObject? {
    val cleaned = raw
      .removePrefix("```json")
      .removePrefix("```")
      .removeSuffix("```")
      .trim()
    return try {
      JSONObject(cleaned)
    } catch (_: Exception) {
      val start = cleaned.indexOf('{')
      val end = cleaned.lastIndexOf('}')
      if (start >= 0 && end > start) {
        runCatching { JSONObject(cleaned.substring(start, end + 1)) }.getOrNull()
      } else {
        null
      }
    }
  }

  private fun parseStringList(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return buildList {
      for (i in 0 until arr.length()) {
        val s = arr.optString(i, "").trim()
        if (s.isNotBlank()) add(s)
      }
    }
  }

  private fun parseMistakeFromJson(obj: JSONObject?): CapturedMistakeInfo? {
    if (obj == null) return null
    val hasMistake = obj.optBoolean("hasGenuineMistake", false)
    val orig = obj.optString("originalText", "").trim()
    val corr = obj.optString("correctedText", "").trim()
    val cat = obj.optString("category", "Grammar").trim().ifBlank { "Grammar" }
    val exp = obj.optString("explanation", "").trim()
    if (!hasMistake || orig.isBlank() || corr.isBlank() || orig.equals(corr, ignoreCase = true)) {
      return null
    }
    return CapturedMistakeInfo(
      category = cat,
      originalText = orig,
      correctedText = corr,
      explanation = exp.ifBlank { "More natural ${cat.lowercase(Locale.US)} phrasing." }
    )
  }

  private fun mistakeSchema(): Schema = Schema.obj(
    mapOf(
      "hasGenuineMistake" to Schema.boolean(),
      "category" to Schema.string(),
      "originalText" to Schema.string(),
      "correctedText" to Schema.string(),
      "explanation" to Schema.string()
    )
  )

  // ===========================================================================
  // 1. LIVING IMMERSION MODE — DYNAMIC LOCATIONS, CHARACTERS & MULTI-TURN MEMORY
  // ===========================================================================

  suspend fun generateDynamicImmersionScene(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    requestedLocationType: String? = null,
    avoidSceneTitles: List<String> = emptyList()
  ): ExploreAiResult<DynamicImmersionScene> {
    val chosenType = requestedLocationType?.takeIf { it.isNotBlank() }
      ?: IMMERSION_LOCATION_POOL.filterNot { poolItem ->
        avoidSceneTitles.any { it.contains(poolItem.take(8), ignoreCase = true) }
      }.randomOrNull()
      ?: IMMERSION_LOCATION_POOL.random()

    val schema = Schema.obj(
      mapOf(
        "locationType" to Schema.string(),
        "placeNameTarget" to Schema.string(),
        "placeNameNative" to Schema.string(),
        "cityAndNeighborhood" to Schema.string(),
        "characterName" to Schema.string(),
        "characterRole" to Schema.string(),
        "characterPersonality" to Schema.string(),
        "preferredVoiceGender" to Schema.enumeration(listOf("female", "male")),
        "situationBriefNative" to Schema.string(),
        "openingGreetingTarget" to Schema.string(),
        "openingGreetingNative" to Schema.string(),
        "missions" to Schema.array(
          Schema.obj(
            mapOf(
              "id" to Schema.string(),
              "title" to Schema.string(),
              "objectiveDescription" to Schema.string()
            )
          )
        ),
        "starterHintsTarget" to Schema.array(Schema.string())
      )
    )

    val avoidNote = if (avoidSceneTitles.isNotEmpty()) {
      "Do NOT repeat these recent places/characters: ${avoidSceneTitles.takeLast(6).joinToString(", ")}."
    } else ""

    val prompt = """
      Generate a fresh, realistic Living Immersion scene in ${learnerCtx.targetLanguageName} for a CEFR ${learnerCtx.cefrLevel} learner.
      Required setting category: $chosenType.
      $avoidNote
      Create a culturally authentic local character (unique first name, realistic role and personality), an authentic city & neighborhood where ${learnerCtx.targetLanguageName} is spoken, a natural opening greeting in ${learnerCtx.targetLanguageName} (with ${learnerCtx.nativeLanguageName} translation), 3 concrete interactive conversation missions achievable in 1-2 turns each, and 3 helpful starter phrases in ${learnerCtx.targetLanguageName}.
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(
        learnerCtx,
        "Create diverse, culturally grounded real-world immersion environments."
      ),
      prompt = prompt,
      schema = schema,
      temperature = 0.85f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        val missionsArr = j.optJSONArray("missions")
        val missions = buildList {
          if (missionsArr != null) {
            for (i in 0 until missionsArr.length()) {
              val m = missionsArr.optJSONObject(i) ?: continue
              val title = m.optString("title", "").trim()
              if (title.isNotBlank()) {
                add(
                  DynamicImmersionMission(
                    id = m.optString("id", "m_${i + 1}").ifBlank { "m_${i + 1}" },
                    title = title,
                    objectiveDescription = m.optString("objectiveDescription", title).trim()
                  )
                )
              }
            }
          }
        }
        val greeting = j.optString("openingGreetingTarget", "").trim()
        val place = j.optString("placeNameTarget", "").trim()
        val charName = j.optString("characterName", "").trim()
        if (greeting.isBlank() || place.isBlank() || charName.isBlank()) {
          ExploreAiResult.Error("Generated scene was incomplete. Tap Retry to create a new scene.")
        } else {
          ExploreAiResult.Success(
            DynamicImmersionScene(
              sceneId = "imm_${learnerCtx.targetLanguageCode}_${System.currentTimeMillis()}",
              locationType = j.optString("locationType", chosenType).ifBlank { chosenType },
              placeNameTarget = place,
              placeNameNative = j.optString("placeNameNative", place),
              cityAndNeighborhood = j.optString("cityAndNeighborhood", ""),
              characterName = charName,
              characterRole = j.optString("characterRole", "Local host"),
              characterPersonality = j.optString("characterPersonality", "Warm and observant"),
              preferredVoiceGender = j.optString("preferredVoiceGender", "female"),
              cefrLevel = learnerCtx.cefrLevel,
              situationBriefNative = j.optString("situationBriefNative", ""),
              openingGreetingTarget = greeting,
              openingGreetingNative = j.optString("openingGreetingNative", ""),
              missions = missions,
              starterHintsTarget = parseStringList(j.optJSONArray("starterHintsTarget")).take(4)
            )
          )
        }
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  suspend fun advanceDynamicImmersionTurn(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    scene: DynamicImmersionScene,
    conversationHistory: List<Pair<String, String>>, // speaker -> text
    rememberedFacts: List<String>,
    userUtterance: String
  ): ExploreAiResult<DynamicImmersionTurnOutcome> {
    val schema = Schema.obj(
      mapOf(
        "characterReplyTarget" to Schema.string(),
        "characterReplyNative" to Schema.string(),
        "naturalnessScore" to Schema.integer(),
        "trustDelta" to Schema.integer(),
        "coachingFeedback" to Schema.string(),
        "grammarCorrection" to Schema.string(),
        "vocabularyTip" to Schema.string(),
        "completedMissionId" to Schema.string(),
        "newlyRememberedFact" to Schema.string(),
        "mistake" to mistakeSchema()
      )
    )

    val pendingMissions = scene.missions.filterNot { it.completed }
    val missionsBlock = if (pendingMissions.isEmpty()) "All missions completed — continue natural free-flow conversation."
    else pendingMissions.joinToString("\n") { "- [${it.id}] ${it.title}: ${it.objectiveDescription}" }

    val historyText = conversationHistory.takeLast(8).joinToString("\n") { (s, t) -> "$s: $t" }
    val memoryText = if (rememberedFacts.isEmpty()) "None yet" else rememberedFacts.joinToString("; ")

    val prompt = """
      Scene: ${scene.placeNameTarget} (${scene.placeNameNative}) in ${scene.cityAndNeighborhood}.
      Character: ${scene.characterName}, ${scene.characterRole} (${scene.characterPersonality}).
      Remembered facts about the learner: $memoryText
      Remaining missions:
      $missionsBlock

      Recent conversation:
      $historyText
      Learner just said: "$userUtterance"

      Respond IN CHARACTER in natural ${learnerCtx.targetLanguageName} appropriate for CEFR ${learnerCtx.cefrLevel}, directly reacting to what the learner actually said.
      - Evaluate whether the learner genuinely satisfied one of the remaining missions above; only return its exact id in completedMissionId if they actually accomplished it (otherwise return empty string "").
      - Set trustDelta between -3 and +12 based strictly on relevance, politeness, and communication quality of the learner's message.
      - Set naturalnessScore (0-100) honestly based on the learner's message.
      - If the learner made a real grammar or phrasing error in ${learnerCtx.targetLanguageName}, populate mistake with hasGenuineMistake=true; if the sentence is already natural, set hasGenuineMistake=false.
      - Extract a brief newlyRememberedFact if the learner shared a preference, name, or plan (otherwise "").
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(
        learnerCtx,
        "Stay in character as ${scene.characterName} while providing accurate, encouraging coaching feedback."
      ),
      prompt = prompt,
      schema = schema,
      temperature = 0.7f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        val reply = j.optString("characterReplyTarget", "").trim()
        if (reply.isBlank()) {
          ExploreAiResult.Error("Character reply was empty. Tap Retry to resend your turn.")
        } else {
          val missionId = j.optString("completedMissionId", "").trim()
            .takeIf { id -> id.isNotBlank() && scene.missions.any { it.id == id && !it.completed } }
          ExploreAiResult.Success(
            DynamicImmersionTurnOutcome(
              characterReplyTarget = reply,
              characterReplyNative = j.optString("characterReplyNative", "").trim(),
              naturalnessScore = j.optInt("naturalnessScore", 75).coerceIn(10, 100),
              trustDelta = j.optInt("trustDelta", 4).coerceIn(-5, 15),
              coachingFeedback = j.optString("coachingFeedback", "").trim(),
              grammarCorrection = j.optString("grammarCorrection", "").trim(),
              vocabularyTip = j.optString("vocabularyTip", "").trim(),
              completedMissionId = missionId,
              newlyRememberedFact = j.optString("newlyRememberedFact", "").trim().takeIf { it.isNotBlank() },
              capturedMistake = parseMistakeFromJson(j.optJSONObject("mistake"))
            )
          )
        }
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  // ===========================================================================
  // 2. CAMERA SPEAKING LENS — IMAGE COMPRESSION, REAL MULTIMODAL ANALYSIS & Q&A
  // ===========================================================================

  /**
   * Downscales a user photo to max 1280px on longest edge and verifies JPEG size < 4MB
   * before sending to multimodal Gemini AI.
   */
  fun prepareCameraBitmapForAi(source: Bitmap): Result<Bitmap> {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) {
      return Result.failure(IllegalArgumentException("The selected image could not be decoded. Please choose another photo or capture a new one."))
    }
    return runCatching {
      val maxDim = 1280
      val longest = max(source.width, source.height)
      val scaled = if (longest > maxDim) {
        val scale = maxDim.toFloat() / longest.toFloat()
        val targetW = (source.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (source.height * scale).roundToInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(source, targetW, targetH, true)
      } else {
        source
      }
      val out = ByteArrayOutputStream()
      scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
      if (out.size() > 4 * 1024 * 1024) {
        out.reset()
        scaled.compress(Bitmap.CompressFormat.JPEG, 60, out)
      }
      scaled
    }
  }

  suspend fun analyzeCameraSceneBitmap(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    rawBitmap: Bitmap
  ): ExploreAiResult<CameraLensSceneAnalysis> {
    val prepared = prepareCameraBitmapForAi(rawBitmap)
    if (prepared.isFailure) {
      return ExploreAiResult.Error(
        prepared.exceptionOrNull()?.message ?: "Unsupported or unreadable image format.",
        canRetry = false
      )
    }
    val bitmap = prepared.getOrThrow()

    val schema = Schema.obj(
      mapOf(
        "sceneTitle" to Schema.string(),
        "visibleObjects" to Schema.array(Schema.string()),
        "sceneSummaryNative" to Schema.string(),
        "vocabulary" to Schema.array(
          Schema.obj(
            mapOf(
              "wordTarget" to Schema.string(),
              "phonetic" to Schema.string(),
              "meaningNative" to Schema.string(),
              "exampleSentenceTarget" to Schema.string(),
              "exampleSentenceNative" to Schema.string()
            )
          )
        ),
        "usefulPhrases" to Schema.array(
          Schema.obj(
            mapOf(
              "phraseTarget" to Schema.string(),
              "translationNative" to Schema.string()
            )
          )
        ),
        "describePromptNative" to Schema.string(),
        "starterQuestionTarget" to Schema.string(),
        "starterQuestionNative" to Schema.string()
      )
    )

    val prompt = """
      Analyze this real photo carefully. Only describe objects, actions, text, or setting details that are genuinely visible in the image.
      Generate:
      - sceneTitle (short title in ${learnerCtx.nativeLanguageName})
      - visibleObjects (4-7 concrete things actually seen in this photo)
      - sceneSummaryNative (1-2 sentence grounded summary in ${learnerCtx.nativeLanguageName})
      - vocabulary: 5-6 high-value ${learnerCtx.targetLanguageName} words matching visible items in the photo, tuned to CEFR ${learnerCtx.cefrLevel}, with phonetic guide, ${learnerCtx.nativeLanguageName} meaning, and a natural example sentence about this scene
      - usefulPhrases: 3 practical ${learnerCtx.targetLanguageName} phrases a speaker could use in this scene, with ${learnerCtx.nativeLanguageName} translations
      - describePromptNative: a clear speaking/writing prompt asking the learner to describe what they see
      - starterQuestionTarget & starterQuestionNative: one engaging follow-up question in ${learnerCtx.targetLanguageName} about the image.
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(
        learnerCtx,
        "Ground every single vocabulary item and sentence in what is genuinely visible in the user's photo."
      ),
      prompt = prompt,
      schema = schema,
      temperature = 0.45f,
      bitmap = bitmap
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        val vocabArr = j.optJSONArray("vocabulary")
        val vocab = buildList {
          if (vocabArr != null) {
            for (i in 0 until vocabArr.length()) {
              val v = vocabArr.optJSONObject(i) ?: continue
              val w = v.optString("wordTarget", "").trim()
              val m = v.optString("meaningNative", "").trim()
              if (w.isNotBlank() && m.isNotBlank()) {
                add(
                  CameraLensVocabEntry(
                    wordTarget = w,
                    phonetic = v.optString("phonetic", "").trim(),
                    meaningNative = m,
                    exampleSentenceTarget = v.optString("exampleSentenceTarget", "").trim(),
                    exampleSentenceNative = v.optString("exampleSentenceNative", "").trim()
                  )
                )
              }
            }
          }
        }
        val phrasesArr = j.optJSONArray("usefulPhrases")
        val phrases = buildList {
          if (phrasesArr != null) {
            for (i in 0 until phrasesArr.length()) {
              val p = phrasesArr.optJSONObject(i) ?: continue
              val pt = p.optString("phraseTarget", "").trim()
              val pn = p.optString("translationNative", "").trim()
              if (pt.isNotBlank()) add(pt to pn)
            }
          }
        }
        if (vocab.isEmpty()) {
          ExploreAiResult.Error("Could not extract vocabulary from this photo. Try a clearer photo or tap Retry.")
        } else {
          ExploreAiResult.Success(
            CameraLensSceneAnalysis(
              sceneTitle = j.optString("sceneTitle", "Photo Scene").ifBlank { "Photo Scene" },
              visibleObjects = parseStringList(j.optJSONArray("visibleObjects")),
              sceneSummaryNative = j.optString("sceneSummaryNative", ""),
              vocabulary = vocab,
              usefulPhrasesTarget = phrases,
              describePromptNative = j.optString("describePromptNative", "Describe what you see in ${learnerCtx.targetLanguageName}."),
              starterQuestionTarget = j.optString("starterQuestionTarget", ""),
              starterQuestionNative = j.optString("starterQuestionNative", "")
            )
          )
        }
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  suspend fun evaluateCameraDescription(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    scene: CameraLensSceneAnalysis,
    learnerDescription: String,
    userQuestionOrFollowUp: Boolean = false
  ): ExploreAiResult<CameraDescriptionFeedback> {
    val schema = Schema.obj(
      mapOf(
        "relevanceScore" to Schema.integer(),
        "grammarScore" to Schema.integer(),
        "groundedAssessment" to Schema.string(),
        "improvedDescriptionTarget" to Schema.string(),
        "followUpQuestionTarget" to Schema.string(),
        "followUpQuestionNative" to Schema.string(),
        "mistake" to mistakeSchema()
      )
    )

    val prompt = """
      Scene title: ${scene.sceneTitle}
      Visible objects: ${scene.visibleObjects.joinToString(", ")}
      Scene summary: ${scene.sceneSummaryNative}
      Learner input (${if (userQuestionOrFollowUp) "follow-up question / reply" else "photo description"}): "$learnerDescription"

      Evaluate the learner's ${learnerCtx.targetLanguageName} response strictly against the visible scene above.
      - relevanceScore (0-100): does it accurately reference what is in the photo or meaningfully answer/ask about it?
      - grammarScore (0-100): accuracy and naturalness for CEFR ${learnerCtx.cefrLevel}.
      - groundedAssessment: concise, encouraging feedback in ${learnerCtx.nativeLanguageName}.
      - improvedDescriptionTarget: a natural upgraded version in ${learnerCtx.targetLanguageName}.
      - followUpQuestionTarget & followUpQuestionNative: a natural next question in ${learnerCtx.targetLanguageName} to continue speaking about this photo.
      - mistake: record a real error only if present (hasGenuineMistake=true/false).
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(
        learnerCtx,
        "Grade the learner's description honestly against the visible scene content."
      ),
      prompt = prompt,
      schema = schema,
      temperature = 0.5f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        ExploreAiResult.Success(
          CameraDescriptionFeedback(
            relevanceScore = j.optInt("relevanceScore", 70).coerceIn(0, 100),
            grammarScore = j.optInt("grammarScore", 70).coerceIn(0, 100),
            groundedAssessment = j.optString("groundedAssessment", "").trim(),
            improvedDescriptionTarget = j.optString("improvedDescriptionTarget", learnerDescription).trim(),
            followUpQuestionTarget = j.optString("followUpQuestionTarget", "").trim(),
            followUpQuestionNative = j.optString("followUpQuestionNative", "").trim(),
            capturedMistake = parseMistakeFromJson(j.optJSONObject("mistake"))
          )
        )
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  // ===========================================================================
  // 3. EMERGENCY PHRASE MODE — REAL 9-CATEGORY OFFLINE PACK GENERATION
  // ===========================================================================

  suspend fun generateEmergencyOfflinePack(
    context: Context,
    learnerCtx: ExploreLearnerContext
  ): ExploreAiResult<OfflineEmergencyPhrasePack> {
    val categories = OfflineEmergencyPhraseRepository.REQUIRED_CATEGORIES
    val schema = Schema.obj(
      mapOf(
        "phrases" to Schema.array(
          Schema.obj(
            mapOf(
              "category" to Schema.enumeration(categories),
              "targetPhrase" to Schema.string(),
              "phoneticGuide" to Schema.string(),
              "nativeTranslation" to Schema.string(),
              "usageNote" to Schema.string(),
              "urgent" to Schema.boolean()
            )
          )
        )
      )
    )

    val prompt = """
      Generate a practical, accurate offline emergency survival phrase pack in ${learnerCtx.targetLanguageName} (with ${learnerCtx.nativeLanguageName} translations and clear phonetic pronunciation guides).
      You MUST include 3 distinct, essential phrases for EVERY SINGLE ONE of these 9 categories (27 phrases total):
      ${categories.joinToString("\n") { "- $it" }}
      Rules:
      - Keep phrases short, clear, polite/direct, and immediately speakable in urgent real-life travel situations.
      - Mark life-safety/medical/police/allergy phrases with urgent=true.
      - Provide accurate ${learnerCtx.targetLanguageName} script AND an easy-to-read pronunciation guide in phoneticGuide.
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(
        learnerCtx,
        "Generate high-accuracy survival phrases for travelers. Never invent gibberish or placeholder translations."
      ),
      prompt = prompt,
      schema = schema,
      temperature = 0.35f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val arr = res.data.optJSONArray("phrases")
        val phrases = buildList {
          if (arr != null) {
            for (i in 0 until arr.length()) {
              val obj = arr.optJSONObject(i) ?: continue
              val cat = obj.optString("category", "").trim()
              val target = obj.optString("targetPhrase", "").trim()
              val nativeTr = obj.optString("nativeTranslation", "").trim()
              if (cat.isNotBlank() && target.isNotBlank() && nativeTr.isNotBlank()) {
                add(
                  OfflineEmergencyPhrase(
                    id = "em_${learnerCtx.targetLanguageCode}_$i",
                    category = cat,
                    targetPhrase = target,
                    phoneticGuide = obj.optString("phoneticGuide", "").trim(),
                    nativeTranslation = nativeTr,
                    usageNote = obj.optString("usageNote", "").trim(),
                    urgent = obj.optBoolean("urgent", false)
                  )
                )
              }
            }
          }
        }
        val coveredCategories = phrases.map { it.category }.distinct().size
        if (phrases.size < 9 || coveredCategories < 6) {
          ExploreAiResult.Error("Downloaded phrase pack was incomplete ($coveredCategories categories). Tap Retry to download cleanly.")
        } else {
          val pack = OfflineEmergencyPhrasePack(
            languageCode = learnerCtx.targetLanguageCode,
            languageName = learnerCtx.targetLanguageName,
            nativeLanguageCode = learnerCtx.nativeLanguageCode,
            downloadedAtEpochMs = System.currentTimeMillis(),
            phrases = phrases
          )
          val saved = OfflineEmergencyPhraseRepository.savePack(context, pack)
          if (saved.isSuccess) {
            ExploreAiResult.Success(saved.getOrThrow())
          } else {
            ExploreAiResult.Error("Could not save offline pack to device storage. Please check storage space and retry.")
          }
        }
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  // ===========================================================================
  // 4. AI DEBATE PARTNER — DYNAMIC TOPICS, MULTI-ROUND & HONEST RUBRIC
  // ===========================================================================

  suspend fun generateDebateTopic(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    customTopicIdea: String? = null,
    avoidTitles: List<String> = emptyList()
  ): ExploreAiResult<DynamicDebateTopic> {
    val schema = Schema.obj(
      mapOf(
        "titleTarget" to Schema.string(),
        "titleNative" to Schema.string(),
        "contextBriefNative" to Schema.string(),
        "proPositionNative" to Schema.string(),
        "conPositionNative" to Schema.string(),
        "suggestedConnectorsTarget" to Schema.array(Schema.string())
      )
    )

    val ideaInstruction = if (!customTopicIdea.isNullOrBlank()) {
      "Build a balanced, thought-provoking debate motion directly from the learner's custom topic: \"$customTopicIdea\"."
    } else {
      val avoid = if (avoidTitles.isNotEmpty()) "Avoid repeating: ${avoidTitles.takeLast(5).joinToString("; ")}." else ""
      "Generate a fresh, engaging, real-world debate topic suited to CEFR ${learnerCtx.cefrLevel} (e.g., technology, cities, education, work, culture, environment, lifestyle). $avoid"
    }

    val prompt = """
      $ideaInstruction
      Provide:
      - titleTarget (debate statement in ${learnerCtx.targetLanguageName})
      - titleNative (clear translation in ${learnerCtx.nativeLanguageName})
      - contextBriefNative (1-2 sentences setting up both sides)
      - proPositionNative (concise Pro stance summary)
      - conPositionNative (concise Con stance summary)
      - suggestedConnectorsTarget (4-5 useful discourse connectors in ${learnerCtx.targetLanguageName} appropriate for CEFR ${learnerCtx.cefrLevel}).
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(learnerCtx, "You are a skilled multilingual debate coach."),
      prompt = prompt,
      schema = schema,
      temperature = 0.8f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        val titleT = j.optString("titleTarget", "").trim()
        if (titleT.isBlank()) {
          ExploreAiResult.Error("Could not generate a debate topic. Tap Retry.")
        } else {
          ExploreAiResult.Success(
            DynamicDebateTopic(
              id = "deb_${learnerCtx.targetLanguageCode}_${System.currentTimeMillis()}",
              titleTarget = titleT,
              titleNative = j.optString("titleNative", titleT).trim(),
              contextBriefNative = j.optString("contextBriefNative", "").trim(),
              proPositionNative = j.optString("proPositionNative", "Support the statement").trim(),
              conPositionNative = j.optString("conPositionNative", "Challenge the statement").trim(),
              cefrLevel = learnerCtx.cefrLevel,
              suggestedConnectorsTarget = parseStringList(j.optJSONArray("suggestedConnectorsTarget")).take(5)
            )
          )
        }
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  suspend fun advanceDebateTurn(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    topic: DynamicDebateTopic,
    learnerStance: String, // "Pro" | "Con"
    history: List<DebateTurnEntry>,
    learnerArgument: String,
    isClarificationRequest: Boolean = false
  ): ExploreAiResult<DebateTurnResponse> {
    val aiStance = if (learnerStance.equals("Pro", ignoreCase = true)) "Con" else "Pro"
    val schema = Schema.obj(
      mapOf(
        "counterArgumentTarget" to Schema.string(),
        "counterArgumentNative" to Schema.string(),
        "quickCoachingNote" to Schema.string(),
        "followUpChallengeTarget" to Schema.string(),
        "mistake" to mistakeSchema()
      )
    )

    val transcript = history.takeLast(8).joinToString("\n") {
      "${if (it.speaker == "learner") "Learner ($learnerStance)" else "AI Opponent ($aiStance)"}: ${it.textTarget}"
    }

    val modeInstruction = if (isClarificationRequest) {
      "The learner asked for clarification on your previous point. Briefly clarify your previous argument in simpler ${learnerCtx.targetLanguageName} at CEFR ${learnerCtx.cefrLevel} and invite their rebuttal."
    } else {
      "Directly engage with the learner's latest argument from the $aiStance side in natural ${learnerCtx.targetLanguageName} (2-3 crisp sentences for CEFR ${learnerCtx.cefrLevel}), acknowledge one point they made, and present a thoughtful counterargument."
    }

    val prompt = """
      Debate motion: ${topic.titleTarget} (${topic.titleNative})
      Learner stance: $learnerStance | Your stance: $aiStance
      Debate history so far:
      $transcript
      Learner's new input: "$learnerArgument"

      $modeInstruction
      Also provide quickCoachingNote (1 short tip in ${learnerCtx.nativeLanguageName}), followUpChallengeTarget (a short probing question in ${learnerCtx.targetLanguageName}), and populate mistake only if the learner made a real error.
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(
        learnerCtx,
        "You are a respectful, articulate live debate opponent."
      ),
      prompt = prompt,
      schema = schema,
      temperature = 0.7f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        val counter = j.optString("counterArgumentTarget", "").trim()
        if (counter.isBlank()) {
          ExploreAiResult.Error("Debate partner response was empty. Tap Retry.")
        } else {
          ExploreAiResult.Success(
            DebateTurnResponse(
              counterArgumentTarget = counter,
              counterArgumentNative = j.optString("counterArgumentNative", "").trim(),
              quickCoachingNote = j.optString("quickCoachingNote", "").trim(),
              followUpChallengeTarget = j.optString("followUpChallengeTarget", "").trim(),
              capturedMistake = parseMistakeFromJson(j.optJSONObject("mistake"))
            )
          )
        }
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  /**
   * Honest rubric evaluation AFTER the learner has actually participated.
   * Refuses to score if learnerTurns is empty, and only assigns a fluency score
   * when at least one learner turn was captured via speech/microphone.
   */
  suspend fun evaluateCompletedDebate(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    topic: DynamicDebateTopic,
    learnerStance: String,
    history: List<DebateTurnEntry>
  ): ExploreAiResult<DebateRubricEvaluation> {
    val learnerTurns = history.filter { it.speaker == "learner" && it.textTarget.isNotBlank() }
    if (learnerTurns.isEmpty()) {
      return ExploreAiResult.Error(
        "Participate in at least one debate round before requesting an evaluation.",
        canRetry = false
      )
    }

    val hasVoiceEvidence = learnerTurns.any { it.spokenViaMic }
    val schema = Schema.obj(
      mapOf(
        "overallScore" to Schema.integer(),
        "grammarScore" to Schema.integer(),
        "vocabularyScore" to Schema.integer(),
        "clarityScore" to Schema.integer(),
        "argumentStructureScore" to Schema.integer(),
        "connectorsUsed" to Schema.array(Schema.string()),
        "counterargumentHandlingNote" to Schema.string(),
        "fluencyScoreIfSpoken" to Schema.integer(),
        "strengths" to Schema.array(Schema.string()),
        "improvementTips" to Schema.array(Schema.string()),
        "upgradedExampleTarget" to Schema.string(),
        "mistakes" to Schema.array(mistakeSchema())
      )
    )

    val transcript = history.joinToString("\n") {
      val mode = if (it.speaker == "learner") {
        if (it.spokenViaMic) "Learner (spoken transcript)" else "Learner (typed)"
      } else "AI Opponent"
      "$mode: ${it.textTarget}"
    }

    val prompt = """
      Evaluate this completed ${learnerCtx.targetLanguageName} debate honestly based ONLY on the learner's actual turns below.
      Motion: ${topic.titleTarget} | Learner stance: $learnerStance | CEFR target: ${learnerCtx.cefrLevel}
      Learner used microphone speech: $hasVoiceEvidence
      Transcript:
      $transcript

      Rubric requirements:
      - Grade grammarScore, vocabularyScore, clarityScore, argumentStructureScore, overallScore honestly (0-100) based on real evidence in the learner's turns. Short/minimal replies must receive lower scores than developed arguments.
      - connectorsUsed: list ONLY connectors the learner actually used in the transcript (empty array if none).
      - counterargumentHandlingNote: explain how well the learner addressed the opponent's points.
      - fluencyScoreIfSpoken: if Learner used microphone speech is true, estimate conversational flow from transcript cohesion (0-100); if false, return 0.
      - strengths (2-3 specific points), improvementTips (2-3 actionable tips), upgradedExampleTarget (an improved rewrite of one of the learner's actual arguments in ${learnerCtx.targetLanguageName}).
      - mistakes: list genuine grammar/vocabulary errors from the learner's turns (empty array if none).
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(
        learnerCtx,
        "Provide rigorous, honest, evidence-based debate assessment."
      ),
      prompt = prompt,
      schema = schema,
      temperature = 0.35f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        val mistakesArr = j.optJSONArray("mistakes")
        val mistakes = buildList {
          if (mistakesArr != null) {
            for (i in 0 until mistakesArr.length()) {
              parseMistakeFromJson(mistakesArr.optJSONObject(i))?.let { add(it) }
            }
          }
        }
        val rawFluency = j.optInt("fluencyScoreIfSpoken", 0).coerceIn(0, 100)
        ExploreAiResult.Success(
          DebateRubricEvaluation(
            overallScore = j.optInt("overallScore", 70).coerceIn(0, 100),
            grammarScore = j.optInt("grammarScore", 70).coerceIn(0, 100),
            vocabularyScore = j.optInt("vocabularyScore", 70).coerceIn(0, 100),
            clarityScore = j.optInt("clarityScore", 70).coerceIn(0, 100),
            argumentStructureScore = j.optInt("argumentStructureScore", 70).coerceIn(0, 100),
            connectorsUsed = parseStringList(j.optJSONArray("connectorsUsed")),
            counterargumentHandlingNote = j.optString("counterargumentHandlingNote", "").trim(),
            fluencyScoreIfSpoken = if (hasVoiceEvidence) rawFluency else null,
            fluencyHonestyNote = if (hasVoiceEvidence) {
              "Transcript flow estimate based on your spoken turns (note: acoustic pronunciation is not measured without raw audio scoring)."
            } else {
              "Fluency not scored because all turns were typed. Use the microphone in your next debate to receive spoken flow feedback."
            },
            strengths = parseStringList(j.optJSONArray("strengths")),
            improvementTips = parseStringList(j.optJSONArray("improvementTips")),
            upgradedExampleTarget = j.optString("upgradedExampleTarget", "").trim(),
            capturedMistakes = mistakes
          )
        )
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  // ===========================================================================
  // 5. CONVERSATION REPAIR LAB — DYNAMIC BREAKDOWNS & MULTI-TURN RECOVERY
  // ===========================================================================

  suspend fun generateRepairScenario(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    requestedBreakdownType: String? = null,
    avoidSettings: List<String> = emptyList()
  ): ExploreAiResult<DynamicRepairScenario> {
    val breakdown = requestedBreakdownType?.takeIf { it.isNotBlank() }
      ?: REPAIR_BREAKDOWN_POOL.random()

    val schema = Schema.obj(
      mapOf(
        "breakdownType" to Schema.string(),
        "settingTitle" to Schema.string(),
        "speakerName" to Schema.string(),
        "speakerRole" to Schema.string(),
        "preferredVoiceGender" to Schema.enumeration(listOf("female", "male")),
        "breakdownPromptTarget" to Schema.string(),
        "breakdownPromptNative" to Schema.string(),
        "challengeNoteNative" to Schema.string(),
        "repairGoalNative" to Schema.string(),
        "suggestedRepairStartersTarget" to Schema.array(Schema.string())
      )
    )

    val avoid = if (avoidSettings.isNotEmpty()) "Do NOT reuse these settings: ${avoidSettings.takeLast(6).joinToString(", ")}." else ""
    val prompt = """
      Create a fresh, realistic Conversation Repair scenario in ${learnerCtx.targetLanguageName} for CEFR ${learnerCtx.cefrLevel}.
      Required breakdown focus: $breakdown.
      $avoid
      Examples of varied settings: train platform gate change, doctor appointment rescheduling, tech support call, coworker project handoff, restaurant dietary check, hotel billing mix-up, delivery address confusion, museum tour schedule.
      Provide:
      - settingTitle (short descriptive title in ${learnerCtx.nativeLanguageName})
      - speakerName & speakerRole
      - preferredVoiceGender ("female" or "male")
      - breakdownPromptTarget: what the native speaker just said in ${learnerCtx.targetLanguageName} that causes the communication breakdown
      - breakdownPromptNative: clear translation in ${learnerCtx.nativeLanguageName}
      - challengeNoteNative: brief note explaining why this is tricky in real life
      - repairGoalNative: what the learner needs to ask, clarify, or politely correct
      - suggestedRepairStartersTarget: 3 natural ${learnerCtx.targetLanguageName} repair starters.
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(learnerCtx, "Design practical real-world communication repair drills."),
      prompt = prompt,
      schema = schema,
      temperature = 0.85f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        val promptTarget = j.optString("breakdownPromptTarget", "").trim()
        if (promptTarget.isBlank()) {
          ExploreAiResult.Error("Generated repair scenario was incomplete. Tap Retry.")
        } else {
          ExploreAiResult.Success(
            DynamicRepairScenario(
              id = "rep_${learnerCtx.targetLanguageCode}_${System.currentTimeMillis()}",
              breakdownType = j.optString("breakdownType", breakdown).ifBlank { breakdown },
              settingTitle = j.optString("settingTitle", "Real-Life Repair").ifBlank { "Real-Life Repair" },
              speakerName = j.optString("speakerName", "Local Speaker"),
              speakerRole = j.optString("speakerRole", "Speaker"),
              preferredVoiceGender = j.optString("preferredVoiceGender", "female"),
              cefrLevel = learnerCtx.cefrLevel,
              breakdownPromptTarget = promptTarget,
              breakdownPromptNative = j.optString("breakdownPromptNative", "").trim(),
              challengeNoteNative = j.optString("challengeNoteNative", "").trim(),
              repairGoalNative = j.optString("repairGoalNative", "").trim(),
              suggestedRepairStartersTarget = parseStringList(j.optJSONArray("suggestedRepairStartersTarget")).take(4)
            )
          )
        }
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  suspend fun evaluateRepairTurn(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    scenario: DynamicRepairScenario,
    history: List<Pair<String, String>>,
    learnerRepairAttempt: String
  ): ExploreAiResult<DynamicRepairTurnEvaluation> {
    val schema = Schema.obj(
      mapOf(
        "repairedSuccessfully" to Schema.boolean(),
        "score" to Schema.integer(),
        "nextDifficultyHint" to Schema.string(),
        "speakerFollowUpTarget" to Schema.string(),
        "speakerFollowUpNative" to Schema.string(),
        "feedbackNative" to Schema.string(),
        "naturalRepairModelTarget" to Schema.string(),
        "mistake" to mistakeSchema()
      )
    )

    val hist = history.takeLast(6).joinToString("\n") { (s, t) -> "$s: $t" }
    val prompt = """
      Conversation Repair scenario: ${scenario.settingTitle} (${scenario.breakdownType})
      Speaker: ${scenario.speakerName} (${scenario.speakerRole})
      Original breakdown: "${scenario.breakdownPromptTarget}"
      Repair goal: ${scenario.repairGoalNative}
      Recent exchange:
      $hist
      Learner's repair attempt: "$learnerRepairAttempt"

      Evaluate whether the learner successfully clarified, asked to repeat, politely corrected, or confirmed the needed detail in ${learnerCtx.targetLanguageName}.
      - Have ${scenario.speakerName} reply naturally in ${learnerCtx.targetLanguageName} (speakerFollowUpTarget + speakerFollowUpNative) so the learner can continue the conversation.
      - Adapt nextDifficultyHint (e.g., "Ready for faster speech" if score >= 85, or "Keep practicing polite confirmation phrases" if < 75).
      - Provide naturalRepairModelTarget (an exemplary native-level way to say it) and mistake only if there is a genuine error.
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(learnerCtx, "Evaluate communication repair strategies honestly."),
      prompt = prompt,
      schema = schema,
      temperature = 0.55f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        ExploreAiResult.Success(
          DynamicRepairTurnEvaluation(
            repairedSuccessfully = j.optBoolean("repairedSuccessfully", false),
            score = j.optInt("score", 65).coerceIn(0, 100),
            nextDifficultyHint = j.optString("nextDifficultyHint", "").trim(),
            speakerFollowUpTarget = j.optString("speakerFollowUpTarget", "").trim(),
            speakerFollowUpNative = j.optString("speakerFollowUpNative", "").trim(),
            feedbackNative = j.optString("feedbackNative", "").trim(),
            naturalRepairModelTarget = j.optString("naturalRepairModelTarget", "").trim(),
            capturedMistake = parseMistakeFromJson(j.optJSONObject("mistake"))
          )
        )
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  // ===========================================================================
  // 6. EXPLAIN IT THREE WAYS — ANY CONCEPT, 3 DEPTHS, FOLLOW-UP & MINI PRACTICE
  // ===========================================================================

  suspend fun generateThreeWayExplanation(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    userConceptQuery: String
  ): ExploreAiResult<ThreeWayExplanationResult> {
    val cleanQuery = userConceptQuery.trim()
    if (cleanQuery.isBlank()) {
      return ExploreAiResult.Error("Enter any grammar rule, phrase, idiom, tense, or cultural nuance to explain.", canRetry = false)
    }

    val schema = Schema.obj(
      mapOf(
        "conceptTitle" to Schema.string(),
        "categoryLabel" to Schema.string(),
        "beginnerExplanationNative" to Schema.string(),
        "beginnerExampleTarget" to Schema.string(),
        "beginnerExampleNative" to Schema.string(),
        "intermediateExplanationNative" to Schema.string(),
        "intermediateExampleTarget" to Schema.string(),
        "intermediateExampleNative" to Schema.string(),
        "advancedExplanationNative" to Schema.string(),
        "advancedExampleTarget" to Schema.string(),
        "advancedExampleNative" to Schema.string(),
        "commonPitfallNative" to Schema.string(),
        "miniPracticePromptNative" to Schema.string(),
        "miniPracticeExpectedTarget" to Schema.string(),
        "miniPracticeHintNative" to Schema.string()
      )
    )

    val prompt = """
      The learner wants you to explain this exact ${learnerCtx.targetLanguageName} concept: "$cleanQuery".
      Teach THIS EXACT concept (never substitute an unrelated topic) at 3 genuinely distinct depth levels:
      1. Beginner (A1-A2): intuitive analogy, zero jargon, simple everyday ${learnerCtx.targetLanguageName} example.
      2. Intermediate (B1-B2): clear structural rule, usage contrast, practical conversational ${learnerCtx.targetLanguageName} example.
      3. Advanced (C1-C2): register, pragmatic nuance, subtle exceptions or native-like phrasing in ${learnerCtx.targetLanguageName}.
      Also include:
      - commonPitfallNative: the #1 mistake learners make with "$cleanQuery"
      - miniPracticePromptNative: a short active-recall translation or fill-in exercise testing "$cleanQuery"
      - miniPracticeExpectedTarget: the model correct answer in ${learnerCtx.targetLanguageName}
      - miniPracticeHintNative: a helpful hint without giving the full answer away.
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(
        learnerCtx,
        "You are a master linguistics teacher who explains any language concept clearly at three distinct levels."
      ),
      prompt = prompt,
      schema = schema,
      temperature = 0.55f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        val bExp = j.optString("beginnerExplanationNative", "").trim()
        val iExp = j.optString("intermediateExplanationNative", "").trim()
        val aExp = j.optString("advancedExplanationNative", "").trim()
        if (bExp.isBlank() || iExp.isBlank() || aExp.isBlank()) {
          ExploreAiResult.Error("Explanation was incomplete. Tap Retry to regenerate.")
        } else {
          ExploreAiResult.Success(
            ThreeWayExplanationResult(
              conceptTitle = j.optString("conceptTitle", cleanQuery).ifBlank { cleanQuery },
              categoryLabel = j.optString("categoryLabel", "Language Concept").ifBlank { "Language Concept" },
              beginnerExplanationNative = bExp,
              beginnerExampleTarget = j.optString("beginnerExampleTarget", "").trim(),
              beginnerExampleNative = j.optString("beginnerExampleNative", "").trim(),
              intermediateExplanationNative = iExp,
              intermediateExampleTarget = j.optString("intermediateExampleTarget", "").trim(),
              intermediateExampleNative = j.optString("intermediateExampleNative", "").trim(),
              advancedExplanationNative = aExp,
              advancedExampleTarget = j.optString("advancedExampleTarget", "").trim(),
              advancedExampleNative = j.optString("advancedExampleNative", "").trim(),
              commonPitfallNative = j.optString("commonPitfallNative", "").trim(),
              miniPracticePromptNative = j.optString("miniPracticePromptNative", "").trim(),
              miniPracticeExpectedTarget = j.optString("miniPracticeExpectedTarget", "").trim(),
              miniPracticeHintNative = j.optString("miniPracticeHintNative", "").trim()
            )
          )
        }
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  suspend fun askExplanationFollowUp(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    concept: ThreeWayExplanationResult,
    followUpQuestion: String
  ): ExploreAiResult<Pair<String, String>> { // Pair(answerNative, exampleTarget)
    val schema = Schema.obj(
      mapOf(
        "answerNative" to Schema.string(),
        "exampleTarget" to Schema.string()
      )
    )
    val prompt = """
      Concept being taught: "${concept.conceptTitle}" (${learnerCtx.targetLanguageName}).
      Learner's follow-up question: "$followUpQuestion"
      Answer directly and clearly in ${learnerCtx.nativeLanguageName} (answerNative) and include one crisp illustrative ${learnerCtx.targetLanguageName} example (exampleTarget).
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(learnerCtx, "Answer the learner's follow-up question clearly and concisely."),
      prompt = prompt,
      schema = schema,
      temperature = 0.5f
    )
    return when (res) {
      is ExploreAiResult.Success -> ExploreAiResult.Success(
        res.data.optString("answerNative", "").trim() to res.data.optString("exampleTarget", "").trim()
      )
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  suspend fun evaluateExplanationMiniExercise(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    concept: ThreeWayExplanationResult,
    learnerAnswer: String
  ): ExploreAiResult<ExplanationMiniPracticeEvaluation> {
    val schema = Schema.obj(
      mapOf(
        "isCorrect" to Schema.boolean(),
        "score" to Schema.integer(),
        "feedbackNative" to Schema.string(),
        "modelAnswerTarget" to Schema.string(),
        "mistake" to mistakeSchema()
      )
    )
    val prompt = """
      Concept: "${concept.conceptTitle}" in ${learnerCtx.targetLanguageName}.
      Exercise prompt: "${concept.miniPracticePromptNative}"
      Reference answer: "${concept.miniPracticeExpectedTarget}"
      Learner's answer: "$learnerAnswer"
      Evaluate whether the learner applied "${concept.conceptTitle}" accurately (accept valid alternative phrasing in ${learnerCtx.targetLanguageName}).
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(learnerCtx, "Grade the practice exercise fairly and clearly."),
      prompt = prompt,
      schema = schema,
      temperature = 0.35f
    )
    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        ExploreAiResult.Success(
          ExplanationMiniPracticeEvaluation(
            isCorrect = j.optBoolean("isCorrect", false),
            score = j.optInt("score", 60).coerceIn(0, 100),
            feedbackNative = j.optString("feedbackNative", "").trim(),
            modelAnswerTarget = j.optString("modelAnswerTarget", concept.miniPracticeExpectedTarget).trim(),
            capturedMistake = parseMistakeFromJson(j.optJSONObject("mistake"))
          )
        )
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  // ===========================================================================
  // 7. CONVERSATION DETECTIVE — DYNAMIC PRAGMATIC CASES & NUANCED FEEDBACK
  // ===========================================================================

  suspend fun generateDetectiveCase(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    requestedPragmaticType: String? = null,
    avoidPhrases: List<String> = emptyList()
  ): ExploreAiResult<DynamicDetectiveCase> {
    val pragmatic = requestedPragmaticType?.takeIf { it.isNotBlank() }
      ?: DETECTIVE_PRAGMATIC_POOL.random()

    val schema = Schema.obj(
      mapOf(
        "pragmaticType" to Schema.string(),
        "settingTitle" to Schema.string(),
        "speakerName" to Schema.string(),
        "preferredVoiceGender" to Schema.enumeration(listOf("female", "male")),
        "contextBriefNative" to Schema.string(),
        "literalPhraseTarget" to Schema.string(),
        "literalTranslationNative" to Schema.string(),
        "interpretationOptions" to Schema.array(Schema.string()),
        "recommendedOptionIndex" to Schema.integer(),
        "nuancedCulturalNote" to Schema.string()
      )
    )

    val avoid = if (avoidPhrases.isNotEmpty()) "Do NOT repeat these phrases: ${avoidPhrases.takeLast(6).joinToString("; ")}." else ""
    val prompt = """
      Generate a realistic Conversation Detective pragmatic-meaning case in ${learnerCtx.targetLanguageName} for CEFR ${learnerCtx.cefrLevel}.
      Focus nuance: $pragmatic.
      $avoid
      Provide:
      - settingTitle (realistic workplace, social, family, or everyday context)
      - speakerName & preferredVoiceGender ("female" or "male")
      - contextBriefNative (2 sentences setting the social context)
      - literalPhraseTarget (what the speaker literally says in ${learnerCtx.targetLanguageName})
      - literalTranslationNative (literal word-for-word meaning in ${learnerCtx.nativeLanguageName})
      - interpretationOptions (exactly 3 plausible interpretations in ${learnerCtx.nativeLanguageName})
      - recommendedOptionIndex (0, 1, or 2 — the most likely pragmatic meaning in this context)
      - nuancedCulturalNote: explain how tone, relationship, and regional/individual style affect this phrase — explicitly avoid overgeneralizing or claiming one universal cultural rule.
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(
        learnerCtx,
        "Teach pragmatic nuance thoughtfully, recognizing that speakers vary by region, relationship, and tone."
      ),
      prompt = prompt,
      schema = schema,
      temperature = 0.85f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        val phrase = j.optString("literalPhraseTarget", "").trim()
        val options = parseStringList(j.optJSONArray("interpretationOptions")).take(4)
        if (phrase.isBlank() || options.size < 2) {
          ExploreAiResult.Error("Generated detective case was incomplete. Tap Retry.")
        } else {
          val recIdx = j.optInt("recommendedOptionIndex", 0).coerceIn(0, options.lastIndex)
          ExploreAiResult.Success(
            DynamicDetectiveCase(
              id = "det_${learnerCtx.targetLanguageCode}_${System.currentTimeMillis()}",
              pragmaticType = j.optString("pragmaticType", pragmatic).ifBlank { pragmatic },
              settingTitle = j.optString("settingTitle", "Social Subtext").ifBlank { "Social Subtext" },
              speakerName = j.optString("speakerName", "Speaker"),
              preferredVoiceGender = j.optString("preferredVoiceGender", "female"),
              contextBriefNative = j.optString("contextBriefNative", "").trim(),
              literalPhraseTarget = phrase,
              literalTranslationNative = j.optString("literalTranslationNative", "").trim(),
              interpretationOptions = options,
              recommendedOptionIndex = recIdx,
              nuancedCulturalNote = j.optString("nuancedCulturalNote", "").trim()
            )
          )
        }
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  suspend fun evaluateDetectiveResponse(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    detectiveCase: DynamicDetectiveCase,
    selectedInterpretation: String?,
    learnerCustomExplanation: String,
    learnerReplyInTargetLang: String
  ): ExploreAiResult<DetectiveResponseEvaluation> {
    val schema = Schema.obj(
      mapOf(
        "understoodNuance" to Schema.boolean(),
        "score" to Schema.integer(),
        "feedbackNative" to Schema.string(),
        "pragmaticExplanationNative" to Schema.string(),
        "naturalResponseModelTarget" to Schema.string(),
        "speakerContinuationTarget" to Schema.string(),
        "speakerContinuationNative" to Schema.string(),
        "mistake" to mistakeSchema()
      )
    )

    val prompt = """
      Conversation Detective case (${detectiveCase.pragmaticType}):
      Context: ${detectiveCase.contextBriefNative}
      Speaker (${detectiveCase.speakerName}) said: "${detectiveCase.literalPhraseTarget}" (Literal: "${detectiveCase.literalTranslationNative}")
      Expected nuance: "${detectiveCase.interpretationOptions.getOrNull(detectiveCase.recommendedOptionIndex).orEmpty()}"

      Learner selected interpretation: "${selectedInterpretation ?: "None selected"}"
      Learner's own explanation: "${learnerCustomExplanation.ifBlank { "None provided" }}"
      Learner's natural reply in ${learnerCtx.targetLanguageName}: "${learnerReplyInTargetLang.ifBlank { "None provided" }}"

      Evaluate:
      1. Did the learner understand the pragmatic subtext?
      2. If they wrote/spoke a reply in ${learnerCtx.targetLanguageName}, evaluate its tone and grammar, provide naturalResponseModelTarget, and generate ${detectiveCase.speakerName}'s natural follow-up reply (speakerContinuationTarget + speakerContinuationNative).
      3. Keep pragmaticExplanationNative nuanced (note that tone and personal style vary).
      4. Populate mistake only if the learner's ${learnerCtx.targetLanguageName} reply contains a genuine error.
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(learnerCtx, "Coach pragmatic comprehension and natural responses."),
      prompt = prompt,
      schema = schema,
      temperature = 0.55f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        ExploreAiResult.Success(
          DetectiveResponseEvaluation(
            understoodNuance = j.optBoolean("understoodNuance", false),
            score = j.optInt("score", 70).coerceIn(0, 100),
            feedbackNative = j.optString("feedbackNative", "").trim(),
            pragmaticExplanationNative = j.optString("pragmaticExplanationNative", detectiveCase.nuancedCulturalNote).trim(),
            naturalResponseModelTarget = j.optString("naturalResponseModelTarget", "").trim(),
            speakerContinuationTarget = j.optString("speakerContinuationTarget", "").trim(),
            speakerContinuationNative = j.optString("speakerContinuationNative", "").trim(),
            capturedMistake = parseMistakeFromJson(j.optJSONObject("mistake"))
          )
        )
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  // ===========================================================================
  // 8. MISTAKE RECOVERY — REAL DRILLS STRICTLY FROM ACTUAL RECORDED MISTAKES
  // ===========================================================================

  suspend fun generateMistakeRecoveryDrills(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    recordedMistakes: List<RepeatedMistakeSummary>
  ): ExploreAiResult<List<DynamicMistakeDrill>> {
    if (recordedMistakes.isEmpty()) {
      return ExploreAiResult.Success(emptyList())
    }

    val schema = Schema.obj(
      mapOf(
        "drills" to Schema.array(
          Schema.obj(
            mapOf(
              "mistakeId" to Schema.string(),
              "category" to Schema.string(),
              "promptNative" to Schema.string(),
              "flawedSentenceTarget" to Schema.string(),
              "expectedCorrectedTarget" to Schema.string(),
              "ruleExplanationNative" to Schema.string()
            )
          )
        )
      )
    )

    val prioritized = recordedMistakes
      .sortedWith(compareBy<RepeatedMistakeSummary> { it.improved }.thenBy { it.masteryScore })
      .take(5)

    val mistakeListText = prioritized.joinToString("\n") { m ->
      "- id=${m.id} | category=${m.category} | wrong=\"${m.exampleWrong}\" | corrected=\"${m.exampleCorrected}\" | rule=\"${m.ruleSummary}\""
    }

    val prompt = """
      Generate targeted spaced-repetition mistake recovery drills strictly from these ACTUAL recorded ${learnerCtx.targetLanguageName} learner mistakes (one drill per mistake id):
      $mistakeListText
      Each drill must give a fresh contextual practice prompt in ${learnerCtx.nativeLanguageName} targeting that exact error pattern, a flawed sentence to fix (or the learner's actual mistake), the expected natural ${learnerCtx.targetLanguageName} fix, and a concise rule explanation in ${learnerCtx.nativeLanguageName}.
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(learnerCtx, "Build spaced-repetition remediation drills strictly from real learner errors."),
      prompt = prompt,
      schema = schema,
      temperature = 0.45f
    )

    return when (res) {
      is ExploreAiResult.Success -> {
        val arr = res.data.optJSONArray("drills")
        val drills = buildList {
          if (arr != null) {
            for (i in 0 until arr.length()) {
              val d = arr.optJSONObject(i) ?: continue
              val promptN = d.optString("promptNative", "").trim()
              val expected = d.optString("expectedCorrectedTarget", "").trim()
              if (promptN.isNotBlank() && expected.isNotBlank()) {
                val matchedId = d.optString("mistakeId", "").trim()
                  .takeIf { id -> prioritized.any { it.id == id } }
                  ?: prioritized.getOrNull(i)?.id
                  ?: "m_$i"
                add(
                  DynamicMistakeDrill(
                    id = "drill_${matchedId}_$i",
                    mistakeId = matchedId,
                    category = d.optString("category", prioritized.firstOrNull { it.id == matchedId }?.category ?: "Grammar"),
                    promptNative = promptN,
                    flawedSentenceTarget = d.optString("flawedSentenceTarget", "").trim(),
                    expectedCorrectedTarget = expected,
                    ruleExplanationNative = d.optString("ruleExplanationNative", "").trim()
                  )
                )
              }
            }
          }
        }
        ExploreAiResult.Success(drills)
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  suspend fun evaluateMistakeRecoveryResponse(
    context: Context,
    learnerCtx: ExploreLearnerContext,
    drill: DynamicMistakeDrill,
    learnerAnswer: String
  ): ExploreAiResult<MistakeDrillEvaluation> {
    val schema = Schema.obj(
      mapOf(
        "passed" to Schema.boolean(),
        "score" to Schema.integer(),
        "feedbackNative" to Schema.string(),
        "correctedModelTarget" to Schema.string()
      )
    )
    val prompt = """
      Mistake category: ${drill.category}
      Prompt: ${drill.promptNative}
      Flawed sentence: "${drill.flawedSentenceTarget}"
      Model fix: "${drill.expectedCorrectedTarget}"
      Learner's correction: "$learnerAnswer"
      Evaluate whether the learner successfully fixed the underlying ${drill.category} issue in ${learnerCtx.targetLanguageName} (accept valid natural alternatives).
    """.trimIndent()

    val res = runStructuredJsonRequest(
      context = context,
      learnerCtx = learnerCtx,
      systemInstruction = learnerSystemInstruction(learnerCtx, "Evaluate error correction fairly and concisely."),
      prompt = prompt,
      schema = schema,
      temperature = 0.3f
    )
    return when (res) {
      is ExploreAiResult.Success -> {
        val j = res.data
        ExploreAiResult.Success(
          MistakeDrillEvaluation(
            passed = j.optBoolean("passed", false),
            score = j.optInt("score", 60).coerceIn(0, 100),
            feedbackNative = j.optString("feedbackNative", "").trim(),
            correctedModelTarget = j.optString("correctedModelTarget", drill.expectedCorrectedTarget).trim()
          )
        )
      }
      is ExploreAiResult.Error -> res
      ExploreAiResult.Offline -> ExploreAiResult.Offline
      is ExploreAiResult.QuotaExceeded -> res
    }
  }

  // ===========================================================================
  // FIRESTORE & LOCAL PERSISTENCE FOR EXPLORE SESSIONS & MISTAKE RECORDS
  // ===========================================================================

  private fun getFirestoreOrNull(context: Context): FirebaseFirestore? {
    return try {
      val dbId = context.applicationContext.getString(R.string.firestore_database_id)
      FirebaseFirestore.getInstance(dbId)
    } catch (e: Exception) {
      Log.w(TAG, "Firestore instance unavailable: ${e.message}")
      null
    }
  }

  suspend fun saveExploreSession(
    context: Context,
    record: ExploreSessionRecord
  ) = withContext(Dispatchers.IO) {
    // 1. Always persist locally for instant offline history
    saveLocalExploreSession(context, record)

    // 2. Sync to user-isolated Firestore path if signed in
    val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: return@withContext
    val db = getFirestoreOrNull(context) ?: return@withContext
    val safeId = record.sessionId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
      .ifBlank { "exp_${System.currentTimeMillis()}" }
    val data = mapOf(
      "userId" to uid,
      "sessionId" to safeId,
      "featureType" to record.featureType,
      "languageCode" to record.languageCode.take(10),
      "title" to record.title.take(240).ifBlank { "Explore Session" },
      "summary" to record.summary.take(3900),
      "cefrLevel" to record.cefrLevel.take(32),
      "turnsCount" to record.turnsCount.coerceIn(0, 500),
      "scorePercent" to record.scorePercent.coerceIn(0, 100),
      "payloadJson" to record.payloadJson.take(38000),
      "updatedAt" to Timestamp.now()
    )
    runCatching {
      db.collection("users")
        .document(uid)
        .collection("explore_sessions")
        .document(safeId)
        .set(data, SetOptions.merge())
        .await()
    }.onFailure {
      Log.w(TAG, "Firestore explore_session sync skipped: ${it.message}")
    }
  }

  fun loadLocalExploreSessions(
    context: Context,
    featureType: String? = null,
    languageCode: String? = null
  ): List<ExploreSessionRecord> {
    val prefs = context.applicationContext.getSharedPreferences(LOCAL_SESSIONS_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString("sessions_json", null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val rec = ExploreSessionRecord(
            sessionId = obj.optString("sessionId", "s_$i"),
            featureType = obj.optString("featureType", ""),
            languageCode = obj.optString("languageCode", ""),
            title = obj.optString("title", ""),
            summary = obj.optString("summary", ""),
            cefrLevel = obj.optString("cefrLevel", ""),
            turnsCount = obj.optInt("turnsCount", 0),
            scorePercent = obj.optInt("scorePercent", 0),
            payloadJson = obj.optString("payloadJson", ""),
            updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
          )
          val matchesType = featureType == null || rec.featureType == featureType
          val matchesLang = languageCode == null || rec.languageCode.equals(languageCode, ignoreCase = true)
          if (matchesType && matchesLang) add(rec)
        }
      }.sortedByDescending { it.updatedAtEpochMs }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun saveLocalExploreSession(context: Context, record: ExploreSessionRecord) {
    val prefs = context.applicationContext.getSharedPreferences(LOCAL_SESSIONS_PREFS, Context.MODE_PRIVATE)
    val existing = loadLocalExploreSessions(context).filterNot { it.sessionId == record.sessionId }
    val updated = (listOf(record) + existing).take(80)
    val arr = JSONArray()
    updated.forEach { rec ->
      arr.put(
        JSONObject().apply {
          put("sessionId", rec.sessionId)
          put("featureType", rec.featureType)
          put("languageCode", rec.languageCode)
          put("title", rec.title)
          put("summary", rec.summary)
          put("cefrLevel", rec.cefrLevel)
          put("turnsCount", rec.turnsCount)
          put("scorePercent", rec.scorePercent)
          put("payloadJson", rec.payloadJson)
          put("updatedAtEpochMs", rec.updatedAtEpochMs)
        }
      )
    }
    prefs.edit().putString("sessions_json", arr.toString()).apply()
  }

  suspend fun syncMistakeRecordToFirestore(
    context: Context,
    mistakeId: String,
    languageCode: String,
    languageName: String,
    category: String,
    originalResponse: String,
    correctedVersion: String,
    explanation: String,
    activitySource: String,
    reviewStatus: String,
    occurrenceCount: Int,
    masteryScore: Int
  ) = withContext(Dispatchers.IO) {
    val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: return@withContext
    val db = getFirestoreOrNull(context) ?: return@withContext
    val safeId = mistakeId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
      .ifBlank { "mst_${System.currentTimeMillis()}" }
    val validStatus = when (reviewStatus) {
      "pending", "practicing", "mastered" -> reviewStatus
      else -> "pending"
    }
    val data = mapOf(
      "userId" to uid,
      "mistakeId" to safeId,
      "languageCode" to languageCode.take(10),
      "languageName" to languageName.take(60).ifBlank { languageCode },
      "category" to category.take(80).ifBlank { "Grammar" },
      "originalResponse" to originalResponse.take(1000),
      "correctedVersion" to correctedVersion.take(1000),
      "explanation" to explanation.take(1500),
      "activitySource" to activitySource.take(80).ifBlank { "Explore" },
      "reviewStatus" to validStatus,
      "occurrenceCount" to occurrenceCount.coerceIn(1, 500),
      "masteryScore" to masteryScore.coerceIn(0, 100),
      "updatedAt" to Timestamp.now()
    )
    runCatching {
      db.collection("users")
        .document(uid)
        .collection("mistake_records")
        .document(safeId)
        .set(data, SetOptions.merge())
        .await()
    }.onFailure {
      Log.w(TAG, "Firestore mistake_record sync skipped: ${it.message}")
    }
  }
}
