package com.haisa.sdk.engine

import com.haisa.sdk.model.BuildStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class BuildEngineTest {

    private var tempDir: File? = null
    private val allowedPrefixes = listOf("/data/data/", "/sdcard/", "/storage/", "/tmp/", System.getProperty("java.io.tmpdir"))

    @Before
    fun setup() {
        BuildEngine.logError = { tag, msg, e ->
            val log = if (e != null) "$tag: $msg - ${e.message}" else "$tag: $msg"
            System.err.println("[TEST] $log")
        }
    }

    @After
    fun teardown() {
        tempDir?.deleteRecursively()
    }

    private fun createBuildEngine() = BuildEngine(allowedPathPrefixes = allowedPrefixes)

    @Test
    fun executeEchoesCommandOutput() {
        runBlocking {
            val dir = createTempDir()
            val buildEngine = createBuildEngine()
            val results = buildEngine.execute(dir.absolutePath, "echo hello_world").toList()

            val messages = results.map { it.message }
            assertTrue(messages.any { it.contains("hello_world") })
            assertEquals(BuildStatus.FINISHED, results.last().status)
        }
    }

    @Test
    fun executeReportsFailureOnNonZeroExit() {
        runBlocking {
            val dir = createTempDir()
            val buildEngine = createBuildEngine()
            val results = buildEngine.execute(dir.absolutePath, "exit 1").toList()

            assertEquals(BuildStatus.FAILED, results.last().status)
            assertTrue(results.last().isError)
        }
    }

    @Test
    fun executeReportsFailureForMissingDirectory() {
        runBlocking {
            val buildEngine = createBuildEngine()
            val results = buildEngine.execute("/nonexistent/path/12345", "echo test").toList()

            assertEquals(BuildStatus.FAILED, results.last().status)
            assertTrue(results.last().isError)
        }
    }

    @Test
    fun executeCapturesStderr() {
        runBlocking {
            val dir = createTempDir()
            val buildEngine = createBuildEngine()
            val results = buildEngine.execute(dir.absolutePath, "echo error_msg >&2").toList()

            val messages = results.map { it.message }
            assertTrue(messages.any { it.contains("error_msg") })
        }
    }

    @Test
    fun sanitizeCommand_removesDangerousPatterns() {
        val buildEngine = createBuildEngine()
        val result = buildEngine.sanitizeCommand("echo hello; rm -rf /")
        assertFalse(result.contains("rm "))
        assertFalse(result.contains(";"))
    }

    @Test
    fun sanitizeCommand_returnsFallbackForBlankResult() {
        val buildEngine = createBuildEngine()
        val result = buildEngine.sanitizeCommand(" ")
        assertEquals("echo 'Empty or invalid command'", result)
    }

    @Test
    fun setTimeout_setsValidTimeout() {
        val buildEngine = createBuildEngine()
        buildEngine.setTimeout(5)
        buildEngine.setTimeout(0)
    }

    @Test
    fun sanitizeCommand_returnsFallbackForBlankResult() {
        val buildEngine = BuildEngine()
        val result = buildEngine.sanitizeCommand("  ")
        assertEquals("echo 'Empty or invalid command'", result)
    }

    @Test
    fun setTimeout_setsValidTimeout() {
        val buildEngine = BuildEngine()
        buildEngine.setTimeout(5)
        buildEngine.setTimeout(0)
    }

    private fun createTempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "build_test_${System.currentTimeMillis()}")
        dir.mkdirs()
        tempDir = dir
        return dir
    }
}
