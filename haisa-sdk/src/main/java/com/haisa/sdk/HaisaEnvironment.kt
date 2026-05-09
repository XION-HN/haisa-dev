package com.haisa.sdk

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Haisa Dev SDK 对外暴露的核心 API
 *
 * 这是第三方项目集成 Haisa Dev 环境的主要入口。
 * 通过此 API 可以：
 * - 管理模块（查看/安装/卸载/切换）
 * - 启动终端会话
 * - 执行构建任务
 * - 注入环境变量到当前进程
 */
class HaisaEnvironment private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val moduleManager: ModuleManager
    private val terminalEngine: TerminalEngine
    private val buildEngine: BuildEngine

    init {
        moduleManager = ModuleManager.getInstance(appContext)
        terminalEngine = TerminalEngine(context)
        buildEngine = BuildEngine(context)
    }

    // ========== 单例模式 ==========
    companion object {
        @Volatile
        private var instance: HaisaEnvironment? = null

        @JvmStatic
        fun getInstance(context: Context): HaisaEnvironment {
            return instance ?: synchronized(this) {
                instance ?: HaisaEnvironment(context).also { instance = it }
            }
        }
    }

    // ==================== Module Management ====================

    /**
     * 获取模块商店中的所有可用模块
     */
    suspend fun getAvailableModules(): List<ModuleInfo> {
        return moduleManager.fetchAvailableModules()
    }

    /**
     * 获取已安装的模块
     */
    fun getInstalledModules(): List<InstalledModule> {
        return moduleManager.getInstalledModules()
    }

    /**
     * 安装指定模块（会自动处理依赖）
     */
    suspend fun installModule(
        moduleId: String,
        version: String? = null
    ): Flow<InstallProgress> {
        return moduleManager.installModule(moduleId, version)
    }

    /**
     * 切换模块版本
     */
    fun switchModuleVersion(moduleId: String, version: String): Boolean {
        return moduleManager.switchVersion(moduleId, version)
    }

    /**
     * 检查模块是否已安装
     */
    fun isModuleInstalled(moduleId: String): Boolean {
        return moduleManager.isInstalled(moduleId)
    }

    /**
     * 检查指定模块的环境是否就绪
     */
    fun isEnvironmentReady(moduleId: String): Boolean {
        return moduleManager.isEnvironmentReady(moduleId)
    }

    // ==================== Terminal ====================

    /**
     * 创建一个新的终端会话
     */
    fun createTerminalSession(
        workingDir: String? = null,
        envVars: Map<String, String>? = null
    ): TerminalSession {
        return terminalEngine.createSession(workingDir, envVars)
    }

    /**
     * 获取当前活跃的终端会话
     */
    fun getActiveSessions(): List<TerminalSession> {
        return terminalEngine.getActiveSessions()
    }

    // ==================== Build System ====================

    /**
     * 执行构建任务
     */
    suspend fun executeBuild(
        projectPath: String,
        buildCommand: String,
        moduleIds: List<String> = listOf()
    ): Flow<BuildProgress> {
        return buildEngine.execute(projectPath, buildCommand, moduleIds)
    }

    // ==================== Environment Injection ====================

    /**
     * 注入指定模块的环境变量到当前进程
     * 适用于需要在当前进程执行命令的场景
     */
    fun injectEnvironment(moduleIds: List<String>): Map<String, String> {
        return EnvironmentInjector.inject(moduleIds)
    }

    /**
     * 获取模块的环境变量（不注入，仅返回）
     */
    fun getModuleEnvironment(moduleId: String): Map<String, String> {
        return moduleManager.getEnvironment(moduleId)
    }

    // ==================== Project Templates ====================

    /**
     * 创建项目并自动配置所需模块
     */
    suspend fun createProject(
        projectName: String,
        template: ProjectTemplate,
        outputDir: String
    ): Result<ProjectConfig> {
        return ProjectGenerator.create(
            appContext,
            projectName,
            template,
            outputDir
        )
    }
}

/**
 * 模块信息
 */
data class ModuleInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val sizeInMB: Int,
    val dependencies: List<String> = listOf(),
    val isInstalled: Boolean = false,
    val installedVersion: String? = null
)

/**
 * 安装进度
 */
data class InstallProgress(
    val moduleId: String,
    val status: InstallStatus,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val message: String = ""
) {
    enum class InstallStatus {
        DOWNLOADING,
        EXTRACTING,
        VERIFYING,
        FINISHED,
        ERROR
    }
}

/**
 * 构建进度
 */
data class BuildProgress(
    val status: BuildStatus,
    val message: String = "",
    val progressPercent: Int = 0,
    val isError: Boolean = false
) {
    enum class BuildStatus {
        PREPARING,
        COMPILING,
        TESTING,
        PACKAGING,
        FINISHED,
        FAILED
    }
}

/**
 * 项目模板类型
 */
enum class ProjectTemplate {
    ANDROID_JAVA,
    ANDROID_KOTLIN,
    PYTHON,
    NODE_JS,
    C_NATIVE,
    RUST,
    GO
}

/**
 * 项目配置
 */
data class ProjectConfig(
    val name: String,
    val path: String,
    val template: ProjectTemplate,
    val requiredModules: List<String>,
    val buildTool: String
)

/**
 * 已安装模块
 */
data class InstalledModule(
    val id: String,
    val version: String,
    val installDate: Long,
    val sizeInBytes: Long
)