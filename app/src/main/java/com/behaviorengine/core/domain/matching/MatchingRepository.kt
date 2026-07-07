package com.behaviorengine.core.domain.matching

/** Business-facing read/write seam over [MatchingStorage] — mirrors [com.behaviorengine.core.domain.objectlearning.ObjectRepository]'s role. */
interface MatchingRepository {
    suspend fun saveStatistics(statistics: MatchingStatistics)
    suspend fun loadStatistics(): List<MatchingStatistics>
    suspend fun saveSuccessfulMatch(match: MatchResult)
    suspend fun loadHistory(): List<MatchResult>
}
