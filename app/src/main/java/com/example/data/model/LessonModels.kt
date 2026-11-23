package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ExerciseType {
  VOCABULARY,
  GRAMMAR,
  LISTENING,
  READING,
  WRITING,
  SPEAKING,
  QUIZ
}

data class Exercise(
  val id: String,
  val type: ExerciseType,
  val prompt: String,
  val explanationNative: String,
  val targetAudioText: String = "",
  val phoneticGuide: String = "",
  val options: List<String> = emptyList(),
  val correctOptionIndex: Int = 0,
  val acceptableAnswers: List<String> = emptyList()
)

data class CurriculumLesson(
  val id: String,
  val title: String,
  val description: String,
  val category: String, // Vocabulary, Grammar, Listening, Reading, Writing, Speaking, Quiz, Revision
  val proficiencyLevel: String, // Beginner, Intermediate, Advanced
  val targetLanguageCode: String,
  val estimatedMinutes: Int = 5,
  val xpReward: Int = 30,
  val exercises: List<Exercise> = emptyList(),
  val cefrLevel: String = when (proficiencyLevel) {
    "Beginner" -> "A1"
    "Intermediate" -> "B1"
    "Advanced" -> "C1"
    else -> "A1"
  },
  val unitTitle: String = "Unit 1: Core Foundations",
  val learningOutcomes: List<String> = listOf(
    "Recognize and use core target expressions in context",
    "Understand spoken pronunciation and sentence structure"
  ),
  val isRevision: Boolean = false
)

@IgnoreExtraProperties
data class LessonProgress(
  @DocumentId val id: String = "",
  val userId: String = "",
  val lessonId: String = "",
  val languageCode: String = "",
  val category: String = "Vocabulary",
  val scorePercent: Long = 0L,
  val xpEarned: Long = 0L,
  val completed: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class PlacementQuestion(
  val id: String,
  val cefrBand: String, // A1, A2, B1, B2, C1, C2
  val skillArea: String, // Vocabulary, Grammar, Reading, Pragmatics
  val prompt: String,
  val options: List<String>,
  val correctIndex: Int,
  val explanation: String
)

data class PlacementTestEvaluation(
  val recommendedCefrBand: String, // A1, A2, B1, B2, C1, C2
  val mappedProficiencyLevel: String, // Beginner, Intermediate, Advanced
  val scorePercent: Int,
  val summaryFeedback: String,
  val disclaimer: String = "Note: This placement result reflects your internal LingoSphere learning stage and is not an official certified CEFR diploma."
)

data class ListeningDialogueLine(
  val speaker: String,
  val targetText: String,
  val nativeTranslation: String,
  val keyVocabWord: String = "",
  val keyVocabMeaning: String = ""
)

data class AcademyComprehensionQuestion(
  val question: String,
  val options: List<String>,
  val correctIndex: Int,
  val explanation: String
)

data class ListeningAcademyItem(
  val id: String,
  val languageCode: String,
  val cefrLevel: String, // A1, A2, B1, B2, C1, C2
  val category: String, // Short Conversation, Story, Dialogue, Everyday Situation
  val title: String,
  val regionalAccents: List<String>, // e.g., ["Spain (Madrid)", "Mexico (CDMX)", "Argentina (Buenos Aires)"]
  val lines: List<ListeningDialogueLine>,
  val questions: List<AcademyComprehensionQuestion>
)

data class ReadingWordGloss(
  val word: String,
  val translation: String,
  val partOfSpeech: String,
  val definition: String,
  val phonetic: String = ""
)

data class ReadingAcademyPassage(
  val id: String,
  val languageCode: String,
  val cefrLevel: String, // A1, A2, B1, B2, C1, C2
  val category: String, // Graded Story, Cultural Article, Everyday Dialogue
  val title: String,
  val paragraphs: List<String>,
  val paragraphTranslations: List<String>,
  val glossary: Map<String, ReadingWordGloss>,
  val questions: List<AcademyComprehensionQuestion>
)

data class DailyLearningCardItem(
  val id: String,
  val languageCode: String,
  val phraseOrIdiom: String,
  val translation: String,
  val explanation: String,
  val phoneticGuide: String = "",
  val category: String = "Phrase of the Day"
)


