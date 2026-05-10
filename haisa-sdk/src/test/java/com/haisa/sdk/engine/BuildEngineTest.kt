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

    @Test
    fun executeEchoesCommandOutput() {
        runBlocking {
            val dir = createTempDir()
            val buildEngine = BuildEngine()
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
            val buildEngine = BuildEngine()
            val results = buildEngine.execute(dir.absolutePath, "exit 1").toList()

            assertEquals(BuildStatus.FAILED, results.last().status)
            assertTrue(results.last().isError)
        }
    }

    @Test
    fun executeReportsFailureForMissingDirectory() {
        runBlocking {
            val buildEngine = BuildEngine()
            val results = buildEngine.execute("/nonexistent/path/12345", "echo test").toList()

            assertEquals(BuildStatus.FAILED, results.last().status)
            assertTrue(results.last().isError)
        }
    }

    @Test
    fun executeCapturesStderr() {
        runBlocking {
            val dir = createTempDir()
            val buildEngine = BuildEngine()
            val results = buildEngine.execute(dir.absolutePath, "echo error_msg >&2").toList()

            val messages = results.map { it.message }
            assertTrue(messages.any { it.contains("error_msg") })
        }
    }

    private fun createTempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "build_test_${System.currentTimeMillis()}")
        dir.mkdirs()
        tempDir = dir
        return dir
    }
}
