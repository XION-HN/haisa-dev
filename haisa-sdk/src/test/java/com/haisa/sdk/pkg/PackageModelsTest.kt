package com.haisa.sdk.pkg

import org.junit.Assert.*
import org.junit.Test

class PackageModelsTest {

    @Test
    fun packageInfo_defaultValues() {
        val pkg = PackageInfo(pkgId = "env-test", name = "Test", version = "1.0.0")
        assertEquals("aarch64", pkg.arch)
        assertEquals("", pkg.description)
        assertEquals("dev", pkg.section)
        assertEquals("optional", pkg.priority)
        assertTrue(pkg.dependencies.isEmpty())
        assertTrue(pkg.provides.isEmpty())
        assertTrue(pkg.conflicts.isEmpty())
        assertTrue(pkg.entryBinaries.isEmpty())
        assertTrue(pkg.envVars.isEmpty())
        assertEquals(0L, pkg.installSizeKb)
        assertEquals("", pkg.sha256)
        assertEquals("", pkg.downloadUrl)
        assertEquals("", pkg.license)
        assertTrue(pkg.ideIntegrations.isEmpty())
    }

    @Test
    fun packageInfo_withFullData() {
        val pkg = PackageInfo(
            pkgId = "env-python",
            name = "Python 3",
            version = "3.11.8",
            arch = "aarch64",
            description = "Python runtime",
            dependencies = listOf("env-base"),
            provides = listOf("python3", "pip3"),
            entryBinaries = mapOf("python3" to "bin/python3"),
            envVars = mapOf("PYTHON_HOME" to "{{install_dir}}"),
            installSizeKb = 40960,
            license = "PSF-2.0",
            ideIntegrations = listOf(
                PackageInfo.IdeIntegration("haisa-ide", listOf("python-run"))
            )
        )
        assertEquals("env-python", pkg.pkgId)
        assertEquals(listOf("env-base"), pkg.dependencies)
        assertEquals(mapOf("python3" to "bin/python3"), pkg.entryBinaries)
        assertEquals(1, pkg.ideIntegrations.size)
        assertEquals("haisa-ide", pkg.ideIntegrations.first().ideType)
    }

    @Test
    fun installedPackage_defaultStatus() {
        val pkg = InstalledPackage(
            pkgId = "env-base",
            version = "1.0.0",
            installPath = "/data/data/com.example/files/packages/../modules/env-base/1.0.0"
        )
        assertEquals(PackageStatus.INSTALLED, pkg.status)
        assertFalse(pkg.autoInstalled)
    }

    @Test
    fun packageOperation_types() {
        assertEquals(5, OperationType.values().size)
        assertNotNull(OperationType.valueOf("INSTALL"))
        assertNotNull(OperationType.valueOf("UPGRADE"))
        assertNotNull(OperationType.valueOf("DOWNGRADE"))
        assertNotNull(OperationType.valueOf("REMOVE"))
        assertNotNull(OperationType.valueOf("SKIP"))
    }

    @Test
    fun dependencyGraph_holdsData() {
        val nodes = mapOf(
            "env-base" to PackageInfo(pkgId = "env-base", name = "Base", version = "1.0"),
            "env-python" to PackageInfo(pkgId = "env-python", name = "Python", version = "3.11")
        )
        val edges = mapOf(
            "env-python" to listOf("env-base"),
            "env-base" to emptyList<String>()
        )
        val graph = DependencyGraph(nodes, edges, listOf("env-base", "env-python"))

        assertEquals(2, graph.nodes.size)
        assertEquals(2, graph.edges.size)
        assertEquals(listOf("env-base", "env-python"), graph.installOrder)
    }

    @Test
    fun packageSearchResult_matchesQuery() {
        val matches = listOf(
            PackageInfo(pkgId = "env-python", name = "Python", version = "3.11")
        )
        val result = PackageSearchResult("python", matches, 8)
        assertEquals("python", result.query)
        assertEquals(1, result.matches.size)
        assertEquals(8, result.totalAvailable)
    }
}
