package com.behaviorengine.core.domain.matching

/**
 * Raw file I/O for matching data — sibling to
 * [com.behaviorengine.core.domain.objectlearning.ObjectLearningStorage], but rooted at its own
 * `Matching/` folder rather than sharing `Teaching/`: statistics/history are IVME's own output,
 * produced long after a teaching session ends, not teaching-pipeline data.
 */
interface MatchingStorage {
    suspend fun writeStatistics(statistics: MatchingStatistics)
    suspend fun listStatistics(): List<MatchingStatistics>
    suspend fun writeHistoryEntry(match: MatchResult)
    suspend fun listHistory(): List<MatchResult>
}
