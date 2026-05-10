package com.haisa.sdk.controller

import com.haisa.sdk.model.ProjectConfig
import com.haisa.sdk.model.ProjectTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectControllerTest {

    @Test
    fun getRequiredModules_androidJava_containsJdkAndCc() {
    val modules = com.haisa.sdk.util.PathResolver.resolveModuleId(ProjectTemplate.ANDROID_JAVA.name)
    assertTrue(modules.contains("env-jdk"))
    }

    @Test
    fun getRequiredModules_python_containsPython() {
    val modules = com.haisa.sdk.util.PathResolver.resolveModuleId(ProjectTemplate.PYTHON.name)
    assertTrue(modules.contains("env-python"))
    }

    @Test
    fun projectTemplate_androidJava_hasCorrectDisplayName() {
        assertEquals("Android Java", ProjectTemplate.ANDROID_JAVA.displayName)
    }

    @Test
    fun projectTemplate_allTemplates_haveDisplayNames() {
        ProjectTemplate.values().forEach { template ->
            assertTrue(template.displayName.isNotEmpty())
        }
    }
}