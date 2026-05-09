package com.haisa.sdk.util

object PathResolver {

    private const val MODULES_BASE = "modules"

    fun getModulesBaseDir(appFilesDir: String): String {
        return "$appFilesDir/$MODULES_BASE"
    }

    fun getModuleDir(appFilesDir: String, moduleId: String, version: String): String {
        return "${getModulesBaseDir(appFilesDir)}/$moduleId/$version"
    }

    fun getModuleBinDir(appFilesDir: String, moduleId: String, version: String): String {
        return "${getModuleDir(appFilesDir, moduleId, version)}/bin"
    }

    fun getModuleLibDir(appFilesDir: String, moduleId: String, version: String): String {
        return "${getModuleDir(appFilesDir, moduleId, version)}/lib"
    }

    fun getActiveSymlinkPath(appFilesDir: String, moduleId: String): String {
        return "${getModulesBaseDir(appFilesDir)}/$moduleId/active"
    }

    fun resolveModuleId(templateName: String): List<String> {
        return when (templateName) {
            "android-java", "android-kotlin" -> listOf("env-jdk", "env-cc")
            "python" -> listOf("env-python")
            "nodejs" -> listOf("env-node")
            "c-native" -> listOf("env-cc")
            "rust" -> listOf("env-rust")
            "go" -> listOf("env-go")
            else -> listOf("env-base")
        }
    }
}