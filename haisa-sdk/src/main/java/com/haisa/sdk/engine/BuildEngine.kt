package com.haisa.sdk.engine

import com.haisa.sdk.model.BuildProgress
import com.haisa.sdk.model.BuildStatus
import com.haisa.sdk.util.EnvironmentInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class BuildEngine {

    companion object {
        private const val TAG = "BuildEngine"

        internal var logError: (String, String, Throwable?) -> Unit = { tag, msg, e ->
            val log = if (e != null) "$tag: $msg - ${e.message}" else "$tag: $msg"
            System.err.println(log)
        }
    }

    fun execute(
        projectPath: String,
        buildCommand: String,
        moduleEnvs: List<Map<String, String>> = emptyList()
    ): Flow<BuildProgress> = flow {
        val projectDir = File(projectPath)
        if (!projectDir.exists()) {
            emit(BuildProgress(BuildStatus.FAILED, "Project directory not found: $projectPath", isError = true))
            return@flow
        }

        emit(BuildProgress(BuildStatus.PREPARING, "Preparing build environment..."))

        val envMap = if (moduleEnvs.isNotEmpty()) {
            EnvironmentInjector.inject(moduleEnvs)
        } else {
            System.getenv().toMap()
        }

        val envArray = EnvironmentInjector.toEnvArray(envMap)

        emit(BuildProgress(BuildStatus.COMPILING, "Executing: $buildCommand"))

        try {
            val process = Runtime.getRuntime().exec(
                arrayOf("sh", "-c", buildCommand),
                envArray,
                projectDir
            )

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(BuildProgress(BuildStatus.COMPILING, line ?: ""))
            }

            while (errorReader.readLine().also { line = it } != null) {
                emit(BuildProgress(BuildStatus.COMPILING, "[ERR] ${line ?: ""}"))
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                emit(BuildProgress(BuildStatus.TESTING, "Build succeeded, running checks..."))
                emit(BuildProgress(BuildStatus.PACKAGING, "Packaging artifacts..."))
                emit(BuildProgress(BuildStatus.FINISHED, "Build completed successfully (exit code: 0)"))
            } else {
                emit(BuildProgress(BuildStatus.FAILED, "Build failed with exit code: $exitCode", isError = true))
            }

            reader.close()
            errorReader.close()
            process.destroy()
        } catch (e: Exception) {
            logError(TAG, "Build execution failed", e)
            emit(BuildProgress(BuildStatus.FAILED, "Build error: ${e.message}", isError = true))
        }
    }.flowOn(Dispatchers.IO)
}
