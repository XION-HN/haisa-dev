package com.haisa.des

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.haisa.sdk.HaisaEnvironment
import com.haisa.sdk.util.EnvironmentInjector
import com.haisa.terminal.TermExec
import com.haisa.terminal.emulatorview.EmulatorView
import com.haisa.terminal.emulatorview.TermSession
import java.io.File

class TerminalActivity : AppCompatActivity() {

    private var termSession: TermSession? = null
    private var emulatorView: EmulatorView? = null
    private var termExec: TermExec? = null
    @Volatile private var processId: Int = -1
    private var pfd: ParcelFileDescriptor? = null

    companion object {
        const val EXTRA_MODULE_IDS = "module_ids"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Terminal"

        initTerminal()
    }

    private fun initTerminal() {
        val session = TermSession()
        session.setKeyListener(null)

        val haisa = HaisaEnvironment.getInstance(this)
        val moduleIds = intent.getStringArrayListExtra(EXTRA_MODULE_IDS) ?: emptyList()

        val envMap = if (moduleIds.isNotEmpty()) {
            haisa.injectEnvironment(moduleIds)
        } else {
            System.getenv().toMap()
        }

        val shell = findShell()
        val homeDir = filesDir.absolutePath
        val envArray = EnvironmentInjector.toEnvArray(
            envMap.toMutableMap().apply {
                put("HOME", homeDir)
                put("TERM", "xterm-256color")
                put("LANG", "en_US.UTF-8")
            }
        )

        val exec = TermExec(shell)
        exec.environment().putAll(envArray.associate { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else entry to ""
        })

        val pfdLocal = ParcelFileDescriptorCompat.openPtmx()
        if (pfdLocal == null) {
            finish()
            return
        }
        pfd = pfdLocal

        Thread {
            try {
                processId = exec.start(pfdLocal)
            } catch (e: Exception) {
                runOnUiThread { finish() }
            }
        }.start()

        val inputStream = ParcelFileDescriptorCompat.getInputStream(pfdLocal)
        val outputStream = ParcelFileDescriptorCompat.getOutputStream(pfdLocal)

        session.setTermIn(inputStream)
        session.setTermOut(outputStream)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        session.initializeEmulator(80, 24)

        termSession = session
        termExec = exec

        emulatorView = findViewById<View>(R.id.terminal_view) as EmulatorView
        emulatorView?.let { view ->
            view.attachSession(session)
            view.setDensity(metrics)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
        }

        session.setUpdateCallback {
            emulatorView?.invalidate()
        }
    }

    private fun findShell(): String {
        val candidates = listOf("/system/bin/sh", "/bin/sh", "/usr/bin/sh")
        for (path in candidates) {
            if (File(path).exists()) return path
        }
        return "sh"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (processId > 0) {
            TermExec.sendSignal(processId, 9)
        }
        termSession?.finish()
        try {
            pfd?.close()
        } catch (_: Exception) {}
        pfd = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (emulatorView?.onKeyDown(keyCode, event) == true) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (emulatorView?.onKeyUp(keyCode, event) == true) return true
        return super.onKeyDown(keyCode, event)
    }
}
