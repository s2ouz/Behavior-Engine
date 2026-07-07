package com.behaviorengine.core.domain.matching

import com.behaviorengine.core.domain.objectlearning.ObjectTemplate

/**
 * Reduces false positives by checking whether a candidate's position is plausible, not just its
 * pixels. Scoped honestly to positional consistency — comparing [CandidateRegion]'s normalized
 * position on [AnalyzedScreen] against [ObjectTemplate.screenPositionX]/[ObjectTemplate.screenPositionY] —
 * rather than the spec's fuller "neighbor objects / layout graph" comparison: that needs re-running
 * detection across the whole screen to find neighboring elements and match them individually, which
 * this phase doesn't attempt. A real, cheap, useful signal; not a full scene-layout matcher.
 */
interface ContextAnalyzer {
    /** 0..1. Returns a neutral 0.5 when [template] has no stored position (pre-SPEC-11 templates). */
    fun score(template: ObjectTemplate, region: CandidateRegion, screen: AnalyzedScreen): Float
}
