package com.behaviorengine.core.data.matching

import com.behaviorengine.core.domain.matching.CacheStats
import com.behaviorengine.core.domain.matching.CandidateRegion
import com.behaviorengine.core.domain.matching.MatchingCache
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private data class CacheEntry(val region: CandidateRegion, val cachedAtMillis: Long)

/** Real implementation of [MatchingCache] — see that interface's KDoc for why it's in-memory only. */
@Singleton
class MatchingCacheImpl @Inject constructor() : MatchingCache {

    private val entries = ConcurrentHashMap<String, CacheEntry>()

    @Volatile private var hits = 0

    @Volatile private var misses = 0

    override fun getCachedLocation(templateId: String): CandidateRegion? {
        val entry = entries[templateId]
        val isExpired = entry != null && System.currentTimeMillis() - entry.cachedAtMillis > MatchingCache.ENTRY_TTL_MILLIS
        if (isExpired) entries.remove(templateId)
        val region = if (isExpired) null else entry?.region
        if (region != null) hits++ else misses++
        return region
    }

    override fun putSuccessfulLocation(templateId: String, region: CandidateRegion) {
        entries[templateId] = CacheEntry(region, System.currentTimeMillis())
    }

    override val stats: CacheStats get() = CacheStats(hits, misses)

    override fun resetStats() {
        hits = 0
        misses = 0
    }

    override fun clear() {
        entries.clear()
        resetStats()
    }
}
