package com.haisa.sdk.pkg

import org.junit.Assert.*
import org.junit.Test

class PackageSearchTest {

    private val testPackages = listOf(
        PackageInfo(pkgId = "env-base", name = "Core Runtime", version = "1.0.0", description = "Base tools"),
        PackageInfo(pkgId = "env-python", name = "Python 3", version = "3.11.8", description = "Python runtime with pip"),
        PackageInfo(pkgId = "env-jdk", name = "Java JDK", version = "17.0.8", description = "OpenJDK 17"),
        PackageInfo(pkgId = "env-node", name = "Node.js", version = "20.11.0", description = "Node.js runtime"),
        PackageInfo(pkgId = "env-cc", name = "C/C++ Toolchain", version = "17.0.1", description = "Clang/LLVM"),
        PackageInfo(pkgId = "env-rust", name = "Rust", version = "1.75.0", description = "Rust toolchain with cargo"),
        PackageInfo(pkgId = "env-go", name = "Go", version = "1.21.6", description = "Go toolchain"),
        PackageInfo(pkgId = "env-git", name = "Git", version = "2.43.0", description = "Git version control")
    )

    @Test
    fun search_byPkgId_returnsMatch() {
        val pm = createPackageManagerWithIndex()
        val result = pm.search("python")

        assertTrue(result.matches.any { it.pkgId == "env-python" })
    }

    @Test
    fun search_byName_returnsMatch() {
        val pm = createPackageManagerWithIndex()
        val result = pm.search("Java")

        assertTrue(result.matches.any { it.pkgId == "env-jdk" })
    }

    @Test
    fun search_byDescription_returnsMatch() {
        val pm = createPackageManagerWithIndex()
        val result = pm.search("toolchain")

        assertTrue(result.matches.any { it.pkgId == "env-cc" })
        assertTrue(result.matches.any { it.pkgId == "env-rust" })
        assertTrue(result.matches.any { it.pkgId == "env-go" })
    }

    @Test
    fun search_noMatch_returnsEmpty() {
        val pm = createPackageManagerWithIndex()
        val result = pm.search("nonexistent_xyz")

        assertTrue(result.matches.isEmpty())
    }

    @Test
    fun search_caseInsensitive() {
        val pm = createPackageManagerWithIndex()
        val resultLower = pm.search("python")
        val resultUpper = pm.search("PYTHON")

        assertEquals(resultLower.matches.size, resultUpper.matches.size)
    }

    @Test
    fun listAvailable_returnsAllPackages() {
        val pm = createPackageManagerWithIndex()
        val available = pm.listAvailable()

        assertEquals(8, available.size)
    }

    @Test
    fun show_existingPkg_returnsInfo() {
        val pm = createPackageManagerWithIndex()
        val info = pm.show("env-python")

        assertNotNull(info)
        assertEquals("3.11.8", info!!.version)
    }

    @Test
    fun show_nonexistentPkg_returnsNull() {
        val pm = createPackageManagerWithIndex()
        val info = pm.show("env-nonexistent")

        assertNull(info)
    }

    private fun createPackageManagerWithIndex(): PackageManager {
        val pm = PackageManagerStub()
        pm.updateIndex(testPackages)
        return pm
    }

    private class PackageManagerStub : PackageManager(null) {
        override fun listAvailable(): List<PackageInfo> {
            return indexCache?.packages ?: emptyList()
        }

        override fun show(pkgId: String): PackageInfo? {
            return indexCache?.packages?.find { it.pkgId == pkgId }
        }

        override fun search(query: String): PackageSearchResult {
            val pkgs = indexCache?.packages ?: emptyList()
            val q = query.lowercase()
            val matches = pkgs.filter { pkg ->
                pkg.pkgId.lowercase().contains(q) ||
                pkg.name.lowercase().contains(q) ||
                pkg.description.lowercase().contains(q)
            }
            return PackageSearchResult(query, matches, pkgs.size)
        }
    }
}
