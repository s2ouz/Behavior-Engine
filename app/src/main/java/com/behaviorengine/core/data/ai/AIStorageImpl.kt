package com.behaviorengine.core.data.ai

import android.content.Context
import com.behaviorengine.core.domain.ai.AIRuntimeStatistics
import com.behaviorengine.core.domain.ai.AIStorage
import com.behaviorengine.core.domain.ai.Decision
import com.behaviorengine.core.domain.ai.FailedAttempt
import com.behaviorengine.core.domain.ai.Prediction
import com.behaviorengine.core.domain.ai.SuccessfulRoute
import com.behaviorengine.core.domain.ai.UIVariation
import com.behaviorengine.core.domain.ai.Workflow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [AIStorage] — its own `AI/` root, sibling to `Matching/`/`Teaching/`, same one-file-per-record JSON convention as the rest of the app. */
@Singleton
class AIStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AIStorage {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val rootDir: File by lazy { File(context.getExternalFilesDir(null), "AI").apply { mkdirs() } }
    private val decisionsDir: File by lazy { File(rootDir, "Decisions").apply { mkdirs() } }
    private val predictionsDir: File by lazy { File(rootDir, "Predictions").apply { mkdirs() } }
    private val runtimeDir: File by lazy { File(rootDir, "Runtime").apply { mkdirs() } }
    private val routesDir: File by lazy { File(rootDir, "Routes").apply { mkdirs() } }
    private val failedAttemptsDir: File by lazy { File(rootDir, "FailedAttempts").apply { mkdirs() } }
    private val uiVariationsDir: File by lazy { File(rootDir, "UIVariations").apply { mkdirs() } }
    private val workflowsDir: File by lazy { File(rootDir, "Workflows").apply { mkdirs() } }

    override suspend fun writeDecision(decision: Decision) = write(decisionsDir, decision.decisionId, Decision.serializer(), decision)

    override suspend fun listDecisions(): List<Decision> = listAll(decisionsDir, Decision.serializer())

    override suspend fun writePrediction(prediction: Prediction) =
        write(predictionsDir, UUID.randomUUID().toString(), Prediction.serializer(), prediction)

    override suspend fun writeRuntimeStatistics(statistics: AIRuntimeStatistics) =
        write(runtimeDir, statistics.id, AIRuntimeStatistics.serializer(), statistics)

    override suspend fun listRuntimeStatistics(): List<AIRuntimeStatistics> = listAll(runtimeDir, AIRuntimeStatistics.serializer())

    override suspend fun writeSuccessfulRoute(route: SuccessfulRoute) = write(routesDir, route.id, SuccessfulRoute.serializer(), route)

    override suspend fun listSuccessfulRoutes(): List<SuccessfulRoute> = listAll(routesDir, SuccessfulRoute.serializer())

    override suspend fun writeFailedAttempt(attempt: FailedAttempt) =
        write(failedAttemptsDir, attempt.id, FailedAttempt.serializer(), attempt)

    override suspend fun listFailedAttempts(): List<FailedAttempt> = listAll(failedAttemptsDir, FailedAttempt.serializer())

    override suspend fun writeUiVariation(variation: UIVariation) =
        write(uiVariationsDir, variation.id, UIVariation.serializer(), variation)

    override suspend fun listUiVariations(): List<UIVariation> = listAll(uiVariationsDir, UIVariation.serializer())

    override suspend fun writeWorkflow(workflow: Workflow) = write(workflowsDir, workflow.id, Workflow.serializer(), workflow)

    override suspend fun readWorkflow(id: String): Workflow? = withContext(Dispatchers.IO) {
        val file = File(workflowsDir, "$id.json")
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString(Workflow.serializer(), file.readText()) }.getOrNull()
    }

    override suspend fun listWorkflowIds(): List<String> = withContext(Dispatchers.IO) {
        workflowsDir.listFiles { file -> file.extension == "json" }?.map { it.nameWithoutExtension } ?: emptyList()
    }

    private suspend fun <T> write(dir: File, id: String, serializer: kotlinx.serialization.KSerializer<T>, value: T) {
        withContext(Dispatchers.IO) {
            File(dir, "$id.json").writeText(json.encodeToString(serializer, value))
        }
    }

    private suspend fun <T> listAll(dir: File, serializer: kotlinx.serialization.KSerializer<T>): List<T> = withContext(Dispatchers.IO) {
        dir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { runCatching { json.decodeFromString(serializer, it.readText()) }.getOrNull() }
            ?: emptyList()
    }
}
