package com.haisa.sdk.pkg

import com.google.gson.annotations.SerializedName

data class PackageInfo(
    @SerializedName("pkg_id")
    val pkgId: String,
    val name: String,
    val version: String,
    val arch: String = "aarch64",
    val description: String = "",
    val maintainer: String = "",
    val section: String = "dev",
    val priority: String = "optional",
    @SerializedName("depends")
    val dependencies: List<String> = emptyList(),
    @SerializedName("provides")
    val provides: List<String> = emptyList(),
    @SerializedName("conflicts")
    val conflicts: List<String> = emptyList(),
    @SerializedName("entry_binaries")
    val entryBinaries: Map<String, String> = emptyMap(),
    @SerializedName("env_vars")
    val envVars: Map<String, String> = emptyMap(),
    @SerializedName("install_size_kb")
    val installSizeKb: Long = 0,
    @SerializedName("sha256")
    val sha256: String = "",
    @SerializedName("download_url")
    val downloadUrl: String = "",
    @SerializedName("repo_url")
    val repoUrl: String = "",
    @SerializedName("license")
    val license: String = "",
    @SerializedName("homepage")
    val homepage: String = "",
    @SerializedName("ide_integrations")
    val ideIntegrations: List<IdeIntegration> = emptyList()
) {
    data class IdeIntegration(
        @SerializedName("ide_type")
        val ideType: String = "",
        val tasks: List<String> = emptyList()
    )
}

data class InstalledPackage(
    val pkgId: String,
    val version: String,
    val arch: String = "aarch64",
    val installDate: Long = 0L,
    val installSizeKb: Long = 0L,
    val installPath: String = "",
    val dependencies: List<String> = emptyList(),
    val status: PackageStatus = PackageStatus.INSTALLED,
    val autoInstalled: Boolean = false
)

enum class PackageStatus {
    INSTALLED,
    HALF_INSTALLED,
    BROKEN,
    HOLD
}

data class PackageIndex(
    val version: String = "1.0",
    @SerializedName("last_updated")
    val lastUpdated: String = "",
    @SerializedName("packages")
    val packages: List<PackageInfo> = emptyList()
)

data class PackageOperation(
    val type: OperationType,
    val pkgId: String,
    val version: String,
    val message: String = ""
)

enum class OperationType {
    INSTALL,
    UPGRADE,
    DOWNGRADE,
    REMOVE,
    SKIP
}

data class DependencyGraph(
    val nodes: Map<String, PackageInfo>,
    val edges: Map<String, List<String>>,
    val installOrder: List<String>
)

data class PackageSearchResult(
    val query: String,
    val matches: List<PackageInfo>,
    val totalAvailable: Int
)
