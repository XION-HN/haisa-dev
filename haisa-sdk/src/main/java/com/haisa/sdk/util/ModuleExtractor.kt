package com.haisa.sdk.util

import com.haisa.sdk.model.ModuleManifest
import com.google.gson.Gson
import java.io.File

object ModuleExtractor {

    private val gson = Gson()

    fun readManifest(moduleDir: File): ModuleManifest? {
        val manifestFile = File(moduleDir, "manifest.json")
        if (!manifestFile.exists()) return null
        return try {
            val json = manifestFile.readText()
            gson.fromJson(json, ModuleManifest::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun validateModuleStructure(moduleDir: File): ValidationResult {
        val errors = mutableListOf<String>()
        if (!moduleDir.exists()) {
            errors.add("Module directory does not exist: ${moduleDir.absolutePath}")
        }
        if (!File(moduleDir, "manifest.json").exists()) {
            errors.add("manifest.json is missing")
        }
        return ValidationResult(errors.isEmpty(), errors)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    )
}