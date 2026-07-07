package com.behaviorengine.core.domain.engine

/** One thing [EngineValidator] found wrong; [component] names which subsystem the check covers. */
data class ValidationIssue(val component: String, val message: String)

/**
 * Result of one [EngineValidator.validate] run. An empty [issues] list means every invariant
 * checked held at [checkedAtMillis] — it does not mean "nothing could ever be wrong," only that
 * nothing *this validator currently knows to check* was violated.
 */
data class ValidationReport(
    val issues: List<ValidationIssue>,
    val checkedAtMillis: Long
) {
    val isValid: Boolean get() = issues.isEmpty()
}
