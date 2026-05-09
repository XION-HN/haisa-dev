package com.haisa.sdk.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModuleExtractorTest {

    @Test
    fun readManifest_validJson_returnsManifest() {
        val dir = createTempModuleDir("test-module")
        File(dir, "manifest.json").writeText("""
            {
                "module_id": "test-module",
                "version": "1.0.0",
                "arch": "aarch64",
                "description": "Test module"
            }
        """.trimIndent())

        val manifest = ModuleExtractor.readManifest(dir)

        assertNotNull(manifest)
        assertEquals("test-module", manifest!!.moduleId)
        assertEquals("1.0.0", manifest.version)

        dir.deleteRecursively()
    }

    @Test
    fun readManifest_missingFile_returnsNull() {
        val dir = createTempModuleDir("no-manifest")

        val manifest = ModuleExtractor.readManifest(dir)

        assertNull(manifest)

        dir.deleteRecursively()
    }

    @Test
    fun readManifest_invalidJson_returnsNull() {
        val dir = createTempModuleDir("bad-json")
        File(dir, "manifest.json").writeText("not valid json")

        val manifest = ModuleExtractor.readManifest(dir)

        assertNull(manifest)

        dir.deleteRecursively()
    }

    @Test
    fun validateModuleStructure_withManifest_isValid() {
        val dir = createTempModuleDir("valid-module")
        File(dir, "manifest.json").writeText("{}")

        val result = ModuleExtractor.validateModuleStructure(dir)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())

        dir.deleteRecursively()
    }

    @Test
    fun validateModuleStructure_withoutManifest_isInvalid() {
        val dir = createTempModuleDir("no-manifest")

        val result = ModuleExtractor.validateModuleStructure(dir)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("manifest.json") })

        dir.deleteRecursively()
    }

    @Test
    fun validateModuleStructure_nonExistentDir_isInvalid() {
        val dir = File("/tmp/nonexistent-module-dir-12345")

        val result = ModuleExtractor.validateModuleStructure(dir)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("does not exist") })
    }

    private fun createTempModuleDir(name: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "haisa_test_$name")
        dir.mkdirs()
        return dir
    }
}