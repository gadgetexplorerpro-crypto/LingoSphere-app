package com.example.data.model

import com.example.data.repository.PictureWordQuestion

data class VocabFlashcard(
  val id: String = "",
  val word: String = "",
  val translation: String = "",
  val phonetic: String = "",
  val partOfSpeech: String = "Vocabulary",
  val cefrLevel: String = "A2",
  val category: String = "General",
  val exampleSentence: String = "",
  val exampleTranslation: String = "",
  val synonyms: List<String> = emptyList(),
  val masteryScore: Int = 1,
  val nextReviewLabel: String = ""
)

data class AiCustomLesson(
  val id: String = "",
  val title: String = "",
  val languageCode: String = "es",
  val cefrLevel: String = "B1",
  val userGoalPrompt: String = "",
  val vocabularyList: List<String> = emptyList(),
  val grammarNotes: List<String> = emptyList(),
  val speakingExercises: List<String> = emptyList(),
  val quizQuestions: List<ReadingQuestion> = emptyList()
)

data class ReadingQuestion(
  val question: String = "",
  val options: List<String> = emptyList(),
  val correctIndex: Int = 0,
  val explanation: String = ""
)

data class ReadingStory(
  val id: String = "",
  val title: String = "",
  val content: String = "",
  val translation: String = "",
  val cefrLevel: String = "A2",
  val questions: List<ReadingQuestion> = emptyList()
)
