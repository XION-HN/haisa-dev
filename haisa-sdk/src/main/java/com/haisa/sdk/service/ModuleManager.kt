package com.haisa.sdk.service

import android.content.Context
import com.haisa.sdk.data.LocalDataSource
import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.InstallStatus
import com.haisa.sdk.model.InstalledModule
import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.network.GitHubApiService
import com.haisa.sdk.network.GitHubReleasesSource
import com.haisa.sdk.repository.ModuleRepository
import com.haisa.sdk.repository.ModuleRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class ModuleManager private constructor(context: Context) {

    private val localDataSource: LocalDataSource = LocalDataSource(context)
    private val remoteSource: GitHubReleasesSource
    private val repository: ModuleRepository

    init {
        val apiService = NetworkProvider.provideApiService()
        remoteSource = GitHubReleasesSource(apiService)
        repository = ModuleRepositoryImpl(localDataSource, remoteSource)
    }

    suspend fun fetchAvailableModules(): List<ModuleInfo> {
        return repository.fetchAvailableModules().getOrDefault(emptyList())
    }

    fun getInstalledModules(): List<InstalledModule> {
        return repository.getInstalledModules()
    }

    fun isInstalled(moduleId: String): Boolean {
        return repository.isModuleInstalled(moduleId)
    }

    fun isEnvironmentReady(moduleId: String): Boolean {
        return repository.isEnvironmentReady(moduleId)
    }

    fun installModule(moduleId: String, version: String? = null): Flow<InstallProgress> {
        return repository.installModule(moduleId, version)
    }

    fun switchVersion(moduleId: String, version: String): Boolean {
        return repository.switchModuleVersion(moduleId, version)
    }

    fun uninstallModule(moduleId: String, version: String? = null): Boolean {
        return repository.uninstallModule(moduleId, version)
    }

    fun getEnvironment(moduleId: String): Map<String, String> {
        return repository.getModuleEnvironment(moduleId)
    }

    fun getModuleInstallPath(moduleId: String, version: String): File {
        return localDataSource.getModuleInstallPath(moduleId, version)
    }

    companion object {
        @Volatile
        private var instance: ModuleManager? = null

        @JvmStatic
        fun getInstance(context: Context): ModuleManager {
            return instance ?: synchronized(this) {
                instance ?: ModuleManager(context).also { instance = it }
            }
        }

        @JvmStatic
        fun resetInstance() {
            instance = null
        }
    }
}