package com.haisa.sdk.pkg

import android.content.Context
import com.haisa.sdk.model.InstallProgress
import com.haisa.sdk.model.InstallStatus
import com.haisa.sdk.network.GitHubReleasesSource
import com.haisa.sdk.pkg.DependencyResolver
import com.haisa.sdk.util.EnvironmentInjector
import com.haisa.sdk.util.ModuleExtractor
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

class PackageManager(context: Context) {

    private val appContext: Context = context.applicationContext
    private val db = PackageDatabase(appContext)
    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var indexCache: PackageIndex? = null

    fun updateIndex(packages: List<PackageInfo>): Int {
        indexCache = PackageIndex(
            version = "1.0",
            lastUpdated = java.time.Instant.now().toString(),
            packages = packages
        )
        return packages.size
    }

    fun search(query: String): PackageSearchResult {
        val pkgs = indexCache?.packages ?: emptyList()
        val q = query.lowercase()
        val matches = pkgs.filter { pkg ->
            pkg.pkgId.lowercase().contains(q) ||
            pkg.name.lowercase().contains(q) ||
            pkg.description.lowercase().contains(q)
        }
        return PackageSearchResult(query, matches, pkgs.size)
    }

    fun show(pkgId: String): PackageInfo? {
        return indexCache?.packages?.find { it.pkgId == pkgId }
            ?: db.getPackageInfo(pkgId)
    }

    fun listInstalled(): List<InstalledPackage> {
        return db.getAllInstalledPackages()
    }

    fun listAvailable(): List<PackageInfo> {
        return indexCache?.packages ?: emptyList()
    }

    fun isInstalled(pkgId: String): Boolean {
        return db.isPackageInstalled(pkgId)
    }

    fun install(pkgId: String, version: String? = null): Flow<InstallProgress> = flow {
        val targetPkg = resolvePackageInfo(pkgId, version)
        if (targetPkg == null) {
            emit(InstallProgress(pkgId, InstallStatus.ERROR, 0, message = "Package not found: $pkgId"))
            return@flow
        }

        val available = indexCache?.packages?.associateBy { it.pkgId } ?: emptyMap()
        val installed = db.getAllInstalledPackages().associateBy { it.pkgId }

        val graphResult = DependencyResolver.resolve(pkgId, available, installed)
        if (graphResult.isFailure) {
            emit(InstallProgress(pkgId, InstallStatus.ERROR, 0, message = graphResult.exceptionOrNull()?.message ?: "Dependency resolution failed"))
            return@flow
        }

        val graph = graphResult.getOrThrow()
        val toInstall = if (graph.installOrder.contains(pkgId)) graph.installOrder else graph.installOrder + pkgId

        for (depId in toInstall) {
            if (db.isPackageInstalled(depId)) continue
            val depPkg = available[depId] ?: continue
            val isAuto = depId != pkgId

            emit(InstallProgress(depId, InstallStatus.DOWNLOADING, 0, message = "Installing dependency: $depId"))

            val installDir = db.getInstallPath(depId, depPkg.version)
            val cacheFile = File(db.getCacheDir(), "${depId}-${depPkg.version}.zip")

            try {
                downloadPackage(depPkg, cacheFile) { bytesRead ->
                    val pct = if (depPkg.installSizeKb > 0) ((bytesRead * 100) / (depPkg.installSizeKb * 1024)).toInt().coerceAtMost(95) else 0
                }

                emit(InstallProgress(depId, InstallStatus.EXTRACTING, 95, message = "Extracting $depId..."))
                installDir.mkdirs()
                extractArchive(cacheFile, installDir)

                emit(InstallProgress(depId, InstallStatus.VERIFYING, 98, message = "Verifying $depId..."))
                val validation = ModuleExtractor.validateModuleStructure(installDir)
                if (!validation.isValid) {
                    emit(InstallProgress(depId, InstallStatus.ERROR, 0, message = "Validation failed: ${validation.errors.joinToString()}"))
                    return@flow
                }

            val installedPkg = InstalledPackage(
                pkgId = depId,
                version = depPkg.version,
                arch = depPkg.arch,
                installDate = System.currentTimeMillis(),
                installSizeKb = calculateDirSizeKb(installDir),
                installPath = installDir.absolutePath,
                dependencies = depPkg.dependencies
            )
                db.saveInstalledPackage(installedPkg)
                db.savePackageInfo(depId, depPkg)
                db.setAutoInstalled(depId, isAuto)

                cacheFile.delete()
            } catch (e: Exception) {
                installDir.deleteRecursively()
                cacheFile.delete()
                emit(InstallProgress(depId, InstallStatus.ERROR, 0, message = "Install failed: ${e.message}"))
                return@flow
            }
        }

        emit(InstallProgress(pkgId, InstallStatus.FINISHED, 100, message = "$pkgId ${targetPkg.version} installed successfully"))
    }.flowOn(Dispatchers.IO)

    fun remove(pkgId: String, purge: Boolean = false): Result<String> {
        if (!db.isPackageInstalled(pkgId)) {
            return Result.failure(Exception("Package not installed: $pkgId"))
        }

        val allInstalled = db.getAllInstalledPackages().associateBy { it.pkgId }
        val reverseDeps = allInstalled.keys.filter { otherId ->
            otherId != pkgId && isDependedOn(pkgId, otherId, allInstalled)
        }

        if (reverseDeps.isNotEmpty()) {
            return Result.failure(Exception("Cannot remove $pkgId: required by ${reverseDeps.joinToString()}"))
        }

        val installed = db.getInstalledPackage(pkgId) ?: return Result.failure(Exception("Package record not found: $pkgId"))
        val installDir = File(installed.installPath)
        if (installDir.exists()) {
            installDir.deleteRecursively()
        }

        db.removeInstalledPackage(pkgId)

        val msg = if (purge) "Purged $pkgId" else "Removed $pkgId"
        return Result.success(msg)
    }

    fun autoremove(): List<String> {
        val installed = db.getAllInstalledPackages().associateBy { it.pkgId }
        val autoFlags = installed.keys.associateWith { db.isAutoInstalled(it) }
        val orphans = DependencyResolver.findOrphans(installed, autoFlags)

        for (pkgId in orphans) {
            remove(pkgId, purge = true)
        }
        return orphans
    }

    fun getEnvironment(pkgId: String): Map<String, String> {
        val installed = db.getInstalledPackage(pkgId) ?: return emptyMap()
        val installDir = File(installed.installPath)
        if (!installDir.exists()) return emptyMap()

        val pkgInfo = db.getPackageInfo(pkgId)
        if (pkgInfo != null && pkgInfo.envVars.isNotEmpty()) {
            val currentPath = System.getenv("PATH") ?: ""
            return pkgInfo.envVars.mapValues { (_, value) ->
                value.replace("{{install_dir}}", installDir.absolutePath)
                    .replace("{{old_path}}", currentPath)
            }
        }

        return mapOf(
            "MODULE_HOME" to installDir.absolutePath,
            "PATH" to "${installDir.absolutePath}/bin:${System.getenv("PATH") ?: ""}"
        )
    }

    fun injectEnvironments(pkgIds: List<String>): Map<String, String> {
        val envs = pkgIds.map { getEnvironment(it) }
        return EnvironmentInjector.inject(envs)
    }

    fun getEntryBinaries(pkgId: String): Map<String, String> {
        val pkgInfo = db.getPackageInfo(pkgId) ?: return emptyMap()
        val installed = db.getInstalledPackage(pkgId) ?: return emptyMap()
        val installDir = File(installed.installPath)
        return pkgInfo.entryBinaries.mapValues { (_, relPath) ->
            "${installDir.absolutePath}/$relPath"
        }
    }

    fun getInstalledVersion(pkgId: String): String? {
        return db.getInstalledPackage(pkgId)?.version
    }

    fun getIdeIntegrations(pkgId: String): List<PackageInfo.IdeIntegration> {
        return db.getPackageInfo(pkgId)?.ideIntegrations ?: emptyList()
    }

    private fun resolvePackageInfo(pkgId: String, version: String?): PackageInfo? {
        val available = indexCache?.packages ?: emptyList()
        val pkg = available.find { it.pkgId == pkgId } ?: return null
        if (version != null && pkg.version != version) return null
        return pkg
    }

    private fun isDependedOn(targetId: String, otherId: String, installed: Map<String, InstalledPackage>): Boolean {
        val otherInfo = db.getPackageInfo(otherId) ?: return false
        return otherInfo.dependencies.any { dep ->
            val (depId, _) = parseSimpleDependency(dep)
            depId == targetId
        }
    }

    private fun parseSimpleDependency(dep: String): Pair<String, String?> {
        val operators = listOf(">=", "<=", ">", "<", "=")
        for (op in operators) {
            val idx = dep.indexOf(op)
            if (idx >= 0) {
                return dep.substring(0, idx).trim() to dep.substring(idx).trim()
            }
        }
        return dep.trim() to null
    }

    private fun downloadPackage(pkg: PackageInfo, destFile: File, onProgress: (Long) -> Unit) {
        val url = pkg.downloadUrl.ifEmpty {
            "https://github.com/XION-HN/haisa-des/releases/download/${pkg.pkgId}-v${pkg.version}/${pkg.pkgId}-${pkg.version}-${pkg.arch}.zip"
        }
        val maxRetries = 3
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            var response: okhttp3.Response? = null
            try {
                val request = Request.Builder().url(url).build()
                response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                val body = response.body ?: throw Exception("Empty body")
                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            onProgress(totalRead)
                        }
                    }
                }

                if (pkg.sha256.isNotEmpty() && !verifyChecksum(destFile, pkg.sha256)) {
                    destFile.delete()
                    throw Exception("SHA-256 checksum mismatch")
                }
                return
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) Thread.sleep((attempt * 1000).toLong())
            } finally {
                response?.close()
            }
        }
        throw lastException ?: Exception("Download failed after $maxRetries attempts")
    }

    private fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(file.readBytes())
        return hash.joinToString("") { "%02x".format(it) } == expectedSha256.lowercase()
    }

    private fun extractArchive(archiveFile: File, targetDir: File) {
        val canonicalTarget = targetDir.canonicalPath
        val maxEntrySize = 256L * 1024 * 1024

        ZipInputStream(FileInputStream(archiveFile)).use { zis ->
            var entry = zis.nextEntry
            val recordedFiles = mutableListOf<String>()
            while (entry != null) {
                val entryFile = File(targetDir, entry.name)
                if (!entryFile.canonicalPath.startsWith(canonicalTarget) || entry.name.contains("..")) {
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
                    entryFile.mkdirs()
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
                    recordedFiles.add(entryFile.absolutePath)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            if (recordedFiles.isNotEmpty()) {
                db.recordFiles(archiveFile.nameWithoutExtension.substringBefore("-"), recordedFiles)
            }
        }
    }

    private fun calculateDirSizeKb(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1024
    }
}
