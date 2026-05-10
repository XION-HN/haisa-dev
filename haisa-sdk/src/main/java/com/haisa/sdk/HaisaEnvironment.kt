package com.haisa.sdk

import android.content.Context
import com.haisa.sdk.engine.BuildEngine
import com.haisa.sdk.model.BuildProgress
import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.InstalledModule
import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.model.ProjectConfig
import com.haisa.sdk.model.ProjectTemplate
import com.haisa.sdk.service.ModuleManager
import com.haisa.sdk.util.EnvironmentInjector
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.io.File

class HaisaEnvironment private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val moduleManager: ModuleManager = ModuleManager.getInstance(appContext)
    private val buildEngine: BuildEngine = BuildEngine()

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
        val moduleEnvs = moduleIds.map { moduleManager.getEnvironment(it) }
        return buildEngine.execute(projectPath, buildCommand, moduleEnvs)
    }

    fun createProject(
        projectName: String,
        template: ProjectTemplate,
        outputDir: String
    ): Result<ProjectConfig> {
        return try {
            val requiredModules = com.haisa.sdk.util.PathResolver.resolveModuleId(template.name)
            val buildTool = when (template) {
                ProjectTemplate.ANDROID_JAVA, ProjectTemplate.ANDROID_KOTLIN -> "gradle"
                ProjectTemplate.PYTHON -> "pip"
                ProjectTemplate.NODE_JS -> "npm"
                ProjectTemplate.C_NATIVE -> "cmake"
                ProjectTemplate.RUST -> "cargo"
                ProjectTemplate.GO -> "go"
            }

            val projectDir = File("$outputDir/$projectName")
            projectDir.mkdirs()

            val srcDir = File(projectDir, "src")
            srcDir.mkdirs()

            when (template) {
                ProjectTemplate.PYTHON -> {
                    File(srcDir, "main.py").writeText(generatePythonTemplate(projectName))
                    File(projectDir, "requirements.txt").writeText("")
                    File(projectDir, "README.md").writeText("# $projectName\n\nPython project created by Haisa Dev.\n")
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

    private fun generatePythonTemplate(name: String): String {
        return "#!/usr/bin/env python3\n" +
            "# $name - Created by Haisa Dev\n\n" +
            "def main():\n" +
            "    print(\"Hello from $name!\")\n\n" +
            "if __name__ == \"__main__\":\n" +
            "    main()\n"
    }

    private fun generateNodeTemplate(name: String): String {
        return "const http = require('http');\n\n" +
            "const port = 3000;\n\n" +
            "const server = http.createServer((req, res) => {\n" +
            "  res.writeHead(200, { 'Content-Type': 'text/plain' });\n" +
            "  res.end('Hello from $name!');\n" +
            "});\n\n" +
            "server.listen(port, () => {\n" +
            "  console.log('Server running at http://localhost:' + port + '/');\n" +
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
            "    println!(\"Hello from Haisa Dev!\");\n" +
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
