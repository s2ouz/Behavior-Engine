package com.behaviorengine.di

import com.behaviorengine.core.data.ai.AIDecisionManagerImpl
import com.behaviorengine.core.data.ai.AIRepositoryImpl
import com.behaviorengine.core.data.ai.AIStorageImpl
import com.behaviorengine.core.data.ai.AdaptiveRecoveryEngineImpl
import com.behaviorengine.core.data.ai.ConfidenceEngineImpl
import com.behaviorengine.core.data.ai.ContextManagerImpl
import com.behaviorengine.core.data.ai.DecisionEngineImpl
import com.behaviorengine.core.data.ai.MemoryEngineImpl
import com.behaviorengine.core.data.ai.NoOpAutomationExecutor
import com.behaviorengine.core.data.ai.PredictionEngineImpl
import com.behaviorengine.core.data.ai.ReasoningEngineImpl
import com.behaviorengine.core.data.ai.StateRecognitionEngineImpl
import com.behaviorengine.core.data.ai.WorkflowRepositoryImpl
import com.behaviorengine.core.domain.ai.AIDecisionManager
import com.behaviorengine.core.domain.ai.AIRepository
import com.behaviorengine.core.domain.ai.AIStorage
import com.behaviorengine.core.domain.ai.AdaptiveRecoveryEngine
import com.behaviorengine.core.domain.ai.AutomationExecutor
import com.behaviorengine.core.domain.ai.ConfidenceEngine
import com.behaviorengine.core.domain.ai.ContextManager
import com.behaviorengine.core.domain.ai.DecisionEngine
import com.behaviorengine.core.domain.ai.MemoryEngine
import com.behaviorengine.core.domain.ai.PredictionEngine
import com.behaviorengine.core.domain.ai.ReasoningEngine
import com.behaviorengine.core.domain.ai.StateRecognitionEngine
import com.behaviorengine.core.domain.ai.WorkflowRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds every Adaptive AI Decision Engine (SPEC-13) contract to its real implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AIDiModule {

    @Binds
    @Singleton
    abstract fun bindAIStorage(impl: AIStorageImpl): AIStorage

    @Binds
    @Singleton
    abstract fun bindAIRepository(impl: AIRepositoryImpl): AIRepository

    @Binds
    @Singleton
    abstract fun bindWorkflowRepository(impl: WorkflowRepositoryImpl): WorkflowRepository

    @Binds
    @Singleton
    abstract fun bindContextManager(impl: ContextManagerImpl): ContextManager

    @Binds
    @Singleton
    abstract fun bindStateRecognitionEngine(impl: StateRecognitionEngineImpl): StateRecognitionEngine

    @Binds
    @Singleton
    abstract fun bindPredictionEngine(impl: PredictionEngineImpl): PredictionEngine

    @Binds
    @Singleton
    abstract fun bindReasoningEngine(impl: ReasoningEngineImpl): ReasoningEngine

    @Binds
    @Singleton
    abstract fun bindConfidenceEngine(impl: ConfidenceEngineImpl): ConfidenceEngine

    @Binds
    @Singleton
    abstract fun bindDecisionEngine(impl: DecisionEngineImpl): DecisionEngine

    @Binds
    @Singleton
    abstract fun bindAdaptiveRecoveryEngine(impl: AdaptiveRecoveryEngineImpl): AdaptiveRecoveryEngine

    @Binds
    @Singleton
    abstract fun bindMemoryEngine(impl: MemoryEngineImpl): MemoryEngine

    @Binds
    @Singleton
    abstract fun bindAutomationExecutor(impl: NoOpAutomationExecutor): AutomationExecutor

    @Binds
    @Singleton
    abstract fun bindAIDecisionManager(impl: AIDecisionManagerImpl): AIDecisionManager
}
