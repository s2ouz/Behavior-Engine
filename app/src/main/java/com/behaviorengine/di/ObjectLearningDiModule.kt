package com.behaviorengine.di

import com.behaviorengine.core.data.objectlearning.CropManagerImpl
import com.behaviorengine.core.data.objectlearning.FeatureExtractionManagerImpl
import com.behaviorengine.core.data.objectlearning.FrameSelectionManagerImpl
import com.behaviorengine.core.data.objectlearning.ObjectLearningManagerImpl
import com.behaviorengine.core.data.objectlearning.ObjectLearningStorageImpl
import com.behaviorengine.core.data.objectlearning.ObjectRepositoryImpl
import com.behaviorengine.core.data.objectlearning.ObjectTemplateManagerImpl
import com.behaviorengine.core.domain.objectlearning.CropManager
import com.behaviorengine.core.domain.objectlearning.FeatureExtractionManager
import com.behaviorengine.core.domain.objectlearning.FrameSelectionManager
import com.behaviorengine.core.domain.objectlearning.OCRManager
import com.behaviorengine.core.domain.objectlearning.ObjectDetectionManager
import com.behaviorengine.core.domain.objectlearning.ObjectLearningManager
import com.behaviorengine.core.domain.objectlearning.ObjectLearningStorage
import com.behaviorengine.core.domain.objectlearning.ObjectRepository
import com.behaviorengine.core.domain.objectlearning.ObjectTemplateManager
import com.behaviorengine.objectlearning.OCRManagerImpl
import com.behaviorengine.objectlearning.ObjectDetectionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds every Smart Object Learning Engine contract to its real implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ObjectLearningDiModule {

    @Binds
    @Singleton
    abstract fun bindFrameSelectionManager(impl: FrameSelectionManagerImpl): FrameSelectionManager

    @Binds
    @Singleton
    abstract fun bindObjectDetectionManager(impl: ObjectDetectionManagerImpl): ObjectDetectionManager

    @Binds
    @Singleton
    abstract fun bindCropManager(impl: CropManagerImpl): CropManager

    @Binds
    @Singleton
    abstract fun bindFeatureExtractionManager(impl: FeatureExtractionManagerImpl): FeatureExtractionManager

    @Binds
    @Singleton
    abstract fun bindOCRManager(impl: OCRManagerImpl): OCRManager

    @Binds
    @Singleton
    abstract fun bindObjectTemplateManager(impl: ObjectTemplateManagerImpl): ObjectTemplateManager

    @Binds
    @Singleton
    abstract fun bindObjectLearningStorage(impl: ObjectLearningStorageImpl): ObjectLearningStorage

    @Binds
    @Singleton
    abstract fun bindObjectRepository(impl: ObjectRepositoryImpl): ObjectRepository

    @Binds
    @Singleton
    abstract fun bindObjectLearningManager(impl: ObjectLearningManagerImpl): ObjectLearningManager
}
