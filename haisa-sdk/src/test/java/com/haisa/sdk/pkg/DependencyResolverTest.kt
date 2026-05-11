package com.haisa.sdk.pkg

import org.junit.Assert.*
import org.junit.Test

class DependencyResolverTest {

    @Test
    fun resolve_simpleDependency_returnsBaseFirst() {
        val available = mapOf(
            "env-python" to PackageInfo(
                pkgId = "env-python",
                name = "Python",
                version = "3.11.8",
                dependencies = listOf("env-base")
            ),
            "env-base" to PackageInfo(
                pkgId = "env-base",
                name = "Core",
                version = "1.0.0",
                dependencies = emptyList()
            )
        )
        val installed = emptyMap<String, InstalledPackage>()

        val result = DependencyResolver.resolve("env-python", available, installed)

        assertTrue(result.isSuccess)
        val graph = result.getOrThrow()
        assertEquals(listOf("env-base", "env-python"), graph.installOrder)
    }

    @Test
    fun resolve_alreadyInstalled_skipsInstalled() {
        val available = mapOf(
            "env-python" to PackageInfo(
                pkgId = "env-python",
                name = "Python",
                version = "3.11.8",
                dependencies = listOf("env-base")
            ),
            "env-base" to PackageInfo(
                pkgId = "env-base",
                name = "Core",
                version = "1.0.0",
                dependencies = emptyList()
            )
        )
        val installed = mapOf(
            "env-base" to InstalledPackage(
                pkgId = "env-base",
                version = "1.0.0",
                installPath = "/data/data/com.example/files/packages/../modules/env-base/1.0.0"
            )
        )

        val result = DependencyResolver.resolve("env-python", available, installed)

        assertTrue(result.isSuccess)
        val graph = result.getOrThrow()
        assertFalse(graph.installOrder.contains("env-base"))
        assertTrue(graph.installOrder.contains("env-python"))
    }

    @Test
    fun resolve_missingDependency_returnsFailure() {
        val available = mapOf(
            "env-python" to PackageInfo(
                pkgId = "env-python",
                name = "Python",
                version = "3.11.8",
                dependencies = listOf("env-nonexistent")
            )
        )
        val installed = emptyMap<String, InstalledPackage>()

        val result = DependencyResolver.resolve("env-python", available, installed)

        assertTrue(result.isFailure)
    }

    @Test
    fun resolve_cycleDetected_returnsFailure() {
        val available = mapOf(
            "a" to PackageInfo(pkgId = "a", name = "A", version = "1.0", dependencies = listOf("b")),
            "b" to PackageInfo(pkgId = "b", name = "B", version = "1.0", dependencies = listOf("a"))
        )
        val installed = emptyMap<String, InstalledPackage>()

        val result = DependencyResolver.resolve("a", available, installed)

        assertTrue(result.isFailure)
    }

    @Test
    fun resolve_noDependencies_returnsSelf() {
        val available = mapOf(
            "env-base" to PackageInfo(
                pkgId = "env-base",
                name = "Core",
                version = "1.0.0",
                dependencies = emptyList()
            )
        )
        val installed = emptyMap<String, InstalledPackage>()

        val result = DependencyResolver.resolve("env-base", available, installed)

        assertTrue(result.isSuccess)
        val graph = result.getOrThrow()
        assertEquals(listOf("env-base"), graph.installOrder)
    }

    @Test
    fun resolve_diamondDependency_deduplicates() {
        val available = mapOf(
            "app" to PackageInfo(pkgId = "app", name = "App", version = "1.0", dependencies = listOf("lib-a", "lib-b")),
            "lib-a" to PackageInfo(pkgId = "lib-a", name = "LibA", version = "1.0", dependencies = listOf("base")),
            "lib-b" to PackageInfo(pkgId = "lib-b", name = "LibB", version = "1.0", dependencies = listOf("base")),
            "base" to PackageInfo(pkgId = "base", name = "Base", version = "1.0", dependencies = emptyList())
        )
        val installed = emptyMap<String, InstalledPackage>()

        val result = DependencyResolver.resolve("app", available, installed)

        assertTrue(result.isSuccess)
        val graph = result.getOrThrow()
        assertEquals(4, graph.installOrder.size)
        assertEquals(4, graph.installOrder.toSet().size)
        assertTrue(graph.installOrder.indexOf("base") < graph.installOrder.indexOf("lib-a"))
        assertTrue(graph.installOrder.indexOf("base") < graph.installOrder.indexOf("lib-b"))
        assertEquals("app", graph.installOrder.last())
    }

    @Test
    fun findOrphans_identifiesAutoInstalledOnly() {
        val installed = mapOf(
            "env-base" to InstalledPackage(pkgId = "env-base", version = "1.0", installPath = "/tmp/base", dependencies = emptyList()),
            "env-python" to InstalledPackage(pkgId = "env-python", version = "3.11", installPath = "/tmp/python", dependencies = listOf("env-base"))
        )
        val autoFlags = mapOf(
            "env-base" to true,
            "env-python" to false
        )

        val orphans = DependencyResolver.findOrphans(installed, autoFlags)

        assertFalse(orphans.contains("env-base"))
        assertFalse(orphans.contains("env-python"))
    }

    @Test
    fun findOrphans_keepsRequiredDeps() {
        val installed = mapOf(
            "env-base" to InstalledPackage(pkgId = "env-base", version = "1.0", installPath = "/tmp/base", dependencies = emptyList()),
            "env-python" to InstalledPackage(pkgId = "env-python", version = "3.11", installPath = "/tmp/python", dependencies = listOf("env-base"))
        )
        val autoFlags = mapOf(
            "env-base" to true,
            "env-python" to false
        )

        val orphans = DependencyResolver.findOrphans(installed, autoFlags)

        assertFalse(orphans.contains("env-base"))
    }

    @Test
    fun findOrphans_noOrphans_returnsEmpty() {
        val installed = mapOf(
            "env-base" to InstalledPackage(pkgId = "env-base", version = "1.0", installPath = "/tmp/base")
        )
        val autoFlags = mapOf("env-base" to false)

        val orphans = DependencyResolver.findOrphans(installed, autoFlags)

        assertTrue(orphans.isEmpty())
    }
}
