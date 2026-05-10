package com.haisa.sdk.ide

import android.content.Context
import com.haisa.sdk.HaisaEnvironment
import com.haisa.sdk.model.BuildProgress
import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.model.ProjectConfig
import com.haisa.sdk.model.ProjectTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.CopyOnWriteArrayList

class HaisaIdeApiImpl private constructor(context: Context) : HaisaIdeApi {

    private val haisa = HaisaEnvironment.getInstance(context)
    private val listeners = CopyOnWriteArrayList<HaisaIdeApi.ProjectListener>()

    override suspend fun getAvailableModules(): List<ModuleInfo> {
        return haisa.getAvailableModules()
    }

    override fun installModule(moduleId: String, version: String?): Flow<InstallProgress> {
        return haisa.installModule(moduleId, version).also { flow ->
            flow.map { it }
        }
    }

    override fun uninstallModule(moduleId: String, version: String?): Boolean {
        val manager = com.haisa.sdk.service.ModuleManager.getInstance(
            haisa.javaClass.getDeclaredField("appContext")
                .apply { isAccessible = true }
                .get(haisa) as Context
        )
        val result = manager.uninstallModule(moduleId, version)
        if (result) {
            listeners.forEach { it.onModuleUninstalled(moduleId) }
        }
        return result
    }

    override fun isModuleInstalled(moduleId: String): Boolean {
        return haisa.isModuleInstalled(moduleId)
    }

    override fun getModuleEnvironment(moduleId: String): Map<String, String> {
        return haisa.getModuleEnvironment(moduleId)
    }

    override fun executeBuild(
        projectPath: String,
        buildCommand: String,
        moduleIds: List<String>
    ): Flow<BuildProgress> {
        listeners.forEach { it.onBuildStarted(projectPath) }
        return haisa.executeBuild(projectPath, buildCommand, moduleIds)
    }

    override fun createProject(
        projectName: String,
        template: ProjectTemplate,
        outputDir: String
    ): Result<ProjectConfig> {
        val result = haisa.createProject(projectName, template, outputDir)
        result.getOrNull()?.let { config ->
            listeners.forEach { it.onProjectCreated(config) }
        }
        return result
    }

    override fun openTerminal(moduleIds: List<String>): HaisaIdeApi.TerminalSession {
        val moduleId = moduleIds.firstOrNull()
        return object : HaisaIdeApi.TerminalSession {
            override val moduleId: String? = moduleId
            override var isAlive: Boolean = true
                private set

            override fun write(input: String) {
                // Terminal I/O handled by TerminalActivity
            }

            override fun readOutput(): String {
                return ""
            }

            override fun close() {
                isAlive = false
            }
        }
    }

    override fun addProjectListener(listener: HaisaIdeApi.ProjectListener) {
        listeners.add(listener)
    }

    override fun removeProjectListener(listener: HaisaIdeApi.ProjectListener) {
        listeners.remove(listener)
    }

    companion object {
        @Volatile
        private var instance: HaisaIdeApiImpl? = null

        @JvmStatic
        fun getInstance(context: Context): HaisaIdeApi {
            return instance ?: synchronized(this) {
                instance ?: HaisaIdeApiImpl(context).also { instance = it }
            }
        }

        @JvmStatic
        fun resetInstance() {
            instance = null
        }
    }
}
