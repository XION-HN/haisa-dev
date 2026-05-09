package com.haisa.sdk.data

import android.content.Context
import android.content.SharedPreferences
import com.haisa.sdk.model.InstalledModule
import com.haisa.sdk.model.ModuleInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalDataSource(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("haisa_modules", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val modulesDir = File(context.filesDir, "modules")
    private val metadataDir = File(context.filesDir, ".metadata")

    init {
        modulesDir.mkdirs()
        metadataDir.mkdirs()
    }

    fun getInstalledModules(): List<InstalledModule> {
        val json = prefs.getString(KEY_INSTALLED, null) ?: return emptyList()
        val type = object : TypeToken<List<InstalledModule>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveInstalledModule(module: InstalledModule) {
        val current = getInstalledModules().toMutableList()
        current.removeAll { it.id == module.id && it.version == module.version }
        current.add(module)
        prefs.edit().putString(KEY_INSTALLED, gson.toJson(current)).apply()
    }

    fun removeInstalledModule(moduleId: String, version: String? = null) {
        val current = getInstalledModules().toMutableList()
        if (version != null) {
            current.removeAll { it.id == moduleId && it.version == version }
        } else {
            current.removeAll { it.id == moduleId }
        }
        prefs.edit().putString(KEY_INSTALLED, gson.toJson(current)).apply()
    }

    fun isModuleInstalled(moduleId: String): Boolean {
        return getInstalledModules().any { it.id == moduleId }
    }

    fun getModuleInstallPath(moduleId: String, version: String): File {
        return File(modulesDir, "$moduleId/$version")
    }

    fun getActiveVersion(moduleId: String): String? {
        return prefs.getString("active_version_$moduleId", null)
    }

    fun setActiveVersion(moduleId: String, version: String) {
        prefs.edit().putString("active_version_$moduleId", version).apply()
    }

    suspend fun getModuleInstallDir(moduleId: String): File? = withContext(Dispatchers.IO) {
        val version = getActiveVersion(moduleId) ?: return@withContext null
        val dir = getModuleInstallPath(moduleId, version)
        if (dir.exists()) dir else null
    }

    fun getAvailableSpaceBytes(): Long {
        return modulesDir.freeSpace
    }

    fun getCacheDir(): File {
        val cache = File(modulesDir, "../tmp")
        cache.mkdirs()
        return cache
    }

    fun clearModuleCache(moduleId: String) {
        val cacheDir = getCacheDir()
        cacheDir.listFiles()?.filter { it.name.startsWith(moduleId) }?.forEach { it.delete() }
    }

    companion object {
        private const val KEY_INSTALLED = "installed_modules"
    }
}