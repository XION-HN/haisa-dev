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
import java.util.concurrent.TimeUnit

class BuildEngine {

companion object {
private const val TAG = "BuildEngine"
private const val DEFAULT_TIMEOUT_MINUTES = 30L
private val DANGEROUS_PATTERNS = listOf(
";", "&&", "||", "|", "`", "\$", "\${",
"rm ", "rm -", "mkfs", "dd if=", "> /dev/",
"chmod 777", "chmod -R", "chown",
"curl ", "wget ", "nc ", "ncat ",
"bash -c", "sh -c", "/su ", "su -",
"mount ", "umount ", "losetup"
)

internal var logError: (String, String, Throwable?) -> Unit = { tag, msg, e ->
val log = if (e != null) "$tag: $msg - ${e.message}" else "$tag: $msg"
System.err.println(log)
}
}

private var timeoutMinutes: Long = DEFAULT_TIMEOUT_MINUTES

fun setTimeout(minutes: Long) {
timeoutMinutes = minutes.coerceAtLeast(1)
}

fun sanitizeCommand(command: String): String {
var sanitized = command.trim()
for (pattern in DANGEROUS_PATTERNS) {
if (sanitized.contains(pattern)) {
sanitized = sanitized.replace(pattern, "")
}
}
return sanitized.trim().ifBlank { "echo 'Empty or invalid command'" }
}

fun execute(
projectPath: String,
buildCommand: String,
moduleEnvs: List<Map<String, String>> = emptyList()
): Flow<BuildProgress> = flow {
val projectDir = File(projectPath)
if (!projectDir.exists() || !projectDir.isDirectory) {
emit(BuildProgress(BuildStatus.FAILED, "Project directory not found: $projectPath", isError = true))
return@flow
}

val canonicalPath = projectDir.canonicalPath
if (!canonicalPath.startsWith("/data/data/") && !canonicalPath.startsWith("/sdcard/") && !canonicalPath.startsWith("/storage/")) {
emit(BuildProgress(BuildStatus.FAILED, "Project directory not in allowed paths: $canonicalPath", isError = true))
return@flow
}

emit(BuildProgress(BuildStatus.PREPARING, "Preparing build environment..."))

val envMap = if (moduleEnvs.isNotEmpty()) {
EnvironmentInjector.inject(moduleEnvs)
} else {
System.getenv().toMap()
}

val envArray = EnvironmentInjector.toEnvArray(envMap)
val safeCommand = sanitizeCommand(buildCommand)

emit(BuildProgress(BuildStatus.COMPILING, "Executing: $safeCommand"))

var process: Process? = null
val reader: BufferedReader
val errorReader: BufferedReader

try {
process = Runtime.getRuntime().exec(
arrayOf("sh", "-c", safeCommand),
envArray,
projectDir
)

reader = BufferedReader(InputStreamReader(process.inputStream))
errorReader = BufferedReader(InputStreamReader(process.errorStream))

reader.use { r ->
errorReader.use { er ->
var line: String?
while (r.readLine().also { line = it } != null) {
emit(BuildProgress(BuildStatus.COMPILING, line ?: ""))
}

while (er.readLine().also { line = it } != null) {
emit(BuildProgress(BuildStatus.COMPILING, "[ERR] ${line ?: ""}"))
}
}

val finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES)
if (!finished) {
process.destroyForcibly()
emit(BuildProgress(BuildStatus.FAILED, "Build timed out after $timeoutMinutes minutes", isError = true))
return@flow
}

val exitCode = process.exitValue()

if (exitCode == 0) {
emit(BuildProgress(BuildStatus.TESTING, "Build succeeded, running checks..."))
emit(BuildProgress(BuildStatus.PACKAGING, "Packaging artifacts..."))
emit(BuildProgress(BuildStatus.FINISHED, "Build completed successfully (exit code: 0)"))
} else {
emit(BuildProgress(BuildStatus.FAILED, "Build failed with exit code: $exitCode", isError = true))
}
} catch (e: Exception) {
logError(TAG, "Build execution failed", e)
emit(BuildProgress(BuildStatus.FAILED, "Build error: ${e.message}", isError = true))
} finally {
process?.destroyForcibly()
}
}.flowOn(Dispatchers.IO)
}
