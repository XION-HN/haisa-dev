package com.haisa.sdk.ide

import android.content.Context
import com.haisa.sdk.HaisaEnvironment
import com.haisa.sdk.model.BuildProgress
import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.model.ProjectConfig
import com.haisa.sdk.model.ProjectTemplate
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class HaisaIdeApiImpl private constructor(context: Context) : HaisaIdeApi {

    private val haisa = HaisaEnvironment.getInstance(context)
    private val pkgManager = haisa.getPackageManager()
    private val listeners = CopyOnWriteArrayList<HaisaIdeApi.ProjectListener>()

    override suspend fun getAvailableModules(): List<ModuleInfo> {
        return haisa.getAvailableModules()
    }

    override fun installModule(moduleId: String, version: String?): Flow<InstallProgress> {
        return haisa.installModule(moduleId, version)
    }

    override fun uninstallModule(moduleId: String, version: String?): Boolean {
        val result = haisa.uninstallModule(moduleId, version)
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

    override suspend fun createProject(
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

            override fun write(input: String) {}

            override fun readOutput(): String = ""

            override fun close() {
                isAlive = false
            }
        }
    }

    override fun getLanguageSdk(languageId: String): HaisaIdeApi.LanguageSdkInfo? {
        val mapping = LANGUAGE_TO_PACKAGE[languageId] ?: return null
        val pkgId = mapping.packageId
        if (!pkgManager.isInstalled(pkgId)) return null

        val pkgInfo = pkgManager.show(pkgId) ?: return null
        val installed = pkgManager.listInstalled().find { it.pkgId == pkgId } ?: return null
        val installDir = File(installed.installPath)
        if (!installDir.exists()) return null

        val env = pkgManager.getEnvironment(pkgId)
        val binaries = pkgManager.getEntryBinaries(pkgId)
        val ideIntegrations = pkgManager.getIdeIntegrations(pkgId)

        val includePaths = mapping.includePathSuffixes.mapNotNull { suffix ->
            val dir = File(installDir, suffix)
            if (dir.exists()) dir.absolutePath else null
        }

        val libraryPaths = mapping.libraryPathSuffixes.mapNotNull { suffix ->
            val dir = File(installDir, suffix)
            if (dir.exists()) dir.absolutePath else null
        }

        return HaisaIdeApi.LanguageSdkInfo(
            languageId = languageId,
            packageName = pkgId,
            version = installed.version,
            homeDir = installDir.absolutePath,
            binaryPaths = binaries,
            includePaths = includePaths,
            libraryPaths = libraryPaths,
            envVars = env,
            ideTasks = ideIntegrations.flatMap { it.tasks }
        )
    }

    override fun getLanguageSdks(): List<HaisaIdeApi.LanguageSdkInfo> {
        return LANGUAGE_TO_PACKAGE.keys.mapNotNull { getLanguageSdk(it) }
    }

    override fun resolveCompletionContext(
        filePath: String,
        moduleIds: List<String>
    ): HaisaIdeApi.CompletionContext {
        val languageId = inferLanguageId(filePath)
        val sdkInfo = getLanguageSdk(languageId)

        val additionalIncludes = mutableListOf<String>()
        val mergedEnv = mutableMapOf<String, String>()

        for (pkgId in moduleIds) {
            val env = pkgManager.getEnvironment(pkgId)
            mergedEnv.putAll(env)

            val pkgInfo = pkgManager.show(pkgId)
            if (pkgInfo != null) {
                val installed = pkgManager.listInstalled().find { it.pkgId == pkgId }
                if (installed != null) {
                    val installDir = File(installed.installPath)
                    val mapping = LANGUAGE_TO_PACKAGE[languageId]
                    if (mapping != null && pkgId == mapping.packageId) {
                        for (suffix in mapping.includePathSuffixes) {
                            val dir = File(installDir, suffix)
                            if (dir.exists()) additionalIncludes.add(dir.absolutePath)
                        }
                    }
                }
            }
        }

        return HaisaIdeApi.CompletionContext(
            languageId = languageId,
            sdkInfo = sdkInfo,
            additionalIncludePaths = additionalIncludes,
            environment = mergedEnv
        )
    }

    override fun addProjectListener(listener: HaisaIdeApi.ProjectListener) {
        listeners.add(listener)
    }

    override fun removeProjectListener(listener: HaisaIdeApi.ProjectListener) {
        listeners.remove(listener)
    }

    private data class PackageLanguageMapping(
        val packageId: String,
        val includePathSuffixes: List<String>,
        val libraryPathSuffixes: List<String>
    )

    companion object {
        private val LANGUAGE_TO_PACKAGE = mapOf(
            "java" to PackageLanguageMapping(
                "env-jdk",
                listOf("include", "lib/jvm/include", "lib/jvm/include/linux"),
                listOf("lib", "lib/jvm/lib")
            ),
            "kotlin" to PackageLanguageMapping(
                "env-jdk",
                listOf("include", "lib/jvm/include", "lib/jvm/include/linux"),
                listOf("lib", "lib/jvm/lib")
            ),
            "python" to PackageLanguageMapping(
                "env-python",
                listOf("include/python3.11", "lib/python3.11/site-packages"),
                listOf("lib", "lib/python3.11/lib-dynload")
            ),
            "javascript" to PackageLanguageMapping(
                "env-node",
                listOf("include/node"),
                listOf("lib", "lib/node_modules")
            ),
            "typescript" to PackageLanguageMapping(
                "env-node",
                listOf("include/node", "lib/node_modules/typescript/lib"),
                listOf("lib", "lib/node_modules")
            ),
            "c" to PackageLanguageMapping(
                "env-cc",
                listOf("include", "include/clang", "include/clang-c"),
                listOf("lib", "lib/clang/17/lib/linux")
            ),
            "cpp" to PackageLanguageMapping(
                "env-cc",
                listOf("include", "include/c++", "include/c++/v1"),
                listOf("lib", "lib/clang/17/lib/linux")
            ),
            "rust" to PackageLanguageMapping(
                "env-rust",
                listOf("lib/rustlib/src/rust/src", "lib/rustlib/src/rust/library"),
                listOf("lib", "lib/rustlib/aarch64-linux-android/lib")
            ),
            "go" to PackageLanguageMapping(
                "env-go",
                listOf("src", "pkg/include"),
                listOf("pkg", "pkg/aarch64-linux-android")
            )
        )

        private val EXTENSION_TO_LANGUAGE = mapOf(
            ".java" to "java",
            ".kt" to "kotlin",
            ".kts" to "kotlin",
            ".py" to "python",
            ".pyw" to "python",
            ".js" to "javascript",
            ".mjs" to "javascript",
            ".cjs" to "javascript",
            ".ts" to "typescript",
            ".tsx" to "typescript",
            ".c" to "c",
            ".h" to "c",
            ".cpp" to "cpp",
            ".cc" to "cpp",
            ".cxx" to "cpp",
            ".hpp" to "cpp",
            ".hxx" to "cpp",
            ".rs" to "rust",
            ".go" to "go"
        )

        private fun inferLanguageId(filePath: String): String {
            val ext = filePath.substringAfterLast('.', "").let { if (it.isNotEmpty()) ".$it" else "" }
            return EXTENSION_TO_LANGUAGE[ext] ?: "unknown"
        }

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
