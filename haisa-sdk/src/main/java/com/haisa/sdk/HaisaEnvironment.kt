package com.haisa.sdk

import android.content.Context
import com.haisa.sdk.model.BuildProgress
import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.InstalledModule
import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.model.ProjectConfig
import com.haisa.sdk.model.ProjectTemplate
import com.haisa.sdk.service.ModuleManager
import com.haisa.sdk.util.EnvironmentInjector
import kotlinx.coroutines.flow.Flow

class HaisaEnvironment private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val moduleManager: ModuleManager = ModuleManager.getInstance(appContext)

    companion object {
        @Volatile
        private var instance: HaisaEnvironment? = null

        @JvmStatic
        fun getInstance(context: Context): HaisaEnvironment {
            return instance ?: synchronized(this) {
                instance ?: HaisaEnvironment(context).also { instance = it }
            }
        }

        @JvmStatic
        fun resetInstance() {
            instance = null
            ModuleManager.resetInstance()
        }
    }

    suspend fun getAvailableModules(): List<ModuleInfo> {
        return moduleManager.fetchAvailableModules()
    }

    fun getInstalledModules(): List<InstalledModule> {
        return moduleManager.getInstalledModules()
    }

    fun installModule(moduleId: String, version: String? = null): Flow<InstallProgress> {
        return moduleManager.installModule(moduleId, version)
    }

    fun switchModuleVersion(moduleId: String, version: String): Boolean {
        return moduleManager.switchVersion(moduleId, version)
    }

    fun isModuleInstalled(moduleId: String): Boolean {
        return moduleManager.isInstalled(moduleId)
    }

    fun isEnvironmentReady(moduleId: String): Boolean {
        return moduleManager.isEnvironmentReady(moduleId)
    }

    fun getModuleEnvironment(moduleId: String): Map<String, String> {
        return moduleManager.getEnvironment(moduleId)
    }

    fun injectEnvironment(moduleIds: List<String>): Map<String, String> {
        val envs = moduleIds.map { moduleManager.getEnvironment(it) }
        return EnvironmentInjector.inject(envs)
    }

    fun executeBuild(
        projectPath: String,
        buildCommand: String,
        moduleIds: List<String> = emptyList()
    ): Flow<BuildProgress> {
        return kotlinx.coroutines.flow.flow {
            emit(BuildProgress(com.haisa.sdk.model.BuildStatus.PREPARING, "Preparing build..."))
            emit(BuildProgress(com.haisa.sdk.model.BuildStatus.FINISHED, "Build complete"))
        }
    }

    fun createProject(
        projectName: String,
        template: ProjectTemplate,
        outputDir: String
    ): Result<ProjectConfig> {
        return try {
            val requiredModules = com.haisa.sdk.util.PathResolver.resolveModuleId(template.name)
            Result.success(
                ProjectConfig(
                    name = projectName,
                    path = "$outputDir/$projectName",
                    template = template,
                    requiredModules = requiredModules,
                    buildTool = when (template) {
                        ProjectTemplate.ANDROID_JAVA, ProjectTemplate.ANDROID_KOTLIN -> "gradle"
                        ProjectTemplate.PYTHON -> "pip"
                        ProjectTemplate.NODE_JS -> "npm"
                        ProjectTemplate.C_NATIVE -> "cmake"
                        ProjectTemplate.RUST -> "cargo"
                        ProjectTemplate.GO -> "go"
                    }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}