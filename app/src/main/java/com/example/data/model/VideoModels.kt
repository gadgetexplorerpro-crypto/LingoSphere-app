package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class LingoVideo(
  val videoId: String,
  val title: String,
  val channelTitle: String,
  val channelId: String = "",
  val thumbnailUrl: String = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
  val durationText: String = "10:00",
  val viewCountText: String = "",
  val publishedAt: String = "",
  val languageCode: String = "es",
  val proficiencyLevel: String = "Beginner",
  val topicCategory: String = "Recommended",
  val description: String = "",
  val hasCaptions: Boolean = true,
  val captionLanguages: List<String> = emptyList(),
  val captionSummary: String = ""
) {
  val youtubeWatchUrl: String
    get() = "https://www.youtube.com/watch?v=$videoId"

  val youtubeChannelUrl: String
    get() = if (channelId.isNotBlank()) {
      "https://www.youtube.com/channel/$channelId"
    } else {
      "https://www.youtube.com/results?search_query=${channelTitle.replace(" ", "+")}"
    }
}

@IgnoreExtraProperties
data class SavedVideo(
  @DocumentId val id: String = "",
  val userId: String = "",
  val videoId: String = "",
  val title: String = "",
  val channelTitle: String = "",
  val thumbnailUrl: String = "",
  val durationText: String = "10:00",
  val languageCode: String = "es",
  val proficiencyLevel: String = "Beginner",
  val topicCategory: String = "Recommended",
  val description: String = "",
  val captionSummary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toLingoVideo(): LingoVideo = LingoVideo(
    videoId = videoId,
    title = title,
    channelTitle = channelTitle,
    thumbnailUrl = thumbnailUrl.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
    durationText = durationText,
    languageCode = languageCode,
    proficiencyLevel = proficiencyLevel,
    topicCategory = topicCategory,
    description = description,
    hasCaptions = captionSummary.isNotBlank(),
    captionSummary = captionSummary
  )
}

@IgnoreExtraProperties
data class VideoPlaylist(
  @DocumentId val id: String = "",
  val userId: String = "",
  val playlistId: String = "",
  val name: String = "",
  val description: String = "",
  val languageCode: String = "es",
  val proficiencyLevel: String = "All",
  val videoIds: List<String> = emptyList(),
  val videoTitles: List<String> = emptyList(),
  val videoThumbnails: List<String> = emptyList(),
  val videoChannels: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VideoWatchHistory(
  @DocumentId val id: String = "",
  val userId: String = "",
  val videoId: String = "",
  val title: String = "",
  val channelTitle: String = "",
  val thumbnailUrl: String = "",
  val durationText: String = "10:00",
  val languageCode: String = "es",
  val proficiencyLevel: String = "Beginner",
  val watchedSeconds: Long = 0L,
  val completedQuiz: Boolean = false,
  val quizScorePercent: Long = 0L,
  val xpEarned: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toLingoVideo(): LingoVideo = LingoVideo(
    videoId = videoId,
    title = title,
    channelTitle = channelTitle,
    thumbnailUrl = thumbnailUrl.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
    durationText = durationText,
    languageCode = languageCode,
    proficiencyLevel = proficiencyLevel,
    topicCategory = "Watch History",
    description = "Previously watched in LingoSphere Videos"
  )
}

data class VideoVocabInsight(
  val term: String,
  val phonetic: String,
  val translation: String,
  val partOfSpeech: String,
  val contextSentence: String
)

data class VideoPronunciationDrill(
  val phraseInTarget: String,
  val phoneticGuide: String,
  val nativeMeaning: String,
  val coachingTip: String
)

data class VideoQuizQuestion(
  val question: String,
  val options: List<String>,
  val correctIndex: Int,
  val explanation: String
)

data class VideoAiStudyPack(
  val videoId: String,
  val summaryInNative: String,
  val culturalOrGrammarNote: String,
  val captionSourceStatus: String,
  val vocabulary: List<VideoVocabInsight>,
  val pronunciationDrills: List<VideoPronunciationDrill>,
  val quizQuestions: List<VideoQuizQuestion>
)
