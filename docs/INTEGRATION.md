# Haisa Des 集成到其他项目的详细方案

> 版本: v1.0  
> 日期: 2026-05-09

---

## 一、作为 SDK 集成 (推荐方式)

### 1.1 Gradle 依赖配置

在项目的 `build.gradle` 或 `build.gradle.kts` 中添加：

```groovy
// build.gradle (Groispy)
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    // 核心 SDK (必需)
implementation 'com.github.haisa-des:haisa-sdk:v1.0.0'

// 可选：终端视图组件 (如果不想使用默认终端)
implementation 'com.github.haisa-des:haisa-terminal:v1.0.0'
}
```

```kotlin
// build.gradle.kts (Kotlin DSL)
repositories {
    maven("https://jitpack.io")
}

dependencies {
implementation("com.github.haisa-des:haisa-sdk:v1.0.0")
implementation("com.github.haisa-des:haisa-terminal:v1.0.0")
}
```

### 1.2 初始化 SDK

在 `Application` 类或主 `Activity` 中初始化：

```kotlin
import com.haisa.sdk.HaisaEnvironment
import com.haisa.sdk.ModuleInfo
import com.haisa.sdk.ProjectTemplate

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Haisa 环境 (单例)
        val haisa = HaisaEnvironment.getInstance(this)
    }
}
```

### 1.3 基础使用示例

#### 检查并安装模块

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val haisa = HaisaEnvironment.getInstance(this)

    fun checkAndInstallJava() {
        lifecycleScope.launch {
            // 检查 Java 模块是否已安装
            if (!haisa.isModuleInstalled("env-jdk")) {
                // 弹出商店界面
                showModuleStoreDialog("env-jdk")
            }
        }
    }

    fun installModule(moduleId: String) {
        lifecycleScope.launch {
            haisa.installModule(moduleId)
                .collect { progress ->
                    when (progress.status) {
                        InstallProgress.InstallStatus.DOWNLOADING -> {
                            showProgress(progress.progressPercent,
                                "正在下载 ${progress.moduleId} (${progress.downloadedBytes/1024/1024}MB / ${progress.totalBytes/1024/1024}MB)")
                        }
                        InstallProgress.InstallStatus.EXTRACTING -> {
                            showProgress(100, "正在解压...")
                        }
                        InstallProgress.InstallStatus.FINISHED -> {
                            showToast("${progress.moduleId} 安装完成")
                        }
                        InstallProgress.InstallStatus.ERROR -> {
                            showToast("安装失败: ${progress.message}")
                        }
                        else -> {}
                    }
                }
        }
    }
}
```

#### 注入环境变量并执行命令

```kotlin
class BuildActivity : AppCompatActivity() {
    private val haisa = HaisaEnvironment.getInstance(this)

    fun runGradleBuild(projectPath: String) {
        lifecycleScope.launch {
            // 注入 Java 环境变量
            val envVars = haisa.injectEnvironment(listOf("env-jdk", "env-cc"))
            
            // 显示环境变量 (调试用)
            envVars.forEach { (key, value) ->
                Log.d("Haisa", "$key=$value")
            }
            // 输出示例:
            // JAVA_HOME=/data/data/com.example/files/modules/env-jdk/17.0.8/usr/lib/jvm/openjdk-17
            // PATH=/data/data/com.example/files/modules/env-jdk/17.0.8/bin:...:/system/bin
            // CC=/data/data/com.example/files/modules/env-cc/15.0.0/bin/clang

            // 执行构建命令
            haisa.executeBuild(projectPath, "./gradlew assembleDebug", listOf("env-jdk"))
                .collect { progress ->
                    when (progress.status) {
                        BuildProgress.BuildStatus.COMPILING -> {
                            updateBuildLog(progress.message)
                        }
                        BuildProgress.BuildStatus.FINISHED -> {
                            showToast("构建成功!")
                        }
                        BuildProgress.BuildStatus.FAILED -> {
                            showToast("构建失败: ${progress.message}")
                        }
                        else -> {}
                    }
                }
        }
    }
}
```

#### 创建项目模板

```kotlin
class NewProjectActivity : AppCompatActivity() {
    private val haisa = HaisaEnvironment.getInstance(this)

    fun createAndroidProject(projectName: String, outputDir: String) {
        lifecycleScope.launch {
            val result = haisa.createProject(
                projectName = projectName,
                template = ProjectTemplate.ANDROID_JAVA,
                outputDir = outputDir
            )

            result.onSuccess { config ->
                Log.i("Haisa", "项目创建成功: ${config.path}")
                // 提示安装所需模块
                showRequiredModulesDialog(config.requiredModules)
            }.onFailure { error ->
                Log.e("Haisa", "项目创建失败", error)
            }
        }
    }
}
```

---

## 二、作为模块集成 (AAR 包)

如果不想使用 JitPack，可以直接下载 AAR 包集成。

### 2.1 下载 AAR

从 GitHub Releases 下载最新 AAR：
```bash
wget https://github.com/haisa-des/haisa-des/releases/download/v1.0.0/haisa-sdk-v1.0.0.aar
```

### 2.2 放入项目

将 `haisa-sdk-v1.0.0.aar` 放入项目的 `libs/` 目录：

```
app/
├── src/
├── libs/
│   └── haisa-sdk-v1.0.0.aar
└── build.gradle
```

### 2.3 Gradle 配置

```groovy
// app/build.gradle
dependencies {
    implementation files('libs/haisa-sdk-v1.0.0.aar')
    
    // 依赖 Haisa SDK 所需的第三方库
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0'
    implementation 'com.squareup.retrofit2:retrofit:2.11.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
```

---

## 三、源码集成 (高度定制)

如果需要深度定制，可以将源码直接集成到项目中。

### 3.1 复制源码模块

将以下模块复制到你的项目中：

```bash
# 在你的项目根目录
git clone https://github.com/haisa-des/haisa-des.git temp-haisa

# 复制核心 SDK
cp -r temp-haisa/haisa-sdk/ haisa-sdk/

# 复制终端模拟器 (可选)
cp -r temp-haisa/terminal-emulator/ terminal-emulator/

# 删除临时目录
rm -rf temp-haisa
```

### 3.2 修改 settings.gradle

```groovy
// settings.gradle
include ':app'
include ':haisa-sdk'
include ':terminal-emulator'
```

### 3.3 修改 app/build.gradle

```groovy
// app/build.gradle
dependencies {
    implementation project(':haisa-sdk')
    implementation project(':terminal-emulator') // 可选
}
```

### 3.4 自定义修改

可以直接修改 `haisa-sdk` 中的代码来实现定制功能：

- **修改模块下载源**: 编辑 `GitHubReleasesSource.kt`
- **修改模块存储路径**: 编辑 `LocalDataSource.kt`
- **修改终端渲染方式**: 编辑 `HaisaTerminalView.kt`
- **添加新语言支持**: 在 `ProjectGenerator.kt` 中添加新模板

---

## 四、集成到 IDE 项目

### 4.1 Haisa IDE 集成示例

```kotlin
// Haisa IDE 中的 IDEApplication.kt
class IDEApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Haisa SDK
        val haisa = HaisaEnvironment.getInstance(this)
        
        // 注册 IDE 插件生命周期
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is MainActivity) {
                    // 主界面创建后，预加载常用模块
                    lifecycleScope.launch {
                        if (!haisa.isEnvironmentReady("env-jdk")) {
                            showEnvSetupDialog()
                        }
                    }
                }
            }
            // ... 其他回调
        })
    }
}
```

### 4.2 构建系统集成

```kotlin
// 在 IDE 的 BuildService 中集成
class BuildService(private val context: Context) {
    private val haisa = HaisaEnvironment.getInstance(context)

    suspend fun buildProject(project: Project, variant: String = "debug"): Flow<BuildProgress> {
        // 获取项目所需的模块
        val requiredModules = project.config.requiredModules
        
        // 确保环境就绪
        requiredModules.forEach { moduleId ->
            if (!haisa.isEnvironmentReady(moduleId)) {
                throw EnvironmentNotReadyException("请先安装 $moduleId 模块")
            }
        }
        
        // 注入环境变量
        val env = haisa.injectEnvironment(requiredModules)
        
        // 执行构建
        return haisa.executeBuild(
            project.path,
            "./gradlew assemble${variant.capitalize()}",
            requiredModules
        )
    }
}
```

### 4.3 终端集成

```kotlin
// 在 IDE 的 TerminalPanel 中集成
class TerminalPanel(context: Context) : FrameLayout(context) {
    private val haisa = HaisaEnvironment.getInstance(context)
    private var session: TerminalSession? = null

    init {
        // 创建终端视图
        val terminalView = HaisaTerminalView(context)
        addView(terminalView)
        
        // 创建终端会话
        session = haisa.createTerminalSession(
            workingDir = "/sdcard/projects",
            envVars = mapOf("TERM" to "xterm-256color")
        )
        
        // 绑定会话到视图
        terminalView.attachSession(session!!)
    }

    fun executeCommand(command: String) {
        session?.write(command)
    }
}
```

---

## 五、常见问题

### Q1: Module 安装失败，提示 "磁盘空间不足"

**解决方案**: 在调用 `installModule()` 前检查可用空间：
```kotlin
if (getAvailableSpaceMB() < module.sizeInMB * 2) {
    showToast("需要 ${module.sizeInMB * 2}MB 可用空间")
    return
}
```

### Q2: 如何自定义模块下载源？

**方案**: 实现自定义的 `ModuleRepository`：
```kotlin
class CustomModuleSource : ModuleRepository {
    override suspend fun fetchModules(): List<ModuleInfo> {
        // 从你自己的服务器获取
        return myApi.getAvailableModules()
    }
    
    override suspend fun downloadModule(moduleId: String, version: String): File {
        // 从你自己的 CDN 下载
        return myCdn.download("$moduleId-$version.tar.xz")
    }
}

// 注册到 Haisa
HaisaEnvironment.getInstance(context).setCustomSource(CustomModuleSource())
```

### Q3: 如何在构建前自动安装缺失的模块？

```kotlin
suspend fun ensureModulesReady(moduleIds: List<String>): Boolean {
    moduleIds.forEach { id ->
        if (!haisa.isModuleInstalled(id)) {
            haisa.installModule(id).collect { progress ->
                if (progress.status == InstallProgress.InstallStatus.ERROR) {
                    throw ModuleInstallException(id, progress.message)
                }
            }
        }
    }
    return true
}
```

---

## 六、API 参考速查

| API | 说明 | 示例 |
| :--- | :--- | :--- |
| `getInstance(context)` | 获取 SDK 实例 | `HaisaEnvironment.getInstance(this)` |
| `isModuleInstalled(id)` | 检查模块 | `haisa.isModuleInstalled("env-jdk")` |
| `installModule(id, ver?)` | 安装模块 | `haisa.installModule("env-jdk", "17.0.8")` |
| `injectEnvironment(list)` | 注入环境 | `haisa.injectEnvironment(listOf("env-jdk"))` |
| `createTerminalSession(dir, vars)` | 创建终端 | `haisa.createTerminalSession("/sdcard", mapOf())` |
| `executeBuild(path, cmd, modules)` | 执行构建 | `haisa.executeBuild("/project", "./gradlew", listOf("env-jdk"))` |
| `createProject(name, template, dir)` | 创建项目 | `haisa.createProject("MyApp", ProjectTemplate.ANDROID_JAVA, "/projects")` |

---

**更多示例**: 参见 [examples/](../examples/) 目录
