package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CommunityProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Central circuit-breaker & exponential backoff manager for Cloud Firestore quota exhaustion
 * (`RESOURCE_EXHAUSTED`), preventing tight retry loops and protecting remaining daily read budget.
 */
object FirestoreQuotaManager {
  private const val BASE_BACKOFF_MS = 60_000L
  private const val MAX_BACKOFF_MS = 300_000L

  private val _isQuotaExhausted = MutableStateFlow(false)
  val isQuotaExhausted: StateFlow<Boolean> = _isQuotaExhausted.asStateFlow()

  private val _cooldownUntilEpochMs = MutableStateFlow(0L)
  val cooldownUntilEpochMs: StateFlow<Long> = _cooldownUntilEpochMs.asStateFlow()

  private val _lastQuotaErrorMessage = MutableStateFlow<String?>(null)
  val lastQuotaErrorMessage: StateFlow<String?> = _lastQuotaErrorMessage.asStateFlow()

  private var consecutiveQuotaHits: Int = 0

  fun isQuotaExhaustedError(throwable: Throwable?): Boolean {
    if (throwable == null) return false
    var curr: Throwable? = throwable
    var depth = 0
    while (curr != null && depth < 12) {
      if (curr is FirebaseFirestoreException &&
        curr.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED
      ) {
        return true
      }
      val msg = curr.message?.lowercase().orEmpty()
      if (msg.contains("resource_exhausted") ||
        msg.contains("quota exceeded") ||
        msg.contains("daily read limit") ||
        msg.contains("free daily read")
      ) {
        return true
      }
      curr = curr.cause
      depth++
    }
    return false
  }

  @Synchronized
  fun shouldBlockRead(nowMs: Long = System.currentTimeMillis()): Boolean {
    val until = _cooldownUntilEpochMs.value
    if (until <= 0L) return false
    if (nowMs < until) {
      FirestoreDiagnostics.recordQuotaBlocked("firestore_read")
      return true
    }
    return false
  }

  @Synchronized
  fun remainingCooldownSeconds(nowMs: Long = System.currentTimeMillis()): Long {
    val remainingMs = (_cooldownUntilEpochMs.value - nowMs).coerceAtLeast(0L)
    return if (remainingMs == 0L) 0L else (remainingMs + 999L) / 1000L
  }

  @Synchronized
  fun recordQuotaExhausted(throwable: Throwable? = null, nowMs: Long = System.currentTimeMillis()) {
    consecutiveQuotaHits = (consecutiveQuotaHits + 1).coerceAtMost(5)
    val multiplier = 1L shl (consecutiveQuotaHits - 1).coerceAtLeast(0)
    val backoffMs = (BASE_BACKOFF_MS * multiplier).coerceAtMost(MAX_BACKOFF_MS)
    _cooldownUntilEpochMs.value = nowMs + backoffMs
    _isQuotaExhausted.value = true
    _lastQuotaErrorMessage.value =
      "Daily Firebase cloud read limit reached. Automatic retries paused for ${backoffMs / 1000L}s."
    if (BuildConfig.DEBUG) {
      Log.w(
        "FirestoreQuota",
        "RESOURCE_EXHAUSTED hit #$consecutiveQuotaHits — halting automatic Firestore reads for ${backoffMs / 1000L}s (${throwable?.message})"
      )
    }
  }

  @Synchronized
  fun recordReadSuccess() {
    if (consecutiveQuotaHits != 0 || _isQuotaExhausted.value || _cooldownUntilEpochMs.value != 0L) {
      consecutiveQuotaHits = 0
      _isQuotaExhausted.value = false
      _cooldownUntilEpochMs.value = 0L
      _lastQuotaErrorMessage.value = null
    }
  }

  @Synchronized
  fun clearCooldownForManualRetry() {
    _cooldownUntilEpochMs.value = 0L
    _isQuotaExhausted.value = false
    _lastQuotaErrorMessage.value = null
  }

  @Synchronized
  fun resetForTests() {
    consecutiveQuotaHits = 0
    _cooldownUntilEpochMs.value = 0L
    _isQuotaExhausted.value = false
    _lastQuotaErrorMessage.value = null
  }
}

/**
 * Time-bounded TTL cache for partner directory & leaderboard pages.
 * Always strips online/presence flags on storage and retrieval so cached partners
 * never falsely display as "Online" without live RTDB verification.
 */
object PartnerProfileCache {
  const val DEFAULT_TTL_MS = 5 * 60_000L

  data class CachedPartnersPage(
    val profiles: List<CommunityProfile>,
    val lastDocId: String?,
    val hasMore: Boolean,
    val fetchedAtEpochMs: Long
  )

  private val cacheMap = ConcurrentHashMap<String, CachedPartnersPage>()

  fun buildKey(filterLanguageCode: String, filterCountry: String): String {
    return "${filterLanguageCode.trim().uppercase()}|${filterCountry.trim().uppercase()}"
  }

  private fun stripPresenceToOffline(profile: CommunityProfile): CommunityProfile {
    return profile.copy(
      isOnline = false,
      presenceState = "offline",
      lastHeartbeatEpochMs = 0L
    )
  }

  fun putPage(
    cacheKey: String,
    profiles: List<CommunityProfile>,
    hasMore: Boolean,
    lastDocId: String?,
    nowMs: Long = System.currentTimeMillis()
  ) {
    val sanitized = profiles.distinctBy { it.userId }.map { stripPresenceToOffline(it) }
    cacheMap[cacheKey] = CachedPartnersPage(
      profiles = sanitized,
      lastDocId = lastDocId,
      hasMore = hasMore,
      fetchedAtEpochMs = nowMs
    )
  }

  fun getPage(
    cacheKey: String,
    nowMs: Long = System.currentTimeMillis(),
    ttlMs: Long = DEFAULT_TTL_MS
  ): CachedPartnersPage? {
    val entry = cacheMap[cacheKey] ?: return null
    if (nowMs - entry.fetchedAtEpochMs > ttlMs) {
      cacheMap.remove(cacheKey)
      return null
    }
    FirestoreDiagnostics.recordCacheHit(cacheKey)
    return entry.copy(profiles = entry.profiles.map { stripPresenceToOffline(it) })
  }

  fun invalidateAll() {
    cacheMap.clear()
  }

  fun resetForTests() {
    cacheMap.clear()
  }
}

/**
 * Debug-only lightweight diagnostics tracking Firestore reads, active snapshot listeners,
 * cache hits, quota-blocked attempts, and presence writes.
 */
object FirestoreDiagnostics {
  private val totalReads = AtomicLong(0L)
  private val listenerAttaches = AtomicLong(0L)
  private val listenerDetaches = AtomicLong(0L)
  private val cacheHits = AtomicLong(0L)
  private val quotaBlocked = AtomicLong(0L)
  private val presenceWrites = AtomicLong(0L)

  fun recordRead(docCount: Int = 1, tag: String = "query") {
    val total = totalReads.addAndGet(docCount.coerceAtLeast(1).toLong())
    if (BuildConfig.DEBUG) {
      Log.d("FirestoreDiag", "Read +$docCount ($tag) | totalReads=$total")
    }
  }

  fun recordListenerAttach(tag: String) {
    val active = (listenerAttaches.incrementAndGet() - listenerDetaches.get()).coerceAtLeast(0L)
    if (BuildConfig.DEBUG) {
      Log.d("FirestoreDiag", "Listener ATTACH [$tag] | approxActiveListeners=$active")
    }
  }

  fun recordListenerDetach(tag: String) {
    val attaches = listenerAttaches.get()
    val detaches = listenerDetaches.incrementAndGet()
    val active = (attaches - detaches).coerceAtLeast(0L)
    if (BuildConfig.DEBUG) {
      Log.d("FirestoreDiag", "Listener DETACH [$tag] | approxActiveListeners=$active")
    }
  }

  fun recordCacheHit(tag: String) {
    val total = cacheHits.incrementAndGet()
    if (BuildConfig.DEBUG) {
      Log.d("FirestoreDiag", "Cache HIT [$tag] | totalCacheHits=$total")
    }
  }

  fun recordQuotaBlocked(tag: String) {
    val total = quotaBlocked.incrementAndGet()
    if (BuildConfig.DEBUG) {
      Log.d("FirestoreDiag", "Read BLOCKED by quota breaker [$tag] | totalBlocked=$total")
    }
  }

  fun recordPresenceWrite(target: String) {
    val total = presenceWrites.incrementAndGet()
    if (BuildConfig.DEBUG) {
      Log.d("FirestoreDiag", "Presence write [$target] | totalPresenceWrites=$total")
    }
  }

  fun snapshotSummary(): String {
    return "reads=${totalReads.get()}, attaches=${listenerAttaches.get()}, detaches=${listenerDetaches.get()}, cacheHits=${cacheHits.get()}, quotaBlocked=${quotaBlocked.get()}, presenceWrites=${presenceWrites.get()}"
  }
}
