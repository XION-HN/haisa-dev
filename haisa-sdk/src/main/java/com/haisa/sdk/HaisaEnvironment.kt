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
import kotlinx.coroutines.flow.Flow

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

            val projectDir = java.io.File("$outputDir/$projectName")
            projectDir.mkdirs()

            val srcDir = java.io.File(projectDir, "src")
            srcDir.mkdirs()

            when (template) {
                ProjectTemplate.PYTHON -> {
                    java.io.File(srcDir, "main.py").writeText(generatePythonTemplate(projectName))
                    java.io.File(projectDir, "requirements.txt").writeText("")
                    java.io.File(projectDir, "README.md").writeText("# $projectName\n\nPython project created by Haisa Dev.\n")
                }
                ProjectTemplate.NODE_JS -> {
                    java.io.File(srcDir, "index.js").writeText(generateNodeTemplate(projectName))
                    java.io.File(projectDir, "package.json").writeText(generatePackageJson(projectName))
                }
                ProjectTemplate.C_NATIVE -> {
                    java.io.File(srcDir, "main.c").writeText(generateCTemplate(projectName))
                    java.io.File(projectDir, "CMakeLists.txt").writeText(generateCMakeLists(projectName))
                }
                ProjectTemplate.ANDROID_JAVA, ProjectTemplate.ANDROID_KOTLIN -> {
                    java.io.File(srcDir, "Main.kt").writeText(generateAndroidKtTemplate(projectName))
                    java.io.File(projectDir, "build.gradle.kts").writeText(generateGradleKtsTemplate(projectName))
                }
                ProjectTemplate.RUST -> {
                    java.io.File(projectDir, "Cargo.toml").writeText(generateCargoToml(projectName))
                    java.io.File(java.io.File(srcDir, "bin"), "main.rs").also { it.parentFile.mkdirs() }
                        .writeText(generateRustTemplate())
                }
                ProjectTemplate.GO -> {
                    java.io.File(srcDir, "main.go").writeText(generateGoTemplate(projectName))
                    java.io.File(projectDir, "go.mod").writeText("module $projectName\ngo 1.21\n")
                }
            }

            val config = ProjectConfig(
                name = projectName,
                path = projectDir.absolutePath,
                template = template,
                requiredModules = requiredModules,
                buildTool = buildTool
            )

            val configFile = java.io.File(projectDir, "haisa-config.json")
            configFile.writeText(com.google.gson.Gson().toJson(config))

            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generatePythonTemplate(name: String): String = """
#!/usr/bin/env python3
"""$name - Created by Haisa Dev"""

def main():
    print("Hello from $name!")

if __name__ == "__main__":
    main()
""".trimIndent()

    private fun generateNodeTemplate(name: String): String = """
const http = require('http');

const port = 3000;

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/plain' });
  res.end('Hello from $name!');
});

server.listen(port, () => {
  console.log(\`Server running at http://localhost:\${port}/\`);
});
""".trimIndent()

    private fun generateCTemplate(name: String): String = """
#include <stdio.h>

int main() {
    printf("Hello from $name!\\n");
    return 0;
}
""".trimIndent()

    private fun generateCMakeLists(name: String): String = """
cmake_minimum_required(VERSION 3.10)
project($name C)

set(CMAKE_C_STANDARD 11)

add_executable($name src/main.c)
""".trimIndent()

    private fun generatePackageJson(name: String): String = """
{
  "name": "$name",
  "version": "1.0.0",
  "main": "src/index.js",
  "scripts": {
    "start": "node src/index.js"
  }
}
""".trimIndent()

    private fun generateAndroidKtTemplate(name: String): String = """
package ${name.replace("-", ".").lowercase()}

fun main() {
    println("Hello from $name!")
}
""".trimIndent()

    private fun generateGradleKtsTemplate(name: String): String = """
plugins {
    kotlin("jvm") version "1.9.0"
}

group = "${name.replace("-", ".")}"
version = "1.0.0"

repositories {
    mavenCentral()
}

tasks.register<JavaExec>("run") {
    mainClass.set("${name.replace("-", ".").lowercase()}.MainKt")
}
""".trimIndent()

    private fun generateCargoToml(name: String): String = """
[package]
name = "$name"
version = "0.1.0"
edition = "2021"

[[bin]]
name = "$name"
path = "src/bin/main.rs"
""".trimIndent()

    private fun generateRustTemplate(): String = """
fn main() {
    println!("Hello from Haisa Dev!");
}
""".trimIndent()

    private fun generateGoTemplate(name: String): String = """
package main

import "fmt"

func main() {
    fmt.Println("Hello from $name!")
}
""".trimIndent()
}
