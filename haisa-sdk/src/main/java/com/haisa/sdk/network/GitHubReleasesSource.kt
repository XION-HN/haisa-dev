package com.haisa.sdk.network

import com.haisa.sdk.model.ModuleInfo
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
    val modules: List<RepoModuleEntry>
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
    private val repoName = "haisa-dev"
    private val indexUrl =
        "https://raw.githubusercontent.com/$repoOwner/$repoName/main/docs/repo-index.json"

    suspend fun fetchAvailableModules(): Result<List<ModuleInfo>> {
        return try {
            val response = apiService.getRepoIndex(indexUrl)
            if (response.isSuccessful) {
                val index = response.body() ?: return Result.failure(Exception("Empty response"))
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