package com.haisa.sdk.engine

import com.haisa.sdk.model.BuildStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BuildEngineTest {

    @Test
    fun `execute echoes command output`() = runBlocking {
        val tempDir = createTempDir()
        val buildEngine = BuildEngine()
        val results = buildEngine.execute(tempDir.absolutePath, "echo hello_world").toList()

        val messages = results.map { it.message }
        assertTrue(messages.any { it.contains("hello_world") })
        assertEquals(BuildStatus.FINISHED, results.last().status)
        tempDir.deleteRecursively()
    }

    @Test
    fun `execute reports failure on non-zero exit`() = runBlocking {
        val tempDir = createTempDir()
        val buildEngine = BuildEngine()
        val results = buildEngine.execute(tempDir.absolutePath, "exit 1").toList()

        assertEquals(BuildStatus.FAILED, results.last().status)
        assertTrue(results.last().isError)
        tempDir.deleteRecursively()
    }

    @Test
    fun `execute reports failure for missing directory`() = runBlocking {
        val buildEngine = BuildEngine()
        val results = buildEngine.execute("/nonexistent/path/12345", "echo test").toList()

        assertEquals(BuildStatus.FAILED, results.last().status)
        assertTrue(results.last().isError)
    }

    @Test
    fun `execute captures stderr`() = runBlocking {
        val tempDir = createTempDir()
        val buildEngine = BuildEngine()
        val results = buildEngine.execute(tempDir.absolutePath, "echo error_msg >&2").toList()

        val messages = results.map { it.message }
        assertTrue(messages.any { it.contains("error_msg") })
        tempDir.deleteRecursively()
    }

    private fun createTempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "build_test_${System.currentTimeMillis()}")
        dir.mkdirs()
        return dir
    }
}
