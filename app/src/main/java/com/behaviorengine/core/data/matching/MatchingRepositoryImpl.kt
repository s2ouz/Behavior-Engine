package com.behaviorengine.core.data.matching

import com.behaviorengine.core.domain.matching.MatchResult
import com.behaviorengine.core.domain.matching.MatchingRepository
import com.behaviorengine.core.domain.matching.MatchingStatistics
import com.behaviorengine.core.domain.matching.MatchingStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchingRepositoryImpl @Inject constructor(
    private val storage: MatchingStorage
) : MatchingRepository {

    override suspend fun saveStatistics(statistics: MatchingStatistics) = storage.writeStatistics(statistics)

    override suspend fun loadStatistics(): List<MatchingStatistics> = storage.listStatistics()

    override suspend fun saveSuccessfulMatch(match: MatchResult) = storage.writeHistoryEntry(match)

    override suspend fun loadHistory(): List<MatchResult> = storage.listHistory()
}
