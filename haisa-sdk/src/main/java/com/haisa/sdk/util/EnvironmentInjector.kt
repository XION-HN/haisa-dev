package com.haisa.sdk.util

object EnvironmentInjector {

    fun inject(
        moduleEnvs: List<Map<String, String>>,
        currentEnv: Map<String, String> = System.getenv()
    ): Map<String, String> {
        val merged = currentEnv.toMutableMap()

        for (env in moduleEnvs) {
            for ((key, value) in env) {
                when {
                    key == "PATH" -> {
                        val existingPath = merged["PATH"] ?: ""
                        merged["PATH"] = "$value:$existingPath"
                    }
                    key.startsWith("LD_") -> {
                        val existing = merged[key] ?: ""
                        merged[key] = if (existing.isNotEmpty()) "$value:$existing" else value
                    }
                    else -> {
                        merged[key] = value
                    }
                }
            }
        }

        return merged
    }

    fun toEnvArray(envMap: Map<String, String>): Array<String> {
        return envMap.map { (key, value) -> "$key=$value" }.toTypedArray()
    }
}