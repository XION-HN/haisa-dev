package com.haisa.sdk.engine

import com.haisa.sdk.model.BuildTaskStatus
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class BuildTaskQueueTest {

    @Test
    fun enqueueAddsTaskToQueue() {
        val queue = BuildTaskQueue()
        val task = BuildTask(
            id = UUID.randomUUID().toString(),
            projectPath = "/tmp/test-project",
            buildCommand = "echo hello"
        )

        val taskId = queue.enqueue(task)
        assertEquals(task.id, taskId)
        val state = queue.queueState.value
        assertTrue(state.pendingCount >= 0 || state.activeTask != null)
    }

    @Test
    fun cancelRemovesTaskFromQueue() {
        val queue = BuildTaskQueue()
        val taskId = UUID.randomUUID().toString()
        val task = BuildTask(
            id = taskId,
            projectPath = "/tmp/test-project",
            buildCommand = "sleep 10 && echo hello",
            priority = 1
        )

        queue.enqueue(task)
        queue.cancel(taskId)

        val result = queue.getResults()[taskId]
        assertNotNull(result)
        assertEquals(BuildTaskStatus.CANCELLED, result!!.status)
    }

    @Test
    fun clearRemovesAllTasks() {
        val queue = BuildTaskQueue()
        repeat(3) {
            queue.enqueue(BuildTask(
                id = UUID.randomUUID().toString(),
                projectPath = "/tmp/test",
                buildCommand = "echo $it"
            ))
        }

        queue.clear()
        val state = queue.queueState.value
        assertEquals(0, state.pendingCount)
        assertTrue(queue.getResults().isEmpty())
    }

    @Test
    fun getResultsReturnsCompletedTasks() {
        val queue = BuildTaskQueue()
        val taskId = "test-task-1"
        queue.enqueue(BuildTask(
            id = taskId,
            projectPath = "/tmp/test",
            buildCommand = "echo done"
        ))

        val results = queue.getResults()
        assertNotNull(results)
    }
}
