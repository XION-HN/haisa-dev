package com.haisa.sdk.controller

import com.haisa.sdk.HaisaEnvironment
import com.haisa.sdk.model.ProjectConfig
import com.haisa.sdk.model.ProjectTemplate

class ProjectController(private val haisa: HaisaEnvironment) {

    suspend fun createProject(
        projectName: String,
        template: ProjectTemplate,
        outputDir: String
    ): Result<ProjectConfig> {
        return haisa.createProject(projectName, template, outputDir)
    }

    fun getRequiredModules(template: ProjectTemplate): List<String> {
        return com.haisa.sdk.util.PathResolver.resolveModuleId(template.name)
    }
}