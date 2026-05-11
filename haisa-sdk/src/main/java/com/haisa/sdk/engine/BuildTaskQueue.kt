package com.haisa.sdk.engine

import com.haisa.sdk.model.BuildProgress
import com.haisa.sdk.model.BuildStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue

data class BuildTask(
    val id: String,
    val projectPath: String,
    val buildCommand: String,
    val moduleIds: List<String> = emptyList(),
    val priority: Int = 0,
    @Volatile var status: BuildTaskStatus = BuildTaskStatus.QUEUED
)

enum class BuildTaskStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class BuildTaskResult(
    val taskId: String,
    val status: BuildTaskStatus,
    val output: List<String> = emptyList()
)

class BuildTaskQueue(
    private val buildEngine: BuildEngine = BuildEngine(),
    private val maxConcurrent: Int = 1
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val queue = ConcurrentLinkedQueue<BuildTask>()
    private val results = mutableMapOf<String, BuildTaskResult>()
    private val processingMutex = Mutex()
    @Volatile private var activeCount = 0

    private val _queueState = MutableStateFlow(BuildQueueState())
    val queueState: StateFlow<BuildQueueState> = _queueState.asStateFlow()

    data class BuildQueueState(
        val pendingCount: Int = 0,
        val activeTask: BuildTask? = null,
        val activeCount: Int = 0,
        val completedCount: Int = 0,
        val failedCount: Int = 0
    )

    fun enqueue(task: BuildTask): String {
        queue.add(task)
        updateState()
        processNext()
        return task.id
    }

    fun cancel(taskId: String) {
        queue.removeIf { it.id == taskId }
        results[taskId] = BuildTaskResult(taskId, BuildTaskStatus.CANCELLED)
        updateState()
    }

    fun getResults(): Map<String, BuildTaskResult> = synchronized(results) { results.toMap() }

    fun clear() {
        queue.clear()
        synchronized(results) { results.clear() }
        updateState()
    }

    private fun processNext() {
        scope.launch {
            processingMutex.withLock {
                if (activeCount >= maxConcurrent) return@launch
                val task = queue.poll() ?: return@launch
                activeCount++
                task.status = BuildTaskStatus.RUNNING
                updateState()

                launch {
                    try {
                        val outputLines = mutableListOf<String>()
                        buildEngine.execute(task.projectPath, task.buildCommand).collect { progress ->
                            outputLines.add(progress.message)
                            if (progress.status == BuildStatus.FAILED) {
                                task.status = BuildTaskStatus.FAILED
                                synchronized(results) {
                                    results[task.id] = BuildTaskResult(task.id, BuildTaskStatus.FAILED, outputLines)
                                }
                            } else if (progress.status == BuildStatus.FINISHED) {
                                task.status = BuildTaskStatus.COMPLETED
                                synchronized(results) {
                                    results[task.id] = BuildTaskResult(task.id, BuildTaskStatus.COMPLETED, outputLines)
                                }
                            }
                        }
                        if (task.status != BuildTaskStatus.FAILED) {
                            task.status = BuildTaskStatus.COMPLETED
                            synchronized(results) {
                                results[task.id] = BuildTaskResult(task.id, BuildTaskStatus.COMPLETED,
                                    results[task.id]?.output ?: emptyList())
                            }
                        }
                    } catch (e: Exception) {
                        task.status = BuildTaskStatus.FAILED
                        synchronized(results) {
                            results[task.id] = BuildTaskResult(task.id, BuildTaskStatus.FAILED, listOf(e.message ?: "Unknown error"))
                        }
                    } finally {
                        activeCount--
                        updateState()
                        processNext()
                    }
                }
            }
        }
    }

    private fun updateState() {
        val completedCount: Int
        val failedCount: Int
        synchronized(results) {
            completedCount = results.count { it.value.status == BuildTaskStatus.COMPLETED }
            failedCount = results.count { it.value.status == BuildTaskStatus.FAILED }
        }
        _queueState.value = BuildQueueState(
            pendingCount = queue.size,
            activeTask = _queueState.value.activeTask?.takeIf { it.status == BuildTaskStatus.RUNNING },
            activeCount = activeCount,
            completedCount = completedCount,
            failedCount = failedCount
        )
    }
}
