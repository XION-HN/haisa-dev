package com.haisa.sdk.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.haisa.sdk.model.InstalledModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocalDataSourceTest {

    private lateinit var localDataSource: LocalDataSource
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("haisa_modules_test", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        localDataSource = LocalDataSource(context)
    }

    @Test
    fun getInstalledModules_initially_returnsEmpty() {
        val modules = localDataSource.getInstalledModules()
        assertTrue(modules.isEmpty())
    }

    @Test
    fun saveInstalledModule_and_retrieveIt() {
        val module = InstalledModule(
            id = "env-jdk",
            version = "17.0.8",
            installDate = System.currentTimeMillis(),
            sizeInBytes = 180 * 1024 * 1024L,
            installPath = "/modules/env-jdk/17.0.8"
        )
        localDataSource.saveInstalledModule(module)

        val retrieved = localDataSource.getInstalledModules()
        assertEquals(1, retrieved.size)
        assertEquals("env-jdk", retrieved[0].id)
        assertEquals("17.0.8", retrieved[0].version)
    }

    @Test
    fun isModuleInstalled_returnsTrueAfterInstall() {
        assertFalse(localDataSource.isModuleInstalled("env-jdk"))

        localDataSource.saveInstalledModule(
            InstalledModule("env-jdk", "17.0.8", 0L, 0L, "")
        )

        assertTrue(localDataSource.isModuleInstalled("env-jdk"))
    }

    @Test
    fun removeInstalledModule_removesSpecificVersion() {
        localDataSource.saveInstalledModule(
            InstalledModule("env-jdk", "17.0.8", 0L, 0L, "")
        )
        localDataSource.saveInstalledModule(
            InstalledModule("env-jdk", "21.0.1", 0L, 0L, "")
        )

        assertEquals(2, localDataSource.getInstalledModules().size)

        localDataSource.removeInstalledModule("env-jdk", "17.0.8")

        val remaining = localDataSource.getInstalledModules()
        assertEquals(1, remaining.size)
        assertEquals("21.0.1", remaining[0].version)
    }

    @Test
    fun removeInstalledModule_removesAllVersions() {
        localDataSource.saveInstalledModule(
            InstalledModule("env-jdk", "17.0.8", 0L, 0L, "")
        )
        localDataSource.saveInstalledModule(
            InstalledModule("env-jdk", "21.0.1", 0L, 0L, "")
        )

        localDataSource.removeInstalledModule("env-jdk")

        assertTrue(localDataSource.getInstalledModules().isEmpty())
    }

    @Test
    fun setActiveVersion_and_getActiveVersion() {
        localDataSource.setActiveVersion("env-jdk", "17.0.8")
        assertEquals("17.0.8", localDataSource.getActiveVersion("env-jdk"))
    }

    @Test
    fun getActiveVersion_returnsNullWhenNotSet() {
        assertEquals(null, localDataSource.getActiveVersion("env-unknown"))
    }

    @Test
    fun saveMultipleModules_allRetrievable() {
        localDataSource.saveInstalledModule(InstalledModule("env-jdk", "17.0.8", 0L, 0L, ""))
        localDataSource.saveInstalledModule(InstalledModule("env-python", "3.11.6", 0L, 0L, ""))
        localDataSource.saveInstalledModule(InstalledModule("env-cc", "15.0.0", 0L, 0L, ""))

        val all = localDataSource.getInstalledModules()
        assertEquals(3, all.size)
        assertTrue(all.any { it.id == "env-jdk" })
        assertTrue(all.any { it.id == "env-python" })
        assertTrue(all.any { it.id == "env-cc" })
    }
}