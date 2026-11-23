package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FlashcardProgress
import com.example.data.model.LingoVideo
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class LingoRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testCreateAndGetProfile_authenticated_succeeds() = runBlocking {
    val uid = signInTestUser("user_profile_${UUID.randomUUID()}@test.com")
    val repo = LingoRepository(firestore, auth)

    val profile = UserProfile(
      userId = uid,
      displayName = "Alice Test",
      nativeLanguageCode = "en",
      targetLanguageCode = "es",
      activeLanguages = listOf("es")
    )

    withTimeout(TIMEOUT_MS) {
      repo.createUserProfile(profile)
      val retrieved = repo.getUserProfile(uid)
      assertNotNull(retrieved)
      assertEquals("Alice Test", retrieved?.displayName)
      assertEquals("es", retrieved?.targetLanguageCode)
    }
  }

  @Test
  fun testRecordLessonCompletion_authenticated_emitsProgress() = runBlocking {
    val uid = signInTestUser("user_lesson_${UUID.randomUUID()}@test.com")
    val repo = LingoRepository(firestore, auth)

    val profile = UserProfile(userId = uid, displayName = "Alice Test")
    repo.createUserProfile(profile)

    val lessonId = "test_lesson_${UUID.randomUUID()}"
    withTimeout(TIMEOUT_MS) {
      repo.recordLessonCompletion(lessonId, "es", "Vocabulary", 100, 30)

      val progressList = repo.observeLessonProgress(uid).first { list ->
        list.any { it.lessonId == lessonId }
      }
      assertTrue(progressList.any { it.lessonId == lessonId && it.completed })
    }
  }

  @Test
  fun testSaveAndObserveFlashcards_authenticated_succeeds() = runBlocking {
    val uid = signInTestUser("user_cards_${UUID.randomUUID()}@test.com")
    val repo = LingoRepository(firestore, auth)

    val wordId = "word_${UUID.randomUUID()}"
    val card = FlashcardProgress(
      id = "card_${UUID.randomUUID()}",
      userId = uid,
      wordId = wordId,
      languageCode = "es",
      frontText = "Hola",
      backText = "Hello",
      pronunciation = "oh-lah",
      boxLevel = 1L
    )

    withTimeout(TIMEOUT_MS) {
      repo.saveFlashcard(card)

      val cards = repo.observeFlashcards(uid).first { list ->
        list.any { it.wordId == wordId }
      }
      assertTrue(cards.any { it.wordId == wordId && it.frontText == "Hola" })
    }
  }

  @Test
  fun testUnauthenticated_observeProgress_rejected() = runBlocking {
    auth.signOut()
    val repo = LingoRepository(firestore)

    try {
      withTimeout(TIMEOUT_MS) {
        repo.observeLessonProgress("some_unauth_user").first()
      }
      fail("Expected exception for unauthenticated query")
    } catch (e: Exception) {
      val fsError = e.unwrapFirestoreException()
      assertTrue(
        "Expected permission denied or cancelled stream, got $e",
        fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED || e.message?.contains("PERMISSION_DENIED") == true || e is kotlinx.coroutines.CancellationException
      )
    }
  }

  @Test
  fun testCallSignalingAndCallHistoryLifecycle_twoUsers_succeeds() = runBlocking {
    val callerUid = signInTestUser("caller_${UUID.randomUUID()}@test.com")
    val repoCaller = LingoRepository(firestore, auth)
    repoCaller.createUserProfile(
      UserProfile(
        userId = callerUid,
        displayName = "Caller User",
        nativeLanguageCode = "en",
        targetLanguageCode = "es"
      )
    )

    val calleeUid = "callee_${UUID.randomUUID()}"

    withTimeout(TIMEOUT_MS) {
      // 1. Caller creates a direct voice/video call room
      val createdRoom = repoCaller.createOrJoinCallRoom(
        calleeId = calleeUid,
        calleeName = "Callee Partner",
        callType = "video",
        matchMode = "direct",
        targetLanguageCode = "es",
        callerName = "Caller User",
        callerNativeLang = "en",
        callerLearningLang = "es"
      )
      assertEquals("ringing", createdRoom.status)
      assertEquals("incoming_call", createdRoom.lastSignalingEvent)
      assertEquals(callerUid, createdRoom.callerId)
      assertEquals(calleeUid, createdRoom.calleeId)

      // 2. Caller ends call after connection and records call history
      repoCaller.endCall(
        roomId = createdRoom.roomId,
        actualDurationSeconds = 45L
      )

      val history = repoCaller.observeCallHistory(callerUid).first { list ->
        list.any { it.callId == createdRoom.roomId }
      }
      val record = history.first { it.callId == createdRoom.roomId }
      assertEquals("completed", record.status)
      assertEquals(45L, record.durationSeconds)
      assertEquals("video", record.callType)
    }
  }

  @Test
  fun testLingoVideos_savePlaylistAndWatchHistory_succeeds() = runBlocking {
    val uid = signInTestUser("video_learner_${UUID.randomUUID()}@test.com")
    val repo = LingoRepository(firestore, auth)

    val sampleVideo = LingoVideo(
      videoId = "DAp_v7EH9AA",
      title = "Spanish in 30 Minutes",
      channelTitle = "SpanishPod101",
      thumbnailUrl = "https://i.ytimg.com/vi/DAp_v7EH9AA/hqdefault.jpg",
      durationText = "28:15",
      languageCode = "es",
      proficiencyLevel = "Beginner",
      topicCategory = "Conversation",
      description = "Essential Spanish phrases for beginners"
    )

    withTimeout(TIMEOUT_MS) {
      // 1. Save video
      repo.saveVideo(sampleVideo)
      val savedList = repo.observeSavedVideos(uid).first { list ->
        list.any { it.videoId == "DAp_v7EH9AA" }
      }
      assertTrue(savedList.any { it.videoId == "DAp_v7EH9AA" && it.languageCode == "es" })

      // 2. Create playlist with initial video
      val playlistId = repo.createVideoPlaylist(
        name = "My Spanish Immersion",
        description = "Daily listening lessons",
        languageCode = "es",
        proficiencyLevel = "Beginner",
        initialVideo = sampleVideo
      )
      assertTrue(playlistId.isNotBlank())
      val playlists = repo.observeVideoPlaylists(uid).first { list ->
        list.any { it.playlistId == playlistId || it.id == playlistId }
      }
      assertTrue(playlists.any { it.name == "My Spanish Immersion" && it.videoIds.contains("DAp_v7EH9AA") })

      // 3. Record watch history
      repo.recordVideoWatchProgress(
        video = sampleVideo,
        watchedSeconds = 180L,
        completedQuiz = true,
        quizScorePercent = 100,
        xpReward = 25
      )
      val watchHistory = repo.observeVideoWatchHistory(uid).first { list ->
        list.any { it.videoId == "DAp_v7EH9AA" }
      }
      assertTrue(watchHistory.any { it.videoId == "DAp_v7EH9AA" && it.completedQuiz && it.watchedSeconds == 180L })
    }
  }

  @Test
  fun testPresenceAndSpeakingInvitationNotification_succeeds() = runBlocking {
    val receiverUid = signInTestUser("receiver_presence_${UUID.randomUUID()}@test.com")
    val senderUid = signInTestUser("sender_presence_${UUID.randomUUID()}@test.com")
    val repoSender = LingoRepository(firestore, auth)
    val senderProfile = UserProfile(
      userId = senderUid,
      displayName = "Sender Learner",
      nativeLanguageCode = "en",
      targetLanguageCode = "es"
    )
    repoSender.createUserProfile(senderProfile)

    withTimeout(TIMEOUT_MS) {
      // 1. Update community profile with real-time presence & last seen privacy controls
      repoSender.syncCommunityProfile(
        profile = senderProfile,
        country = "United States",
        countryFlag = "🇺🇸",
        bio = "Practicing speaking daily",
        interests = listOf("Travel", "Culture"),
        isOnline = true,
        allowCalls = true,
        presenceState = "online",
        showLastSeen = true
      )

      val myComm = repoSender.observeMyCommunityProfile(senderUid).first { it != null }
      assertNotNull(myComm)
      assertEquals("online", myComm?.presenceState)
      assertTrue(myComm?.isEffectivelyOnline() == true)

      // 2. Send speaking invitation to a partner (writes chat message + deep-linked notification)
      repoSender.sendSpeakingInvitation(
        senderName = "Sender Learner",
        receiverId = receiverUid,
        receiverName = "Receiver Partner",
        topic = "Daily travel expressions"
      )

      // 3. Also create a Word of the Day notification for senderUid and verify deep-link fields
      repoSender.createNotification(
        targetUserId = senderUid,
        title = "intriguing — Word of the Day",
        message = "Meaning: Very interesting or mysterious.",
        type = "word_of_the_day",
        targetRoute = "word_of_the_day",
        targetId = "intriguing",
        word = "intriguing",
        meaning = "Very interesting or mysterious.",
        pronunciation = "/ɪnˈtriːɡɪŋ/ (in-TREE-ging)",
        exampleSentence = "That's an intriguing idea!"
      )

      val notifs = repoSender.observeNotifications(senderUid).first { list ->
        list.any { it.type == "word_of_the_day" }
      }
      val wodNotif = notifs.first { it.type == "word_of_the_day" }
      assertEquals("word_of_the_day", wodNotif.targetRoute)
      assertEquals("intriguing", wodNotif.word)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_learner@test.com"
    const val TIMEOUT_MS = 5000L
  }
}

