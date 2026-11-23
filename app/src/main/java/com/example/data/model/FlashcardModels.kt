package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class FlashcardItem(
  val wordId: String,
  val languageCode: String,
  val frontText: String,
  val backText: String,
  val pronunciation: String = "",
  val exampleSentence: String = "",
  val boxLevel: Int = 1
)

@IgnoreExtraProperties
data class FlashcardProgress(
  @DocumentId val id: String = "",
  val userId: String = "",
  val wordId: String = "",
  val languageCode: String = "",
  val frontText: String = "",
  val backText: String = "",
  val pronunciation: String = "",
  val boxLevel: Long = 1L, // 1 to 5 (mapped from SM-2 interval/stability for Firestore schema compatibility)
  val nextReviewEpochDay: Long = 0L,
  val mastered: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class SrsRating(val label: String, val qualityScore: Int, val defaultIntervalHint: String) {
  AGAIN("Again", 1, "< 1d"),
  HARD("Hard", 2, "1–2d"),
  GOOD("Good", 4, "3–7d"),
  EASY("Easy", 5, "7–21d")
}

enum class RecallMode(val label: String, val description: String) {
  FLIP("Card Flip", "Classic visual front-to-back active recall"),
  LISTENING("Listening Recall", "Hear the target word and recall its meaning"),
  TYPING("Typing Recall", "Type the exact target word from its translation"),
  SPEAKING("Speaking Recall", "Speak the target word aloud to verify pronunciation")
}

data class SrsCardMetadata(
  val wordId: String,
  val easeFactor: Double = 2.5,
  val intervalDays: Int = 0,
  val repetitions: Int = 0,
  val lapses: Int = 0,
  val lastRating: String = "GOOD"
)

data class SrsReviewComputation(
  val updatedBoxLevel: Long,
  val nextReviewEpochDay: Long,
  val mastered: Boolean,
  val updatedMetadata: SrsCardMetadata
)

/**
 * Documented SM-2 / FSRS Hybrid Spaced Repetition Scheduling Engine.
 * Calculates adaptive review intervals based on learner recall quality (Again, Hard, Good, Easy),
 * ease factor (1.30 .. 3.00), repetition streak, and lapse history while preserving 1..5 boxLevel
 * compatibility with existing Firestore rules.
 */
object SpacedRepetitionEngine {

  fun computeNextReview(
    card: FlashcardProgress,
    currentMeta: SrsCardMetadata?,
    rating: SrsRating,
    todayEpochDay: Long
  ): SrsReviewComputation {
    val meta = currentMeta ?: SrsCardMetadata(
      wordId = card.wordId.ifBlank { card.id },
      easeFactor = 2.5,
      intervalDays = when (card.boxLevel) {
        1L -> 0
        2L -> 2
        3L -> 5
        4L -> 12
        else -> 21
      },
      repetitions = (card.boxLevel - 1L).coerceAtLeast(0L).toInt(),
      lapses = 0
    )

    var ef = meta.easeFactor
    var reps = meta.repetitions
    var interval = meta.intervalDays
    var lapses = meta.lapses

    when (rating) {
      SrsRating.AGAIN -> {
        reps = 0
        interval = 0 // Due immediately / today
        lapses += 1
        ef = (ef - 0.20).coerceAtLeast(1.30)
      }
      SrsRating.HARD -> {
        reps = (reps + 1).coerceAtLeast(1)
        interval = if (interval <= 1) 1 else kotlin.math.max(2, (interval * 1.2).toInt())
        ef = (ef - 0.15).coerceAtLeast(1.30)
      }
      SrsRating.GOOD -> {
        reps += 1
        interval = when (reps) {
          1 -> 2
          2 -> 5
          else -> kotlin.math.max(interval + 2, kotlin.math.round(interval * ef).toInt())
        }
      }
      SrsRating.EASY -> {
        reps += 1
        ef = (ef + 0.15).coerceAtMost(3.00)
        interval = when (reps) {
          1 -> 4
          2 -> 9
          else -> kotlin.math.max(interval + 4, kotlin.math.round(interval * ef * 1.3).toInt())
        }
      }
    }

    val boundedInterval = interval.coerceIn(0, 180)
    val nextBoxLevel = when {
      boundedInterval == 0 -> 1L
      boundedInterval <= 2 -> 2L
      boundedInterval <= 6 -> 3L
      boundedInterval <= 14 -> 4L
      else -> 5L
    }
    val isMastered = nextBoxLevel >= 5L && rating != SrsRating.AGAIN

    return SrsReviewComputation(
      updatedBoxLevel = nextBoxLevel,
      nextReviewEpochDay = todayEpochDay + boundedInterval.toLong(),
      mastered = isMastered,
      updatedMetadata = SrsCardMetadata(
        wordId = card.wordId.ifBlank { card.id },
        easeFactor = ef,
        intervalDays = boundedInterval,
        repetitions = reps,
        lapses = lapses,
        lastRating = rating.name
      )
    )
  }

  fun previewIntervalLabel(card: FlashcardProgress, currentMeta: SrsCardMetadata?, rating: SrsRating): String {
    val result = computeNextReview(card, currentMeta, rating, todayEpochDay = 0L)
    val days = result.nextReviewEpochDay
    return if (days <= 0L) "Today" else "${days}d"
  }

  fun previewIntervalLabel(currentMeta: SrsCardMetadata?, boxLevel: Long, rating: SrsRating): String {
    val dummyCard = FlashcardProgress(id = "preview", boxLevel = boxLevel)
    return previewIntervalLabel(dummyCard, currentMeta, rating)
  }

  fun isCardDue(card: FlashcardProgress, currentMeta: SrsCardMetadata?, nowMs: Long = System.currentTimeMillis()): Boolean {
    val todayEpochDay = nowMs / 86_400_000L
    if (card.nextReviewEpochDay <= 0L) {
      return card.boxLevel <= 2L || (currentMeta != null && currentMeta.intervalDays == 0)
    }
    return card.nextReviewEpochDay <= todayEpochDay
  }

  fun isDifficultCard(card: FlashcardProgress, currentMeta: SrsCardMetadata?): Boolean {
    if (currentMeta != null) {
      return currentMeta.lapses > 0 || currentMeta.easeFactor < 2.1 || currentMeta.lastRating == SrsRating.AGAIN.name
    }
    return card.boxLevel == 1L && !card.mastered
  }
}


