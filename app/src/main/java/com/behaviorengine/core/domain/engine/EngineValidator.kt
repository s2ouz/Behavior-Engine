package com.behaviorengine.core.domain.engine

/**
 * Runtime self-checks encoding invariants the rest of the engine relies on holding — e.g. no two
 * registered modules sharing an id, or [EngineHealthMonitor] never reporting the runtime active
 * while the engine itself isn't considered alive. Most of these hold by construction today (no
 * real [EngineModule] exists yet to violate the registry invariant); their value is as a
 * regression guard a future phase's [EngineDiagnosticsManager] call can catch immediately if an
 * edit ever breaks one, rather than as evidence something is currently broken.
 */
interface EngineValidator {

    /** Runs every known check and returns what, if anything, failed. */
    fun validate(): ValidationReport
}
