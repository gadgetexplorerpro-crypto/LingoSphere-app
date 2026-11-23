package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CallHistoryRecord
import com.example.data.model.CallRoom
import com.example.data.model.CommunityProfile
import com.example.data.model.DirectChatMessage
import com.example.data.model.FlashcardProgress
import com.example.data.model.FriendRequest
import com.example.data.model.LessonProgress
import com.example.data.model.LingoVideo
import com.example.data.model.SavedVideo
import com.example.data.model.SpacedRepetitionEngine
import com.example.data.model.SpeakingSession
import com.example.data.model.SrsCardMetadata
import com.example.data.model.SrsRating
import com.example.data.model.SrsReviewComputation
import com.example.data.model.UserBlock
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.model.VideoPlaylist
import com.example.data.model.VideoWatchHistory
import com.example.ai.AiErrorHandler
import com.example.ai.UpgradeSheetMode
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PartnersPageResult(
  val profiles: List<CommunityProfile>,
  val lastDocument: DocumentSnapshot? = null,
  val hasMore: Boolean = false,
  val fromCache: Boolean = false,
  val quotaExhausted: Boolean = false,
  val errorMessage: String? = null
)

class LingoRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  // --- User Profile ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val path = "users/$userId"
    val docRef = db.collection("users").document(userId)
    docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          try {
            val profile = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            profile?.let {
              if (it.userId.isNotBlank()) it else it.copy(userId = snapshot.id)
            }
          } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            UserProfile(
              userId = snapshot.getString("userId") ?: snapshot.id,
              displayName = snapshot.getString("displayName") ?: "Lingo Learner",
              nativeLanguageCode = snapshot.getString("nativeLanguageCode") ?: "en",
              targetLanguageCode = snapshot.getString("targetLanguageCode") ?: "es",
              proficiencyLevel = snapshot.getString("proficiencyLevel") ?: "Beginner",
              xpPoints = snapshot.getLong("xpPoints") ?: 0L,
              level = snapshot.getLong("level") ?: 1L,
              currentStreak = snapshot.getLong("currentStreak") ?: 1L,
              longestStreak = snapshot.getLong("longestStreak") ?: 1L,
              lastActiveDate = snapshot.getString("lastActiveDate") ?: "",
              dailyGoalMinutes = snapshot.getLong("dailyGoalMinutes") ?: 15L,
              subscriptionTier = snapshot.getString("subscriptionTier") ?: "free",
              purchaseToken = snapshot.getString("purchaseToken"),
              isFreeTrial = snapshot.getBoolean("isFreeTrial") ?: false,
              hasUsedFreeTrial = snapshot.getBoolean("hasUsedFreeTrial") ?: false,
              subscriptionAutoRenewing = snapshot.getBoolean("subscriptionAutoRenewing") ?: true,
              subscriptionExpiryEpochMs = snapshot.getLong("subscriptionExpiryEpochMs") ?: 0L
            )
          }
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        emit(UserProfile(userId = userId))
      }
      .collect { emit(it) }
  }

  suspend fun getUserProfile(userId: String): UserProfile? {
    val path = "users/$userId"
    return try {
      val snapshot = db.collection("users").document(userId).get().await()
      if (snapshot.exists()) {
        try {
          val profile = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          profile?.let {
            if (it.userId.isNotBlank()) it else it.copy(userId = snapshot.id)
          }
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.GET, path)
          UserProfile(
            userId = snapshot.getString("userId") ?: snapshot.id,
            displayName = snapshot.getString("displayName") ?: "Lingo Learner",
            nativeLanguageCode = snapshot.getString("nativeLanguageCode") ?: "en",
            targetLanguageCode = snapshot.getString("targetLanguageCode") ?: "es",
            proficiencyLevel = snapshot.getString("proficiencyLevel") ?: "Beginner",
            xpPoints = snapshot.getLong("xpPoints") ?: 0L,
            level = snapshot.getLong("level") ?: 1L,
            currentStreak = snapshot.getLong("currentStreak") ?: 1L,
            longestStreak = snapshot.getLong("longestStreak") ?: 1L,
            lastActiveDate = snapshot.getString("lastActiveDate") ?: "",
            dailyGoalMinutes = snapshot.getLong("dailyGoalMinutes") ?: 15L,
            subscriptionTier = snapshot.getString("subscriptionTier") ?: "free",
            purchaseToken = snapshot.getString("purchaseToken"),
            isFreeTrial = snapshot.getBoolean("isFreeTrial") ?: false,
            hasUsedFreeTrial = snapshot.getBoolean("hasUsedFreeTrial") ?: false,
            subscriptionAutoRenewing = snapshot.getBoolean("subscriptionAutoRenewing") ?: true,
            subscriptionExpiryEpochMs = snapshot.getLong("subscriptionExpiryEpochMs") ?: 0L
          )
        }
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      null
    }
  }

  suspend fun createUserProfile(profile: UserProfile) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
      val normalizedTier = when (profile.subscriptionTier.lowercase()) {
        "premium_pro_999", "pro_monthly", "pro" -> "premium_pro"
        "free", "premium_monthly", "premium_pro", "premium_yearly" -> profile.subscriptionTier.lowercase()
        else -> "free"
      }
      val payload = hashMapOf<String, Any>(
        "userId" to uid,
        "displayName" to profile.displayName.ifBlank { "Lingo Learner" },
        "nativeLanguageCode" to profile.nativeLanguageCode,
        "targetLanguageCode" to profile.targetLanguageCode,
        "activeLanguages" to profile.activeLanguages,
        "proficiencyLevel" to profile.proficiencyLevel,
        "xpPoints" to profile.xpPoints,
        "level" to profile.level,
        "currentStreak" to profile.currentStreak,
        "longestStreak" to profile.longestStreak,
        "lastActiveDate" to today,
        "dailyGoalMinutes" to profile.dailyGoalMinutes,
        "dailyAiSpeakingCount" to profile.dailyAiSpeakingCount,
        "dailyAiResetDate" to today,
        "subscriptionTier" to normalizedTier,
        "isFreeTrial" to profile.isFreeTrial,
        "hasUsedFreeTrial" to (profile.hasUsedFreeTrial || profile.isFreeTrial),
        "subscriptionAutoRenewing" to profile.subscriptionAutoRenewing,
        "subscriptionExpiryEpochMs" to profile.subscriptionExpiryEpochMs.coerceAtLeast(0L),
        "completedLessonIds" to profile.completedLessonIds,
        "achievements" to profile.achievements,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (!profile.purchaseToken.isNullOrBlank()) {
        payload["purchaseToken"] = profile.purchaseToken
      }
      if (profile.avatarUrl.isNotBlank()) {
        payload["avatarUrl"] = profile.avatarUrl
      }
      try {
        docRef.set(payload).await()
      } catch (e: Exception) {
        if (payload.containsKey("avatarUrl")) {
          payload.remove("avatarUrl")
          docRef.set(payload).await()
        } else {
          throw e
        }
      }
      syncCommunityProfile(profile.copy(userId = uid))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateUserProfile(profile: UserProfile) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val normalizedTier = when (profile.subscriptionTier.lowercase()) {
        "premium_pro_999", "pro_monthly", "pro" -> "premium_pro"
        "free", "premium_monthly", "premium_pro", "premium_yearly" -> profile.subscriptionTier.lowercase()
        else -> "free"
      }
      val payload = hashMapOf<String, Any>(
        "displayName" to profile.displayName,
        "nativeLanguageCode" to profile.nativeLanguageCode,
        "targetLanguageCode" to profile.targetLanguageCode,
        "activeLanguages" to profile.activeLanguages,
        "proficiencyLevel" to profile.proficiencyLevel,
        "xpPoints" to profile.xpPoints,
        "level" to profile.level,
        "currentStreak" to profile.currentStreak,
        "longestStreak" to profile.longestStreak,
        "lastActiveDate" to profile.lastActiveDate,
        "dailyGoalMinutes" to profile.dailyGoalMinutes,
        "dailyAiSpeakingCount" to profile.dailyAiSpeakingCount,
        "dailyAiResetDate" to profile.dailyAiResetDate,
        "subscriptionTier" to normalizedTier,
        "isFreeTrial" to profile.isFreeTrial,
        "hasUsedFreeTrial" to (profile.hasUsedFreeTrial || profile.isFreeTrial),
        "subscriptionAutoRenewing" to profile.subscriptionAutoRenewing,
        "subscriptionExpiryEpochMs" to profile.subscriptionExpiryEpochMs.coerceAtLeast(0L),
        "completedLessonIds" to profile.completedLessonIds,
        "achievements" to profile.achievements,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (!profile.purchaseToken.isNullOrBlank()) {
        payload["purchaseToken"] = profile.purchaseToken
      }
      if (profile.languageLevels.isNotEmpty()) {
        payload["languageLevels"] = profile.languageLevels
      }
      payload["avatarUrl"] = profile.avatarUrl
      try {
        docRef.update(payload).await()
      } catch (e: Exception) {
        val fallbackPayload = HashMap(payload).apply {
          remove("languageLevels")
          remove("avatarUrl")
        }
        try {
          docRef.update(fallbackPayload).await()
        } catch (_: Exception) {
          throw e
        }
      }
      syncCommunityProfile(profile.copy(userId = uid))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun saveUserPrivateEmail(email: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("private").document("info")
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "email" to email,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
    }
  }

  suspend fun saveFloatingContinuePrefs(prefsModel: com.example.data.model.FloatingContinueButtonPrefs) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("ui_preferences").document("floating_continue")
    val safeSide = if (prefsModel.snapSide.equals("left", ignoreCase = true)) "left" else "right"
    val safeFraction = prefsModel.verticalFraction.coerceIn(0f, 1f).toDouble()
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "enabled" to prefsModel.enabled,
          "snapSide" to safeSide,
          "verticalFraction" to safeFraction,
          "hasSeenDragHint" to prefsModel.hasSeenDragHint,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
    }
  }

  fun observeFloatingContinuePrefs(userId: String): Flow<com.example.data.model.FloatingContinueButtonPrefs?> = callbackFlow {
    if (userId.isBlank()) {
      trySend(null)
      close()
      return@callbackFlow
    }
    val docRef = db.collection("users").document(userId).collection("ui_preferences").document("floating_continue")
    val listener = docRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        trySend(null)
        return@addSnapshotListener
      }
      if (snapshot != null && snapshot.exists()) {
        val enabled = snapshot.getBoolean("enabled") ?: true
        val rawSide = snapshot.getString("snapSide") ?: "right"
        val safeSide = if (rawSide.equals("left", ignoreCase = true)) "left" else "right"
        val vertical = (snapshot.getDouble("verticalFraction") ?: 0.78).toFloat().coerceIn(0f, 1f)
        val hasSeenHint = snapshot.getBoolean("hasSeenDragHint") ?: false
        trySend(
          com.example.data.model.FloatingContinueButtonPrefs(
            enabled = enabled,
            snapSide = safeSide,
            verticalFraction = vertical,
            hasSeenDragHint = hasSeenHint
          )
        )
      } else {
        trySend(null)
      }
    }
    awaitClose { listener.remove() }
  }

  suspend fun clearUserLearningHistory(userId: String) {
    val uid = requireUserId()
    if (uid != userId) throw SecurityException("Cannot clear history of another user")
    try {
      val sessionSnapshots = db.collection("speaking_sessions").whereEqualTo("userId", uid).get().await()
      for (doc in sessionSnapshots.documents) {
        doc.reference.delete().await()
      }
    } catch (_: Exception) {}
    try {
      val historyDocs = db.collection("video_watch_history").whereEqualTo("userId", uid).get().await()
      for (doc in historyDocs.documents) doc.reference.delete().await()
    } catch (_: Exception) {}
  }

  suspend fun deleteUserAccountAndData(userId: String) {
    val uid = requireUserId()
    if (uid != userId) throw SecurityException("Cannot delete account of another user")
    try {
      // 1. Delete lesson progress
      val progressSnapshots = db.collection("lesson_progress").whereEqualTo("userId", uid).get().await()
      for (doc in progressSnapshots.documents) {
        doc.reference.delete().await()
      }
      // 2. Delete flashcards
      val flashcardSnapshots = db.collection("flashcards").whereEqualTo("userId", uid).get().await()
      for (doc in flashcardSnapshots.documents) {
        doc.reference.delete().await()
      }
      // 3. Delete speaking sessions
      val sessionSnapshots = db.collection("speaking_sessions").whereEqualTo("userId", uid).get().await()
      for (doc in sessionSnapshots.documents) {
        doc.reference.delete().await()
      }
      // 4. Delete community profile, blocks, notifications
      try {
        db.collection("community_profiles").document(uid).delete().await()
      } catch (ignored: Exception) {}
      try {
        val blockDocs = db.collection("user_blocks").whereEqualTo("userId", uid).get().await()
        for (doc in blockDocs.documents) doc.reference.delete().await()
      } catch (ignored: Exception) {}
      try {
        val notifDocs = db.collection("notifications").whereEqualTo("userId", uid).get().await()
        for (doc in notifDocs.documents) doc.reference.delete().await()
      } catch (ignored: Exception) {}
      try {
        val savedDocs = db.collection("saved_videos").whereEqualTo("userId", uid).get().await()
        for (doc in savedDocs.documents) doc.reference.delete().await()
      } catch (ignored: Exception) {}
      try {
        val playlistDocs = db.collection("video_playlists").whereEqualTo("userId", uid).get().await()
        for (doc in playlistDocs.documents) doc.reference.delete().await()
      } catch (ignored: Exception) {}
      try {
        val historyDocs = db.collection("video_watch_history").whereEqualTo("userId", uid).get().await()
        for (doc in historyDocs.documents) doc.reference.delete().await()
      } catch (ignored: Exception) {}
      try {
        val fcmDocs = db.collection("fcm_tokens").whereEqualTo("userId", uid).get().await()
        for (doc in fcmDocs.documents) doc.reference.delete().await()
      } catch (ignored: Exception) {}
      // 5. Delete private info and AI usage quotas
      try {
        db.collection("users").document(uid).collection("private").document("info").delete().await()
      } catch (ignored: Exception) {}
      try {
        db.collection("ai_usage_quotas").document(uid).delete().await()
      } catch (ignored: Exception) {}
      try {
        val sumRef = db.collection("referral_summaries").document(uid)
        val sumSnap = sumRef.get().await()
        val refCode = sumSnap.getString("referralCode").orEmpty()
        if (refCode.isNotBlank()) {
          runCatching { db.collection("referral_codes").document(refCode).delete().await() }
        }
        sumRef.delete().await()
      } catch (ignored: Exception) {}
      // 6. Delete user profile
      db.collection("users").document(uid).delete().await()
      // 7. Delete Firebase Auth user
      auth.currentUser?.delete()?.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/$uid")
      throw e
    }
  }

  // --- Community Profile & Global Partner Discovery ---

  private val _communityProfilesLoadError = MutableStateFlow<String?>(null)
  val communityProfilesLoadError: StateFlow<String?> = _communityProfilesLoadError.asStateFlow()

  private val _partnersDirectoryFlow = MutableStateFlow<List<CommunityProfile>>(emptyList())
  val partnersQuotaExhausted: StateFlow<Boolean> = FirestoreQuotaManager.isQuotaExhausted

  private fun isFailedPreconditionError(throwable: Throwable?): Boolean {
    var curr: Throwable? = throwable
    var depth = 0
    while (curr != null && depth < 10) {
      if (curr is FirebaseFirestoreException &&
        curr.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
      ) {
        return true
      }
      val msg = curr.message?.lowercase().orEmpty()
      if (msg.contains("failed_precondition") || msg.contains("requires an index")) {
        return true
      }
      curr = curr.cause
      depth++
    }
    return false
  }

  private fun recordListenerError(error: Throwable, op: OperationType, path: String) {
    if (error is Exception) handleFirestoreError(error, op, path)
    if (FirestoreQuotaManager.isQuotaExhaustedError(error)) {
      FirestoreQuotaManager.recordQuotaExhausted(error)
      _communityProfilesLoadError.value = FirestoreQuotaManager.lastQuotaErrorMessage.value
    }
  }

  private fun normalizeProficiencyBand(raw: String?): String {
    val clean = raw?.trim().orEmpty()
    return when {
      clean.equals("Advanced", ignoreCase = true) ||
        clean.equals("Native", ignoreCase = true) ||
        clean.startsWith("C1", ignoreCase = true) ||
        clean.startsWith("C2", ignoreCase = true) -> "Advanced"
      clean.equals("Intermediate", ignoreCase = true) ||
        clean.startsWith("B1", ignoreCase = true) ||
        clean.startsWith("B2", ignoreCase = true) -> "Intermediate"
      else -> "Beginner"
    }
  }

  private fun parseCommunityProfileDocument(doc: DocumentSnapshot): CommunityProfile? {
    if (!doc.exists()) return null
    val parsed = try {
      doc.toObject(CommunityProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (_: Exception) {
      null
    }
    if (parsed != null) {
      val uid = parsed.userId.ifBlank { doc.id }
      if (uid.isBlank()) return null
      return parsed.copy(
        userId = uid,
        displayName = parsed.displayName.ifBlank { "Lingo Partner" },
        country = parsed.country.ifBlank { "Global" },
        countryFlag = parsed.countryFlag.ifBlank { "🌍" },
        nativeLanguageCode = parsed.nativeLanguageCode.ifBlank { "en" },
        targetLanguageCode = parsed.targetLanguageCode.ifBlank { "es" },
        proficiencyLevel = normalizeProficiencyBand(parsed.proficiencyLevel)
      )
    }
    return try {
      val uid = (doc.get("userId") as? String)?.takeIf { it.isNotBlank() } ?: doc.id
      if (uid.isBlank()) return null
      val displayName = (doc.get("displayName") as? String)?.takeIf { it.isNotBlank() } ?: "Lingo Partner"
      val avatarUrl = (doc.get("avatarUrl") as? String).orEmpty()
      val country = (doc.get("country") as? String)?.takeIf { it.isNotBlank() } ?: "Global"
      val countryFlag = (doc.get("countryFlag") as? String)?.takeIf { it.isNotBlank() } ?: "🌍"
      val bio = (doc.get("bio") as? String)?.takeIf { it.isNotBlank() } ?: "Practicing languages on LingoSphere"
      val rawInterests = doc.get("interests")
      val interests = when (rawInterests) {
        is List<*> -> rawInterests.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }.take(15)
        is String -> rawInterests.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(15)
        else -> listOf("Travel", "Culture", "Music")
      }
      val nativeLang = (doc.get("nativeLanguageCode") as? String)?.takeIf { it.isNotBlank() } ?: "en"
      val targetLang = (doc.get("targetLanguageCode") as? String)?.takeIf { it.isNotBlank() } ?: "es"
      val proficiency = normalizeProficiencyBand(doc.get("proficiencyLevel")?.toString())
      val xp = (doc.get("xpPoints") as? Number)?.toLong() ?: 0L
      val lvl = ((doc.get("level") as? Number)?.toLong() ?: 1L).coerceAtLeast(1L)
      val streak = (doc.get("currentStreak") as? Number)?.toLong() ?: 1L
      val isOnline = (doc.get("isOnline") as? Boolean) ?: false
      val allowCalls = (doc.get("allowCalls") as? Boolean) ?: true
      val presenceState = (doc.get("presenceState") as? String)?.takeIf { it.isNotBlank() }
        ?: if (isOnline) "online" else "offline"
      val lastSeenMs = (doc.get("lastSeenEpochMs") as? Number)?.toLong() ?: 0L
      val lastHbMs = (doc.get("lastHeartbeatEpochMs") as? Number)?.toLong() ?: lastSeenMs
      val showLastSeen = (doc.get("showLastSeen") as? Boolean) ?: true
      val updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      CommunityProfile(
        userId = uid,
        displayName = displayName,
        avatarUrl = avatarUrl,
        country = country,
        countryFlag = countryFlag,
        bio = bio,
        interests = if (interests.isEmpty()) listOf("Travel", "Culture", "Music") else interests,
        nativeLanguageCode = nativeLang,
        targetLanguageCode = targetLang,
        proficiencyLevel = proficiency,
        xpPoints = xp,
        level = lvl,
        currentStreak = streak,
        isOnline = isOnline,
        allowCalls = allowCalls,
        presenceState = presenceState,
        lastSeenEpochMs = lastSeenMs,
        lastHeartbeatEpochMs = lastHbMs,
        showLastSeen = showLastSeen,
        updatedAt = updatedAt
      )
    } catch (_: Exception) {
      null
    }
  }

  suspend fun fetchCommunityProfileById(partnerId: String): CommunityProfile? {
    if (partnerId.isBlank()) return null
    if (FirestoreQuotaManager.shouldBlockRead()) return null
    return try {
      val doc = db.collection("community_profiles").document(partnerId).get().await()
      FirestoreDiagnostics.recordRead(1, "community_profile_by_id")
      FirestoreQuotaManager.recordReadSuccess()
      parseCommunityProfileDocument(doc)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (FirestoreQuotaManager.isQuotaExhaustedError(e)) {
        FirestoreQuotaManager.recordQuotaExhausted(e)
      }
      null
    }
  }

  suspend fun syncCommunityProfile(
    profile: UserProfile,
    country: String? = null,
    countryFlag: String? = null,
    bio: String? = null,
    interests: List<String>? = null,
    isOnline: Boolean? = null,
    allowCalls: Boolean? = null,
    presenceState: String? = null,
    showLastSeen: Boolean? = null
  ) {
    val uid = auth.currentUser?.uid ?: return
    val docRef = db.collection("community_profiles").document(uid)
    try {
      val existingSnap = try {
        docRef.get().await()
      } catch (_: Exception) {
        null
      }
      val existingComm = existingSnap?.let { parseCommunityProfileDocument(it) }

      val nowMs = System.currentTimeMillis()
      val resolvedOnline = isOnline ?: existingComm?.isOnline ?: true
      val rawPresence = presenceState ?: if (resolvedOnline) "online" else "offline"
      val resolvedPresenceState = if (rawPresence in listOf("online", "away", "offline", "in_call", "available_to_match", "busy")) {
        rawPresence
      } else if (resolvedOnline) {
        "online"
      } else {
        "offline"
      }
      val resolvedShowLastSeen = showLastSeen ?: existingComm?.showLastSeen ?: true
      val effectiveAvatarUrl = profile.avatarUrl.ifBlank { existingComm?.avatarUrl.orEmpty() }
      val resolvedCountry = (country ?: existingComm?.country).orEmpty().trim().ifBlank { "United States" }.take(60)
      val resolvedFlag = (countryFlag ?: existingComm?.countryFlag).orEmpty().trim().ifBlank { "🇺🇸" }.take(16)
      val resolvedBio = (bio ?: existingComm?.bio).orEmpty().trim()
        .ifBlank { "Practicing languages & real-time conversations on LingoSphere!" }
        .take(400)
      val resolvedInterests = (interests ?: existingComm?.interests ?: listOf("Travel", "Culture", "Music"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(15)
        .ifEmpty { listOf("Travel", "Culture", "Music") }

      val payload = hashMapOf<String, Any>(
        "userId" to uid,
        "displayName" to profile.displayName.trim().ifBlank { existingComm?.displayName ?: "Lingo Learner" }.take(80),
        "country" to resolvedCountry,
        "countryFlag" to resolvedFlag,
        "bio" to resolvedBio,
        "interests" to resolvedInterests,
        "nativeLanguageCode" to profile.nativeLanguageCode.trim().ifBlank { "en" }.take(10),
        "targetLanguageCode" to profile.targetLanguageCode.trim().ifBlank { "es" }.take(10),
        "proficiencyLevel" to normalizeProficiencyBand(profile.proficiencyLevel),
        "xpPoints" to profile.xpPoints.coerceAtLeast(0L),
        "level" to profile.level.coerceAtLeast(1L),
        "currentStreak" to profile.currentStreak.coerceAtLeast(0L),
        "isOnline" to resolvedOnline,
        "allowCalls" to (allowCalls ?: existingComm?.allowCalls ?: true),
        "presenceState" to resolvedPresenceState,
        "lastSeenEpochMs" to nowMs,
        "lastHeartbeatEpochMs" to nowMs,
        "showLastSeen" to resolvedShowLastSeen,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (effectiveAvatarUrl.isNotBlank() && effectiveAvatarUrl.length <= 150000) {
        payload["avatarUrl"] = effectiveAvatarUrl
      }
      try {
        docRef.set(payload).await()
      } catch (_: Exception) {
        if (payload.containsKey("avatarUrl")) {
          payload.remove("avatarUrl")
          docRef.set(payload).await()
        }
      }
    } catch (e: Exception) {
      // Do not fail user profile operations if community sync encounters transient error
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
    }
  }

  fun observeMyCommunityProfile(userId: String): Flow<CommunityProfile?> = flow {
    val path = "community_profiles/$userId"
    db.collection("community_profiles").document(userId)
      .snapshots()
      .onStart { FirestoreDiagnostics.recordListenerAttach("my_community_profile") }
      .onCompletion { FirestoreDiagnostics.recordListenerDetach("my_community_profile") }
      .map { snap -> parseCommunityProfileDocument(snap) }
      .catch { error ->
        recordListenerError(error, OperationType.GET, path)
        emit(null)
      }
      .collect { emit(it) }
  }

  /**
   * Fetches one paginated, server-filtered slice of `community_profiles`.
   * Replaces full-collection scans with cursor pagination (`.limit(pageSize)` + `.startAfter(lastDoc)`),
   * TTL caching, exponential backoff on transient errors, and immediate halt on `RESOURCE_EXHAUSTED`.
   */
  suspend fun fetchPartnersPage(
    filterLanguageCode: String = "ALL",
    filterCountry: String = "ALL",
    myTargetLanguageCode: String = "en",
    pageSize: Int = 15,
    lastDoc: DocumentSnapshot? = null,
    forceRefresh: Boolean = false
  ): PartnersPageResult {
    val path = "community_profiles"
    val normalizedLang = filterLanguageCode.trim().ifBlank { "ALL" }
    val normalizedCountry = filterCountry.trim().ifBlank { "ALL" }
    val cacheKey = PartnerProfileCache.buildKey(
      filterLanguageCode = if (normalizedLang.equals("TANDEM", ignoreCase = true)) {
        "TANDEM_${myTargetLanguageCode.trim()}"
      } else {
        normalizedLang
      },
      filterCountry = normalizedCountry
    )

    if (forceRefresh) {
      FirestoreQuotaManager.clearCooldownForManualRetry()
    }

    // Return valid cached page on initial page load unless force-refreshed
    if (lastDoc == null && !forceRefresh) {
      val cached = PartnerProfileCache.getPage(cacheKey)
      if (cached != null) {
        _communityProfilesLoadError.value = null
        mergeIntoPartnersDirectory(cached.profiles)
        return PartnersPageResult(
          profiles = cached.profiles,
          lastDocument = null,
          hasMore = cached.hasMore,
          fromCache = true
        )
      }
    }

    // Circuit breaker: halt reads while quota cooldown is active
    if (FirestoreQuotaManager.shouldBlockRead()) {
      val msg = FirestoreQuotaManager.lastQuotaErrorMessage.value
        ?: "Daily Firebase cloud read limit reached."
      _communityProfilesLoadError.value = msg
      return PartnersPageResult(
        profiles = emptyList(),
        hasMore = false,
        quotaExhausted = true,
        errorMessage = msg
      )
    }

    var attempt = 0
    val maxAttempts = 2
    var includeOrderBy = true
    while (attempt < maxAttempts) {
      try {
        var query: Query = db.collection(path)
        if (!normalizedCountry.equals("ALL", ignoreCase = true)) {
          query = query.whereEqualTo("country", normalizedCountry)
        }
        when {
          normalizedLang.equals("TANDEM", ignoreCase = true) && myTargetLanguageCode.isNotBlank() -> {
            query = query.whereEqualTo("nativeLanguageCode", myTargetLanguageCode)
          }
          !normalizedLang.equals("ALL", ignoreCase = true) -> {
            query = query.whereEqualTo("nativeLanguageCode", normalizedLang)
          }
        }
        if (includeOrderBy) {
          query = query.orderBy("xpPoints", Query.Direction.DESCENDING)
        }
        if (lastDoc != null) {
          query = query.startAfter(lastDoc)
        }
        query = query.limit(pageSize.coerceIn(1, 30).toLong())

        val snapshot = query.get().await()
        FirestoreDiagnostics.recordRead(snapshot.size(), "partners_page")
        FirestoreQuotaManager.recordReadSuccess()
        _communityProfilesLoadError.value = null

        val parsed = snapshot.documents
          .mapNotNull { doc -> parseCommunityProfileDocument(doc) }
          .filter { it.userId.isNotBlank() }
          .distinctBy { it.userId }
        val newLastDoc = snapshot.documents.lastOrNull()
        val hasMore = snapshot.documents.size >= pageSize.coerceIn(1, 30)

        if (lastDoc == null) {
          PartnerProfileCache.putPage(
            cacheKey = cacheKey,
            profiles = parsed,
            hasMore = hasMore,
            lastDocId = newLastDoc?.id
          )
        }
        mergeIntoPartnersDirectory(parsed)

        return PartnersPageResult(
          profiles = parsed,
          lastDocument = newLastDoc,
          hasMore = hasMore,
          fromCache = false
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        if (FirestoreQuotaManager.isQuotaExhaustedError(e)) {
          FirestoreQuotaManager.recordQuotaExhausted(e)
          val msg = FirestoreQuotaManager.lastQuotaErrorMessage.value
            ?: "Daily Firebase cloud read limit reached."
          _communityProfilesLoadError.value = msg
          handleFirestoreError(e, OperationType.LIST, path)
          return PartnersPageResult(
            profiles = emptyList(),
            hasMore = false,
            quotaExhausted = true,
            errorMessage = msg
          )
        }
        if (includeOrderBy && isFailedPreconditionError(e)) {
          // Composite index may still be deploying; retry once without orderBy
          includeOrderBy = false
          attempt++
          continue
        }
        attempt++
        if (attempt >= maxAttempts) {
          val msg = e.localizedMessage ?: "Failed to load community profiles from Firebase."
          _communityProfilesLoadError.value = msg
          handleFirestoreError(e, OperationType.LIST, path)
          return PartnersPageResult(
            profiles = emptyList(),
            hasMore = false,
            errorMessage = msg
          )
        }
        delay(1000L * attempt)
      }
    }
    return PartnersPageResult(profiles = emptyList(), hasMore = false)
  }

  suspend fun fetchTopXpLeaderboard(
    limitCount: Int = 20,
    forceRefresh: Boolean = false
  ): List<CommunityProfile> {
    val safeLimit = limitCount.coerceIn(5, 30)
    val cacheKey = "TOP_XP|$safeLimit"
    if (forceRefresh) {
      FirestoreQuotaManager.clearCooldownForManualRetry()
    } else {
      PartnerProfileCache.getPage(cacheKey)?.let { cached ->
        return cached.profiles
      }
    }

    if (FirestoreQuotaManager.shouldBlockRead()) {
      return emptyList()
    }

    return try {
      val snap = db.collection("community_profiles")
        .orderBy("xpPoints", Query.Direction.DESCENDING)
        .limit(safeLimit.toLong())
        .get()
        .await()
      FirestoreDiagnostics.recordRead(snap.size(), "top_xp_leaderboard")
      FirestoreQuotaManager.recordReadSuccess()
      val list = snap.documents
        .mapNotNull { parseCommunityProfileDocument(it) }
        .filter { it.userId.isNotBlank() }
        .distinctBy { it.userId }
      PartnerProfileCache.putPage(cacheKey, list, hasMore = false, lastDocId = null)
      mergeIntoPartnersDirectory(list)
      list
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (FirestoreQuotaManager.isQuotaExhaustedError(e)) {
        FirestoreQuotaManager.recordQuotaExhausted(e)
        _communityProfilesLoadError.value = FirestoreQuotaManager.lastQuotaErrorMessage.value
      }
      handleFirestoreError(e, OperationType.LIST, "community_profiles/leaderboard")
      emptyList()
    }
  }

  private fun mergeIntoPartnersDirectory(incoming: List<CommunityProfile>) {
    if (incoming.isEmpty()) return
    val current = _partnersDirectoryFlow.value
    val merged = LinkedHashMap<String, CommunityProfile>(current.size + incoming.size)
    for (p in current) {
      if (p.userId.isNotBlank()) merged[p.userId] = p
    }
    for (p in incoming) {
      if (p.userId.isNotBlank()) merged[p.userId] = p
    }
    _partnersDirectoryFlow.value = merged.values.toList()
  }

  suspend fun fetchCommunityProfilesOnce(): List<CommunityProfile> {
    return fetchPartnersPage(pageSize = 20, lastDoc = null, forceRefresh = false).profiles
  }

  /**
   * Shared directory stream backed by bounded paginated fetches + cache (no unbounded collection listener).
   */
  fun observeCommunityProfiles(): Flow<List<CommunityProfile>> = _partnersDirectoryFlow.asStateFlow()

  // --- Friend Requests & Connections ---

  fun observeFriendRequests(userId: String): Flow<List<FriendRequest>> {
    val sentFlow = db.collection("friend_requests")
      .whereEqualTo("senderId", userId)
      .limit(50)
      .snapshots()
      .onStart { FirestoreDiagnostics.recordListenerAttach("friend_requests_sent") }
      .onCompletion { FirestoreDiagnostics.recordListenerDetach("friend_requests_sent") }
      .map { it.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { err ->
        recordListenerError(err, OperationType.LIST, "friend_requests")
        emit(emptyList())
      }

    val receivedFlow = db.collection("friend_requests")
      .whereEqualTo("receiverId", userId)
      .limit(50)
      .snapshots()
      .onStart { FirestoreDiagnostics.recordListenerAttach("friend_requests_received") }
      .onCompletion { FirestoreDiagnostics.recordListenerDetach("friend_requests_received") }
      .map { it.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { err ->
        recordListenerError(err, OperationType.LIST, "friend_requests")
        emit(emptyList())
      }

    return combine(sentFlow, receivedFlow) { sent, received ->
      (sent + received).distinctBy { it.id }
    }
  }

  suspend fun sendFriendRequest(senderName: String, receiverId: String, receiverName: String) {
    val uid = requireUserId()
    if (receiverId.isBlank() || receiverId == uid) return
    val reverseRequestId = "freq_${receiverId}_${uid}"
    val reverseDocRef = db.collection("friend_requests").document(reverseRequestId)
    try {
      val reverseSnap = reverseDocRef.get().await()
      if (reverseSnap.exists() && reverseSnap.getString("status") == "pending") {
        reverseDocRef.update(
          mapOf(
            "status" to "accepted",
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
        return
      }
    } catch (_: Exception) {}

    val requestId = "freq_${uid}_${receiverId}"
    val docRef = db.collection("friend_requests").document(requestId)
    try {
      docRef.set(
        mapOf(
          "senderId" to uid,
          "senderName" to senderName.ifBlank { "Lingo Learner" },
          "receiverId" to receiverId,
          "receiverName" to receiverName.ifBlank { "Partner" },
          "status" to "pending",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      createNotification(
        targetUserId = receiverId,
        title = "New Friend Request",
        message = "$senderName sent you a language partner request!",
        type = "friend_request",
        targetRoute = "speaking_invitation",
        targetId = uid,
        senderId = uid,
        senderName = senderName.ifBlank { "Lingo Learner" }
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun sendSpeakingInvitation(
    senderName: String,
    receiverId: String,
    receiverName: String,
    topic: String = "Daily English Speaking Practice"
  ) {
    val uid = requireUserId()
    if (receiverId.isBlank() || receiverId == uid) return
    val cleanSender = senderName.ifBlank { "Speaking Partner" }
    val inviteMsg = "👋 Hi ${receiverName.ifBlank { "there" }}! Let's practice speaking about: \"$topic\""
    sendDirectMessage(
      senderName = cleanSender,
      receiverId = receiverId,
      text = inviteMsg
    )
    createNotification(
      targetUserId = receiverId,
      title = "Speaking Invitation from $cleanSender",
      message = "$cleanSender invited you to practice speaking: \"$topic\"",
      type = "speaking_invitation",
      targetRoute = "speaking_invitation",
      targetId = uid,
      senderId = uid,
      senderName = cleanSender
    )
  }

  suspend fun respondToFriendRequest(requestId: String, accept: Boolean) {
    requireUserId()
    val docRef = db.collection("friend_requests").document(requestId)
    try {
      docRef.update(
        mapOf(
          "status" to if (accept) "accepted" else "declined",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun cancelOrRemoveFriendRequest(requestId: String) {
    requireUserId()
    if (requestId.isBlank()) return
    val docRef = db.collection("friend_requests").document(requestId)
    try {
      docRef.delete().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
    }
  }

  // --- Direct Real-Time Chat Messages ---

  fun buildChatRoomId(uid1: String, uid2: String): String {
    return if (uid1 < uid2) "chat_${uid1}_${uid2}" else "chat_${uid2}_${uid1}"
  }

  fun observeChatMessages(partnerId: String): Flow<List<DirectChatMessage>> {
    val uid = auth.currentUser?.uid ?: return flow { emit(emptyList()) }
    val roomId = buildChatRoomId(uid, partnerId)

    val sentFlow = flow {
      db.collection("chat_messages")
        .whereEqualTo("senderId", uid)
        .whereEqualTo("chatRoomId", roomId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(40)
        .snapshots()
        .onStart { FirestoreDiagnostics.recordListenerAttach("chat_sent_$roomId") }
        .onCompletion { FirestoreDiagnostics.recordListenerDetach("chat_sent_$roomId") }
        .map { snap ->
          snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .collect { emit(it) }
    }.catch { err ->
      if (isFailedPreconditionError(err)) {
        db.collection("chat_messages")
          .whereEqualTo("senderId", uid)
          .whereEqualTo("chatRoomId", roomId)
          .limit(40)
          .snapshots()
          .map { it.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
          .catch { emit(emptyList()) }
          .collect { emit(it) }
      } else {
        recordListenerError(err, OperationType.LIST, "chat_messages")
        emit(emptyList())
      }
    }

    val receivedFlow = flow {
      db.collection("chat_messages")
        .whereEqualTo("receiverId", uid)
        .whereEqualTo("chatRoomId", roomId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(40)
        .snapshots()
        .onStart { FirestoreDiagnostics.recordListenerAttach("chat_recv_$roomId") }
        .onCompletion { FirestoreDiagnostics.recordListenerDetach("chat_recv_$roomId") }
        .map { snap ->
          snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .collect { emit(it) }
    }.catch { err ->
      if (isFailedPreconditionError(err)) {
        db.collection("chat_messages")
          .whereEqualTo("receiverId", uid)
          .whereEqualTo("chatRoomId", roomId)
          .limit(40)
          .snapshots()
          .map { it.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
          .catch { emit(emptyList()) }
          .collect { emit(it) }
      } else {
        recordListenerError(err, OperationType.LIST, "chat_messages")
        emit(emptyList())
      }
    }

    return combine(sentFlow, receivedFlow) { sent, received ->
      (sent + received)
        .distinctBy { it.id }
        .sortedWith(
          compareBy<DirectChatMessage> { it.createdAt?.seconds ?: 0L }
            .thenBy { it.createdAt?.nanoseconds ?: 0 }
        )
    }
  }

  fun observeAllMyChatMessages(userId: String): Flow<List<DirectChatMessage>> {
    if (userId.isBlank()) return flow { emit(emptyList()) }

    val sentFlow = flow {
      db.collection("chat_messages")
        .whereEqualTo("senderId", userId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(30)
        .snapshots()
        .onStart { FirestoreDiagnostics.recordListenerAttach("all_chat_sent") }
        .onCompletion { FirestoreDiagnostics.recordListenerDetach("all_chat_sent") }
        .map { snap ->
          snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { it.receiverId.isNotBlank() && it.receiverId != "PUBLIC_ROOM" && !it.chatRoomId.startsWith("roomchat_") }
        }
        .collect { emit(it) }
    }.catch { err ->
      if (isFailedPreconditionError(err)) {
        db.collection("chat_messages")
          .whereEqualTo("senderId", userId)
          .limit(30)
          .snapshots()
          .map { snap ->
            snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .filter { it.receiverId.isNotBlank() && it.receiverId != "PUBLIC_ROOM" && !it.chatRoomId.startsWith("roomchat_") }
          }
          .catch { emit(emptyList()) }
          .collect { emit(it) }
      } else {
        recordListenerError(err, OperationType.LIST, "chat_messages")
        emit(emptyList())
      }
    }

    val receivedFlow = flow {
      db.collection("chat_messages")
        .whereEqualTo("receiverId", userId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(30)
        .snapshots()
        .onStart { FirestoreDiagnostics.recordListenerAttach("all_chat_recv") }
        .onCompletion { FirestoreDiagnostics.recordListenerDetach("all_chat_recv") }
        .map { snap ->
          snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { it.senderId.isNotBlank() && !it.chatRoomId.startsWith("roomchat_") }
        }
        .collect { emit(it) }
    }.catch { err ->
      if (isFailedPreconditionError(err)) {
        db.collection("chat_messages")
          .whereEqualTo("receiverId", userId)
          .limit(30)
          .snapshots()
          .map { snap ->
            snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .filter { it.senderId.isNotBlank() && !it.chatRoomId.startsWith("roomchat_") }
          }
          .catch { emit(emptyList()) }
          .collect { emit(it) }
      } else {
        recordListenerError(err, OperationType.LIST, "chat_messages")
        emit(emptyList())
      }
    }

    return combine(sentFlow, receivedFlow) { sent, received ->
      (sent + received)
        .distinctBy { it.id }
        .sortedWith(
          compareByDescending<DirectChatMessage> { it.createdAt?.seconds ?: 0L }
            .thenByDescending { it.createdAt?.nanoseconds ?: 0 }
        )
    }
  }

  suspend fun sendDirectMessage(
    senderName: String,
    receiverId: String,
    text: String,
    translatedText: String? = null,
    messageId: String? = null,
    messageType: String = "text",
    replyToMessageId: String = "",
    replyToSenderName: String = "",
    replyToText: String = "",
    audioDataBase64: String = "",
    audioDurationMs: Long = 0L,
    audioMimeType: String = "audio/mp4",
    audioWaveform: List<Int> = emptyList(),
    scheduleTopic: String = "",
    scheduleDateLabel: String = "",
    scheduleTimeLabel: String = "",
    scheduleTimezone: String = "",
    scheduleStatus: String = "pending",
    scheduleProposedText: String = ""
  ): String {
    val uid = requireUserId()
    val roomId = buildChatRoomId(uid, receiverId)
    val resolvedId = messageId?.takeIf { it.isNotBlank() } ?: "msg_${UUID.randomUUID()}"
    val docRef = db.collection("chat_messages").document(resolvedId)
    try {
      val cleanText = text.trim().ifBlank {
        when (messageType) {
          "voice" -> "🎤 Voice message (${(audioDurationMs / 1000L).coerceAtLeast(1L)}s)"
          "schedule_invite" -> "📅 Language Exchange Invitation: $scheduleTopic"
          else -> "Message"
        }
      }.take(2000)

      val payload = hashMapOf<String, Any>(
        "chatRoomId" to roomId,
        "senderId" to uid,
        "senderName" to senderName.ifBlank { "Learner" }.take(80),
        "receiverId" to receiverId,
        "text" to cleanText,
        "messageType" to messageType,
        "deliveryStatus" to "delivered",
        "createdAt" to FieldValue.serverTimestamp()
      )
      if (!translatedText.isNullOrBlank()) {
        payload["translatedText"] = translatedText.take(2000)
      }
      if (replyToMessageId.isNotBlank()) {
        payload["replyToMessageId"] = replyToMessageId.take(128)
        payload["replyToSenderName"] = replyToSenderName.take(80)
        payload["replyToText"] = replyToText.take(500)
      }
      if (audioDataBase64.isNotBlank()) {
        payload["audioDataBase64"] = audioDataBase64.take(450000)
        payload["audioDurationMs"] = audioDurationMs.coerceIn(0L, 600000L)
        payload["audioMimeType"] = audioMimeType.take(64)
        payload["audioWaveform"] = audioWaveform.take(80).map { it.coerceIn(0, 100) }
      }
      if (messageType == "schedule_invite" || scheduleTopic.isNotBlank()) {
        payload["scheduleTopic"] = scheduleTopic.take(200)
        payload["scheduleDateLabel"] = scheduleDateLabel.take(120)
        payload["scheduleTimeLabel"] = scheduleTimeLabel.take(120)
        payload["scheduleTimezone"] = scheduleTimezone.take(80)
        payload["scheduleStatus"] = scheduleStatus
        if (scheduleProposedText.isNotBlank()) {
          payload["scheduleProposedText"] = scheduleProposedText.take(240)
        }
      }
      docRef.set(payload).await()
      return resolvedId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun markConversationMessagesRead(partnerId: String, incomingMessages: List<DirectChatMessage>) {
    val uid = auth.currentUser?.uid ?: return
    val unread = incomingMessages.filter {
      it.senderId == partnerId && it.receiverId == uid && it.deliveryStatus != "read" && it.id.isNotBlank()
    }
    if (unread.isEmpty()) return
    val nowMs = System.currentTimeMillis()
    unread.forEach { msg ->
      val docRef = db.collection("chat_messages").document(msg.id)
      try {
        docRef.update(
          mapOf(
            "deliveryStatus" to "read",
            "readAtEpochMs" to nowMs
          )
        ).await()
      } catch (_: Exception) {}
    }
  }

  suspend fun updateDirectMessageFields(messageId: String, updates: Map<String, Any?>) {
    requireUserId()
    if (messageId.isBlank() || updates.isEmpty()) return
    val docRef = db.collection("chat_messages").document(messageId)
    try {
      val cleanUpdates = updates.filterValues { it != null }.mapValues { it.value as Any }
      docRef.update(cleanUpdates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun deleteDirectMessage(messageId: String) {
    val uid = requireUserId()
    if (messageId.isBlank()) return
    val docRef = db.collection("chat_messages").document(messageId)
    try {
      val snap = docRef.get().await()
      if (snap.exists() && snap.getString("senderId") == uid) {
        docRef.delete().await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
    }
  }

  // --- Real-Time Voice & Video Call Rooms, Speaking Rooms & Smart Matching ---

  suspend fun isUserBusyInCall(targetUserId: String): Boolean {
    if (targetUserId.isBlank() || FirestoreQuotaManager.shouldBlockRead()) return false
    return try {
      val nowMs = System.currentTimeMillis()
      val activeStatuses = listOf("ringing", "accepted", "connected")
      val asCaller = db.collection("call_rooms")
        .whereEqualTo("callerId", targetUserId)
        .limit(5)
        .get()
        .await()
      val asCallee = db.collection("call_rooms")
        .whereEqualTo("calleeId", targetUserId)
        .limit(5)
        .get()
        .await()
      FirestoreDiagnostics.recordRead(asCaller.size() + asCallee.size(), "busy_check")

      (asCaller.documents + asCallee.documents).any { doc ->
        val st = doc.getString("status") ?: ""
        val updatedTs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: nowMs
        st in activeStatuses && (nowMs - updatedTs) < 120_000L
      }
    } catch (_: Exception) {
      false
    }
  }

  suspend fun createOrJoinCallRoom(
    callerName: String,
    calleeId: String,
    calleeName: String,
    callType: String,
    matchMode: String,
    targetLanguageCode: String,
    callerNativeLang: String = "en",
    callerLearningLang: String = targetLanguageCode,
    calleeNativeLang: String = targetLanguageCode,
    calleeLearningLang: String = "en",
    conversationTopic: String = ""
  ): CallRoom {
    val uid = requireUserId()
    val allowedMatchModes = setOf("direct", "random", "country", "interest", "quick", "language", "speaking_room")
    val normalizedMatchMode = when (val lower = matchMode.trim().lowercase()) {
      in allowedMatchModes -> lower
      "anyone" -> "random"
      else -> "random"
    }
    val normalizedCallType = if (callType.trim().equals("video", ignoreCase = true)) "video" else "voice"
    val normalizedTargetLang = targetLanguageCode.trim().takeIf { it.length >= 2 }?.take(10) ?: "en"
    val normalizedCallerName = callerName.trim().ifBlank { "Lingo Learner" }.take(80)
    val normalizedCalleeName = calleeName.trim().take(80)
    val normalizedCallerNativeLang = callerNativeLang.trim().ifBlank { "en" }.take(10)
    val normalizedCallerLearningLang = callerLearningLang.trim().ifBlank { normalizedTargetLang }.take(10)
    val normalizedCalleeNativeLang = calleeNativeLang.trim().ifBlank { normalizedTargetLang }.take(10)
    val normalizedCalleeLearningLang = calleeLearningLang.trim().ifBlank { "en" }.take(10)
    val normalizedTopic = conversationTopic.trim().take(300)

    try {
      // If matching mode (random, country, interest), atomically search and claim an open waiting room from another real learner
      if (normalizedMatchMode != "direct" && normalizedMatchMode != "speaking_room") {
        val nowMs = System.currentTimeMillis()
        val waitingSnap = db.collection("call_rooms")
          .whereEqualTo("status", "waiting")
          .limit(10)
          .get()
          .await()
        FirestoreDiagnostics.recordRead(waitingSnap.size(), "matchmaking_waiting_rooms")

        val candidates = waitingSnap.documents.filter { doc ->
          val caller = doc.getString("callerId") ?: ""
          val mode = doc.getString("matchMode") ?: ""
          val lang = doc.getString("targetLanguageCode") ?: ""
          val topic = doc.getString("conversationTopic") ?: ""
          val updatedMs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: nowMs
          val isRecent = (nowMs - updatedMs) <= 120_000L
          val langCompatible = normalizedMatchMode == "random" || lang == normalizedTargetLang ||
            doc.getString("callerNativeLang") == normalizedTargetLang
          val topicCompatible = normalizedMatchMode != "interest" || normalizedTopic.isBlank() ||
            topic.equals(normalizedTopic, ignoreCase = true) || lang == normalizedTargetLang

          caller.isNotBlank() &&
            caller != uid &&
            mode != "speaking_room" &&
            mode != "direct" &&
            isRecent &&
            langCompatible &&
            topicCompatible
        }

        for (candidateDoc in candidates) {
          val claimedRoom = try {
            db.runTransaction { tx ->
              val fresh = tx.get(candidateDoc.reference)
              val st = fresh.getString("status") ?: ""
              val currentCallee = fresh.getString("calleeId") ?: ""
              if (st == "waiting" && (currentCallee.isBlank() || currentCallee == "waiting_partner")) {
                val existingCallType = fresh.getString("callType") ?: normalizedCallType
                val isVid = existingCallType == "video"
                tx.update(
                  candidateDoc.reference,
                  mapOf(
                    "calleeId" to uid,
                    "calleeName" to normalizedCallerName,
                    "calleeNativeLang" to normalizedCallerNativeLang,
                    "calleeLearningLang" to normalizedCallerLearningLang,
                    "status" to "accepted",
                    "lastSignalingEvent" to "call_accepted",
                    "connectionState" to "connecting",
                    "isCalleeVideoEnabled" to isVid,
                    "updatedAt" to FieldValue.serverTimestamp()
                  )
                )
                CallRoom(
                  roomId = fresh.id,
                  callerId = fresh.getString("callerId") ?: "",
                  callerName = fresh.getString("callerName") ?: "Partner",
                  callerNativeLang = fresh.getString("callerNativeLang") ?: normalizedTargetLang,
                  callerLearningLang = fresh.getString("callerLearningLang") ?: normalizedCallerNativeLang,
                  calleeId = uid,
                  calleeName = normalizedCallerName,
                  calleeNativeLang = normalizedCallerNativeLang,
                  calleeLearningLang = normalizedCallerLearningLang,
                  callType = existingCallType,
                  matchMode = normalizedMatchMode,
                  targetLanguageCode = fresh.getString("targetLanguageCode") ?: normalizedTargetLang,
                  conversationTopic = fresh.getString("conversationTopic") ?: normalizedTopic,
                  status = "accepted",
                  lastSignalingEvent = "call_accepted",
                  isVideoEnabled = isVid,
                  isCallerVideoEnabled = fresh.getBoolean("isCallerVideoEnabled") ?: isVid,
                  isCalleeVideoEnabled = isVid,
                  connectionState = "connecting"
                )
              } else {
                null
              }
            }.await()
          } catch (_: Exception) {
            null
          }
          if (claimedRoom != null) {
            return claimedRoom
          }
        }
      } else if (calleeId.isNotBlank()) {
        // Direct call: check if recipient allows incoming calls and is not busy
        val partnerCommSnap = try {
          db.collection("community_profiles").document(calleeId).get().await()
        } catch (_: Exception) {
          null
        }
        if (partnerCommSnap != null && partnerCommSnap.exists() && partnerCommSnap.getBoolean("allowCalls") == false) {
          throw IllegalStateException("${calleeName.ifBlank { "Partner" }} has disabled incoming partner calls.")
        }
        if (isUserBusyInCall(calleeId)) {
          throw IllegalStateException("${calleeName.ifBlank { "Partner" }} is currently busy on another call.")
        }
      }

      // Create a new live room in Firestore
      val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val hasTargetCallee = calleeId.isNotBlank() && calleeId != "waiting_partner"
      val initialStatus = if (hasTargetCallee && normalizedMatchMode != "speaking_room") "ringing" else "waiting"
      val initialEvent = if (initialStatus == "ringing") "incoming_call" else "call_initiated"
      val isVid = normalizedCallType == "video"
      val docRef = db.collection("call_rooms").document(roomId)
      val payload = mapOf(
        "roomId" to roomId,
        "callerId" to uid,
        "callerName" to normalizedCallerName,
        "callerNativeLang" to normalizedCallerNativeLang,
        "callerLearningLang" to normalizedCallerLearningLang,
        "calleeId" to calleeId.ifBlank { "waiting_partner" }.take(128),
        "calleeName" to normalizedCalleeName,
        "calleeNativeLang" to normalizedCalleeNativeLang,
        "calleeLearningLang" to normalizedCalleeLearningLang,
        "callType" to normalizedCallType,
        "matchMode" to normalizedMatchMode,
        "targetLanguageCode" to normalizedTargetLang,
        "status" to initialStatus,
        "lastSignalingEvent" to initialEvent,
        "isCallerMuted" to false,
        "isCalleeMuted" to false,
        "isVideoEnabled" to isVid,
        "isCallerVideoEnabled" to isVid,
        "isCalleeVideoEnabled" to isVid,
        "conversationTopic" to normalizedTopic,
        "callerIceCandidates" to emptyList<String>(),
        "calleeIceCandidates" to emptyList<String>(),
        "connectionState" to "new",
        "connectedAtEpochMs" to 0L,
        "endedAtEpochMs" to 0L,
        "endedBy" to "",
        "durationSeconds" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()

      if (hasTargetCallee && initialStatus == "ringing") {
        createNotification(
          targetUserId = calleeId,
          title = "Incoming ${normalizedCallType.replaceFirstChar { it.uppercase() }} Call",
          message = "$normalizedCallerName is calling you to practice ${normalizedTargetLang.uppercase()}!",
          type = "call_invite",
          targetRoute = "incoming_call",
          targetId = roomId,
          senderId = uid,
          senderName = normalizedCallerName
        )
      }

      return CallRoom(
        roomId = roomId,
        callerId = uid,
        callerName = normalizedCallerName,
        callerNativeLang = normalizedCallerNativeLang,
        callerLearningLang = normalizedCallerLearningLang,
        calleeId = calleeId.ifBlank { "waiting_partner" },
        calleeName = normalizedCalleeName,
        calleeNativeLang = normalizedCalleeNativeLang,
        calleeLearningLang = normalizedCalleeLearningLang,
        callType = normalizedCallType,
        matchMode = normalizedMatchMode,
        targetLanguageCode = normalizedTargetLang,
        conversationTopic = normalizedTopic,
        status = initialStatus,
        lastSignalingEvent = initialEvent,
        isVideoEnabled = isVid,
        isCallerVideoEnabled = isVid,
        isCalleeVideoEnabled = isVid,
        connectionState = "new"
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "call_rooms")
      throw e
    }
  }

  // --- Real Live Speaking Rooms (Group Voice Rooms) ---

  fun observeActiveSpeakingRooms(): Flow<List<CallRoom>> = flow {
    val path = "call_rooms"
    db.collection(path)
      .whereEqualTo("matchMode", "speaking_room")
      .limit(25)
      .snapshots()
      .onStart { FirestoreDiagnostics.recordListenerAttach("speaking_rooms") }
      .onCompletion { FirestoreDiagnostics.recordListenerDetach("speaking_rooms") }
      .map { snap ->
        val nowMs = System.currentTimeMillis()
        snap.documents.mapNotNull { it.toCallRoomModel() }
          .filter { room ->
            val updatedMs = room.updatedAt?.toDate()?.time ?: room.createdAt?.toDate()?.time ?: nowMs
            val isFresh = (nowMs - updatedMs) <= 2 * 60 * 60 * 1000L // within 2 hours
            room.status !in listOf("ended", "cancelled", "rejected", "missed", "failed", "closed") &&
              room.participantIds.isNotEmpty() &&
              isFresh
          }
          .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        recordListenerError(error, OperationType.LIST, path)
        emit(emptyList())
      }
      .collect { emit(it) }
  }

  suspend fun createSpeakingRoom(
    hostName: String,
    hostNativeLang: String,
    roomTitle: String,
    targetLanguageCode: String,
    conversationTopic: String,
    isPublic: Boolean,
    participantLimit: Int
  ): CallRoom {
    val uid = requireUserId()
    val cleanTitle = roomTitle.trim().ifBlank { "$conversationTopic Practice" }.take(140)
    val cleanTopic = conversationTopic.trim().ifBlank { cleanTitle }.take(280)
    val cleanHostName = hostName.trim().ifBlank { "Host" }.take(70)
    val safeLimit = participantLimit.coerceIn(2, 50)
    val roomId = "spk_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val docRef = db.collection("call_rooms").document(roomId)

    try {
      val payload = mapOf(
        "roomId" to roomId,
        "callerId" to uid,
        "callerName" to cleanHostName,
        "callerNativeLang" to hostNativeLang,
        "callerLearningLang" to targetLanguageCode,
        "calleeId" to "waiting_partner",
        "calleeName" to "",
        "calleeNativeLang" to targetLanguageCode,
        "calleeLearningLang" to hostNativeLang,
        "callType" to "voice",
        "matchMode" to "speaking_room",
        "targetLanguageCode" to targetLanguageCode,
        "status" to "waiting",
        "lastSignalingEvent" to "call_initiated",
        "isCallerMuted" to false,
        "isCalleeMuted" to false,
        "isVideoEnabled" to false,
        "isCallerVideoEnabled" to false,
        "isCalleeVideoEnabled" to false,
        "conversationTopic" to cleanTopic,
        "roomTitle" to cleanTitle,
        "isPublic" to isPublic,
        "participantLimit" to safeLimit,
        "participantIds" to listOf(uid),
        "participantNames" to listOf(cleanHostName),
        "mutedParticipantIds" to emptyList<String>(),
        "handRaisedParticipantIds" to emptyList<String>(),
        "callerIceCandidates" to emptyList<String>(),
        "calleeIceCandidates" to emptyList<String>(),
        "connectionState" to "new",
        "connectedAtEpochMs" to System.currentTimeMillis(),
        "endedAtEpochMs" to 0L,
        "durationSeconds" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()

      return CallRoom(
        roomId = roomId,
        callerId = uid,
        callerName = cleanHostName,
        callerNativeLang = hostNativeLang,
        callerLearningLang = targetLanguageCode,
        calleeId = "waiting_partner",
        calleeName = "",
        calleeNativeLang = targetLanguageCode,
        calleeLearningLang = hostNativeLang,
        callType = "voice",
        matchMode = "speaking_room",
        targetLanguageCode = targetLanguageCode,
        status = "waiting",
        lastSignalingEvent = "call_initiated",
        conversationTopic = cleanTopic,
        roomTitle = cleanTitle,
        isPublic = isPublic,
        participantLimit = safeLimit,
        participantIds = listOf(uid),
        participantNames = listOf(cleanHostName),
        connectionState = "new",
        connectedAtEpochMs = System.currentTimeMillis()
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun joinSpeakingRoom(
    roomId: String,
    userName: String,
    userNativeLang: String,
    userLearningLang: String
  ): CallRoom {
    val uid = requireUserId()
    val cleanName = userName.trim().ifBlank { "Participant" }.take(70)
    val docRef = db.collection("call_rooms").document(roomId)

    return try {
      db.runTransaction { tx ->
        val snap = tx.get(docRef)
        if (!snap.exists()) {
          throw IllegalStateException("Speaking room no longer exists.")
        }
        val status = snap.getString("status") ?: "ended"
        if (status in listOf("ended", "cancelled", "closed")) {
          throw IllegalStateException("This speaking room has already ended.")
        }
        @Suppress("UNCHECKED_CAST")
        val currentIds = (snap.get("participantIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val currentNames = (snap.get("participantNames") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val limit = (snap.getLong("participantLimit") ?: 8L).toInt().coerceAtLeast(2)

        if (!currentIds.contains(uid) && currentIds.size >= limit) {
          throw IllegalStateException("This speaking room is full ($limit/$limit participants).")
        }

        val updatedIds = if (currentIds.contains(uid)) currentIds else (currentIds + uid)
        val updatedNames = if (currentIds.contains(uid)) currentNames else (currentNames + cleanName)
        val callerId = snap.getString("callerId") ?: ""
        val existingCalleeId = snap.getString("calleeId") ?: "waiting_partner"

        val updates = hashMapOf<String, Any>(
          "participantIds" to updatedIds,
          "participantNames" to updatedNames,
          "lastSignalingEvent" to "participant_joined",
          "updatedAt" to FieldValue.serverTimestamp()
        )

        if (uid != callerId && (existingCalleeId.isBlank() || existingCalleeId == "waiting_partner" || existingCalleeId == uid)) {
          updates["calleeId"] = uid
          updates["calleeName"] = cleanName
          updates["calleeNativeLang"] = userNativeLang
          updates["calleeLearningLang"] = userLearningLang
          updates["status"] = "accepted"
          updates["connectionState"] = "connecting"
        }

        tx.update(docRef, updates)
      }.await()

      val freshSnap = docRef.get().await()
      freshSnap.toCallRoomModel() ?: throw IllegalStateException("Unable to load speaking room state.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun leaveSpeakingRoom(roomId: String) {
    val uid = auth.currentUser?.uid ?: return
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      db.runTransaction { tx ->
        val snap = tx.get(docRef)
        if (!snap.exists()) return@runTransaction
        @Suppress("UNCHECKED_CAST")
        val currentIds = (snap.get("participantIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val currentNames = (snap.get("participantNames") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val mutedIds = (snap.get("mutedParticipantIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val handIds = (snap.get("handRaisedParticipantIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        val idx = currentIds.indexOf(uid)
        val updatedIds = currentIds.filterNot { it == uid }
        val updatedNames = if (idx in currentNames.indices) {
          currentNames.filterIndexed { i, _ -> i != idx }
        } else {
          currentNames
        }
        val updatedMuted = mutedIds.filterNot { it == uid }
        val updatedHands = handIds.filterNot { it == uid }

        if (updatedIds.isEmpty()) {
          tx.update(
            docRef,
            mapOf(
              "participantIds" to emptyList<String>(),
              "participantNames" to emptyList<String>(),
              "mutedParticipantIds" to emptyList<String>(),
              "handRaisedParticipantIds" to emptyList<String>(),
              "status" to "ended",
              "connectionState" to "closed",
              "lastSignalingEvent" to "call_ended",
              "endedAtEpochMs" to System.currentTimeMillis(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        } else {
          val updates = hashMapOf<String, Any>(
            "participantIds" to updatedIds,
            "participantNames" to updatedNames,
            "mutedParticipantIds" to updatedMuted,
            "handRaisedParticipantIds" to updatedHands,
            "lastSignalingEvent" to "participant_left",
            "updatedAt" to FieldValue.serverTimestamp()
          )
          val currentCallee = snap.getString("calleeId") ?: ""
          if (currentCallee == uid) {
            val nextPeer = updatedIds.firstOrNull { it != snap.getString("callerId") } ?: "waiting_partner"
            updates["calleeId"] = nextPeer
            if (nextPeer == "waiting_partner") {
              updates["status"] = "waiting"
            }
          }
          tx.update(docRef, updates)
        }
      }.await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun toggleSpeakingRoomMute(roomId: String, targetUserId: String, isMuted: Boolean) {
    requireUserId()
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      val op = if (isMuted) FieldValue.arrayUnion(targetUserId) else FieldValue.arrayRemove(targetUserId)
      docRef.update(
        mapOf(
          "mutedParticipantIds" to op,
          "lastSignalingEvent" to "room_updated",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun toggleSpeakingRoomHandRaised(roomId: String, targetUserId: String, isRaised: Boolean) {
    requireUserId()
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      val op = if (isRaised) FieldValue.arrayUnion(targetUserId) else FieldValue.arrayRemove(targetUserId)
      docRef.update(
        mapOf(
          "handRaisedParticipantIds" to op,
          "lastSignalingEvent" to "room_updated",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  fun observeSpeakingRoomChat(roomId: String): Flow<List<DirectChatMessage>> {
    val chatKey = "roomchat_$roomId"
    return flow {
      db.collection("chat_messages")
        .whereEqualTo("receiverId", "PUBLIC_ROOM")
        .whereEqualTo("chatRoomId", chatKey)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(40)
        .snapshots()
        .onStart { FirestoreDiagnostics.recordListenerAttach("speaking_room_chat_$roomId") }
        .onCompletion { FirestoreDiagnostics.recordListenerDetach("speaking_room_chat_$roomId") }
        .map { snap ->
          snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt?.seconds ?: 0L }
        }
        .collect { emit(it) }
    }.catch { err ->
      if (isFailedPreconditionError(err)) {
        db.collection("chat_messages")
          .whereEqualTo("receiverId", "PUBLIC_ROOM")
          .whereEqualTo("chatRoomId", chatKey)
          .limit(40)
          .snapshots()
          .map { snap ->
            snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .sortedBy { it.createdAt?.seconds ?: 0L }
          }
          .catch { emit(emptyList()) }
          .collect { emit(it) }
      } else {
        recordListenerError(err, OperationType.LIST, "chat_messages")
        emit(emptyList())
      }
    }
  }

  suspend fun sendSpeakingRoomMessage(roomId: String, senderName: String, text: String) {
    val uid = requireUserId()
    val clean = text.trim()
    if (clean.isBlank()) return
    val msgId = "rmsg_${UUID.randomUUID()}"
    val docRef = db.collection("chat_messages").document(msgId)
    try {
      docRef.set(
        mapOf(
          "chatRoomId" to "roomchat_$roomId",
          "senderId" to uid,
          "senderName" to senderName.ifBlank { "Participant" }.take(70),
          "receiverId" to "PUBLIC_ROOM",
          "text" to clean.take(1000),
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  private fun DocumentSnapshot.toCallRoomModel(): CallRoom? {
    if (!exists()) return null
    val callTypeStr = getString("callType") ?: "voice"
    val defaultVideo = callTypeStr == "video"
    @Suppress("UNCHECKED_CAST")
    val callerCands = (get("callerIceCandidates") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    @Suppress("UNCHECKED_CAST")
    val calleeCands = (get("calleeIceCandidates") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    @Suppress("UNCHECKED_CAST")
    val partIds = (get("participantIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    @Suppress("UNCHECKED_CAST")
    val partNames = (get("participantNames") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    @Suppress("UNCHECKED_CAST")
    val mutedIds = (get("mutedParticipantIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    @Suppress("UNCHECKED_CAST")
    val handIds = (get("handRaisedParticipantIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

    return CallRoom(
      roomId = getString("roomId")?.ifBlank { id } ?: id,
      callerId = getString("callerId") ?: "",
      callerName = getString("callerName") ?: "",
      callerNativeLang = getString("callerNativeLang") ?: "en",
      callerLearningLang = getString("callerLearningLang") ?: "es",
      calleeId = getString("calleeId") ?: "",
      calleeName = getString("calleeName") ?: "",
      calleeNativeLang = getString("calleeNativeLang") ?: "es",
      calleeLearningLang = getString("calleeLearningLang") ?: "en",
      callType = callTypeStr,
      matchMode = getString("matchMode") ?: "direct",
      targetLanguageCode = getString("targetLanguageCode") ?: "es",
      status = getString("status") ?: "waiting",
      lastSignalingEvent = getString("lastSignalingEvent") ?: "call_initiated",
      isCallerMuted = getBoolean("isCallerMuted") ?: getBoolean("callerMuted") ?: false,
      isCalleeMuted = getBoolean("isCalleeMuted") ?: getBoolean("calleeMuted") ?: false,
      isVideoEnabled = getBoolean("isVideoEnabled") ?: getBoolean("videoEnabled") ?: defaultVideo,
      isCallerVideoEnabled = getBoolean("isCallerVideoEnabled") ?: getBoolean("callerVideoEnabled") ?: defaultVideo,
      isCalleeVideoEnabled = getBoolean("isCalleeVideoEnabled") ?: getBoolean("calleeVideoEnabled") ?: defaultVideo,
      conversationTopic = getString("conversationTopic") ?: "",
      roomTitle = getString("roomTitle") ?: "",
      isPublic = getBoolean("isPublic") ?: true,
      participantLimit = (getLong("participantLimit") ?: 8L).toInt(),
      participantIds = partIds,
      participantNames = partNames,
      mutedParticipantIds = mutedIds,
      handRaisedParticipantIds = handIds,
      sdpOffer = getString("sdpOffer"),
      sdpAnswer = getString("sdpAnswer"),
      callerIceCandidates = callerCands,
      calleeIceCandidates = calleeCands,
      connectionState = getString("connectionState") ?: "new",
      connectedAtEpochMs = getLong("connectedAtEpochMs") ?: 0L,
      endedAtEpochMs = getLong("endedAtEpochMs") ?: 0L,
      endedBy = getString("endedBy") ?: "",
      durationSeconds = getLong("durationSeconds") ?: 0L,
      createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
      updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    )
  }

  private val terminatedCallRoomIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

  fun isRoomMarkedTerminal(roomId: String): Boolean {
    return roomId.isNotBlank() && terminatedCallRoomIds.contains(roomId)
  }

  fun markRoomTerminalLocally(roomId: String) {
    if (roomId.isNotBlank()) {
      terminatedCallRoomIds.add(roomId)
    }
  }

  suspend fun getCallRoomSnapshot(roomId: String): CallRoom? {
    if (roomId.isBlank()) return null
    return try {
      val snap = db.collection("call_rooms").document(roomId).get().await()
      snap.toCallRoomModel()?.also { room ->
        if (room.isTerminalStatus()) {
          terminatedCallRoomIds.add(room.roomId)
        }
      }
    } catch (_: Exception) {
      null
    }
  }

  fun observeCallRoom(roomId: String): Flow<CallRoom?> = flow {
    val path = "call_rooms/$roomId"
    db.collection("call_rooms").document(roomId)
      .snapshots()
      .onStart { FirestoreDiagnostics.recordListenerAttach("call_room_$roomId") }
      .onCompletion { FirestoreDiagnostics.recordListenerDetach("call_room_$roomId") }
      .map { snap ->
        snap.toCallRoomModel()?.also { room ->
          if (room.isTerminalStatus()) {
            terminatedCallRoomIds.add(room.roomId)
          }
        }
      }
      .catch { error ->
        recordListenerError(error, OperationType.GET, path)
        emit(null)
      }
      .collect { emit(it) }
  }

  suspend fun updateCallRoomState(
    roomId: String,
    status: String? = null,
    isCallerMuted: Boolean? = null,
    isCalleeMuted: Boolean? = null,
    isVideoEnabled: Boolean? = null,
    isCallerVideoEnabled: Boolean? = null,
    isCalleeVideoEnabled: Boolean? = null,
    conversationTopic: String? = null
  ) {
    if (isRoomMarkedTerminal(roomId)) return
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      val updates = hashMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
      if (status != null) updates["status"] = status
      if (isCallerMuted != null) updates["isCallerMuted"] = isCallerMuted
      if (isCalleeMuted != null) updates["isCalleeMuted"] = isCalleeMuted
      if (isVideoEnabled != null) updates["isVideoEnabled"] = isVideoEnabled
      if (isCallerVideoEnabled != null) updates["isCallerVideoEnabled"] = isCallerVideoEnabled
      if (isCalleeVideoEnabled != null) updates["isCalleeVideoEnabled"] = isCalleeVideoEnabled
      if (conversationTopic != null) updates["conversationTopic"] = conversationTopic
      docRef.update(updates).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  fun observeIncomingCalls(): Flow<CallRoom?> = flow {
    val uid = auth.currentUser?.uid ?: return@flow
    db.collection("call_rooms")
      .whereEqualTo("calleeId", uid)
      .whereEqualTo("status", "ringing")
      .limit(5)
      .snapshots()
      .onStart { FirestoreDiagnostics.recordListenerAttach("incoming_calls") }
      .onCompletion { FirestoreDiagnostics.recordListenerDetach("incoming_calls") }
      .map { querySnap ->
        val nowMs = System.currentTimeMillis()
        querySnap.documents
          .mapNotNull { it.toCallRoomModel() }
          .firstOrNull { room ->
            val createdMs = room.createdAt?.toDate()?.time
              ?: room.updatedAt?.toDate()?.time
              ?: nowMs
            val ageMs = nowMs - createdMs
            !room.isTerminalStatus() &&
              !isRoomMarkedTerminal(room.roomId) &&
              ageMs in -60_000L..38_000L
          }
      }
      .catch { error ->
        recordListenerError(error, OperationType.LIST, "call_rooms")
        emit(null)
      }
      .collect { emit(it) }
  }

  suspend fun markCallRinging(roomId: String) {
    if (isRoomMarkedTerminal(roomId)) return
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      docRef.update(
        mapOf(
          "lastSignalingEvent" to "call_ringing",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun sendSdpOffer(roomId: String, sdpOffer: String, isIceRestart: Boolean = false) {
    if (isRoomMarkedTerminal(roomId)) return
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      if (isIceRestart) {
        val currentSnap = try { docRef.get().await() } catch (_: Exception) { null }
        val existingAnswer = currentSnap?.getString("sdpAnswer")
        val existingConnState = currentSnap?.getString("connectionState")
        val existingStatus = currentSnap?.getString("status")
        if (!existingAnswer.isNullOrBlank() && existingConnState != "reconnecting" && existingStatus != "failed") {
          return
        }
      }
      val updates = hashMapOf<String, Any?>(
        "sdpOffer" to sdpOffer,
        "lastSignalingEvent" to "webrtc_offer",
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (isIceRestart) {
        updates["sdpAnswer"] = null
        updates["callerIceCandidates"] = emptyList<String>()
        updates["calleeIceCandidates"] = emptyList<String>()
        updates["connectionState"] = "connecting"
      }
      docRef.update(updates).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun sendSdpAnswer(roomId: String, sdpAnswer: String) {
    if (isRoomMarkedTerminal(roomId)) return
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      docRef.update(
        mapOf(
          "status" to "accepted",
          "sdpAnswer" to sdpAnswer,
          "lastSignalingEvent" to "webrtc_answer",
          "connectionState" to "connecting",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun addIceCandidate(roomId: String, candidate: String, isCaller: Boolean) {
    if (isRoomMarkedTerminal(roomId)) return
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      val fieldName = if (isCaller) "callerIceCandidates" else "calleeIceCandidates"
      docRef.update(
        mapOf(
          fieldName to FieldValue.arrayUnion(candidate),
          "lastSignalingEvent" to "ice_candidate",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun updateConnectionState(roomId: String, connectionState: String) {
    if (isRoomMarkedTerminal(roomId)) return
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      val updates = hashMapOf<String, Any>(
        "connectionState" to connectionState,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (connectionState == "connected" || connectionState == "completed") {
        updates["status"] = "connected"
        updates["lastSignalingEvent"] = "call_connected"
        updates["connectedAtEpochMs"] = System.currentTimeMillis()
      } else if (connectionState == "failed") {
        updates["lastSignalingEvent"] = "call_failed"
      }
      docRef.update(updates).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun acceptCall(roomId: String, isVideo: Boolean) {
    if (isRoomMarkedTerminal(roomId)) return
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      docRef.update(
        mapOf(
          "status" to "accepted",
          "lastSignalingEvent" to "call_accepted",
          "isVideoEnabled" to isVideo,
          "isCalleeVideoEnabled" to isVideo,
          "connectionState" to "connecting",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun rejectCall(roomId: String, isBusy: Boolean = false) {
    markRoomTerminalLocally(roomId)
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      val nowMs = System.currentTimeMillis()
      val uid = auth.currentUser?.uid ?: ""
      docRef.update(
        mapOf(
          "status" to if (isBusy) "busy" else "rejected",
          "lastSignalingEvent" to "call_rejected",
          "connectionState" to "closed",
          "endedAtEpochMs" to nowMs,
          "endedBy" to uid,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      saveCallHistoryFromRoom(roomId, finalStatus = "rejected", durationSec = 0L)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun cancelCall(roomId: String) {
    markRoomTerminalLocally(roomId)
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      val nowMs = System.currentTimeMillis()
      val uid = auth.currentUser?.uid ?: ""
      // Update Firestore status immediately so the remote ringing phone stops immediately
      docRef.update(
        mapOf(
          "status" to "cancelled",
          "lastSignalingEvent" to "call_cancelled",
          "connectionState" to "closed",
          "endedAtEpochMs" to nowMs,
          "endedBy" to uid,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

      val snap = docRef.get().await()
      val calleeId = snap.getString("calleeId").orEmpty()
      val callerId = snap.getString("callerId").orEmpty()
      val callerName = snap.getString("callerName")?.ifBlank { "Partner" } ?: "Partner"
      val callType = snap.getString("callType") ?: "voice"

      if (calleeId.isNotBlank() && calleeId != "waiting_partner") {
        createNotification(
          targetUserId = calleeId,
          title = "Missed ${callType.replaceFirstChar { it.uppercase() }} Call",
          message = "Missed $callType call from $callerName",
          type = "missed_call",
          targetRoute = "missed_call",
          targetId = roomId,
          senderId = callerId,
          senderName = callerName
        )
      }
      saveCallHistoryFromRoom(roomId, finalStatus = "cancelled", durationSec = 0L)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun markCallMissed(roomId: String) {
    markRoomTerminalLocally(roomId)
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      val snap = docRef.get().await()
      val currentStatus = snap.getString("status") ?: ""
      if (currentStatus != "ringing" && currentStatus != "waiting") return

      val nowMs = System.currentTimeMillis()
      val uid = auth.currentUser?.uid ?: ""
      docRef.update(
        mapOf(
          "status" to "missed",
          "lastSignalingEvent" to "call_missed",
          "connectionState" to "closed",
          "endedAtEpochMs" to nowMs,
          "endedBy" to uid,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

      val calleeId = snap.getString("calleeId") ?: ""
      val callerId = snap.getString("callerId") ?: ""
      val callerName = snap.getString("callerName") ?: "Partner"
      val callType = snap.getString("callType") ?: "voice"
      if (calleeId.isNotBlank() && calleeId != "waiting_partner") {
        createNotification(
          targetUserId = calleeId,
          title = "Missed ${callType.replaceFirstChar { it.uppercase() }} Call",
          message = "Missed $callType call from $callerName",
          type = "missed_call",
          targetRoute = "missed_call",
          targetId = roomId,
          senderId = callerId,
          senderName = callerName
        )
      }
      saveCallHistoryFromRoom(roomId, finalStatus = "missed", durationSec = 0L)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun markCallFailed(roomId: String) {
    markRoomTerminalLocally(roomId)
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      val nowMs = System.currentTimeMillis()
      val uid = auth.currentUser?.uid ?: ""
      docRef.update(
        mapOf(
          "status" to "failed",
          "lastSignalingEvent" to "call_failed",
          "connectionState" to "failed",
          "endedAtEpochMs" to nowMs,
          "endedBy" to uid,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      saveCallHistoryFromRoom(roomId, finalStatus = "failed", durationSec = 0L)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun endCall(roomId: String, actualDurationSeconds: Long) {
    markRoomTerminalLocally(roomId)
    val docRef = db.collection("call_rooms").document(roomId)
    try {
      val nowMs = System.currentTimeMillis()
      val uid = auth.currentUser?.uid ?: ""
      // Immediately update Firestore status to "ended" so the remote device's listener fires without delay
      docRef.update(
        mapOf(
          "status" to "ended",
          "lastSignalingEvent" to "call_ended",
          "connectionState" to "closed",
          "endedAtEpochMs" to nowMs,
          "endedBy" to uid,
          "durationSeconds" to actualDurationSeconds,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

      val finalHistoryStatus = if (actualDurationSeconds > 0L) "completed" else "cancelled"
      saveCallHistoryFromRoom(roomId, finalStatus = finalHistoryStatus, durationSec = actualDurationSeconds)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  // --- Call History Persistence & Queries ---

  private suspend fun saveCallHistoryFromRoom(
    roomId: String,
    finalStatus: String,
    durationSec: Long
  ) {
    try {
      val snap = db.collection("call_rooms").document(roomId).get().await()
      if (!snap.exists()) return
      val callerId = snap.getString("callerId") ?: return
      val calleeId = snap.getString("calleeId") ?: "waiting_partner"
      if (callerId.isBlank()) return

      val createdMs = snap.getTimestamp("createdAt")?.toDate()?.time
        ?: (System.currentTimeMillis() - (durationSec * 1000L))
      val endedMs = snap.getLong("endedAtEpochMs")?.takeIf { it > 0L } ?: System.currentTimeMillis()

      val record = CallHistoryRecord(
        callId = roomId,
        callerId = callerId,
        callerName = snap.getString("callerName") ?: "Learner",
        receiverId = calleeId.ifBlank { "waiting_partner" },
        receiverName = snap.getString("calleeName")?.ifBlank { "Language Partner" } ?: "Language Partner",
        callType = snap.getString("callType") ?: "voice",
        targetLanguageCode = snap.getString("targetLanguageCode") ?: "es",
        startTimeEpochMs = createdMs,
        endTimeEpochMs = endedMs,
        durationSeconds = durationSec,
        status = finalStatus
      )
      saveCallHistoryRecord(record)
    } catch (_: Exception) {}
  }

  suspend fun saveCallHistoryRecord(record: CallHistoryRecord) {
    requireUserId()
    val callId = record.callId.ifBlank { "call_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
    val docRef = db.collection("call_history").document(callId)
    try {
      docRef.set(
        mapOf(
          "callId" to callId,
          "callerId" to record.callerId,
          "callerName" to record.callerName.ifBlank { "Learner" },
          "receiverId" to record.receiverId.ifBlank { "waiting_partner" },
          "receiverName" to record.receiverName.ifBlank { "Partner" },
          "callType" to record.callType,
          "targetLanguageCode" to record.targetLanguageCode,
          "startTimeEpochMs" to record.startTimeEpochMs,
          "endTimeEpochMs" to record.endTimeEpochMs,
          "durationSeconds" to record.durationSeconds,
          "status" to record.status,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
    }
  }

  fun observeCallHistory(userId: String): Flow<List<CallHistoryRecord>> {
    val outgoingFlow = db.collection("call_history")
      .whereEqualTo("callerId", userId)
      .limit(25)
      .snapshots()
      .onStart { FirestoreDiagnostics.recordListenerAttach("call_history_out") }
      .onCompletion { FirestoreDiagnostics.recordListenerDetach("call_history_out") }
      .map { it.toObjects(CallHistoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { err ->
        recordListenerError(err, OperationType.LIST, "call_history")
        emit(emptyList())
      }

    val incomingFlow = db.collection("call_history")
      .whereEqualTo("receiverId", userId)
      .limit(25)
      .snapshots()
      .onStart { FirestoreDiagnostics.recordListenerAttach("call_history_in") }
      .onCompletion { FirestoreDiagnostics.recordListenerDetach("call_history_in") }
      .map { it.toObjects(CallHistoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { err ->
        recordListenerError(err, OperationType.LIST, "call_history")
        emit(emptyList())
      }

    return combine(outgoingFlow, incomingFlow) { outgoing, incoming ->
      (outgoing + incoming)
        .distinctBy { it.callId.ifBlank { it.id } }
        .sortedByDescending { it.startTimeEpochMs.takeIf { ms -> ms > 0L } ?: (it.createdAt?.seconds ?: 0L) * 1000L }
    }
  }

  // --- Reporting & Blocking ---

  suspend fun reportUser(targetUserId: String, reason: String, details: String) {
    val uid = requireUserId()
    val reportId = "rep_${UUID.randomUUID()}"
    val docRef = db.collection("user_reports").document(reportId)
    try {
      docRef.set(
        mapOf(
          "reporterId" to uid,
          "targetUserId" to targetUserId,
          "reason" to reason,
          "details" to details,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun blockUser(blockedUserId: String, blockedUserName: String) {
    val uid = requireUserId()
    val blockId = "blk_${uid}_${blockedUserId}"
    val docRef = db.collection("user_blocks").document(blockId)
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "blockedUserId" to blockedUserId,
          "blockedUserName" to blockedUserName,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun unblockUser(blockId: String) {
    requireUserId()
    val docRef = db.collection("user_blocks").document(blockId)
    try {
      docRef.delete().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
    }
  }

  fun observeBlockedUsers(userId: String): Flow<List<UserBlock>> = flow {
    val path = "user_blocks"
    db.collection(path)
      .whereEqualTo("userId", userId)
      .limit(50)
      .snapshots()
      .onStart { FirestoreDiagnostics.recordListenerAttach("blocked_users") }
      .onCompletion { FirestoreDiagnostics.recordListenerDetach("blocked_users") }
      .map { it.toObjects(UserBlock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { error ->
        recordListenerError(error, OperationType.LIST, path)
        emit(emptyList())
      }
      .collect { emit(it) }
  }

  // --- Notifications ---

  fun observeNotifications(userId: String): Flow<List<UserNotification>> = flow {
    val path = "notifications"
    db.collection(path)
      .whereEqualTo("userId", userId)
      .limit(30)
      .snapshots()
      .onStart { FirestoreDiagnostics.recordListenerAttach("notifications") }
      .onCompletion { FirestoreDiagnostics.recordListenerDetach("notifications") }
      .map { snap ->
        snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        recordListenerError(error, OperationType.LIST, path)
        emit(emptyList())
      }
      .collect { emit(it) }
  }

  suspend fun createNotification(
    targetUserId: String,
    title: String,
    message: String,
    type: String = "info",
    targetRoute: String = "",
    targetId: String = "",
    senderId: String = "",
    senderName: String = "",
    word: String = "",
    meaning: String = "",
    pronunciation: String = "",
    exampleSentence: String = ""
  ) {
    val notifId = "notif_${UUID.randomUUID()}"
    val docRef = db.collection("notifications").document(notifId)
    try {
      val payload = hashMapOf<String, Any>(
        "userId" to targetUserId,
        "title" to title,
        "message" to message,
        "type" to type,
        "isRead" to false,
        "createdAt" to FieldValue.serverTimestamp()
      )
      if (targetRoute.isNotBlank()) payload["targetRoute"] = targetRoute
      if (targetId.isNotBlank()) payload["targetId"] = targetId
      if (senderId.isNotBlank()) payload["senderId"] = senderId
      if (senderName.isNotBlank()) payload["senderName"] = senderName
      if (word.isNotBlank()) payload["word"] = word
      if (meaning.isNotBlank()) payload["meaning"] = meaning
      if (pronunciation.isNotBlank()) payload["pronunciation"] = pronunciation
      if (exampleSentence.isNotBlank()) payload["exampleSentence"] = exampleSentence
      docRef.set(payload).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
    }
  }

  suspend fun markNotificationRead(notificationId: String) {
    val docRef = db.collection("notifications").document(notificationId)
    try {
      docRef.update("isRead", true).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  // --- Lesson Progress ---

  companion object {
    val ALLOWED_LESSON_CATEGORIES = setOf("Vocabulary", "Grammar", "Listening", "Reading", "Writing", "Quiz")
    val ALLOWED_SPEAKING_MODES = setOf("Tutor", "Roleplay")
    val ALLOWED_PROFICIENCY_LEVELS = setOf("Beginner", "Intermediate", "Advanced")

    fun normalizeFirestoreLessonCategory(rawCategory: String): String {
      val trimmed = rawCategory.trim()
      if (trimmed in ALLOWED_LESSON_CATEGORIES) return trimmed
      val lower = trimmed.lowercase()
      return when {
        lower.contains("gram") -> "Grammar"
        lower.contains("listen") || lower.contains("speak") || lower.contains("pronun") -> "Listening"
        lower.contains("read") || lower.contains("story") -> "Reading"
        lower.contains("writ") -> "Writing"
        lower.contains("quiz") || lower.contains("revis") || lower.contains("review") || lower.contains("assess") -> "Quiz"
        else -> "Vocabulary"
      }
    }

    fun normalizeFirestoreSpeakingMode(rawMode: String): String {
      val trimmed = rawMode.trim()
      if (trimmed in ALLOWED_SPEAKING_MODES) return trimmed
      val lower = trimmed.lowercase()
      return if (lower.contains("role") || lower.contains("mission") || lower.contains("interview")) {
        "Roleplay"
      } else {
        "Tutor"
      }
    }
  }

  suspend fun recordLessonCompletion(
    lessonId: String,
    languageCode: String,
    category: String,
    scorePercent: Int,
    xpGained: Int
  ) {
    val uid = requireUserId()
    val safeLessonId = lessonId.trim().ifBlank { "lesson_default" }.take(100)
    val safeCategory = normalizeFirestoreLessonCategory(category)
    val safeScore = scorePercent.coerceIn(0, 100).toLong()
    val safeXp = xpGained.coerceIn(0, 500).toLong()
    val progressId = "${uid}_${safeLessonId}"
    val docRef = db.collection("lesson_progress").document(progressId)
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    try {
      val user = getUserProfile(uid)
      val alreadyCompleted = user?.completedLessonIds?.contains(safeLessonId) == true
      val effectiveXp = if (alreadyCompleted) 0L else safeXp

      docRef.set(
        mapOf(
          "userId" to uid,
          "lessonId" to safeLessonId,
          "languageCode" to languageCode,
          "category" to safeCategory,
          "scorePercent" to safeScore,
          "xpEarned" to effectiveXp,
          "completed" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

      if (user != null) {
        val newXp = user.xpPoints + effectiveXp
        val newLevel = (newXp / 100) + 1
        val completedList = (user.completedLessonIds + safeLessonId).distinct().takeLast(500)
        val isNewDay = user.lastActiveDate != today
        val newStreak = if (isNewDay) user.currentStreak + 1 else user.currentStreak
        val longestStreak = maxOf(user.longestStreak, newStreak)

        val achievements = user.achievements.toMutableList()
        if (completedList.size >= 1 && !achievements.contains("first_lesson")) {
          achievements.add("first_lesson")
          createNotification(uid, "Achievement Unlocked! 🎯", "You unlocked the 'First Step' badge for completing your first lesson.", "achievement")
        }
        if (completedList.size >= 5 && !achievements.contains("scholar_5")) {
          achievements.add("scholar_5")
          createNotification(uid, "Achievement Unlocked! 🏅", "You unlocked the 'Diligent Scholar' badge!", "achievement")
        }
        if (newStreak >= 3 && !achievements.contains("streak_3")) {
          achievements.add("streak_3")
          createNotification(uid, "3-Day Streak! 🔥", "You unlocked the '3-Day Dynamo' achievement!", "achievement")
        }

        updateUserProfile(
          user.copy(
            xpPoints = newXp,
            level = newLevel,
            currentStreak = newStreak,
            longestStreak = longestStreak,
            lastActiveDate = today,
            completedLessonIds = completedList,
            achievements = achievements.distinct().takeLast(100)
          )
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun incrementDailyAiSpeaking(userId: String) {
    val user = getUserProfile(userId) ?: return
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val isReset = user.dailyAiResetDate != today
    val newCount = if (isReset) 1L else user.dailyAiSpeakingCount + 1L

    val achievements = user.achievements.toMutableList()
    if (!achievements.contains("first_speak")) {
      achievements.add("first_speak")
      createNotification(userId, "Voice Pioneer Unlocked! 🎙️", "You practiced speaking with the AI Voice Tutor!", "achievement")
    }

    updateUserProfile(
      user.copy(
        dailyAiSpeakingCount = newCount,
        dailyAiResetDate = today,
        xpPoints = user.xpPoints + 15L,
        achievements = achievements
      )
    )
  }

  suspend fun unlockCulturalAchievementAndXp(
    achievementId: String,
    notificationTitle: String,
    notificationMessage: String,
    xpBonus: Int
  ) {
    val uid = requireUserId()
    val user = getUserProfile(uid) ?: return
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val updatedAchievements = (user.achievements + achievementId).distinct().takeLast(50)
    val isNewlyUnlocked = !user.achievements.contains(achievementId)
    val xpToAdd = if (isNewlyUnlocked) xpBonus.toLong() else (xpBonus / 2).coerceAtLeast(10).toLong()
    val newXp = user.xpPoints + xpToAdd
    val newLevel = (newXp / 100L) + 1L

    updateUserProfile(
      user.copy(
        xpPoints = newXp,
        level = newLevel,
        lastActiveDate = today,
        achievements = updatedAchievements
      )
    )
    if (isNewlyUnlocked && notificationTitle.isNotBlank()) {
      createNotification(
        targetUserId = uid,
        title = notificationTitle,
        message = notificationMessage,
        type = "achievement"
      )
    }
  }

  fun observeLessonProgress(userId: String): Flow<List<LessonProgress>> = flow {
    val path = "lesson_progress"
    db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(LessonProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
      .collect { emit(it) }
  }

  // --- Spaced Repetition Flashcards ---

  fun observeFlashcards(userId: String): Flow<List<FlashcardProgress>> = flow {
    val path = "flashcards"
    db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(FlashcardProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
      .collect { emit(it) }
  }

  suspend fun saveFlashcard(card: FlashcardProgress) {
    val uid = requireUserId()
    val cardId = if (card.id.isNotBlank()) card.id else "card_${UUID.randomUUID()}"
    val docRef = db.collection("flashcards").document(cardId)
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "wordId" to card.wordId,
          "languageCode" to card.languageCode,
          "frontText" to card.frontText,
          "backText" to card.backText,
          "pronunciation" to card.pronunciation,
          "boxLevel" to card.boxLevel,
          "nextReviewEpochDay" to card.nextReviewEpochDay,
          "mastered" to card.mastered,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun deleteFlashcard(cardId: String) {
    if (cardId.isBlank()) return
    requireUserId()
    val docRef = db.collection("flashcards").document(cardId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
    }
  }

  suspend fun updateFlashcardBox(card: FlashcardProgress, isCorrect: Boolean) {
    requireUserId()
    val docRef = db.collection("flashcards").document(card.id)
    val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
    val newBox = if (isCorrect) minOf(5L, card.boxLevel + 1) else maxOf(1L, card.boxLevel - 1)
    val intervalDays = when (newBox) {
      1L -> 1L
      2L -> 3L
      3L -> 7L
      4L -> 14L
      else -> 30L
    }
    val nextReview = currentDay + intervalDays
    val mastered = newBox >= 5L

    try {
      docRef.update(
        mapOf(
          "boxLevel" to newBox,
          "nextReviewEpochDay" to nextReview,
          "mastered" to mastered,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun reviewFlashcardWithRating(
    card: FlashcardProgress,
    rating: SrsRating,
    currentMeta: SrsCardMetadata?
  ): SrsReviewComputation {
    requireUserId()
    val docRef = db.collection("flashcards").document(card.id)
    val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
    val computation = SpacedRepetitionEngine.computeNextReview(
      card = card,
      currentMeta = currentMeta,
      rating = rating,
      todayEpochDay = currentDay
    )

    try {
      docRef.update(
        mapOf(
          "boxLevel" to computation.updatedBoxLevel,
          "nextReviewEpochDay" to computation.nextReviewEpochDay,
          "mastered" to computation.mastered,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
    return computation
  }

  suspend fun saveVocabularyAsFlashcard(
    languageCode: String,
    frontText: String,
    backText: String,
    pronunciation: String = ""
  ) {
    val uid = requireUserId()
    val cleanFront = frontText.trim().take(120)
    val cleanBack = backText.trim().take(180)
    if (cleanFront.isBlank() || cleanBack.isBlank()) return
    val sanitizedSlug = cleanFront.lowercase().replace(Regex("[^a-z0-9]"), "_").take(24)
    val wordId = "${languageCode}_${sanitizedSlug}_${System.currentTimeMillis() % 10000}"
    val cardId = "${uid}_$wordId"
    val todayDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
    saveFlashcard(
      FlashcardProgress(
        id = cardId,
        userId = uid,
        wordId = wordId,
        languageCode = languageCode,
        frontText = cleanFront,
        backText = cleanBack,
        pronunciation = pronunciation.ifBlank { cleanFront }.take(120),
        boxLevel = 1L,
        nextReviewEpochDay = todayDay,
        mastered = false
      )
    )
  }

  // --- Speaking Sessions ---

  fun observeSpeakingSessions(userId: String): Flow<List<SpeakingSession>> = flow {
    val path = "speaking_sessions"
    db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(SpeakingSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
      .collect { emit(it) }
  }

  suspend fun saveSpeakingSession(session: SpeakingSession) {
    val uid = requireUserId()
    val sessionId = if (session.id.isNotBlank()) session.id else "sess_${UUID.randomUUID()}"
    val docRef = db.collection("speaking_sessions").document(sessionId)
    val safeMode = normalizeFirestoreSpeakingMode(session.mode)
    val taggedScenarioTitle = if (session.mode.trim() !in ALLOWED_SPEAKING_MODES && !session.scenarioTitle.startsWith("[")) {
      "[${session.mode.trim()}] ${session.scenarioTitle}"
    } else {
      session.scenarioTitle
    }.ifBlank { "AI Speaking Session" }.take(120)
    val safeProficiency = if (session.proficiencyLevel in ALLOWED_PROFICIENCY_LEVELS) {
      session.proficiencyLevel
    } else {
      "Beginner"
    }

    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "languageCode" to session.languageCode,
          "mode" to safeMode,
          "scenarioTitle" to taggedScenarioTitle,
          "proficiencyLevel" to safeProficiency,
          "turnsCount" to session.turnsCount.coerceIn(0L, 500L),
          "fluencyScore" to session.fluencyScore.coerceIn(0L, 100L),
          "grammarFeedbackSummary" to session.grammarFeedbackSummary.take(2000),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      incrementDailyAiSpeaking(uid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  // --- LingoSphere Videos: Saved Videos, Playlists & Watch History ---

  private fun sanitizeDocIdSegment(raw: String): String {
    return raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(50)
  }

  fun observeSavedVideos(userId: String): Flow<List<SavedVideo>> = flow {
    val path = "saved_videos"
    db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(SavedVideo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
      .collect { emit(it) }
  }

  suspend fun saveVideo(video: LingoVideo) {
    val uid = requireUserId()
    val safeVid = sanitizeDocIdSegment(video.videoId)
    val savedId = "sv_${uid}_${safeVid}".take(120)
    val docRef = db.collection("saved_videos").document(savedId)
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "videoId" to video.videoId.take(60),
          "title" to video.title.ifBlank { "Language Video" }.take(240),
          "channelTitle" to video.channelTitle.ifBlank { "LingoSphere Studio" }.take(140),
          "thumbnailUrl" to video.thumbnailUrl.take(490),
          "durationText" to video.durationText.take(35),
          "languageCode" to video.languageCode.take(10),
          "proficiencyLevel" to (video.proficiencyLevel.takeIf { it in listOf("Beginner", "Intermediate", "Advanced") } ?: "Beginner"),
          "topicCategory" to video.topicCategory.ifBlank { "Recommended" }.take(75),
          "description" to video.description.take(1900),
          "captionSummary" to video.captionSummary.take(3900),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun removeSavedVideo(videoId: String) {
    val uid = requireUserId()
    val safeVid = sanitizeDocIdSegment(videoId)
    val savedId = "sv_${uid}_${safeVid}".take(120)
    val docRef = db.collection("saved_videos").document(savedId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observeVideoPlaylists(userId: String): Flow<List<VideoPlaylist>> = flow {
    val path = "video_playlists"
    db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(VideoPlaylist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
      .collect { emit(it) }
  }

  suspend fun createVideoPlaylist(
    name: String,
    description: String,
    languageCode: String,
    proficiencyLevel: String = "All",
    initialVideo: LingoVideo? = null
  ): String {
    val uid = requireUserId()
    val playlistId = "pl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("video_playlists").document(playlistId)
    val validLevel = if (proficiencyLevel in listOf("Beginner", "Intermediate", "Advanced", "All")) proficiencyLevel else "All"
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "playlistId" to playlistId,
          "name" to name.trim().ifBlank { "My Language Playlist" }.take(95),
          "description" to description.trim().take(290),
          "languageCode" to languageCode.take(10),
          "proficiencyLevel" to validLevel,
          "videoIds" to (if (initialVideo != null) listOf(initialVideo.videoId) else emptyList<String>()),
          "videoTitles" to (if (initialVideo != null) listOf(initialVideo.title.take(200)) else emptyList<String>()),
          "videoThumbnails" to (if (initialVideo != null) listOf(initialVideo.thumbnailUrl.take(400)) else emptyList<String>()),
          "videoChannels" to (if (initialVideo != null) listOf(initialVideo.channelTitle.take(120)) else emptyList<String>()),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      return playlistId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun addVideoToPlaylist(playlist: VideoPlaylist, video: LingoVideo) {
    requireUserId()
    val docId = playlist.playlistId.ifBlank { playlist.id }
    if (docId.isBlank()) return
    if (playlist.videoIds.contains(video.videoId) || playlist.videoIds.size >= 100) return
    val docRef = db.collection("video_playlists").document(docId)
    try {
      docRef.update(
        mapOf(
          "videoIds" to (playlist.videoIds + video.videoId),
          "videoTitles" to (playlist.videoTitles + video.title.take(200)),
          "videoThumbnails" to (playlist.videoThumbnails + video.thumbnailUrl.take(400)),
          "videoChannels" to (playlist.videoChannels + video.channelTitle.take(120)),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun removeVideoFromPlaylist(playlist: VideoPlaylist, videoId: String) {
    requireUserId()
    val docId = playlist.playlistId.ifBlank { playlist.id }
    val index = playlist.videoIds.indexOf(videoId)
    if (docId.isBlank() || index < 0) return
    val docRef = db.collection("video_playlists").document(docId)
    try {
      val newIds = playlist.videoIds.toMutableList().apply { removeAt(index) }
      val newTitles = playlist.videoTitles.toMutableList().apply { if (index < size) removeAt(index) }
      val newThumbs = playlist.videoThumbnails.toMutableList().apply { if (index < size) removeAt(index) }
      val newChannels = playlist.videoChannels.toMutableList().apply { if (index < size) removeAt(index) }
      docRef.update(
        mapOf(
          "videoIds" to newIds,
          "videoTitles" to newTitles,
          "videoThumbnails" to newThumbs,
          "videoChannels" to newChannels,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteVideoPlaylist(playlistId: String) {
    requireUserId()
    if (playlistId.isBlank()) return
    val docRef = db.collection("video_playlists").document(playlistId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observeVideoWatchHistory(userId: String): Flow<List<VideoWatchHistory>> = flow {
    val path = "video_watch_history"
    db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(VideoWatchHistory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: (it.createdAt?.seconds ?: 0L) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
      .collect { emit(it) }
  }

  suspend fun recordVideoWatchProgress(
    video: LingoVideo,
    watchedSeconds: Long,
    completedQuiz: Boolean = false,
    quizScorePercent: Int = 0,
    xpReward: Int = 0
  ) {
    val uid = requireUserId()
    val safeVid = sanitizeDocIdSegment(video.videoId)
    val historyId = "vh_${uid}_${safeVid}".take(120)
    val docRef = db.collection("video_watch_history").document(historyId)
    val validLevel = video.proficiencyLevel.takeIf { it in listOf("Beginner", "Intermediate", "Advanced") } ?: "Beginner"

    try {
      val existingQuery = db.collection("video_watch_history")
        .whereEqualTo("userId", uid)
        .whereEqualTo("videoId", video.videoId.take(60))
        .get()
        .await()
      val existingSnap = existingQuery.documents.firstOrNull()
      if (existingSnap != null && existingSnap.exists()) {
        val prevWatched = existingSnap.getLong("watchedSeconds") ?: 0L
        val prevQuizDone = existingSnap.getBoolean("completedQuiz") ?: false
        val prevScore = existingSnap.getLong("quizScorePercent") ?: 0L
        val prevXp = existingSnap.getLong("xpEarned") ?: 0L

        docRef.update(
          mapOf(
            "watchedSeconds" to maxOf(prevWatched, watchedSeconds),
            "completedQuiz" to (prevQuizDone || completedQuiz),
            "quizScorePercent" to maxOf(prevScore, quizScorePercent.toLong().coerceIn(0L, 100L)),
            "xpEarned" to (prevXp + xpReward.toLong()).coerceAtMost(500L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "videoId" to video.videoId.take(60),
            "title" to video.title.ifBlank { "Language Video" }.take(240),
            "channelTitle" to video.channelTitle.ifBlank { "LingoSphere Studio" }.take(140),
            "thumbnailUrl" to video.thumbnailUrl.take(490),
            "durationText" to video.durationText.take(35),
            "languageCode" to video.languageCode.take(10),
            "proficiencyLevel" to validLevel,
            "watchedSeconds" to maxOf(0L, watchedSeconds),
            "completedQuiz" to completedQuiz,
            "quizScorePercent" to quizScorePercent.toLong().coerceIn(0L, 100L),
            "xpEarned" to xpReward.toLong().coerceIn(0L, 500L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }

      if (xpReward > 0) {
        val user = getUserProfile(uid)
        if (user != null) {
          val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
          val newXp = user.xpPoints + xpReward
          val newLevel = (newXp / 100) + 1
          updateUserProfile(
            user.copy(
              xpPoints = newXp,
              level = newLevel,
              lastActiveDate = today
            )
          )
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  // --- Gemini AI Account-Based Quota & Cost Telemetry Cloud Sync ---

  suspend fun syncAiUsageQuota(snapshot: com.example.ai.AccountAiUsageSnapshot) {
    val uid = auth.currentUser?.uid ?: return
    if (uid.isBlank() || (snapshot.userId != "anonymous" && snapshot.userId != uid)) return
    val docRef = db.collection("ai_usage_quotas").document(uid)
    val payload = hashMapOf<String, Any>(
      "userId" to uid,
      "subscriptionTier" to snapshot.subscriptionTier,
      "currentMonthKey" to snapshot.currentMonthKey.take(16),
      "currentDayKey" to snapshot.currentDayKey.take(16),
      "monthlyAiVoiceSecondsUsed" to snapshot.monthlyAiVoiceSecondsUsed.coerceAtLeast(0),
      "monthlyAiVideoSecondsUsed" to snapshot.monthlyAiVideoSecondsUsed.coerceAtLeast(0),
      "dailyGrammarChecksUsed" to snapshot.dailyGrammarChecksUsed.coerceAtLeast(0),
      "dailyPronunciationChecksUsed" to snapshot.dailyPronunciationChecksUsed.coerceAtLeast(0),
      "dailyIntelligentStudioUsed" to snapshot.dailyIntelligentStudioUsed.coerceAtLeast(0),
      "dailyHumanCallSecondsUsed" to snapshot.dailyHumanCallSecondsUsed.coerceAtLeast(0),
      "monthlyInputTokens" to snapshot.monthlyInputTokens.coerceAtLeast(0L),
      "monthlyOutputTokens" to snapshot.monthlyOutputTokens.coerceAtLeast(0L),
      "monthlyAudioInputSeconds" to snapshot.monthlyAudioInputSeconds.coerceAtLeast(0),
      "monthlyAudioOutputSeconds" to snapshot.monthlyAudioOutputSeconds.coerceAtLeast(0),
      "monthlyFlashLiteCalls" to snapshot.monthlyFlashLiteCalls.coerceAtLeast(0),
      "monthlyFlashStandardCalls" to snapshot.monthlyFlashStandardCalls.coerceAtLeast(0),
      "monthlyEstimatedCostUsd" to snapshot.monthlyEstimatedCostUsd.coerceAtLeast(0.0),
      "compressedHistoryTokensSaved" to snapshot.compressedHistoryTokensSaved.coerceAtLeast(0L),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
    }
  }

  suspend fun fetchAiUsageQuota(userId: String): com.example.ai.AccountAiUsageSnapshot? {
    val uid = auth.currentUser?.uid ?: return null
    if (uid != userId) return null
    val docRef = db.collection("ai_usage_quotas").document(uid)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) return null
      com.example.ai.AccountAiUsageSnapshot(
        userId = snap.getString("userId") ?: uid,
        subscriptionTier = snap.getString("subscriptionTier") ?: "free",
        currentMonthKey = snap.getString("currentMonthKey") ?: "",
        currentDayKey = snap.getString("currentDayKey") ?: "",
        monthlyAiVoiceSecondsUsed = (snap.getLong("monthlyAiVoiceSecondsUsed") ?: 0L).toInt(),
        monthlyAiVideoSecondsUsed = (snap.getLong("monthlyAiVideoSecondsUsed") ?: 0L).toInt(),
        dailyGrammarChecksUsed = (snap.getLong("dailyGrammarChecksUsed") ?: 0L).toInt(),
        dailyPronunciationChecksUsed = (snap.getLong("dailyPronunciationChecksUsed") ?: 0L).toInt(),
        dailyIntelligentStudioUsed = (snap.getLong("dailyIntelligentStudioUsed") ?: 0L).toInt(),
        dailyHumanCallSecondsUsed = (snap.getLong("dailyHumanCallSecondsUsed") ?: 0L).toInt(),
        monthlyInputTokens = snap.getLong("monthlyInputTokens") ?: 0L,
        monthlyOutputTokens = snap.getLong("monthlyOutputTokens") ?: 0L,
        monthlyAudioInputSeconds = (snap.getLong("monthlyAudioInputSeconds") ?: 0L).toInt(),
        monthlyAudioOutputSeconds = (snap.getLong("monthlyAudioOutputSeconds") ?: 0L).toInt(),
        monthlyFlashLiteCalls = (snap.getLong("monthlyFlashLiteCalls") ?: 0L).toInt(),
        monthlyFlashStandardCalls = (snap.getLong("monthlyFlashStandardCalls") ?: 0L).toInt(),
        monthlyEstimatedCostUsd = snap.getDouble("monthlyEstimatedCostUsd") ?: 0.0,
        compressedHistoryTokensSaved = snap.getLong("compressedHistoryTokensSaved") ?: 0L
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  private suspend fun postBackendJsonWithTokens(
    endpointName: String,
    payload: JSONObject,
    upgradeModeOnProRequired: UpgradeSheetMode = UpgradeSheetMode.AI_UPGRADE,
    featureKeyOnProRequired: String = "ai.pro"
  ): Result<JSONObject> = withContext(Dispatchers.IO) {
    try {
      val currentUser = auth.currentUser
        ?: return@withContext Result.failure(IllegalStateException("Sign in to continue."))
      com.example.ensureAppCheckConfiguredIfPending()

      suspend fun executeAttempt(forceRefresh: Boolean): Pair<Int, String> {
        val idToken = currentUser.getIdToken(forceRefresh).await()?.token
          ?: throw IllegalStateException("Session expired. Sign in to continue.")
        val appCheckToken = runCatching {
          FirebaseAppCheck.getInstance().getAppCheckToken(forceRefresh).await()?.token
        }.getOrNull().orEmpty()

        val url = URL("https://us-central1-gen-lang-client-0662926032.cloudfunctions.net/$endpointName")
        val conn = (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "POST"
          connectTimeout = 10_000
          readTimeout = 12_000
          doOutput = true
          setRequestProperty("Authorization", "Bearer $idToken")
          if (appCheckToken.isNotBlank()) {
            setRequestProperty("X-Firebase-AppCheck", appCheckToken)
          }
          setRequestProperty("Content-Type", "application/json")
        }
        return try {
          conn.outputStream.use { os ->
            os.write(payload.toString().toByteArray(Charsets.UTF_8))
          }
          val code = conn.responseCode
          val stream = if (code in 200..299) conn.inputStream else conn.errorStream
          val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
          code to responseText
        } finally {
          conn.disconnect()
        }
      }

      var (code, responseText) = executeAttempt(forceRefresh = false)
      if (code == 401) {
        val retry = executeAttempt(forceRefresh = true)
        code = retry.first
        responseText = retry.second
      }

      val parsed = runCatching {
        if (responseText.isNotBlank()) JSONObject(responseText) else JSONObject()
      }.getOrNull()

      if (code == 403 && parsed?.optString("code").equals("PRO_REQUIRED", ignoreCase = true)) {
        val serverMsg = parsed?.optString("error").orEmpty().ifBlank {
          "Unlock full access with LingoSphere Pro."
        }
        AiErrorHandler.requestUpgrade(
          mode = upgradeModeOnProRequired,
          featureKey = featureKeyOnProRequired,
          reason = serverMsg
        )
        return@withContext Result.failure(IllegalStateException(serverMsg))
      }

      if (code in 200..299 && parsed != null) {
        Result.success(parsed)
      } else {
        val uiErr = AiErrorHandler.handle(
          serverCode = parsed?.optString("code"),
          serverMessage = parsed?.optString("error") ?: responseText,
          contextTag = endpointName
        )
        Result.failure(IllegalStateException(uiErr.message))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uiErr = AiErrorHandler.handle(throwable = e, contextTag = endpointName)
      Result.failure(IllegalStateException(uiErr.message, e))
    }
  }

  suspend fun verifySubscriptionEntitlementWithBackend(
    packageName: String,
    subscriptionTier: String,
    productId: String,
    basePlanId: String,
    purchaseToken: String,
    isFreeTrial: Boolean,
    subscriptionAutoRenewing: Boolean,
    subscriptionExpiryEpochMs: Long
  ): Result<JSONObject> {
    val payload = JSONObject().apply {
      put("packageName", packageName)
      put("subscriptionTier", subscriptionTier)
      put("productId", productId)
      put("basePlanId", basePlanId)
      put("purchaseToken", purchaseToken)
      put("isFreeTrial", isFreeTrial)
      put("subscriptionAutoRenewing", subscriptionAutoRenewing)
      put("subscriptionExpiryEpochMs", subscriptionExpiryEpochMs)
    }
    return postBackendJsonWithTokens("verifySubscriptionEntitlement", payload)
  }

  suspend fun verifyLessonAccessWithBackend(lessonId: String): Result<JSONObject> {
    val payload = JSONObject().apply {
      put("lessonId", lessonId.trim())
    }
    return postBackendJsonWithTokens(
      endpointName = "getLessonContent",
      payload = payload,
      upgradeModeOnProRequired = UpgradeSheetMode.LESSONS,
      featureKeyOnProRequired = AccessPolicyFeatureKeys.LESSONS_DAILY
    )
  }

  suspend fun completeLessonViaBackend(
    lessonId: String,
    languageCode: String,
    category: String,
    scorePercent: Int,
    xpGained: Int
  ): Result<JSONObject> {
    val payload = JSONObject().apply {
      put("lessonId", lessonId.trim())
      put("languageCode", languageCode.trim())
      put("category", category.trim())
      put("scorePercent", scorePercent.coerceIn(0, 100))
      put("xpGained", xpGained.coerceIn(5, 150))
      put("timezone", java.util.TimeZone.getDefault().id)
    }
    return postBackendJsonWithTokens(
      endpointName = "completeLesson",
      payload = payload,
      upgradeModeOnProRequired = UpgradeSheetMode.LESSONS,
      featureKeyOnProRequired = AccessPolicyFeatureKeys.LESSONS_DAILY
    )
  }

  suspend fun completeLearningActivityViaBackend(activityType: String): Result<JSONObject> {
    val payload = JSONObject().apply {
      put("activityType", activityType.trim())
      put("timezone", java.util.TimeZone.getDefault().id)
    }
    return postBackendJsonWithTokens(
      endpointName = "completeLearningActivity",
      payload = payload
    )
  }

  suspend fun recordOrCheckBookAccessViaBackend(
    bookId: String,
    gutendexId: Int? = null
  ): Result<JSONObject> {
    val payload = JSONObject().apply {
      put("bookId", bookId.trim())
      if (gutendexId != null) {
        put("gutendexId", gutendexId)
      }
      put("timezone", java.util.TimeZone.getDefault().id)
    }
    val res = postBackendJsonWithTokens(
      endpointName = "getBookContentUrl",
      payload = payload,
      upgradeModeOnProRequired = UpgradeSheetMode.BOOKS,
      featureKeyOnProRequired = AccessPolicyFeatureKeys.BOOKS_READING
    )
    res.getOrNull()?.let { json ->
      if (json.optBoolean("unlocked", false)) {
        AccessPolicyManager.recordOpenedBookLocallyForMonth(bookId, gutendexId)
      }
    }
    return res
  }
}
