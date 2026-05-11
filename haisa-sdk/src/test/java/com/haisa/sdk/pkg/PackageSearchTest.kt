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

    private fun search(query: String, packages: List<PackageInfo>): PackageSearchResult {
        val q = query.lowercase()
        val matches = packages.filter { pkg ->
            pkg.pkgId.lowercase().contains(q) ||
            pkg.name.lowercase().contains(q) ||
            pkg.description.lowercase().contains(q)
        }
        return PackageSearchResult(query, matches, packages.size)
    }

    @Test
    fun search_byPkgId_returnsMatch() {
        val result = search("python", testPackages)
        assertTrue(result.matches.any { it.pkgId == "env-python" })
    }

    @Test
    fun search_byName_returnsMatch() {
        val result = search("Java", testPackages)
        assertTrue(result.matches.any { it.pkgId == "env-jdk" })
    }

    @Test
    fun search_byDescription_returnsMatch() {
        val result = search("toolchain", testPackages)
        assertTrue(result.matches.any { it.pkgId == "env-cc" })
        assertTrue(result.matches.any { it.pkgId == "env-rust" })
        assertTrue(result.matches.any { it.pkgId == "env-go" })
    }

    @Test
    fun search_noMatch_returnsEmpty() {
        val result = search("nonexistent_xyz", testPackages)
        assertTrue(result.matches.isEmpty())
    }

    @Test
    fun search_caseInsensitive() {
        val resultLower = search("python", testPackages)
        val resultUpper = search("PYTHON", testPackages)
        assertEquals(resultLower.matches.size, resultUpper.matches.size)
    }

    @Test
    fun search_partialMatch() {
        val result = search("env-", testPackages)
        assertEquals(8, result.matches.size)
    }

    @Test
    fun searchResult_totalAvailable() {
        val result = search("python", testPackages)
        assertEquals(8, result.totalAvailable)
        assertEquals(1, result.matches.size)
    }
}
