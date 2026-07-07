package com.behaviorengine.core.domain.matching

import com.behaviorengine.core.domain.objectlearning.ObjectTemplate

/**
 * Reduces the search area before the expensive multi-scale/feature/OCR stages run. Combines, per
 * spec: a cached last-known location, an OCR-text-region seed (when [ObjectTemplate.ocrText] is
 * non-empty), and a grid scan of the screen scored by edge-density and dominant-color similarity
 * to [ObjectTemplate]. "Visual saliency" and "previous successful locations" from the spec's list
 * collapse into the same cache lookup ([MatchingCache]) here — there is no independently trained
 * saliency model in this project (see [com.behaviorengine.core.domain.objectlearning.ObjectDetectionManager]'s
 * KDoc for the same no-native-dependency stance), so edge density + color similarity stand in as
 * the honest, real saliency proxy.
 */
interface CandidateSearchEngine {
    suspend fun findCandidates(template: ObjectTemplate, screen: AnalyzedScreen): List<CandidateRegion>
}
