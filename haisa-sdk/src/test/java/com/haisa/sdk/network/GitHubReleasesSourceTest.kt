package com.haisa.sdk.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleasesSourceTest {

    @Test
    fun repoModuleEntry_hasCorrectFields() {
        val entry = RepoModuleEntry(
            id = "env-jdk",
            name = "Java JDK",
            description = "OpenJDK runtime",
            latest = "17.0.8",
            sizeMb = 180,
            dependencies = listOf("env-base")
        )

        assertEquals("env-jdk", entry.id)
        assertEquals("Java JDK", entry.name)
        assertEquals("17.0.8", entry.latest)
        assertEquals(180, entry.sizeMb)
        assertTrue(entry.dependencies.contains("env-base"))
    }

    @Test
    fun gitHubAsset_hasDownloadUrl() {
        val asset = GitHubAsset(
            id = 1L,
            name = "env-jdk-17.0.8-aarch64.tar.xz",
            size = 180 * 1024 * 1024L,
            downloadUrl = "https://github.com/example/releases/download/env-jdk-17/env-jdk-17.0.8-aarch64.tar.xz",
            contentType = "application/x-xz"
        )

        assertTrue(asset.name.endsWith(".tar.xz"))
        assertTrue(asset.downloadUrl.startsWith("https://"))
        assertTrue(asset.size > 0)
    }

    @Test
    fun repoIndex_hasBaseUrl() {
        val index = RepoIndex(
            version = "1.0.0",
            lastUpdated = "2026-05-09T00:00:00Z",
            baseUrl = "https://github.com/example/releases/download",
            modules = emptyList()
        )

        assertEquals("1.0.0", index.version)
        assertTrue(index.baseUrl.startsWith("https://"))
    }
}