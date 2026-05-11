package com.haisa.sdk.model

import com.google.gson.annotations.SerializedName

data class ModuleManifest(
    @SerializedName("module_id")
    val moduleId: String,
    @SerializedName("package_id")
    val packageId: String = "",
    val version: String = "1.0.0",
    val arch: String = "aarch64",
    val description: String = "",
    @SerializedName("min_base_version")
    val minBaseVersion: String = "1.0.0",
    val dependencies: List<String> = emptyList(),
    @SerializedName("entry_binaries")
    val entryBinaries: Map<String, String> = emptyMap(),
    @SerializedName("env_vars")
    val envVars: Map<String, String> = emptyMap(),
    @SerializedName("install_size_mb")
    val installSizeMb: Int = 0,
    @SerializedName("ide_integrations")
    val ideIntegrations: List<IdeIntegration> = emptyList()
) {
    data class IdeIntegration(
        @SerializedName("ide_type")
        val ideType: String = "",
        val tasks: List<String> = emptyList()
    )

    fun resolveEnvVars(installDir: String, oldPath: String): Map<String, String> {
        return envVars.mapValues { (_, value) ->
            value
                .replace("{{install_dir}}", installDir)
                .replace("{{old_path}}", oldPath)
        }
    }
}

data class ModuleInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val sizeInMB: Int,
    val dependencies: List<String> = emptyList(),
    val isInstalled: Boolean = false,
    val installedVersion: String? = null,
    val downloadUrl: String = "",
    val sha256: String = ""
)

enum class InstallStatus {
    IDLE,
    DOWNLOADING,
    EXTRACTING,
    VERIFYING,
    FINISHED,
    ERROR
}

data class InstallProgress(
    val moduleId: String,
    val status: InstallStatus,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val message: String = ""
)

enum class BuildStatus {
    IDLE,
    PREPARING,
    COMPILING,
    TESTING,
    PACKAGING,
    FINISHED,
    FAILED
}

data class BuildProgress(
    val status: BuildStatus,
    val message: String = "",
    val progressPercent: Int = 0,
    val isError: Boolean = false
)

enum class ProjectTemplate(val displayName: String) {
    ANDROID_JAVA("Android Java"),
    ANDROID_KOTLIN("Android Kotlin"),
    PYTHON("Python Script"),
    NODE_JS("Node.js"),
    C_NATIVE("C Native"),
    RUST("Rust"),
    GO("Go")
}

data class ProjectConfig(
    val name: String,
    val path: String,
    val template: ProjectTemplate,
    val requiredModules: List<String>,
    val buildTool: String
)