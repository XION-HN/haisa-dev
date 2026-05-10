package com.haisa.sdk.repository

import com.haisa.sdk.data.LocalDataSource
import com.haisa.sdk.model.InstalledModule
import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.network.GitHubApiService
import com.haisa.sdk.network.GitHubReleasesSource
import com.haisa.sdk.network.RepoIndex
import com.haisa.sdk.network.RepoModuleEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response

@RunWith(MockitoJUnitRunner::class)
class ModuleRepositoryTest {

    @Mock
    private lateinit var localDataSource: LocalDataSource

    @Mock
    private lateinit var apiService: GitHubApiService

    private lateinit var remoteSource: GitHubReleasesSource
    private lateinit var repository: ModuleRepositoryImpl

    @Before
    fun setup() {
        `when`(localDataSource.getInstalledModules()).thenReturn(emptyList())
        `when`(localDataSource.isModuleInstalled(anyString())).thenReturn(false)
        `when`(localDataSource.getActiveVersion(anyString())).thenReturn(null)
        remoteSource = GitHubReleasesSource(apiService)
        repository = ModuleRepositoryImpl(localDataSource, remoteSource)
    }

    @Test
    fun `getInstalledModules returns empty when none installed`() {
        val result = repository.getInstalledModules()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `isModuleInstalled returns false for unknown module`() {
        assertFalse(repository.isModuleInstalled("env-unknown"))
    }

    @Test
    fun `isEnvironmentReady returns false when not installed`() {
        assertFalse(repository.isEnvironmentReady("env-python"))
    }

    @Test
    fun `getModuleEnvironment returns empty map when not installed`() {
        val env = repository.getModuleEnvironment("env-unknown")
        assertTrue(env.isEmpty())
    }

    @Test
    fun `uninstallModule delegates to localDataSource`() {
        `when`(localDataSource.getActiveVersion("env-python")).thenReturn("3.11.8")
        val result = repository.uninstallModule("env-python", "3.11.8")
        verify(localDataSource).removeInstalledModule("env-python", "3.11.8")
        assertTrue(result)
    }

    @Test
    fun `switchModuleVersion returns false when version not installed`() {
        `when`(localDataSource.getModuleInstallPath("env-python", "3.12.0"))
            .thenReturn(java.io.File("/nonexistent/modules/env-python/3.12.0"))
        assertFalse(repository.switchModuleVersion("env-python", "3.12.0"))
    }

    @Test
    fun `fetchAvailableModules merges installed status`() = runBlocking {
        val repoIndex = RepoIndex(
            version = "1.0.0",
            lastUpdated = "2026-05-10T00:00:00Z",
            baseUrl = "https://example.com",
            modules = listOf(
                RepoModuleEntry("env-python", "Python 3", "Python runtime", "3.11.8", 40, listOf("env-base"))
            )
        )
        `when`(apiService.getRepoIndex(anyString())).thenReturn(Response.success(repoIndex))
        `when`(localDataSource.getInstalledModules()).thenReturn(listOf(
            InstalledModule("env-python", "3.11.8", System.currentTimeMillis(), 40 * 1024 * 1024, "/data/modules/env-python/3.11.8")
        ))

        val result = repository.fetchAvailableModules()
        assertTrue(result.isSuccess)
        val modules = result.getOrDefault(emptyList())
        val pythonModule = modules.find { it.id == "env-python" }
        assertNotNull(pythonModule)
        assertTrue(pythonModule!!.isInstalled)
        assertEquals("3.11.8", pythonModule.installedVersion)
    }
}
