package com.haisa.sdk.util

import java.util.Locale

object VersionManager {

    data class SemanticVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preRelease: String? = null,
        val buildMetadata: String? = null
    ) : Comparable<SemanticVersion> {

        val versionCode: Int
            get() = major * 10000 + minor * 100 + patch

        val versionName: String
            get() = buildString {
                append("$major.$minor.$patch")
                preRelease?.let { append("-$it") }
                buildMetadata?.let { append("+$it") }
            }

        val isPreRelease: Boolean
            get() = preRelease != null

        val isStable: Boolean
            get() = preRelease == null

        override fun compareTo(other: SemanticVersion): Int {
            val majorCmp = major.compareTo(other.major)
            if (majorCmp != 0) return majorCmp
            val minorCmp = minor.compareTo(other.minor)
            if (minorCmp != 0) return minorCmp
            val patchCmp = patch.compareTo(other.patch)
            if (patchCmp != 0) return patchCmp

            if (preRelease == null && other.preRelease == null) return 0
            if (preRelease != null && other.preRelease == null) return -1
            if (preRelease == null && other.preRelease != null) return 1

            return comparePreRelease(preRelease!!, other.preRelease!!)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SemanticVersion) return false
            return major == other.major && minor == other.minor && patch == other.patch &&
                    preRelease == other.preRelease
        }

        override fun hashCode(): Int {
            var result = major
            result = 31 * result + minor
            result = 31 * result + patch
            result = 31 * result + (preRelease?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String = versionName

        fun nextPatch(): SemanticVersion = copy(patch = patch + 1, preRelease = null, buildMetadata = null)
        fun nextMinor(): SemanticVersion = copy(minor = minor + 1, patch = 0, preRelease = null, buildMetadata = null)
        fun nextMajor(): SemanticVersion = copy(major = major + 1, minor = 0, patch = 0, preRelease = null, buildMetadata = null)

        fun withPreRelease(label: String): SemanticVersion = copy(preRelease = label, buildMetadata = null)
        fun withBuildMetadata(label: String): SemanticVersion = copy(buildMetadata = label)

        companion object {
            val ZERO = SemanticVersion(0, 0, 0)

            fun parse(version: String): SemanticVersion? {
                val regex = Regex(
                    """^(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z0-9.\-]+))?(?:\+([a-zA-Z0-9.\-]+))?$"""
                )
                val match = regex.matchEntire(version.trim()) ?: return null

                return try {
                    SemanticVersion(
                        major = match.groupValues[1].toInt(),
                        minor = match.groupValues[2].toInt(),
                        patch = match.groupValues[3].toInt(),
                        preRelease = match.groupValues[4].takeIf { it.isNotEmpty() },
                        buildMetadata = match.groupValues[5].takeIf { it.isNotEmpty() }
                    )
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
    }

    enum class BumpType {
        MAJOR, MINOR, PATCH
    }

    fun parse(version: String): SemanticVersion? = SemanticVersion.parse(version)

    fun isCompatible(current: SemanticVersion, required: SemanticVersion): Boolean {
        if (current.major != required.major) return false
        if (current.major == 0) {
            return current.minor == required.minor && current.patch >= required.patch
        }
        return current.minor > required.minor ||
                (current.minor == required.minor && current.patch >= required.patch)
    }

    fun satisfiesRequirement(version: SemanticVersion, requirement: String): Boolean {
        val regex = Regex("""^(>=|>|<=|<|=)?\s*(\d+\.\d+\.\d+)(?:-([a-zA-Z0-9.\-]+))?$""")
        val match = regex.matchEntire(requirement.trim()) ?: return false

        val operator = match.groupValues[1].ifEmpty { ">=" }
        val requiredVersion = parse(match.groupValues[2]) ?: return false

        val cmp = version.compareTo(requiredVersion)
        return when (operator) {
            ">=" -> cmp >= 0
            ">" -> cmp > 0
            "<=" -> cmp <= 0
            "<" -> cmp < 0
            "=" -> cmp == 0
            else -> false
        }
    }

    fun findLatestVersion(versions: List<SemanticVersion>): SemanticVersion? {
        return versions.filter { it.isStable }.maxOrNull()
    }

    fun findLatestPreRelease(versions: List<SemanticVersion>): SemanticVersion? {
        return versions.maxOrNull()
    }

    fun bump(version: SemanticVersion, type: BumpType): SemanticVersion {
        return when (type) {
            BumpType.MAJOR -> version.nextMajor()
            BumpType.MINOR -> version.nextMinor()
            BumpType.PATCH -> version.nextPatch()
        }
    }

    fun comparePreRelease(a: String, b: String): Int {
        val partsA = a.split(".")
        val partsB = b.split(".")

        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val partA = partsA.getOrNull(i) ?: return -1
            val partB = partsB.getOrNull(i) ?: return 1

            val numA = partA.toIntOrNull()
            val numB = partB.toIntOrNull()

            val cmp = when {
                numA != null && numB != null -> numA.compareTo(numB)
                numA != null -> -1
                numB != null -> 1
                else -> partA.compareTo(partB, ignoreCase = true)
            }

            if (cmp != 0) return cmp
        }

        return 0
    }
}