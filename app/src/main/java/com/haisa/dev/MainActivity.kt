package com.haisa.dev

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haisa.sdk.HaisaEnvironment
import com.haisa.sdk.InstallProgress
import com.haisa.sdk.ModuleInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Haisa Dev 主界面 Activity
 *
 * 展示模块商店、终端入口和构建日志
 */
class MainActivity : AppCompatActivity() {

    private lateinit var haisa: HaisaEnvironment
    private lateinit var moduleAdapter: ModuleAdapter
    private lateinit var progressBar: ProgressBar
    private var currentInstallJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 Haisa SDK
        haisa = HaisaEnvironment.getInstance(this)

        // 初始化 UI
        initUI()

        // 加载模块列表
        loadModules()
    }

    private fun initUI() {
        // 设置 RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.module_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        moduleAdapter = ModuleAdapter(emptyList()) { module ->
            onModuleClick(module)
        }
        recyclerView.adapter = moduleAdapter

        // 进度条
        progressBar = findViewById(R.id.progress_bar)
        progressBar.visibility = View.GONE
    }

    /**
     * 从远程加载可用模块列表
     */
    private fun loadModules() {
        lifecycleScope.launch {
            try {
                showLoading(true)
                val modules = haisa.getAvailableModules()
                moduleAdapter.updateData(modules)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "加载模块失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * 点击模块时的处理
     */
    private fun onModuleClick(module: ModuleInfo) {
        if (module.isInstalled) {
            // 已安装，显示管理对话框
            showModuleManageDialog(module)
        } else {
            // 未安装，显示安装对话框
            showInstallDialog(module)
        }
    }

    /**
     * 显示模块安装对话框
     */
    private fun showInstallDialog(module: ModuleInfo) {
        AlertDialog.Builder(this)
            .setTitle(module.name)
            .setMessage("""
                ${module.description}
                
                大小: ${module.sizeInMB} MB
                版本: ${module.version}
                ${if (module.dependencies.isNotEmpty()) "依赖: ${module.dependencies.joinToString()}" else ""}
            """.trimIndent())
            .setPositiveButton("安装") { _, _ ->
                installModule(module)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 执行模块安装
     */
    private fun installModule(module: ModuleInfo) {
        currentInstallJob?.cancel()

        currentInstallJob = lifecycleScope.launch {
            haisa.installModule(module.id, module.version)
                .collect { progress ->
                    when (progress.status) {
                        InstallProgress.InstallStatus.DOWNLOADING -> {
                            showLoading(true)
                            val percent = progress.progressPercent
                            progressBar.progress = percent
                            updateStatus("正在下载... $percent%")
                        }
                        InstallProgress.InstallStatus.EXTRACTING -> {
                            updateStatus("正在解压...")
                        }
                        InstallProgress.InstallStatus.VERIFYING -> {
                            updateStatus("正在验证...")
                        }
                        InstallProgress.InstallStatus.FINISHED -> {
                            showLoading(false)
                            Toast.makeText(
                                this@MainActivity,
                                "${module.name} 安装完成",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadModules() // 刷新列表
                        }
                        InstallProgress.InstallStatus.ERROR -> {
                            showLoading(false)
                            Toast.makeText(
                                this@MainActivity,
                                "安装失败: ${progress.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
        }
    }

    /**
     * 显示模块管理对话框（已安装）
     */
    private fun showModuleManageDialog(module: ModuleInfo) {
        val items = arrayOf("切换版本", "卸载模块")
        AlertDialog.Builder(this)
            .setTitle(module.name)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showVersionSwitchDialog(module)
                    1 -> uninstallModule(module)
                }
            }
            .show()
    }

    private fun showVersionSwitchDialog(module: ModuleInfo) {
        // 展示所有可用版本
        val versions = listOf("17.0.8", "21.0.1") // 从远程获取
        AlertDialog.Builder(this)
            .setTitle("选择版本")
            .setItems(versions.toTypedArray()) { _, which ->
                haisa.switchModuleVersion(module.id, versions[which])
                Toast.makeText(this, "已切换到 ${versions[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun uninstallModule(module: ModuleInfo) {
        AlertDialog.Builder(this)
            .setTitle("确认卸载")
            .setMessage("确定要卸载 ${module.name} 吗？")
            .setPositiveButton("卸载") { _, _ ->
                // 执行卸载
                Toast.makeText(this, "已卸载", Toast.LENGTH_SHORT).show()
                loadModules()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateStatus(message: String) {
        // 更新状态栏或日志
    }

    // ========== Menu ==========

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadModules()
                true
            }
            R.id.action_terminal -> {
                // 打开终端
                openTerminal()
                true
            }
            R.id.action_settings -> {
                // 打开设置
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openTerminal() {
        // 启动 TerminalActivity
        // val intent = Intent(this, TerminalActivity::class.java)
        // startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        currentInstallJob?.cancel()
    }
}