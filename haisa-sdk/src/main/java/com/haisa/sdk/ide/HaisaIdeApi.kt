package com.haisa.sdk.ide

import com.haisa.sdk.model.BuildProgress
import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.model.ProjectConfig
import com.haisa.sdk.model.ProjectTemplate
import kotlinx.coroutines.flow.Flow

interface HaisaIdeApi {

    suspend fun getAvailableModules(): List<ModuleInfo>

    fun installModule(moduleId: String, version: String? = null): Flow<InstallProgress>

    fun uninstallModule(moduleId: String, version: String?): Boolean

    fun isModuleInstalled(moduleId: String): Boolean

    fun getModuleEnvironment(moduleId: String): Map<String, String>

    fun executeBuild(
        projectPath: String,
        buildCommand: String,
        moduleIds: List<String> = emptyList()
    ): Flow<BuildProgress>

    fun createProject(
        projectName: String,
        template: ProjectTemplate,
        outputDir: String
    ): Result<ProjectConfig>

    fun openTerminal(moduleIds: List<String>): TerminalSession

    interface TerminalSession {
        val moduleId: String?
        val isAlive: Boolean
        fun write(input: String)
        fun readOutput(): String
        fun close()
    }

    interface ProjectListener {
        fun onProjectCreated(config: ProjectConfig) {}
        fun onBuildStarted(projectPath: String) {}
        fun onBuildProgress(projectPath: String, progress: BuildProgress) {}
        fun onBuildCompleted(projectPath: String, success: Boolean) {}
        fun onModuleInstalled(moduleId: String) {}
        fun onModuleUninstalled(moduleId: String) {}
    }

    fun addProjectListener(listener: ProjectListener)
    fun removeProjectListener(listener: ProjectListener)
}
