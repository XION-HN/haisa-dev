package com.haisa.sdk.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionManagerTest {

    private val vm = VersionManager

    @Test
    fun parse_validSimpleVersion() {
        val v = vm.parse("1.2.3")!!
        assertEquals(1, v.major)
        assertEquals(2, v.minor)
        assertEquals(3, v.patch)
        assertNull(v.preRelease)
        assertNull(v.buildMetadata)
        assertEquals("1.2.3", v.versionName)
    }

    @Test
    fun parse_versionWithPreRelease() {
        val v = vm.parse("1.0.0-alpha")!!
        assertEquals(1, v.major)
        assertEquals("alpha", v.preRelease)
        assertTrue(v.isPreRelease)
        assertFalse(v.isStable)
    }

    @Test
    fun parse_versionWithPreReleaseAndBuildMetadata() {
        val v = vm.parse("2.1.0-beta.1+build.123")!!
        assertEquals(2, v.major)
        assertEquals(1, v.minor)
        assertEquals(0, v.patch)
        assertEquals("beta.1", v.preRelease)
        assertEquals("build.123", v.buildMetadata)
    }

    @Test
    fun parse_versionWithBuildMetadataOnly() {
        val v = vm.parse("1.0.0+build.456")!!
        assertEquals("1.0.0+build.456", v.versionName)
        assertNull(v.preRelease)
        assertEquals("build.456", v.buildMetadata)
    }

    @Test
    fun parse_zeroVersion() {
        val v = vm.parse("0.0.0")!!
        assertEquals(0, v.major)
        assertEquals(0, v.minor)
        assertEquals(0, v.patch)
    }

    @Test
    fun parse_invalidVersions_returnNull() {
        assertNull(vm.parse(""))
        assertNull(vm.parse("1"))
        assertNull(vm.parse("1.2"))
        assertNull(vm.parse("1.2.3.4"))
        assertNull(vm.parse("a.b.c"))
        assertNull(vm.parse("v1.2.3"))
        assertNull(vm.parse("1.2.3-"))
        assertNull(vm.parse("01.02.03"))
    }

    @Test
    fun parse_versionWithWhitespace() {
        val v = vm.parse("  1.2.3  ")!!
        assertEquals("1.2.3", v.versionName)
    }

    @Test
    fun versionCode_calculatesCorrectly() {
        assertEquals(10000, vm.parse("1.0.0")!!.versionCode)
        assertEquals(10203, vm.parse("1.2.3")!!.versionCode)
        assertEquals(20100, vm.parse("2.1.0")!!.versionCode)
        assertEquals(0, vm.parse("0.0.0")!!.versionCode)
        assertEquals(30099, vm.parse("3.0.99")!!.versionCode)
    }

    @Test
    fun versionCode_monotonicallyIncreasing() {
        val versions = listOf("0.1.0", "0.2.0", "1.0.0", "1.0.1", "1.1.0", "2.0.0")
        val codes = versions.map { vm.parse(it)!!.versionCode }
        for (i in 1 until codes.size) {
            assertTrue(codes[i] > codes[i - 1])
        }
    }

    @Test
    fun compareTo_majorVersionTakesPrecedence() {
        val v1 = vm.parse("1.9.9")!!
        val v2 = vm.parse("2.0.0")!!
        assertTrue(v1 < v2)
        assertTrue(v2 > v1)
    }

    @Test
    fun compareTo_minorVersionTakesPrecedence() {
        val v1 = vm.parse("1.1.9")!!
        val v2 = vm.parse("1.2.0")!!
        assertTrue(v1 < v2)
    }

    @Test
    fun compareTo_patchVersion() {
        val v1 = vm.parse("1.0.0")!!
        val v2 = vm.parse("1.0.1")!!
        assertTrue(v1 < v2)
    }

    @Test
    fun compareTo_equalVersions() {
        val v1 = vm.parse("1.2.3")!!
        val v2 = vm.parse("1.2.3")!!
        assertEquals(0, v1.compareTo(v2))
        assertTrue(v1 == v2)
    }

    @Test
    fun compareTo_preReleaseLowerThanRelease() {
        val v1 = vm.parse("1.0.0-alpha")!!
        val v2 = vm.parse("1.0.0")!!
        assertTrue(v1 < v2)
        assertTrue(v2 > v1)
    }

    @Test
    fun compareTo_preReleaseOrdering() {
        val alpha = vm.parse("1.0.0-alpha")!!
        val beta = vm.parse("1.0.0-beta")!!
        val rc = vm.parse("1.0.0-rc")!!
        assertTrue(alpha < beta)
        assertTrue(beta < rc)
    }

    @Test
    fun compareTo_preReleaseNumericOrdering() {
        val v1 = vm.parse("1.0.0-alpha.1")!!
        val v2 = vm.parse("1.0.0-alpha.2")!!
        val v3 = vm.parse("1.0.0-alpha.10")!!
        assertTrue(v1 < v2)
        assertTrue(v2 < v3)
    }

    @Test
    fun compareTo_numericPreReleaseLowerThanString() {
        val v1 = vm.parse("1.0.0-1")!!
        val v2 = vm.parse("1.0.0-alpha")!!
        assertTrue(v1 < v2)
    }

    @Test
    fun bump_patch() {
        val v = vm.parse("1.2.3")!!
        val bumped = vm.bump(v, VersionManager.BumpType.PATCH)
        assertEquals("1.2.4", bumped.versionName)
    }

    @Test
    fun bump_minor_resetsPatch() {
        val v = vm.parse("1.2.3")!!
        val bumped = vm.bump(v, VersionManager.BumpType.MINOR)
        assertEquals("1.3.0", bumped.versionName)
    }

    @Test
    fun bump_major_resetsMinorAndPatch() {
        val v = vm.parse("1.2.3")!!
        val bumped = vm.bump(v, VersionManager.BumpType.MAJOR)
        assertEquals("2.0.0", bumped.versionName)
    }

    @Test
    fun bump_preReleaseVersion_clearsPreRelease() {
        val v = vm.parse("1.0.0-alpha.1")!!
        val bumped = vm.bump(v, VersionManager.BumpType.PATCH)
        assertEquals("1.0.1", bumped.versionName)
        assertTrue(bumped.isStable)
    }

    @Test
    fun nextPatch() {
        val v = vm.parse("1.2.3")!!
        assertEquals("1.2.4", v.nextPatch().versionName)
    }

    @Test
    fun nextMinor() {
        val v = vm.parse("1.2.3")!!
        assertEquals("1.3.0", v.nextMinor().versionName)
    }

    @Test
    fun nextMajor() {
        val v = vm.parse("1.2.3")!!
        assertEquals("2.0.0", v.nextMajor().versionName)
    }

    @Test
    fun withPreRelease() {
        val v = vm.parse("1.0.0")!!
        assertEquals("1.0.0-rc.1", v.withPreRelease("rc.1").versionName)
    }

    @Test
    fun withBuildMetadata() {
        val v = vm.parse("1.0.0")!!
        assertEquals("1.0.0+build.42", v.withBuildMetadata("build.42").versionName)
    }

    @Test
    fun isCompatible_sameMajorHigherMinor() {
        val current = vm.parse("1.5.0")!!
        val required = vm.parse("1.2.0")!!
        assertTrue(vm.isCompatible(current, required))
    }

    @Test
    fun isCompatible_sameMajorSameMinorHigherPatch() {
        val current = vm.parse("1.2.5")!!
        val required = vm.parse("1.2.0")!!
        assertTrue(vm.isCompatible(current, required))
    }

    @Test
    fun isCompatible_differentMajor_incompatible() {
        val current = vm.parse("2.0.0")!!
        val required = vm.parse("1.0.0")!!
        assertFalse(vm.isCompatible(current, required))
    }

    @Test
    fun isCompatible_zeroMajor_requiresExactMinor() {
        val current = vm.parse("0.2.0")!!
        val required = vm.parse("0.1.0")!!
        assertFalse(vm.isCompatible(current, required))
    }

    @Test
    fun isCompatible_zeroMajor_sameMinor_sufficientPatch() {
        val current = vm.parse("0.1.5")!!
        val required = vm.parse("0.1.0")!!
        assertTrue(vm.isCompatible(current, required))
    }

    @Test
    fun satisfiesRequirement_greaterThanOrEqual() {
        val v = vm.parse("1.2.3")!!
        assertTrue(vm.satisfiesRequirement(v, ">=1.0.0"))
        assertTrue(vm.satisfiesRequirement(v, ">=1.2.0"))
        assertTrue(vm.satisfiesRequirement(v, ">=1.2.3"))
        assertFalse(vm.satisfiesRequirement(v, ">=1.3.0"))
    }

    @Test
    fun satisfiesRequirement_greaterThan() {
        val v = vm.parse("1.2.3")!!
        assertTrue(vm.satisfiesRequirement(v, ">1.0.0"))
        assertTrue(vm.satisfiesRequirement(v, ">1.2.2"))
        assertFalse(vm.satisfiesRequirement(v, ">1.2.3"))
    }

    @Test
    fun satisfiesRequirement_lessThan() {
        val v = vm.parse("1.2.3")!!
        assertTrue(vm.satisfiesRequirement(v, "<2.0.0"))
        assertFalse(vm.satisfiesRequirement(v, "<1.0.0"))
    }

    @Test
    fun satisfiesRequirement_equalTo() {
        val v = vm.parse("1.2.3")!!
        assertTrue(vm.satisfiesRequirement(v, "=1.2.3"))
        assertFalse(vm.satisfiesRequirement(v, "=1.2.4"))
    }

    @Test
    fun satisfiesRequirement_defaultOperatorIsGreaterThanOrEqual() {
        val v = vm.parse("1.2.3")!!
        assertTrue(vm.satisfiesRequirement(v, "1.0.0"))
        assertTrue(vm.satisfiesRequirement(v, "1.2.3"))
        assertFalse(vm.satisfiesRequirement(v, "2.0.0"))
    }

    @Test
    fun findLatestVersion_ignoresPreReleases() {
        val versions = listOf(
            vm.parse("1.0.0-alpha")!!,
            vm.parse("1.0.0-beta")!!,
            vm.parse("1.0.0")!!
        )
        assertEquals("1.0.0", vm.findLatestVersion(versions)!!.versionName)
    }

    @Test
    fun findLatestVersion_returnsHighestStable() {
        val versions = listOf(
            vm.parse("0.9.0")!!,
            vm.parse("1.0.0")!!,
            vm.parse("1.1.0")!!,
            vm.parse("2.0.0-beta")!!
        )
        assertEquals("1.1.0", vm.findLatestVersion(versions)!!.versionName)
    }

    @Test
    fun findLatestVersion_emptyList_returnsNull() {
        assertNull(vm.findLatestVersion(emptyList()))
    }

    @Test
    fun findLatestPreRelease_includesPreReleases() {
        val versions = listOf(
            vm.parse("1.0.0")!!,
            vm.parse("2.0.0-beta.1")!!
        )
        assertEquals("2.0.0-beta.1", vm.findLatestPreRelease(versions)!!.versionName)
    }

    @Test
    fun comparePreRelease_sameIdentifier_equal() {
        assertEquals(0, vm.comparePreRelease("alpha", "alpha"))
    }

    @Test
    fun comparePreRelease_alphabeticOrdering() {
        assertTrue(vm.comparePreRelease("alpha", "beta") < 0)
        assertTrue(vm.comparePreRelease("beta", "rc") < 0)
    }

    @Test
    fun comparePreRelease_numericPreRelease() {
        assertTrue(vm.comparePreRelease("1", "2") < 0)
        assertTrue(vm.comparePreRelease("9", "10") < 0)
    }

    @Test
    fun comparePreRelease_dottedIdentifiers() {
        assertTrue(vm.comparePreRelease("alpha.1", "alpha.2") < 0)
        assertTrue(vm.comparePreRelease("alpha.9", "alpha.10") < 0)
    }

    @Test
    fun comparePreRelease_numericLowerThanString() {
        assertTrue(vm.comparePreRelease("1", "alpha") < 0)
    }

    @Test
    fun moduleScenario_envJdkRequiresBaseVersion() {
        val baseInstalled = vm.parse("1.2.0")!!
        val baseRequired = vm.parse("1.0.0")!!
        assertTrue(vm.isCompatible(baseInstalled, baseRequired))
    }

    @Test
    fun moduleScenario_envJdkRequiresHigherBaseVersion() {
        val baseInstalled = vm.parse("1.0.0")!!
        val baseRequired = vm.parse("1.5.0")!!
        assertFalse(vm.isCompatible(baseInstalled, baseRequired))
    }

    @Test
    fun moduleScenario_manifestMinBaseVersionCheck() {
        val baseInstalled = vm.parse("2.0.0")!!
        val manifest = com.haisa.sdk.model.ModuleManifest(
            moduleId = "env-jdk",
            minBaseVersion = "1.0.0"
        )
        val required = vm.parse(manifest.minBaseVersion)!!
        assertTrue(vm.satisfiesRequirement(baseInstalled, ">=1.0.0"))
        assertFalse(vm.isCompatible(baseInstalled, required))
    }

    @Test
    fun moduleScenario_releaseVersionSelection() {
        val available = listOf(
            vm.parse("1.0.0-alpha")!!,
            vm.parse("1.0.0-beta")!!,
            vm.parse("1.0.0-rc.1")!!,
            vm.parse("1.0.0")!!,
            vm.parse("1.0.1")!!,
            vm.parse("1.1.0-beta")!!
        )
        val latest = vm.findLatestVersion(available)!!
        assertEquals("1.0.1", latest.versionName)
    }

    @Test
    fun moduleScenario_versionRoundTrip() {
        val original = "2.5.13-rc.3+build.789"
        val parsed = vm.parse(original)!!
        assertEquals(original, parsed.versionName)
    }

    @Test
    fun moduleScenario_versionCodeForAndroid() {
        val v1 = vm.parse("1.0.0")!!
        val v2 = vm.parse("1.1.0")!!
        val v3 = vm.parse("2.0.0")!!
        assertTrue(v1.versionCode < v2.versionCode)
        assertTrue(v2.versionCode < v3.versionCode)
        assertEquals(10000, v1.versionCode)
        assertEquals(10100, v2.versionCode)
        assertEquals(20000, v3.versionCode)
    }
}
