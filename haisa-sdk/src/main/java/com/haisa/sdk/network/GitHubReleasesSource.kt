package com.haisa.sdk.network

import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.pkg.PackageInfo
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

data class GitHubRelease(
    val id: Long,
    @SerializedName("tag_name")
    val tagName: String,
    val name: String,
    val body: String?,
    @SerializedName("published_at")
    val publishedAt: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val id: Long,
    val name: String,
    val size: Long,
    @SerializedName("browser_download_url")
    val downloadUrl: String,
    @SerializedName("content_type")
    val contentType: String
)

data class RepoIndex(
    val version: String,
    @SerializedName("last_updated")
    val lastUpdated: String,
    @SerializedName("base_url")
    val baseUrl: String,
    val modules: List<RepoModuleEntry>,
    val packages: List<RepoPackageEntry> = emptyList()
)

data class RepoModuleEntry(
    val id: String,
    val name: String,
    val description: String,
    val latest: String,
    @SerializedName("size_mb")
    val sizeMb: Int,
    val dependencies: List<String>
)

data class RepoPackageEntry(
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
    val license: String = "",
    val homepage: String = "",
    @SerializedName("ide_integrations")
    val ideIntegrations: List<RepoIdeIntegration> = emptyList()
)

data class RepoIdeIntegration(
    @SerializedName("ide_type")
    val ideType: String = "",
    val tasks: List<String> = emptyList()
)

interface GitHubApiService {
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<GitHubRelease>>

    @GET
    suspend fun getRepoIndex(@Url url: String): Response<RepoIndex>
}

class GitHubReleasesSource(private val apiService: GitHubApiService) {

    private val repoOwner = "XION-HN"
    private val repoName = "haisa-des"
    private val indexUrl =
        "https://raw.githubusercontent.com/$repoOwner/$repoName/main/docs/repo-index.json"

    suspend fun fetchAvailableModules(): Result<List<ModuleInfo>> {
        return try {
            val response = apiService.getRepoIndex(indexUrl)
            if (response.isSuccessful) {
                val index = response.body() ?: return Result.failure(Exception("Empty response"))
                if (index.packages.isNotEmpty()) {
                    val modules = index.packages.map { entry ->
                        val downloadUrl = entry.downloadUrl.ifEmpty {
                            "${index.baseUrl}/${entry.pkgId}-v${entry.version}/${entry.pkgId}-${entry.version}-${entry.arch}.zip"
                        }
                        ModuleInfo(
                            id = entry.pkgId,
                            name = entry.name,
                            version = entry.version,
                            description = entry.description,
                            sizeInMB = (entry.installSizeKb / 1024).toInt().coerceAtLeast(1),
                            dependencies = entry.dependencies,
                            downloadUrl = downloadUrl,
                            sha256 = entry.sha256
                        )
                    }
                    Result.success(modules)
                } else {
                    val modules = index.modules.map { entry ->
                        val downloadUrl = "${index.baseUrl}/${entry.id}-${entry.latest}-aarch64"
                        ModuleInfo(
                            id = entry.id,
                            name = entry.name,
                            version = entry.latest,
                            description = entry.description,
                            sizeInMB = entry.sizeMb,
                            dependencies = entry.dependencies,
                            downloadUrl = downloadUrl
                        )
                    }
                    Result.success(modules)
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPackageIndex(): Result<List<PackageInfo>> {
        return try {
            val response = apiService.getRepoIndex(indexUrl)
            if (response.isSuccessful) {
                val index = response.body() ?: return Result.failure(Exception("Empty response"))
                val packages = index.packages.map { entry ->
                    PackageInfo(
                        pkgId = entry.pkgId,
                        name = entry.name,
                        version = entry.version,
                        arch = entry.arch,
                        description = entry.description,
                        maintainer = entry.maintainer,
                        section = entry.section,
                        priority = entry.priority,
                        dependencies = entry.dependencies,
                        provides = entry.provides,
                        conflicts = entry.conflicts,
                        entryBinaries = entry.entryBinaries,
                        envVars = entry.envVars,
                        installSizeKb = entry.installSizeKb,
                        sha256 = entry.sha256,
                        downloadUrl = entry.downloadUrl.ifEmpty {
                            "${index.baseUrl}/${entry.pkgId}-v${entry.version}/${entry.pkgId}-${entry.version}-${entry.arch}.zip"
                        },
                        license = entry.license,
                        homepage = entry.homepage,
                        ideIntegrations = entry.ideIntegrations.map { ii ->
                            PackageInfo.IdeIntegration(ii.ideType, ii.tasks)
                        }
                    )
                }
                Result.success(packages)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchModuleReleases(moduleId: String): Result<List<GitHubRelease>> {
        return try {
            val response = apiService.getReleases(repoOwner, repoName)
            if (response.isSuccessful) {
                val releases = response.body()?.filter {
                    it.tagName.startsWith(moduleId)
                } ?: emptyList()
                Result.success(releases)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}