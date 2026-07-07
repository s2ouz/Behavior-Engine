package com.behaviorengine.core.data.matching

import android.content.Context
import com.behaviorengine.core.domain.matching.MatchResult
import com.behaviorengine.core.domain.matching.MatchingStatistics
import com.behaviorengine.core.domain.matching.MatchingStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [MatchingStorage] — its own `Matching/` root, sibling to `Teaching/`; see
 * that interface's KDoc for why matching output isn't nested under the teaching folder.
 */
@Singleton
class MatchingStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MatchingStorage {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val rootDir: File by lazy { File(context.getExternalFilesDir(null), "Matching").apply { mkdirs() } }
    private val statisticsDir: File by lazy { File(rootDir, "Statistics").apply { mkdirs() } }
    private val historyDir: File by lazy { File(rootDir, "History").apply { mkdirs() } }

    override suspend fun writeStatistics(statistics: MatchingStatistics) {
        withContext(Dispatchers.IO) {
            File(statisticsDir, "${statistics.id}.json").writeText(json.encodeToString(MatchingStatistics.serializer(), statistics))
        }
    }

    override suspend fun listStatistics(): List<MatchingStatistics> = withContext(Dispatchers.IO) {
        statisticsDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { runCatching { json.decodeFromString(MatchingStatistics.serializer(), it.readText()) }.getOrNull() }
            ?: emptyList()
    }

    override suspend fun writeHistoryEntry(match: MatchResult) {
        withContext(Dispatchers.IO) {
            File(historyDir, "${match.id}.json").writeText(json.encodeToString(MatchResult.serializer(), match))
        }
    }

    override suspend fun listHistory(): List<MatchResult> = withContext(Dispatchers.IO) {
        historyDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { runCatching { json.decodeFromString(MatchResult.serializer(), it.readText()) }.getOrNull() }
            ?: emptyList()
    }
}
