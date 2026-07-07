package com.behaviorengine.di

import com.behaviorengine.core.data.matching.CandidateSearchEngineImpl
import com.behaviorengine.core.data.matching.ConfidenceEngineImpl
import com.behaviorengine.core.data.matching.ContextAnalyzerImpl
import com.behaviorengine.core.data.matching.DebugOverlayManagerImpl
import com.behaviorengine.core.data.matching.FeatureMatcherImpl
import com.behaviorengine.core.data.matching.MatchingCacheImpl
import com.behaviorengine.core.data.matching.MatchingRepositoryImpl
import com.behaviorengine.core.data.matching.MatchingServiceConnectionImpl
import com.behaviorengine.core.data.matching.MatchingStorageImpl
import com.behaviorengine.core.data.matching.MultiScaleMatcherImpl
import com.behaviorengine.core.data.matching.OCRMatcherImpl
import com.behaviorengine.core.data.matching.ScreenAnalyzerImpl
import com.behaviorengine.core.data.matching.VisualMatchingManagerImpl
import com.behaviorengine.core.domain.matching.CandidateSearchEngine
import com.behaviorengine.core.domain.matching.ConfidenceEngine
import com.behaviorengine.core.domain.matching.ContextAnalyzer
import com.behaviorengine.core.domain.matching.DebugOverlayManager
import com.behaviorengine.core.domain.matching.FeatureMatcher
import com.behaviorengine.core.domain.matching.MatchingCache
import com.behaviorengine.core.domain.matching.MatchingRepository
import com.behaviorengine.core.domain.matching.MatchingServiceConnection
import com.behaviorengine.core.domain.matching.MatchingStorage
import com.behaviorengine.core.domain.matching.MultiScaleMatcher
import com.behaviorengine.core.domain.matching.OCRMatcher
import com.behaviorengine.core.domain.matching.ScreenAnalyzer
import com.behaviorengine.core.domain.matching.VisualMatchingManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds every Intelligent Visual Matching Engine (SPEC-11) contract to its real implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class VisualMatchingDiModule {

    @Binds
    @Singleton
    abstract fun bindScreenAnalyzer(impl: ScreenAnalyzerImpl): ScreenAnalyzer

    @Binds
    @Singleton
    abstract fun bindCandidateSearchEngine(impl: CandidateSearchEngineImpl): CandidateSearchEngine

    @Binds
    @Singleton
    abstract fun bindMultiScaleMatcher(impl: MultiScaleMatcherImpl): MultiScaleMatcher

    @Binds
    @Singleton
    abstract fun bindFeatureMatcher(impl: FeatureMatcherImpl): FeatureMatcher

    @Binds
    @Singleton
    abstract fun bindOCRMatcher(impl: OCRMatcherImpl): OCRMatcher

    @Binds
    @Singleton
    abstract fun bindContextAnalyzer(impl: ContextAnalyzerImpl): ContextAnalyzer

    @Binds
    @Singleton
    abstract fun bindConfidenceEngine(impl: ConfidenceEngineImpl): ConfidenceEngine

    @Binds
    @Singleton
    abstract fun bindMatchingCache(impl: MatchingCacheImpl): MatchingCache

    @Binds
    @Singleton
    abstract fun bindMatchingStorage(impl: MatchingStorageImpl): MatchingStorage

    @Binds
    @Singleton
    abstract fun bindMatchingRepository(impl: MatchingRepositoryImpl): MatchingRepository

    @Binds
    @Singleton
    abstract fun bindMatchingServiceConnection(impl: MatchingServiceConnectionImpl): MatchingServiceConnection

    @Binds
    @Singleton
    abstract fun bindDebugOverlayManager(impl: DebugOverlayManagerImpl): DebugOverlayManager

    @Binds
    @Singleton
    abstract fun bindVisualMatchingManager(impl: VisualMatchingManagerImpl): VisualMatchingManager
}
