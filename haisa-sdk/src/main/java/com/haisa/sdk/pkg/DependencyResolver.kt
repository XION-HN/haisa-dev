package com.haisa.sdk.pkg

import com.haisa.sdk.util.VersionManager.SemanticVersion

object DependencyResolver {

    fun resolve(
        targetPkgId: String,
        availablePackages: Map<String, PackageInfo>,
        installedPackages: Map<String, InstalledPackage>
    ): Result<DependencyGraph> {
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val nodes = mutableMapOf<String, PackageInfo>()
        val edges = mutableMapOf<String, MutableList<String>>()
        val installOrder = mutableListOf<String>()

        fun visit(pkgId: String, constraint: String? = null): Boolean {
            if (pkgId in visited) return true
            if (pkgId in visiting) return false

            visiting.add(pkgId)

            val installed = installedPackages[pkgId]
            if (installed != null && constraint == null) {
                visited.add(pkgId)
                visiting.remove(pkgId)
                return true
            }

            val available = availablePackages[pkgId]
            if (available == null && installed == null) {
                return false
            }

            val pkg = available ?: return true

            if (constraint != null && installed != null) {
                val installedVer = SemanticVersion.parse(installed.version)
                val requiredVer = SemanticVersion.parse(constraint.trimStart('>', '=', ' ', '<'))
                if (installedVer != null && requiredVer != null) {
                    if (installedVer < requiredVer) {
                        if (available == null) return false
                    }
                }
            }

            nodes[pkgId] = pkg
            edges[pkgId] = mutableListOf()

            for (dep in pkg.dependencies) {
                val (depId, depConstraint) = parseDependency(dep)
                edges[pkgId]!!.add(depId)
                if (!visit(depId, depConstraint)) {
                    return false
                }
            }

            visiting.remove(pkgId)
            visited.add(pkgId)
            installOrder.add(pkgId)
            return true
        }

        if (!visit(targetPkgId)) {
            return Result.failure(Exception("Dependency resolution failed: cycle or missing package detected for $targetPkgId"))
        }

        val needed = installOrder.filter { pkgId ->
            installedPackages[pkgId] == null
        }

        return Result.success(DependencyGraph(nodes, edges, needed))
    }

    fun findOrphans(
        installedPackages: Map<String, InstalledPackage>,
        autoInstalledFlags: Map<String, Boolean>
    ): List<String> {
        val explicitlyInstalled = installedPackages.keys.filter {
            autoInstalledFlags[it] != true
        }.toSet()

        val allRequired = mutableSetOf<String>()
        for (pkgId in explicitlyInstalled) {
            collectTransitiveDeps(pkgId, installedPackages, allRequired)
        }

        return installedPackages.keys.filter { pkgId ->
            pkgId !in explicitlyInstalled && pkgId !in allRequired && autoInstalledFlags[pkgId] == true
        }
    }

    private fun collectTransitiveDeps(
        pkgId: String,
        installedPackages: Map<String, InstalledPackage>,
        result: MutableSet<String>
    ) {
        if (pkgId in result) return
        result.add(pkgId)
        val pkg = installedPackages[pkgId] ?: return
        val availablePackages = mutableMapOf<String, PackageInfo>()
        for (dep in listOf<String>()) {
            val (depId, _) = parseDependency(dep)
            if (depId in installedPackages) {
                collectTransitiveDeps(depId, installedPackages, result)
            }
        }
    }

    private fun parseDependency(dep: String): Pair<String, String?> {
        val operators = listOf(">=", "<=", ">", "<", "=")
        for (op in operators) {
            val idx = dep.indexOf(op)
            if (idx >= 0) {
                val pkgId = dep.substring(0, idx).trim()
                val constraint = dep.substring(idx).trim()
                return pkgId to constraint
            }
        }
        return dep.trim() to null
    }
}
