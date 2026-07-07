package com.behaviorengine.core.domain.matching

data class CacheStats(val hits: Int, val misses: Int)

/**
 * In-memory only, deliberately — a cache surviving process death would need its entries
 * revalidated against a live screen anyway (the UI may have changed since the app was last
 * running), so nothing is gained by persisting it; durable match history already lives in
 * [MatchingRepository]. Entries expire automatically after [ENTRY_TTL_MILLIS] so a stale cached
 * location can't be trusted indefinitely once the UI moves on.
 */
interface MatchingCache {
    /** Last successful [CandidateRegion] for [templateId], or `null` if absent/expired. Recording a hit/miss either way. */
    fun getCachedLocation(templateId: String): CandidateRegion?

    fun putSuccessfulLocation(templateId: String, region: CandidateRegion)

    val stats: CacheStats

    fun resetStats()

    fun clear()

    companion object {
        const val ENTRY_TTL_MILLIS = 5 * 60 * 1000L
    }
}
