package com.example.data.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

data class OfflineEmergencyPhrase(
  val id: String,
  val category: String,
  val targetPhrase: String,
  val phoneticGuide: String,
  val nativeTranslation: String,
  val usageNote: String,
  val urgent: Boolean = false
)

data class OfflineEmergencyPhrasePack(
  val languageCode: String,
  val languageName: String,
  val nativeLanguageCode: String,
  val downloadedAtEpochMs: Long,
  val phrases: List<OfflineEmergencyPhrase>
) {
  val categoryCount: Int
    get() = phrases.map { it.category }.distinct().size
}

/**
 * Stores real, validated emergency phrase packs on local device storage so they work
 * 100% offline after download. Never simulates downloads or reports fake availability.
 */
object OfflineEmergencyPhraseRepository {

  private const val DIR_NAME = "emergency_packs"

  val REQUIRED_CATEGORIES: List<String> = listOf(
    "Travel emergencies",
    "Medical assistance",
    "Lost documents",
    "Police and safety",
    "Directions",
    "Transportation",
    "Hotels",
    "Food allergies",
    "Communication problems"
  )

  private fun packDir(context: Context): File {
    val dir = File(context.applicationContext.filesDir, DIR_NAME)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  private fun packFile(context: Context, languageCode: String): File {
    val safeCode = languageCode.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9_-]"), "")
      .ifBlank { "en" }
    return File(packDir(context), "$safeCode.json")
  }

  fun hasDownloadedPack(context: Context, languageCode: String): Boolean {
    val file = packFile(context, languageCode)
    if (!file.exists() || file.length() <= 16L) return false
    return loadPack(context, languageCode)?.phrases?.isNotEmpty() == true
  }

  fun listDownloadedLanguageCodes(context: Context): Set<String> {
    val dir = packDir(context)
    val files = dir.listFiles() ?: return emptySet()
    return files
      .filter { it.isFile && it.name.endsWith(".json") && it.length() > 16L }
      .map { it.name.removeSuffix(".json") }
      .filter { loadPack(context, it)?.phrases?.isNotEmpty() == true }
      .toSet()
  }

  fun savePack(context: Context, pack: OfflineEmergencyPhrasePack): Result<OfflineEmergencyPhrasePack> {
    val cleanedPhrases = pack.phrases.filter {
      it.category.isNotBlank() && it.targetPhrase.isNotBlank() && it.nativeTranslation.isNotBlank()
    }
    if (cleanedPhrases.isEmpty()) {
      return Result.failure(IllegalArgumentException("Cannot save an empty emergency phrase pack."))
    }

    val sanitizedPack = pack.copy(
      languageCode = pack.languageCode.trim().lowercase(Locale.US),
      downloadedAtEpochMs = if (pack.downloadedAtEpochMs > 0L) pack.downloadedAtEpochMs else System.currentTimeMillis(),
      phrases = cleanedPhrases
    )

    return runCatching {
      val root = JSONObject().apply {
        put("version", 1)
        put("languageCode", sanitizedPack.languageCode)
        put("languageName", sanitizedPack.languageName)
        put("nativeLanguageCode", sanitizedPack.nativeLanguageCode)
        put("downloadedAtEpochMs", sanitizedPack.downloadedAtEpochMs)
        val arr = JSONArray()
        sanitizedPack.phrases.forEach { phrase ->
          arr.put(
            JSONObject().apply {
              put("id", phrase.id)
              put("category", phrase.category)
              put("targetPhrase", phrase.targetPhrase)
              put("phoneticGuide", phrase.phoneticGuide)
              put("nativeTranslation", phrase.nativeTranslation)
              put("usageNote", phrase.usageNote)
              put("urgent", phrase.urgent)
            }
          )
        }
        put("phrases", arr)
      }

      val targetFile = packFile(context, sanitizedPack.languageCode)
      val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
      tempFile.writeText(root.toString(), Charsets.UTF_8)
      if (targetFile.exists()) targetFile.delete()
      if (!tempFile.renameTo(targetFile)) {
        targetFile.writeText(root.toString(), Charsets.UTF_8)
        tempFile.delete()
      }
      sanitizedPack
    }
  }

  fun loadPack(context: Context, languageCode: String): OfflineEmergencyPhrasePack? {
    val file = packFile(context, languageCode)
    if (!file.exists() || !file.isFile) return null
    return try {
      val raw = file.readText(Charsets.UTF_8)
      if (raw.isBlank()) return null
      val root = JSONObject(raw)
      val arr = root.optJSONArray("phrases") ?: return null
      val phrases = buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val target = obj.optString("targetPhrase", "").trim()
          val translation = obj.optString("nativeTranslation", "").trim()
          val category = obj.optString("category", "").trim()
          if (target.isNotBlank() && translation.isNotBlank() && category.isNotBlank()) {
            add(
              OfflineEmergencyPhrase(
                id = obj.optString("id", "ep_${languageCode}_$i"),
                category = category,
                targetPhrase = target,
                phoneticGuide = obj.optString("phoneticGuide", "").trim(),
                nativeTranslation = translation,
                usageNote = obj.optString("usageNote", "").trim(),
                urgent = obj.optBoolean("urgent", false)
              )
            )
          }
        }
      }
      if (phrases.isEmpty()) return null
      OfflineEmergencyPhrasePack(
        languageCode = root.optString("languageCode", languageCode.lowercase(Locale.US)),
        languageName = root.optString("languageName", languageCode.uppercase(Locale.US)),
        nativeLanguageCode = root.optString("nativeLanguageCode", "en"),
        downloadedAtEpochMs = root.optLong("downloadedAtEpochMs", file.lastModified()),
        phrases = phrases
      )
    } catch (_: Exception) {
      null
    }
  }

  fun deletePack(context: Context, languageCode: String): Boolean {
    val file = packFile(context, languageCode)
    return if (file.exists()) file.delete() else false
  }
}
