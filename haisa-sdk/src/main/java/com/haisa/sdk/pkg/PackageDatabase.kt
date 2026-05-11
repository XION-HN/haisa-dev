package com.haisa.sdk.pkg

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class PackageDatabase(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("haisa_pkg_db", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val baseDir = File(context.filesDir, "packages")
    private val infoDir = File(baseDir, "info")
    private val listDir = File(baseDir, "list")

    init {
        baseDir.mkdirs()
        infoDir.mkdirs()
        listDir.mkdirs()
    }

    fun getInstalledPackage(pkgId: String): InstalledPackage? {
        val json = prefs.getString("pkg_$pkgId", null) ?: return null
        return try { gson.fromJson(json, InstalledPackage::class.java) } catch (_: Exception) { null }
    }

    fun getAllInstalledPackages(): List<InstalledPackage> {
        val keys = prefs.all.keys.filter { it.startsWith("pkg_") }
        return keys.mapNotNull { key ->
            val json = prefs.getString(key, null) ?: return@mapNotNull null
            try { gson.fromJson(json, InstalledPackage::class.java) } catch (_: Exception) { null }
        }
    }

    fun saveInstalledPackage(pkg: InstalledPackage) {
        prefs.edit().putString("pkg_${pkg.pkgId}", gson.toJson(pkg)).apply()
        saveFileList(pkg.pkgId, pkg.installPath)
    }

    fun removeInstalledPackage(pkgId: String) {
        prefs.edit().remove("pkg_$pkgId").apply()
        prefs.edit().remove("auto_$pkgId").apply()
        deleteFileList(pkgId)
    }

    fun isAutoInstalled(pkgId: String): Boolean {
        return prefs.getBoolean("auto_$pkgId", false)
    }

    fun setAutoInstalled(pkgId: String, auto: Boolean) {
        prefs.edit().putBoolean("auto_$pkgId", auto).apply()
    }

    fun isPackageInstalled(pkgId: String): Boolean {
        return prefs.contains("pkg_$pkgId")
    }

    fun getActiveVersion(pkgId: String): String? {
        return getInstalledPackage(pkgId)?.version
    }

    fun getInstallPath(pkgId: String, version: String): File {
        return File(baseDir, "../modules/$pkgId/$version")
    }

    fun getCacheDir(): File {
        val cache = File(baseDir, "../tmp")
        cache.mkdirs()
        return cache
    }

    fun getAvailableSpaceBytes(): Long {
        return baseDir.freeSpace
    }

    fun recordFiles(pkgId: String, files: List<String>) {
        val listFile = File(listDir, "$pkgId.list")
        listFile.writeText(files.joinToString("\n"))
    }

    fun getRecordedFiles(pkgId: String): List<String> {
        val listFile = File(listDir, "$pkgId.list")
        if (!listFile.exists()) return emptyList()
        return listFile.readLines()
    }

    fun savePackageInfo(pkgId: String, info: PackageInfo) {
        val infoFile = File(infoDir, "$pkgId.json")
        infoFile.writeText(gson.toJson(info))
    }

    fun getPackageInfo(pkgId: String): PackageInfo? {
        val infoFile = File(infoDir, "$pkgId.json")
        if (!infoFile.exists()) return null
        return try { gson.fromJson(infoFile.readText(), PackageInfo::class.java) } catch (_: Exception) { null }
    }

    private fun saveFileList(pkgId: String, installPath: String) {
        val dir = File(installPath)
        if (!dir.exists()) return
        val files = dir.walkTopDown().filter { it.isFile }.map { it.absolutePath }.toList()
        recordFiles(pkgId, files)
    }

    private fun deleteFileList(pkgId: String) {
        File(listDir, "$pkgId.list").delete()
        File(infoDir, "$pkgId.json").delete()
    }
}
