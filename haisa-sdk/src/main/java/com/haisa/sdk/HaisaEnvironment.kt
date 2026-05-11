package com.haisa.sdk

import android.content.Context
import com.haisa.sdk.engine.BuildEngine
import com.haisa.sdk.model.BuildProgress
import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.model.ProjectConfig
import com.haisa.sdk.model.ProjectTemplate
import com.haisa.sdk.pkg.PackageInfo
import com.haisa.sdk.pkg.PackageManager
import com.haisa.sdk.pkg.PackageModels.PackageIndex
import com.haisa.sdk.network.GitHubReleasesSource
import com.haisa.sdk.service.NetworkProvider
import com.haisa.sdk.util.EnvironmentInjector
import com.haisa.sdk.util.PathResolver
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class HaisaEnvironment private constructor(appContext: Context) {

    private val appContext: Context = appContext.applicationContext
    private val packageManager: PackageManager = PackageManager(appContext)
    private val buildEngine: BuildEngine = BuildEngine()
    private val remoteSource = GitHubReleasesSource(NetworkProvider.provideApiService())

    companion object {
        @Volatile
        private var instance: HaisaEnvironment? = null

        private val VALID_MODULE_ID_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}$")
        private val VALID_PROJECT_NAME_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}$")
        private const val MAX_PROJECT_NAME_LENGTH = 128

        @JvmStatic
        fun getInstance(context: Context): HaisaEnvironment {
            return instance ?: synchronized(this) {
                instance ?: HaisaEnvironment(context.applicationContext).also { instance = it }
            }
        }

        @JvmStatic
        fun resetInstance() {
            instance = null
        }
    }

    fun getPackageManager(): PackageManager = packageManager

    suspend fun getAvailableModules(): List<ModuleInfo> {
        val available = packageManager.listAvailable()
        val installed = packageManager.listInstalled().associateBy { it.pkgId }
        return available.map { pkg ->
            ModuleInfo(
                id = pkg.pkgId,
                name = pkg.name,
                version = pkg.version,
                description = pkg.description,
                sizeInMB = (pkg.installSizeKb / 1024).toInt().coerceAtLeast(1),
                dependencies = pkg.dependencies,
                isInstalled = installed.containsKey(pkg.pkgId),
                installedVersion = installed[pkg.pkgId]?.version,
                downloadUrl = pkg.downloadUrl,
                sha256 = pkg.sha256
            )
        }
    }

    fun getInstalledModules(): List<ModuleInfo> {
        return packageManager.listInstalled().map { installed ->
            val pkgInfo = packageManager.show(installed.pkgId)
            ModuleInfo(
                id = installed.pkgId,
                name = pkgInfo?.name ?: installed.pkgId.removePrefix("env-").uppercase(),
                version = installed.version,
                description = pkgInfo?.description ?: "Installed package",
                sizeInMB = (installed.installSizeKb / 1024).toInt().coerceAtLeast(1),
                dependencies = pkgInfo?.dependencies ?: emptyList(),
                isInstalled = true,
                installedVersion = installed.version
            )
        }
    }

    fun installModule(moduleId: String, version: String? = null): Flow<InstallProgress> {
        validateModuleId(moduleId)
        return packageManager.install(moduleId, version)
    }

    fun switchModuleVersion(moduleId: String, version: String): Boolean {
        validateModuleId(moduleId)
        return packageManager.getInstalledVersion(moduleId) != null
    }

    fun uninstallModule(moduleId: String, version: String? = null): Boolean {
        validateModuleId(moduleId)
        val result = packageManager.remove(moduleId, purge = true)
        return result.isSuccess
    }

    fun isModuleInstalled(moduleId: String): Boolean {
        return packageManager.isInstalled(moduleId)
    }

    fun isEnvironmentReady(moduleId: String): Boolean {
        if (!packageManager.isInstalled(moduleId)) return false
        val env = packageManager.getEnvironment(moduleId)
        return env.containsKey("PATH")
    }

    fun getModuleEnvironment(moduleId: String): Map<String, String> {
        return packageManager.getEnvironment(moduleId)
    }

    fun injectEnvironment(moduleIds: List<String>): Map<String, String> {
        moduleIds.forEach { validateModuleId(it) }
        return packageManager.injectEnvironments(moduleIds)
    }

    fun executeBuild(
        projectPath: String,
        buildCommand: String,
        moduleIds: List<String> = emptyList()
    ): Flow<BuildProgress> {
        moduleIds.forEach { validateModuleId(it) }
        val moduleEnvs = moduleIds.map { packageManager.getEnvironment(it) }
        return buildEngine.execute(projectPath, buildCommand, moduleEnvs)
    }

    suspend fun createProject(
        projectName: String,
        template: ProjectTemplate,
        outputDir: String
    ): Result<ProjectConfig> = withContext(Dispatchers.IO) {
        validateProjectName(projectName)

        val outputDirFile = File(outputDir)
        if (!outputDirFile.exists() || !outputDirFile.isDirectory) {
            return@withContext Result.failure(IllegalArgumentException("Output directory does not exist: $outputDir"))
        }

        val canonicalOutput = outputDirFile.canonicalPath
        if (!canonicalOutput.startsWith("/data/data/") && !canonicalOutput.startsWith("/sdcard/") && !canonicalOutput.startsWith("/storage/")) {
            return@withContext Result.failure(IllegalArgumentException("Output directory not in allowed paths: $canonicalOutput"))
        }

        try {
            val requiredModules = PathResolver.resolveModuleId(template.name)
            val buildTool = when (template) {
                ProjectTemplate.ANDROID_JAVA, ProjectTemplate.ANDROID_KOTLIN -> "gradle"
                ProjectTemplate.PYTHON -> "pip"
                ProjectTemplate.NODE_JS -> "npm"
                ProjectTemplate.C_NATIVE -> "cmake"
                ProjectTemplate.RUST -> "cargo"
                ProjectTemplate.GO -> "go"
            }

            val projectDir = File("$outputDir/$projectName")
            if (projectDir.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Project directory already exists: ${projectDir.absolutePath}"))
            }
            projectDir.mkdirs()

            val srcDir = File(projectDir, "src")
            srcDir.mkdirs()

            when (template) {
                ProjectTemplate.PYTHON -> {
                    File(srcDir, "main.py").writeText(generatePythonTemplate(projectName))
                    File(projectDir, "requirements.txt").writeText("")
                }
                ProjectTemplate.NODE_JS -> {
                    File(srcDir, "index.js").writeText(generateNodeTemplate(projectName))
                    File(projectDir, "package.json").writeText(generatePackageJson(projectName))
                }
                ProjectTemplate.C_NATIVE -> {
                    File(srcDir, "main.c").writeText(generateCTemplate(projectName))
                    File(projectDir, "CMakeLists.txt").writeText(generateCMakeLists(projectName))
                }
                ProjectTemplate.ANDROID_JAVA, ProjectTemplate.ANDROID_KOTLIN -> {
                    File(srcDir, "Main.kt").writeText(generateAndroidKtTemplate(projectName))
                    File(projectDir, "build.gradle.kts").writeText(generateGradleKtsTemplate(projectName))
                }
                ProjectTemplate.RUST -> {
                    File(projectDir, "Cargo.toml").writeText(generateCargoToml(projectName))
                    val binDir = File(srcDir, "bin")
                    binDir.mkdirs()
                    File(binDir, "main.rs").writeText(generateRustTemplate())
                }
                ProjectTemplate.GO -> {
                    File(srcDir, "main.go").writeText(generateGoTemplate(projectName))
                    File(projectDir, "go.mod").writeText("module $projectName\ngo 1.21\n")
                }
            }

            val config = ProjectConfig(
                name = projectName,
                path = projectDir.absolutePath,
                template = template,
                requiredModules = requiredModules,
                buildTool = buildTool
            )

            val configFile = File(projectDir, "haisa-config.json")
            configFile.writeText(Gson().toJson(config))

            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updatePackageIndex(packages: List<PackageInfo>): Int {
        return packageManager.updateIndex(packages)
    }

    fun searchPackages(query: String): List<PackageInfo> {
        return packageManager.search(query).matches
    }

    suspend fun refreshPackageIndex(): Result<Int> {
        val result = remoteSource.fetchPackageIndex()
        if (result.isFailure) return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        val packages = result.getOrThrow()
        val count = packageManager.updateIndex(packages)
        return Result.success(count)
    }

    private fun validateModuleId(moduleId: String) {
        require(moduleId.matches(VALID_MODULE_ID_REGEX)) {
            "Invalid module ID: $moduleId"
        }
    }

    private fun validateProjectName(projectName: String) {
        require(projectName.length <= MAX_PROJECT_NAME_LENGTH) {
            "Project name too long (max $MAX_PROJECT_NAME_LENGTH chars)"
        }
        require(projectName.matches(VALID_PROJECT_NAME_REGEX)) {
            "Invalid project name: $projectName"
        }
        require(!projectName.contains("..")) {
            "Project name must not contain path traversal sequences"
        }
    }

    private fun generatePythonTemplate(name: String): String {
        return "#!/usr/bin/env python3\n" +
            "# $name - Created by Haisa Des\n\n" +
            "def main():\n" +
            "    print(\"Hello from $name!\")\n\n" +
            "if __name__ == \"__main__\":\n" +
            "    main()\n"
    }

    private fun generateNodeTemplate(name: String): String {
        return "const http = require('http');\n\n" +
            "const port = 3000;\n\n" +
            "const server = http.createServer((req, res) => {\n" +
            "    res.writeHead(200, { 'Content-Type': 'text/plain' });\n" +
            "    res.end('Hello from $name!');\n" +
            "});\n\n" +
            "server.listen(port, () => {\n" +
            "    console.log('Server running at http://localhost:' + port + '/');\n" +
            "});\n"
    }

    private fun generateCTemplate(name: String): String {
        return "#include <stdio.h>\n\n" +
            "int main() {\n" +
            "    printf(\"Hello from $name!\\n\");\n" +
            "    return 0;\n" +
            "}\n"
    }

    private fun generateCMakeLists(name: String): String {
        return "cmake_minimum_required(VERSION 3.10)\n" +
            "project($name C)\n\n" +
            "set(CMAKE_C_STANDARD 11)\n\n" +
            "add_executable($name src/main.c)\n"
    }

    private fun generatePackageJson(name: String): String {
        return "{\n" +
            "  \"name\": \"$name\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"main\": \"src/index.js\",\n" +
            "  \"scripts\": {\n" +
            "    \"start\": \"node src/index.js\"\n" +
            "  }\n" +
            "}\n"
    }

    private fun generateAndroidKtTemplate(name: String): String {
        val pkg = name.replace("-", ".").lowercase()
        return "package $pkg\n\n" +
            "fun main() {\n" +
            "    println(\"Hello from $name!\")\n" +
            "}\n"
    }

    private fun generateGradleKtsTemplate(name: String): String {
        val group = name.replace("-", ".")
        val pkg = name.replace("-", ".").lowercase()
        return "plugins {\n" +
            "    kotlin(\"jvm\") version \"1.9.0\"\n" +
            "}\n\n" +
            "group = \"$group\"\n" +
            "version = \"1.0.0\"\n\n" +
            "repositories {\n" +
            "    mavenCentral()\n" +
            "}\n\n" +
            "tasks.register<JavaExec>(\"run\") {\n" +
            "    mainClass.set(\"$pkg.MainKt\")\n" +
            "}\n"
    }

    private fun generateCargoToml(name: String): String {
        return "[package]\n" +
            "name = \"$name\"\n" +
            "version = \"0.1.0\"\n" +
            "edition = \"2021\"\n\n" +
            "[[bin]]\n" +
            "name = \"$name\"\n" +
            "path = \"src/bin/main.rs\"\n"
    }

    private fun generateRustTemplate(): String {
        return "fn main() {\n" +
            "    println!(\"Hello from Haisa Des!\");\n" +
            "}\n"
    }

    private fun generateGoTemplate(name: String): String {
        return "package main\n\n" +
            "import \"fmt\"\n\n" +
            "func main() {\n" +
            "    fmt.Println(\"Hello from $name!\")\n" +
            "}\n"
    }
}
