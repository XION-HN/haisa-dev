package com.haisa.sdk.repository

import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.InstallStatus
import com.haisa.sdk.model.InstalledModule
import com.haisa.sdk.model.ModuleInfo
import com.haisa.sdk.data.LocalDataSource
import com.haisa.sdk.network.GitHubReleasesSource
import com.haisa.sdk.util.ModuleExtractor
import com.haisa.sdk.util.PathResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

interface ModuleRepository {
    suspend fun fetchAvailableModules(): Result<List<ModuleInfo>>
    fun getInstalledModules(): List<InstalledModule>
    fun isModuleInstalled(moduleId: String): Boolean
    fun isEnvironmentReady(moduleId: String): Boolean
    fun installModule(moduleId: String, version: String? = null): Flow<InstallProgress>
    fun switchModuleVersion(moduleId: String, version: String): Boolean
    fun uninstallModule(moduleId: String, version: String? = null): Boolean
    fun getModuleEnvironment(moduleId: String): Map<String, String>
}

class ModuleRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val remoteSource: GitHubReleasesSource
) : ModuleRepository {

    private val cache = mutableMapOf<String, List<ModuleInfo>>()
    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val TAG = "ModuleRepository"
    }

    override suspend fun fetchAvailableModules(): Result<List<ModuleInfo>> {
        val remoteResult = remoteSource.fetchAvailableModules()
        if (remoteResult.isSuccess) {
            val remoteModules = remoteResult.getOrDefault(emptyList())
            val installed = localDataSource.getInstalledModules()
            val merged = remoteModules.map { rm ->
                val installedModule = installed.find { it.id == rm.id }
                rm.copy(
                    isInstalled = installedModule != null,
                    installedVersion = installedModule?.version
                )
            }
            cache["available"] = merged
            return Result.success(merged)
        }
        val cached = cache["available"]
        if (cached != null) return Result.success(cached)
        val installed = localDataSource.getInstalledModules()
        if (installed.isNotEmpty()) {
            return Result.success(installed.map { it.toModuleInfo() })
        }
        return remoteResult
    }

    override fun getInstalledModules(): List<InstalledModule> {
        return localDataSource.getInstalledModules()
    }

    override fun isModuleInstalled(moduleId: String): Boolean {
        return localDataSource.isModuleInstalled(moduleId)
    }

    override fun isEnvironmentReady(moduleId: String): Boolean {
        if (!isModuleInstalled(moduleId)) return false
        val version = localDataSource.getActiveVersion(moduleId) ?: return false
        val installDir = localDataSource.getModuleInstallPath(moduleId, version)
        return installDir.exists() && installDir.listFiles()?.isNotEmpty() == true
    }

    override fun installModule(moduleId: String, version: String?): Flow<InstallProgress> = flow {
        val targetVersion = version ?: resolveLatestVersion(moduleId) ?: "1.0.0"
        val installDir = localDataSource.getModuleInstallPath(moduleId, targetVersion)

        val dependencies = resolveDependencies(moduleId)
        for (depId in dependencies) {
            if (!isModuleInstalled(depId)) {
                emit(InstallProgress(moduleId, InstallStatus.DOWNLOADING, 0,
                    message = "Installing dependency: $depId"))
                installModuleInternal(depId, resolveLatestVersion(depId) ?: "1.0.0")
            }
        }

        emit(InstallProgress(moduleId, InstallStatus.DOWNLOADING, 0))

        val downloadUrl = resolveDownloadUrl(moduleId, targetVersion)
        val cacheFile = File(localDataSource.getCacheDir(), "$moduleId-$targetVersion.zip")

        try {
            val moduleInfo = cache["available"]?.find { it.id == moduleId }
            val totalSize = (moduleInfo?.sizeInMB ?: 10) * 1024L * 1024L

            var lastEmittedPercent = 0
            downloadFile(downloadUrl, cacheFile) { bytesRead ->
                val percent = if (totalSize > 0) ((bytesRead * 100) / totalSize).toInt().coerceAtMost(95) else 0
                lastEmittedPercent = percent
            }

            emit(InstallProgress(moduleId, InstallStatus.DOWNLOADING, lastEmittedPercent,
                downloadedBytes = cacheFile.length(), totalBytes = totalSize))

            emit(InstallProgress(moduleId, InstallStatus.EXTRACTING, 95,
                message = "Extracting module..."))

            installDir.mkdirs()
            extractArchive(cacheFile, installDir)

            val validation = ModuleExtractor.validateModuleStructure(installDir)
            if (!validation.isValid) {
                emit(InstallProgress(moduleId, InstallStatus.ERROR, 0,
                    message = "Validation failed: ${validation.errors.joinToString()}"))
                return@flow
            }

            emit(InstallProgress(moduleId, InstallStatus.VERIFYING, 98,
                message = "Verifying integrity..."))

            localDataSource.saveInstalledModule(
                InstalledModule(
                    id = moduleId,
                    version = targetVersion,
                    installDate = System.currentTimeMillis(),
                    sizeInBytes = calculateDirSize(installDir),
                    installPath = installDir.absolutePath
                )
            )
            localDataSource.setActiveVersion(moduleId, targetVersion)

            cacheFile.delete()

            emit(InstallProgress(moduleId, InstallStatus.FINISHED, 100))
        } catch (e: Exception) {
            System.err.println("$TAG: Install failed for $moduleId - ${e.message}")
            installDir.deleteRecursively()
            cacheFile.delete()
            emit(InstallProgress(moduleId, InstallStatus.ERROR, 0,
                message = e.message ?: "Installation failed"))
        }
    }.flowOn(Dispatchers.IO)

    override fun switchModuleVersion(moduleId: String, version: String): Boolean {
        val installDir = localDataSource.getModuleInstallPath(moduleId, version)
        if (!installDir.exists()) return false
        localDataSource.setActiveVersion(moduleId, version)
        return true
    }

    override fun uninstallModule(moduleId: String, version: String?): Boolean {
        val targetVersion = version ?: localDataSource.getActiveVersion(moduleId) ?: return false
        val installDir = localDataSource.getModuleInstallPath(moduleId, targetVersion)
        if (installDir.exists()) {
            installDir.deleteRecursively()
        }
        localDataSource.removeInstalledModule(moduleId, version)
        return true
    }

    override fun getModuleEnvironment(moduleId: String): Map<String, String> {
        val version = localDataSource.getActiveVersion(moduleId) ?: return emptyMap()
        val installDir = localDataSource.getModuleInstallPath(moduleId, version)
        if (!installDir.exists()) return emptyMap()

        val manifest = ModuleExtractor.readManifest(installDir)
        if (manifest != null && manifest.envVars.isNotEmpty()) {
            val currentPath = System.getenv("PATH") ?: ""
            return manifest.resolveEnvVars(installDir.absolutePath, currentPath)
        }

        return mapOf(
            "MODULE_HOME" to installDir.absolutePath,
            "PATH" to "${installDir.absolutePath}/bin:${System.getenv("PATH") ?: ""}"
        )
    }

    private suspend fun installModuleInternal(moduleId: String, version: String) {
        val installDir = localDataSource.getModuleInstallPath(moduleId, version)
        val downloadUrl = resolveDownloadUrl(moduleId, version)
        val cacheFile = File(localDataSource.getCacheDir(), "$moduleId-$version.zip")

        downloadFile(downloadUrl, cacheFile) { /* silent for deps */ }
        installDir.mkdirs()
        extractArchive(cacheFile, installDir)

        localDataSource.saveInstalledModule(
            InstalledModule(
                id = moduleId,
                version = version,
                installDate = System.currentTimeMillis(),
                sizeInBytes = calculateDirSize(installDir),
                installPath = installDir.absolutePath
            )
        )
        localDataSource.setActiveVersion(moduleId, version)
        cacheFile.delete()
    }

    private fun downloadFile(url: String, destFile: File, onProgress: (Long) -> Unit) {
        val maxRetries = 3
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            var response: okhttp3.Response? = null
            try {
                val request = Request.Builder().url(url).build()
                response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Download failed: HTTP ${response.code}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            onProgress(totalBytesRead)
                        }
                    }
                }
                return
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    Thread.sleep((attempt * 1000).toLong())
                }
            } finally {
                response?.close()
            }
        }

        throw lastException ?: Exception("Download failed after $maxRetries attempts")
    }

    private fun extractArchive(archiveFile: File, targetDir: File) {
        val canonicalTargetDir = targetDir.canonicalPath
        val maxEntrySize = 256L * 1024 * 1024

        ZipInputStream(FileInputStream(archiveFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryFile = File(targetDir, entry.name)
                val canonicalEntryPath = entryFile.canonicalPath

                if (!canonicalEntryPath.startsWith(canonicalTargetDir)) {
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }

                if (entry.name.contains("..")) {
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }

                if (entry.size > maxEntrySize) {
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }

                if (entry.isDirectory) {
                    if (!entry.name.startsWith("/") && !entry.name.contains("..")) {
                        entryFile.mkdirs()
                    }
                } else {
                    entryFile.parentFile?.mkdirs()
                    FileOutputStream(entryFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        var totalRead = 0L
                        while (zis.read(buffer).also { len = it } > 0) {
                            totalRead += len
                            if (totalRead > maxEntrySize) break
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun resolveDownloadUrl(moduleId: String, version: String): String {
        val cached = cache["available"]?.find { it.id == moduleId }
        if (cached != null && cached.downloadUrl.isNotEmpty()) {
            return cached.downloadUrl
        }
        return "https://github.com/XION-HN/haisa-des/releases/download/${moduleId}-v${version}/${moduleId}-${version}-aarch64.zip"
    }

    private suspend fun resolveLatestVersion(moduleId: String): String? {
        val cached = cache["available"]?.find { it.id == moduleId }
        return cached?.version
    }

    private fun resolveDependencies(moduleId: String): List<String> {
        val cached = cache["available"]?.find { it.id == moduleId }
        return cached?.dependencies ?: emptyList()
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun InstalledModule.toModuleInfo(): ModuleInfo {
        return ModuleInfo(
            id = id,
            name = id.removePrefix("env-").uppercase(),
            version = version,
            description = "Installed module",
            sizeInMB = (sizeInBytes / 1024 / 1024).toInt(),
            isInstalled = true,
            installedVersion = version
        )
    }
}
