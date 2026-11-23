package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CommunityProfile(
  val userId: String = "",
  val displayName: String = "",
  val avatarUrl: String = "",
  val country: String = "Global",
  val countryFlag: String = "🌍",
  val bio: String = "Practicing languages on LingoSphere",
  val interests: List<String> = listOf("Travel", "Culture", "Music"),
  val nativeLanguageCode: String = "en",
  val targetLanguageCode: String = "es",
  val proficiencyLevel: String = "Beginner",
  val xpPoints: Long = 0L,
  val level: Long = 1L,
  val currentStreak: Long = 1L,
  val isOnline: Boolean = true,
  val allowCalls: Boolean = true,
  val presenceState: String = "online", // online, away, offline
  val lastSeenEpochMs: Long = 0L,
  val lastHeartbeatEpochMs: Long = 0L,
  val showLastSeen: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  companion object {
    const val PRESENCE_TIMEOUT_MS = 60_000L
    const val AWAY_TIMEOUT_MS = 300_000L
  }

  /**
   * Returns true only when the user is marked online AND their heartbeat/timestamp is under
   * 60 seconds old (automatically transitions crashed/disconnected sessions to offline).
   */
  fun isEffectivelyOnline(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
    if (!isOnline) return false
    val normalizedState = presenceState.trim().lowercase()
    if (normalizedState == "offline" || normalizedState == "away") return false
    if (lastHeartbeatEpochMs > 0L) {
      return (nowEpochMs - lastHeartbeatEpochMs) <= PRESENCE_TIMEOUT_MS
    }
    if (lastSeenEpochMs > 0L) {
      return (nowEpochMs - lastSeenEpochMs) <= PRESENCE_TIMEOUT_MS
    }
    val serverUpdatedMs = updatedAt?.toDate()?.time ?: 0L
    if (serverUpdatedMs > 0L) {
      return (nowEpochMs - serverUpdatedMs) <= PRESENCE_TIMEOUT_MS
    }
    return true
  }

  /**
   * Returns true when the user's app is in the background ("away") or marked "busy" within the active window.
   */
  fun isEffectivelyAway(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
    val normalizedState = presenceState.trim().lowercase()
    if (normalizedState == "busy" && isEffectivelyOnline(nowEpochMs)) return true
    if (isEffectivelyOnline(nowEpochMs)) return false
    if (normalizedState != "away" && normalizedState != "busy") return false
    val refMs = lastHeartbeatEpochMs.takeIf { it > 0L }
      ?: lastSeenEpochMs.takeIf { it > 0L }
      ?: updatedAt?.toDate()?.time
      ?: 0L
    if (refMs > 0L && (nowEpochMs - refMs) > AWAY_TIMEOUT_MS) {
      return false
    }
    return true
  }

  fun effectivePresenceLabel(nowEpochMs: Long = System.currentTimeMillis()): String {
    val normalizedState = presenceState.trim().lowercase()
    return when {
      isEffectivelyOnline(nowEpochMs) -> when (normalizedState) {
        "in_call" -> "In Call"
        "available_to_match" -> "Available to Match"
        "busy" -> "Busy"
        else -> "Online"
      }
      isEffectivelyAway(nowEpochMs) -> "Busy"
      else -> "Offline"
    }
  }

  /**
   * Formats "Last seen ..." when the user is offline or away, respecting privacy controls.
   */
  fun formatLastSeen(nowEpochMs: Long = System.currentTimeMillis()): String? {
    if (isEffectivelyOnline(nowEpochMs)) return effectivePresenceLabel(nowEpochMs)
    if (!showLastSeen) return null
    val ts = lastSeenEpochMs.takeIf { it > 0L }
      ?: lastHeartbeatEpochMs.takeIf { it > 0L }
      ?: updatedAt?.toDate()?.time
      ?: 0L
    if (ts <= 0L) return null
    val diffMs = (nowEpochMs - ts).coerceAtLeast(0L)
    val diffMinutes = diffMs / 60_000L
    val diffHours = diffMinutes / 60L
    val diffDays = diffHours / 24L
    return when {
      diffMinutes < 1L -> "Last seen just now"
      diffMinutes < 60L -> "Last seen ${diffMinutes}m ago"
      diffHours < 24L -> "Last seen ${diffHours}h ago"
      diffDays == 1L -> "Last seen yesterday"
      diffDays < 30L -> "Last seen ${diffDays}d ago"
      else -> "Last seen recently"
    }
  }
}

@IgnoreExtraProperties
data class FriendRequest(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val receiverId: String = "",
  val receiverName: String = "",
  val status: String = "pending", // pending, accepted, declined
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DirectChatMessage(
  @DocumentId val id: String = "",
  val chatRoomId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val receiverId: String = "",
  val text: String = "",
  val translatedText: String? = null,
  val messageType: String = "text", // text, voice, schedule_invite
  val deliveryStatus: String = "sent", // sent, delivered, read
  val audioDataBase64: String = "",
  val audioDurationMs: Long = 0L,
  val audioMimeType: String = "audio/mp4",
  val audioWaveform: List<Int> = emptyList(),
  val replyToMessageId: String = "",
  val replyToSenderName: String = "",
  val replyToText: String = "",
  val transcriptText: String? = null,
  val grammarCorrectionText: String? = null,
  val editedAtEpochMs: Long = 0L,
  val readAtEpochMs: Long = 0L,
  val scheduleTopic: String = "",
  val scheduleDateLabel: String = "",
  val scheduleTimeLabel: String = "",
  val scheduleTimezone: String = "",
  val scheduleStatus: String = "pending", // pending, accepted, declined, rescheduled
  val scheduleProposedText: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
) {
  fun isVoiceMessage(): Boolean {
    return messageType == "voice" || audioDataBase64.isNotBlank()
  }

  fun isScheduledInvitation(): Boolean {
    return messageType == "schedule_invite" ||
      scheduleTopic.isNotBlank() ||
      text.startsWith("📅 Scheduled Language Exchange") ||
      text.startsWith("📅 Exchange Session:")
  }

  fun resolvedScheduleTopic(): String {
    if (scheduleTopic.isNotBlank()) return scheduleTopic
    val topicAfter = text.substringAfter("Topic:", "").substringBefore("•").trim()
    return topicAfter.ifBlank { "50/50 Language Exchange Practice" }
  }

  fun resolvedScheduleDateLabel(): String {
    if (scheduleDateLabel.isNotBlank()) return scheduleDateLabel
    val timeSlot = text.substringAfter("Time:", "").substringBefore("•").trim()
    if (timeSlot.contains(" at ", ignoreCase = true)) {
      return timeSlot.substringBefore(" at ").trim()
    }
    return timeSlot.ifBlank { "Upcoming Session" }
  }

  fun resolvedScheduleTimeLabel(): String {
    if (scheduleTimeLabel.isNotBlank()) return scheduleTimeLabel
    val timeSlot = text.substringAfter("Time:", "").substringBefore("•").trim()
    if (timeSlot.contains(" at ", ignoreCase = true)) {
      return timeSlot.substringAfter(" at ").trim()
    }
    return timeSlot.ifBlank { "Flexible Time" }
  }

  fun resolvedScheduleTimezone(): String {
    if (scheduleTimezone.isNotBlank()) return scheduleTimezone
    return java.util.TimeZone.getDefault().getDisplayName(false, java.util.TimeZone.SHORT)
  }
}

@IgnoreExtraProperties
data class CallRoom(
  val roomId: String = "",
  val callerId: String = "",
  val callerName: String = "",
  val callerNativeLang: String = "en",
  val callerLearningLang: String = "es",
  val calleeId: String = "",
  val calleeName: String = "",
  val calleeNativeLang: String = "es",
  val calleeLearningLang: String = "en",
  val callType: String = "voice", // voice, video
  val matchMode: String = "direct", // direct, random, country, interest, speaking_room
  val targetLanguageCode: String = "es",
  val status: String = "waiting", // waiting, ringing, accepted, connected, ended, rejected, cancelled, missed, failed, busy
  val lastSignalingEvent: String = "call_initiated",
  val isCallerMuted: Boolean = false,
  val isCalleeMuted: Boolean = false,
  val isVideoEnabled: Boolean = false,
  val isCallerVideoEnabled: Boolean = false,
  val isCalleeVideoEnabled: Boolean = false,
  val conversationTopic: String = "",
  val roomTitle: String = "",
  val isPublic: Boolean = true,
  val participantLimit: Int = 8,
  val participantIds: List<String> = emptyList(),
  val participantNames: List<String> = emptyList(),
  val mutedParticipantIds: List<String> = emptyList(),
  val handRaisedParticipantIds: List<String> = emptyList(),
  val sdpOffer: String? = null,
  val sdpAnswer: String? = null,
  val callerIceCandidates: List<String> = emptyList(),
  val calleeIceCandidates: List<String> = emptyList(),
  val connectionState: String = "new", // new, checking, connecting, connected, completed, reconnecting, disconnected, failed, closed
  val connectedAtEpochMs: Long = 0L,
  val endedAtEpochMs: Long = 0L,
  val endedBy: String = "",
  val durationSeconds: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun isTerminalStatus(): Boolean {
    return status.lowercase() in setOf(
      "ended", "rejected", "declined", "cancelled", "missed", "failed", "busy", "closed"
    ) || connectionState.lowercase() == "closed" || endedAtEpochMs > 0L
  }
}

@IgnoreExtraProperties
data class CallHistoryRecord(
  @DocumentId val id: String = "",
  val callId: String = "",
  val callerId: String = "",
  val callerName: String = "",
  val receiverId: String = "",
  val receiverName: String = "",
  val callType: String = "voice", // voice, video
  val targetLanguageCode: String = "es",
  val startTimeEpochMs: Long = 0L,
  val endTimeEpochMs: Long = 0L,
  val durationSeconds: Long = 0L,
  val status: String = "completed", // completed, missed, rejected, cancelled, failed
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserReport(
  @DocumentId val id: String = "",
  val reporterId: String = "",
  val targetUserId: String = "",
  val reason: String = "",
  val details: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserBlock(
  @DocumentId val id: String = "",
  val userId: String = "",
  val blockedUserId: String = "",
  val blockedUserName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val message: String = "",
  val type: String = "info", // friend_request, call_invite, speaking_invitation, missed_call, word_of_the_day, vocab_challenge, grammar_tip, speaking_reminder, achievement, system
  val isRead: Boolean = false,
  val targetRoute: String = "",
  val targetId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val word: String = "",
  val meaning: String = "",
  val pronunciation: String = "",
  val exampleSentence: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

data class CountryInfo(
  val name: String,
  val flagEmoji: String,
  val primaryLanguageCode: String
) {
  companion object {
    val COUNTRIES = listOf(
      CountryInfo("United States", "🇺🇸", "en"),
      CountryInfo("Spain", "🇪🇸", "es"),
      CountryInfo("France", "🇫🇷", "fr"),
      CountryInfo("Germany", "🇩🇪", "de"),
      CountryInfo("Japan", "🇯🇵", "ja"),
      CountryInfo("China", "🇨🇳", "zh"),
      CountryInfo("South Korea", "🇰🇷", "ko"),
      CountryInfo("Saudi Arabia", "🇸🇦", "ar"),
      CountryInfo("India", "🇮🇳", "hi"),
      CountryInfo("Brazil", "🇧🇷", "pt"),
      CountryInfo("Italy", "🇮🇹", "it"),
      CountryInfo("Turkey", "🇹🇷", "tr"),
      CountryInfo("Mexico", "🇲🇽", "es"),
      CountryInfo("United Kingdom", "🇬🇧", "en"),
      CountryInfo("Canada", "🇨🇦", "en")
    )

    val INTEREST_TOPICS = listOf(
      "Travel",
      "Music",
      "Technology",
      "Movies & TV",
      "Cooking & Food",
      "Business & Career",
      "Sports & Fitness",
      "Books & Literature",
      "Gaming",
      "Art & Design",
      "Science",
      "Culture & History"
    )

    val CONVERSATION_STARTERS = listOf(
      "Introduce yourself & where you live",
      "Favorite traditional food from your country",
      "Why are you learning this language?",
      "Best travel destination you have visited",
      "Daily routine & favorite weekend hobby",
      "Movies, music & cultural traditions"
    )
  }
}
