package com.haisa.sdk.controller

import com.haisa.sdk.HaisaEnvironment
import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.ModuleInfo
import kotlinx.coroutines.flow.Flow

class ModuleController(private val haisa: HaisaEnvironment) {

    suspend fun loadModules(): List<ModuleInfo> {
        return haisa.getAvailableModules()
    }

    fun isModuleInstalled(moduleId: String): Boolean {
        return haisa.isModuleInstalled(moduleId)
    }

    fun isEnvironmentReady(moduleId: String): Boolean {
        return haisa.isEnvironmentReady(moduleId)
    }

    fun installModule(moduleId: String, version: String? = null): Flow<InstallProgress> {
        return haisa.installModule(moduleId, version)
    }

    fun switchModuleVersion(moduleId: String, version: String): Boolean {
        return haisa.switchModuleVersion(moduleId, version)
    }

    fun getModuleEnvironment(moduleId: String): Map<String, String> {
        return haisa.getModuleEnvironment(moduleId)
    }

    fun injectEnvironment(moduleIds: List<String>): Map<String, String> {
        return haisa.injectEnvironment(moduleIds)
    }
}