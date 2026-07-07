package com.behaviorengine.core.domain.matching

import kotlinx.serialization.Serializable

/** One [VisualMatchingManager.findObject]/[VisualMatchingManager.findAllObjects] run's performance record, for [MatchingRepository]. */
@Serializable
data class MatchingStatistics(
    val id: String,
    val processingTimeMillis: Long,
    val searchedRegions: Int,
    val matchedTemplates: Int,
    val cacheHits: Int,
    val cacheMisses: Int,
    val confidence: Int,
    val recordedAtMillis: Long
)
