package com.haisa.sdk.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentInjectorTest {

    @Test
    fun inject_mergesMultipleModuleEnvs() {
        val env1 = mapOf("JAVA_HOME" to "/modules/env-jdk/17", "PATH" to "/modules/env-jdk/bin")
        val env2 = mapOf("PYTHON_HOME" to "/modules/env-python/3.11", "PATH" to "/modules/env-python/bin")
        val currentEnv = mapOf("PATH" to "/system/bin", "HOME" to "/data")

        val result = EnvironmentInjector.inject(listOf(env1, env2), currentEnv)

        assertEquals("/modules/env-jdk/bin:/modules/env-python/bin:/system/bin", result["PATH"])
        assertEquals("/modules/env-jdk/17", result["JAVA_HOME"])
        assertEquals("/modules/env-python/3.11", result["PYTHON_HOME"])
        assertEquals("/data", result["HOME"])
    }

    @Test
    fun inject_handlesEmptyModuleEnvs() {
        val currentEnv = mapOf("PATH" to "/system/bin")
        val result = EnvironmentInjector.inject(emptyList(), currentEnv)

        assertEquals("/system/bin", result["PATH"])
    }

    @Test
    fun inject_overridesNonPathVars() {
        val env1 = mapOf("JAVA_HOME" to "/jdk-17")
        val env2 = mapOf("JAVA_HOME" to "/jdk-21")
        val currentEnv = mapOf("JAVA_HOME" to "/system/jdk")

        val result = EnvironmentInjector.inject(listOf(env1, env2), currentEnv)

        assertEquals("/jdk-21", result["JAVA_HOME"])
    }

    @Test
    fun inject_concatenatesLdLibraryPath() {
        val env1 = mapOf("LD_LIBRARY_PATH" to "/modules/env-jdk/lib")
        val env2 = mapOf("LD_LIBRARY_PATH" to "/modules/env-cc/lib")
        val currentEnv = mapOf("LD_LIBRARY_PATH" to "/system/lib")

        val result = EnvironmentInjector.inject(listOf(env1, env2), currentEnv)

        assertEquals("/modules/env-jdk/lib:/modules/env-cc/lib:/system/lib", result["LD_LIBRARY_PATH"])
    }

    @Test
    fun toEnvArray_convertsCorrectly() {
        val envMap = mapOf("HOME" to "/data", "PATH" to "/bin")
        val array = EnvironmentInjector.toEnvArray(envMap)

        assertEquals(2, array.size)
        assertTrue(array.any { it == "HOME=/data" })
        assertTrue(array.any { it == "PATH=/bin" })
    }

    @Test
    fun toEnvArray_handlesEmptyMap() {
        val array = EnvironmentInjector.toEnvArray(emptyMap())
        assertTrue(array.isEmpty())
    }
}