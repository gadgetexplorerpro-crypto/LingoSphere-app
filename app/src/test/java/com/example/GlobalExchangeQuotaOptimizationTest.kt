package com.example

import com.example.avatar.TutorPersona
import com.example.avatar.ui.visualStyleForTutor
import com.example.data.model.CommunityProfile
import com.example.data.repository.FirestoreQuotaManager
import com.example.data.repository.PartnerProfileCache
import com.example.presence.PresenceManager
import com.example.presence.RtdbPresenceRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GlobalExchangeQuotaOptimizationTest {

  @Before
  fun setUp() {
    FirestoreQuotaManager.resetForTests()
    PartnerProfileCache.resetForTests()
  }

  @After
  fun tearDown() {
    FirestoreQuotaManager.resetForTests()
    PartnerProfileCache.resetForTests()
  }

  @Test
  fun testQuotaManagerDetectsResourceExhaustedAndAppliesExponentialBackoff() {
    val directException = FirebaseFirestoreException(
      "RESOURCE_EXHAUSTED: Firestore free daily read quota exceeded",
      FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED
    )
    val wrappedException = RuntimeException("Query failed", directException)
    val permissionException = FirebaseFirestoreException(
      "PERMISSION_DENIED",
      FirebaseFirestoreException.Code.PERMISSION_DENIED
    )

    assertTrue(FirestoreQuotaManager.isQuotaExhaustedError(directException))
    assertTrue(FirestoreQuotaManager.isQuotaExhaustedError(wrappedException))
    assertFalse(FirestoreQuotaManager.isQuotaExhaustedError(permissionException))

    val t0 = 1_000_000L
    assertFalse(FirestoreQuotaManager.shouldBlockRead(t0))

    // Hit #1 -> 60s cooldown
    FirestoreQuotaManager.recordQuotaExhausted(directException, nowMs = t0)
    assertTrue(FirestoreQuotaManager.isQuotaExhausted.value)
    assertTrue(FirestoreQuotaManager.shouldBlockRead(t0 + 30_000L))
    assertEquals(30L, FirestoreQuotaManager.remainingCooldownSeconds(t0 + 30_000L))
    assertFalse(FirestoreQuotaManager.shouldBlockRead(t0 + 60_001L))

    // Hit #2 -> 120s exponential backoff
    val t1 = t0 + 61_000L
    FirestoreQuotaManager.recordQuotaExhausted(directException, nowMs = t1)
    assertTrue(FirestoreQuotaManager.shouldBlockRead(t1 + 90_000L))
    assertFalse(FirestoreQuotaManager.shouldBlockRead(t1 + 120_001L))

    // Manual retry clears cooldown immediately
    FirestoreQuotaManager.clearCooldownForManualRetry()
    assertFalse(FirestoreQuotaManager.isQuotaExhausted.value)
    assertFalse(FirestoreQuotaManager.shouldBlockRead(t1 + 1_000L))

    // Read success resets consecutive counter back to initial 60s step
    FirestoreQuotaManager.recordQuotaExhausted(directException, nowMs = t1)
    FirestoreQuotaManager.recordReadSuccess()
    assertFalse(FirestoreQuotaManager.isQuotaExhausted.value)
    FirestoreQuotaManager.recordQuotaExhausted(directException, nowMs = t1)
    assertEquals(60L, FirestoreQuotaManager.remainingCooldownSeconds(t1))
  }

  @Test
  fun testPartnerCacheStripsOnlinePresenceAndRespectsTtl() {
    val liveLookingProfile = CommunityProfile(
      userId = "u_partner_1",
      displayName = "Maria",
      nativeLanguageCode = "es",
      targetLanguageCode = "en",
      isOnline = true,
      presenceState = "online",
      lastHeartbeatEpochMs = 999_999L
    )

    val key = PartnerProfileCache.buildKey("es", "Spain")
    val putTime = 10_000_000L
    PartnerProfileCache.putPage(
      cacheKey = key,
      profiles = listOf(liveLookingProfile),
      hasMore = true,
      lastDocId = "u_partner_1",
      nowMs = putTime
    )

    val hit = PartnerProfileCache.getPage(key, nowMs = putTime + 60_000L)
    assertNotNull(hit)
    assertEquals(1, hit!!.profiles.size)
    // Cached profiles MUST NOT pretend the partner is online without live RTDB verification
    assertFalse(hit.profiles.first().isOnline)
    assertEquals("offline", hit.profiles.first().presenceState)
    assertEquals(0L, hit.profiles.first().lastHeartbeatEpochMs)

    // After 5-minute TTL expires, cache returns null
    val expired = PartnerProfileCache.getPage(key, nowMs = putTime + PartnerProfileCache.DEFAULT_TTL_MS + 1L)
    assertNull(expired)
  }

  @Test
  fun testPresenceResolutionCombinesCachedProfileWithLiveRtdbAccurately() {
    val now = 20_000_000L
    val cachedOfflineProfile = CommunityProfile(
      userId = "u_partner_2",
      displayName = "Ken",
      isOnline = false,
      presenceState = "offline",
      lastHeartbeatEpochMs = 0L
    )

    // Without RTDB record, cached partner stays offline
    val resolvedNoRtdb = PresenceManager.resolvePartnerPresence(
      profile = cachedOfflineProfile,
      rtdbMap = emptyMap(),
      nowEpochMs = now
    )
    assertFalse(resolvedNoRtdb.isOnline)
    assertEquals("offline", resolvedNoRtdb.presenceState)

    // With fresh RTDB online record, partner becomes accurately online
    val rtdbLiveMap = mapOf(
      "u_partner_2" to RtdbPresenceRecord(
        userId = "u_partner_2",
        state = "online",
        isOnline = true,
        showLastSeen = true,
        lastSeenEpochMs = now - 5_000L,
        lastHeartbeatEpochMs = now - 5_000L
      )
    )
    val resolvedLive = PresenceManager.resolvePartnerPresence(
      profile = cachedOfflineProfile,
      rtdbMap = rtdbLiveMap,
      nowEpochMs = now
    )
    assertTrue(resolvedLive.isOnline)
    assertEquals("online", resolvedLive.presenceState)

    // With expired RTDB heartbeat beyond timeout, partner automatically resolves offline/away
    val staleRtdbMap = mapOf(
      "u_partner_2" to RtdbPresenceRecord(
        userId = "u_partner_2",
        state = "online",
        isOnline = true,
        showLastSeen = true,
        lastSeenEpochMs = now - 600_000L,
        lastHeartbeatEpochMs = now - 600_000L
      )
    )
    val resolvedStale = PresenceManager.resolvePartnerPresence(
      profile = cachedOfflineProfile,
      rtdbMap = staleRtdbMap,
      nowEpochMs = now
    )
    assertFalse(resolvedStale.isOnline)
    assertEquals("offline", resolvedStale.presenceState)
  }

  @Test
  fun testAllTutorPersonasResolveDistinctAnimatedCanvasStyles() {
    assertTrue("Expected at least 8 tutors", TutorPersona.ALL_TUTORS.size >= 8)
    val styles = TutorPersona.ALL_TUTORS.map { tutor ->
      val style = visualStyleForTutor(tutor)
      assertNotNull(style)
      style
    }
    val uniqueHairAndOutfitSignatures = styles.map { "${it.hairStyle}_${it.hairPrimary.value}_${it.outfitPrimary.value}" }.toSet()
    assertEquals(TutorPersona.ALL_TUTORS.size, uniqueHairAndOutfitSignatures.size)
  }
}
