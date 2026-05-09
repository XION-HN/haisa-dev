package com.haisa.sdk.controller

import com.haisa.sdk.HaisaEnvironment
import com.haisa.sdk.model.BuildProgress
import kotlinx.coroutines.flow.Flow

class BuildController(private val haisa: HaisaEnvironment) {

    fun executeBuild(
        projectPath: String,
        buildCommand: String,
        moduleIds: List<String> = emptyList()
    ): Flow<BuildProgress> {
        return haisa.executeBuild(projectPath, buildCommand, moduleIds)
    }

    fun prepareEnvironment(moduleIds: List<String>): Map<String, String> {
        return haisa.injectEnvironment(moduleIds)
    }
}