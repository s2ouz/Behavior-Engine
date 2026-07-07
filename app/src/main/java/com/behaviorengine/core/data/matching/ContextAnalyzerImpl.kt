package com.behaviorengine.core.data.matching

import com.behaviorengine.core.domain.matching.AnalyzedScreen
import com.behaviorengine.core.domain.matching.CandidateRegion
import com.behaviorengine.core.domain.matching.ContextAnalyzer
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

private const val NEUTRAL_SCORE = 0.5f
private const val MAX_NORMALIZED_DISTANCE = 1.4142135f // sqrt(2): the diagonal of the normalized 0..1 x 0..1 screen

/** Real implementation of [ContextAnalyzer] — see that interface's KDoc for the honest scope (positional consistency, not a full layout graph). */
@Singleton
class ContextAnalyzerImpl @Inject constructor() : ContextAnalyzer {

    override fun score(template: ObjectTemplate, region: CandidateRegion, screen: AnalyzedScreen): Float {
        if (template.screenPositionX < 0f || template.screenPositionY < 0f) return NEUTRAL_SCORE

        val candidateX = region.centerX / screen.width
        val candidateY = region.centerY / screen.height
        val dx = candidateX - template.screenPositionX
        val dy = candidateY - template.screenPositionY
        val distance = sqrt(dx * dx + dy * dy)
        return (1f - distance / MAX_NORMALIZED_DISTANCE).coerceIn(0f, 1f)
    }
}
