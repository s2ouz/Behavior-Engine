/**
 * Clean Architecture data layer: repository implementations and data sources backing
 * [com.behaviorengine.core.domain] contracts. First populated in v0.6.0 by
 * [com.behaviorengine.core.data.profile.UserProfileRepositoryImpl] (DataStore-backed). v0.7.0
 * adds [com.behaviorengine.core.data.objects.VisualObjectRepositoryImpl] — in-memory only for
 * now, since there's no image data yet to make real persistence meaningful. v0.8.0 adds
 * [com.behaviorengine.core.data.teaching.TeachingRepositoryImpl] and
 * [com.behaviorengine.core.data.teaching.TeachingManagerImpl] — also in-memory, same reasoning.
 */
package com.behaviorengine.core.data
