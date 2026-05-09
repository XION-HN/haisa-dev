package com.haisa.sdk.repository

import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.InstallStatus
import com.haisa.sdk.model.InstalledModule
import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.data.LocalDataSource
import com.haisa.sdk.network.GitHubReleasesSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

interface ModuleRepository {
    suspend fun fetchAvailableModules(): Result<List<ModuleInfo>>
    fun getInstalledModules(): List<InstalledModule>
    fun isModuleInstalled(moduleId: String): Boolean
    fun isEnvironmentReady(moduleId: String): Boolean
    fun installModule(moduleId: String, version: String? = null): Flow<InstallProgress>
    fun switchModuleVersion(moduleId: String, version: String): Boolean
    fun uninstallModule(moduleId: String, version: String? = null): Boolean
    fun getModuleEnvironment(moduleId: String): Map<String, String>
}

class ModuleRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val remoteSource: GitHubReleasesSource
) : ModuleRepository {

    private val cache = mutableMapOf<String, List<ModuleInfo>>()

    override suspend fun fetchAvailableModules(): Result<List<ModuleInfo>> {
        val remoteResult = remoteSource.fetchAvailableModules()
        if (remoteResult.isSuccess) {
            val remoteModules = remoteResult.getOrDefault(emptyList())
            val installed = localDataSource.getInstalledModules()
            val merged = remoteModules.map { rm ->
                val installedModule = installed.find { it.id == rm.id }
                rm.copy(
                    isInstalled = installedModule != null,
                    installedVersion = installedModule?.version
                )
            }
            cache["available"] = merged
            return Result.success(merged)
        }
        val cached = cache["available"]
        if (cached != null) return Result.success(cached)
        val installed = localDataSource.getInstalledModules()
        if (installed.isNotEmpty()) {
            return Result.success(installed.map { it.toModuleInfo() })
        }
        return remoteResult
    }

    override fun getInstalledModules(): List<InstalledModule> {
        return localDataSource.getInstalledModules()
    }

    override fun isModuleInstalled(moduleId: String): Boolean {
        return localDataSource.isModuleInstalled(moduleId)
    }

    override fun isEnvironmentReady(moduleId: String): Boolean {
        if (!isModuleInstalled(moduleId)) return false
        val version = localDataSource.getActiveVersion(moduleId) ?: return false
        val installDir = localDataSource.getModuleInstallPath(moduleId, version)
        return installDir.exists() && installDir.listFiles()?.isNotEmpty() == true
    }

    override fun installModule(moduleId: String, version: String?): Flow<InstallProgress> = flow {
        val targetVersion = version ?: "1.0.0"
        val installDir = localDataSource.getModuleInstallPath(moduleId, targetVersion)

        emit(InstallProgress(moduleId, InstallStatus.DOWNLOADING, 0))

        installDir.mkdirs()

        localDataSource.saveInstalledModule(
            InstalledModule(
                id = moduleId,
                version = targetVersion,
                installDate = System.currentTimeMillis(),
                sizeInBytes = calculateDirSize(installDir),
                installPath = installDir.absolutePath
            )
        )
        localDataSource.setActiveVersion(moduleId, targetVersion)

        emit(InstallProgress(moduleId, InstallStatus.FINISHED, 100))
    }.flowOn(Dispatchers.IO)

    override fun switchModuleVersion(moduleId: String, version: String): Boolean {
        val installDir = localDataSource.getModuleInstallPath(moduleId, version)
        if (!installDir.exists()) return false
        localDataSource.setActiveVersion(moduleId, version)
        return true
    }

    override fun uninstallModule(moduleId: String, version: String?): Boolean {
        localDataSource.removeInstalledModule(moduleId, version)
        return true
    }

    override fun getModuleEnvironment(moduleId: String): Map<String, String> {
        val version = localDataSource.getActiveVersion(moduleId) ?: return emptyMap()
        val installDir = localDataSource.getModuleInstallPath(moduleId, version)
        if (!installDir.exists()) return emptyMap()
        return mapOf(
            "MODULE_HOME" to installDir.absolutePath,
            "PATH" to "${installDir.absolutePath}/bin"
        )
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun InstalledModule.toModuleInfo(): ModuleInfo {
        return ModuleInfo(
            id = id,
            name = id.removePrefix("env-").uppercase(),
            version = version,
            description = "Installed module",
            sizeInMB = (sizeInBytes / 1024 / 1024).toInt(),
            isInstalled = true,
            installedVersion = version
        )
    }
}