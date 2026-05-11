# Haisa Des 开发计划

> 版本: v2.0
> 日期: 2026-05-11
> 定位: 纯伪 Linux 库（非应用），供第三方 Android IDE 集成

---

## 项目定位变更说明

Haisa Des 已从独立 Android 应用转变为**纯库项目**：

- **App 模块**已改为 `com.android.library`，无启动 Activity
- **模块商店 UI** 已全部删除，所有功能通过 SDK API 暴露
- **包管理器**（`com.haisa.sdk.pkg`）替代旧的 ModuleManager/ModuleRepository 层
- **IDE 集成 API** 新增语言 SDK 信息暴露和语法补全上下文解析

---

## 已完成工作

### 阶段一：项目重命名与 GPL 清除

| 任务 | 状态 |
| :--- | :--- |
| haisa-dev → haisa-des 全面重命名（28+文件） | ✅ |
| `com.haisa.dev` → `com.haisa.des` 包名迁移 | ✅ |
| `HaisaDev` → `HaisaDes` 类名迁移 | ✅ |
| jackpal-termexec2 → haisa-termexec (CMakeLists+Java) | ✅ |
| `jackpal.androidterm` → `com.haisa.terminal` (AIDL+Java) | ✅ |
| 零残留验证：`grep -rn "haisa-dev\|jackpal"` 返回空 | ✅ |

### 阶段二：安全加固

| 任务 | 状态 |
| :--- | :--- |
| BuildEngine `sanitizeCommand()` 防命令注入 | ✅ |
| 路径白名单：`/data/data/` `/sdcard/` `/storage/` | ✅ |
| 30分钟进程超时 + `destroyForcibly()` | ✅ |
| ModuleRepository zip-slip 修复（canonical path + `..` 拒绝） | ✅ |
| 256MB 条目大小限制 | ✅ |
| OkHttp response body 关闭（finally 块） | ✅ |
| 3次重试 + 指数退避 + SHA-256 校验 | ✅ |
| BuildTaskQueue 竞态条件修复（Mutex + synchronized） | ✅ |

### 阶段三：架构重构

| 任务 | 状态 |
| :--- | :--- |
| App 模块 → `com.android.library` | ✅ |
| 删除启动 Activity intent-filter | ✅ |
| 删除模块商店所有 Fragment/Adapter/Layout | ✅ |
| HaisaEnvironment context 泄漏修复 | ✅ |
| `createProject()` 改为 `suspend fun` | ✅ |
| `validateModuleId()` + `validateProjectName()` | ✅ |
| ParcelFileDescriptorCompat RandomAccessFile 泄漏修复 | ✅ |
| TerminalActivity `@Volatile processId` + PFD close | ✅ |

### 阶段四：包管理器

| 任务 | 状态 |
| :--- | :--- |
| PackageModels.kt 数据模型 | ✅ |
| PackageDatabase.kt 安装状态数据库 | ✅ |
| DependencyResolver.kt 拓扑排序 + 环检测 | ✅ |
| PackageManager.kt 核心（install/remove/autoremove） | ✅ |
| HaisaEnvironment 集成 PackageManager | ✅ |
| 旧 ModuleManager/ModuleRepository 已弃用（死代码） | ✅ |
| GitHubReleasesSource 支持 v2.0 repo-index.json | ✅ |
| `refreshPackageIndex()` 远程索引拉取 | ✅ |

### 阶段五：模块定义

| 模块 | 版本 | 状态 |
| :--- | :--- | :--- |
| env-base | 1.0.0 | ✅ |
| env-cc (Clang/LLVM) | 17.0.1 | ✅ |
| env-git | 2.43.0 | ✅ |
| env-jdk (OpenJDK 17) | 17.0.8 | ✅ |
| env-node (Node.js 20 LTS) | 20.11.0 | ✅ |
| env-python (3.11) | 3.11.8 | ✅ |
| env-rust (1.75) | 1.75.0 | ✅ |
| env-go (1.21) | 1.21.6 | ✅ |

### 阶段六：IDE 集成 API

| 任务 | 状态 |
| :--- | :--- |
| `LanguageSdkInfo` 数据类（语言→包→SDK信息） | ✅ |
| `CompletionContext` 数据类（语言+SDK+include路径+环境） | ✅ |
| `getLanguageSdk(languageId)` 按语言查 SDK | ✅ |
| `getLanguageSdks()` 列出所有已安装 SDK | ✅ |
| `resolveCompletionContext(filePath, moduleIds)` 补全上下文 | ✅ |
| 语言映射：Java/Kotlin/Python/JS/TS/C/C++/Rust/Go | ✅ |
| 文件扩展名→语言自动推断 | ✅ |

---

## 待完成工作

### 阶段七：模块构建流水线

| 序号 | 任务 | 优先级 | 状态 |
| :--- | :--- | :--- | :--- |
| 7.1 | GitHub Actions `build-modules.yml` 完善 | 高 | ✅ CI修复 |
| 7.2 | env-base 基础模块编译 (sh, busybox, libc) | 高 | ⏳ |
| 7.3 | env-cc Clang/LLVM 交叉编译 | 高 | ⏳ |
| 7.4 | env-jdk OpenJDK 17 移植 | 高 | ⏳ |
| 7.5 | env-python 3.11 编译 | 高 | ⏳ |
| 7.6 | env-node Node.js 20 编译 | 中 | ⏳ |
| 7.7 | env-git 编译 | 中 | ⏳ |
| 7.8 | env-rust 1.75 编译 | 中 | ⏳ |
| 7.9 | env-go 1.21 编译 | 中 | ⏳ |
| 7.10 | GitHub Pages 托管 repo-index.json | 中 | ⏳ |
| 7.11 | SHA-256 校验和生成与填充 | 高 | ⏳ |

### 阶段八：SDK 完善

| 序号 | 任务 | 优先级 | 状态 |
| :--- | :--- | :--- | :--- |
| 8.1 | PackageManager 升级逻辑（版本比较 + 下载替换） | 高 | ⏳ |
| 8.2 | 多架构支持（arm64-v8a, armeabi-v7a, x86_64） | 中 | ⏳ |
| 8.3 | 包冲突检测（`conflicts` 字段） | 中 | ⏳ |
| 8.4 | 离线安装支持（从本地 ZIP 安装） | 中 | ⏳ |
| 8.5 | 包签名验证（除 SHA-256 外） | 低 | ⏳ |
| 8.6 | 清理死代码（ModuleManager/ModuleRepository/LocalDataSource） | 中 | ⏳ |

### 阶段九：IDE 集成增强

| 序号 | 任务 | 优先级 | 状态 |
| :--- | :--- | :--- | :--- |
| 9.1 | 依赖补全（package.json/requirements.txt/Cargo.toml → 可用包列表） | 高 | ⏳ |
| 9.2 | 项目分析 API（语言检测 + 依赖扫描 + 构建工具识别） | 高 | ⏳ |
| 9.3 | LSP 协议适配层（gopls/rust-analyzer/clangd） | 中 | ⏳ |
| 9.4 | 调试支持 API（JDB/gdb/lldb 进程管理） | 低 | ⏳ |

### 阶段十：发布

| 序号 | 任务 | 优先级 | 状态 |
| :--- | :--- | :--- | :--- |
| 10.1 | v1.0 Release | 高 | ⏳ |
| 10.2 | JitPack/Maven Central SDK 发布 | 高 | ⏳ |
| 10.3 | API 文档（KDoc + 示例代码） | 中 | ⏳ |
| 10.4 | 集成指南（Haisa IDE + 第三方 IDE） | 中 | ⏳ |

---

## 架构概览

```
┌──────────────────────────────────────────────┐
│            第三方 Android IDE 应用              │
│  (通过 HaisaIdeApi 接口集成)                    │
└───────────────┬──────────────────────────────┘
                │
┌───────────────▼──────────────────────────────┐
│              haisa-sdk (AAR 库)                │
│                                               │
│  ┌─────────────┐  ┌──────────────────────┐   │
│  │ HaisaIdeApi │  │  HaisaEnvironment    │   │
│  │ (接口层)     │──│  (门面/协调层)        │   │
│  └─────────────┘  └───┬──────────────────┘   │
│                       │                       │
│  ┌────────────────────▼──────────────────┐   │
│  │          PackageManager               │   │
│  │  ┌───────────┐ ┌────────────────────┐ │   │
│  │  │ PkgDatabase│ │ DependencyResolver │ │   │
│  │  └───────────┘ └────────────────────┘ │   │
│  └───────────────────────────────────────┘   │
│                                               │
│  ┌──────────────┐  ┌───────────────────┐     │
│  │ BuildEngine  │  │ EnvironmentInjector│     │
│  └──────────────┘  └───────────────────┘     │
│                                               │
│  ┌──────────────────────────────────────┐    │
│  │      terminal-emulator (AAR)         │    │
│  │  ITerminal.aidl / haisa-termexec.so  │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

---

## 包管理器架构

```
PackageManager
  ├── updateIndex(packages)       → 更新可用包索引缓存
  ├── search(query)               → 按 pkgId/name/description 搜索
  ├── show(pkgId)                 → 查看包详情
  ├── install(pkgId, version?)    → 安装（含依赖解析 + 下载 + 解压 + 校验）
  ├── remove(pkgId, purge?)       → 移除（含反向依赖检查）
  ├── autoremove()                → 清理孤立的自动安装依赖
  ├── getEnvironment(pkgId)       → 获取已安装包的环境变量
  ├── injectEnvironments(pkgIds)  → 合并多个包的环境变量
  ├── getEntryBinaries(pkgId)     → 获取可执行文件绝对路径
  ├── getIdeIntegrations(pkgId)   → 获取 IDE 任务列表
  └── getInstalledVersion(pkgId)  → 获取已安装版本号

DependencyResolver
  ├── resolve(target, available, installed) → 拓扑排序 + 环检测
  └── findOrphans(installed, autoFlags)     → 孤立依赖发现

PackageDatabase (SharedPreferences)
  ├── 安装状态持久化
  ├── 自动安装标记
  └── 文件列表记录
```

---

## IDE 集成 API

```kotlin
interface HaisaIdeApi {
    // 模块管理
    suspend fun getAvailableModules(): List<ModuleInfo>
    fun installModule(moduleId, version?): Flow<InstallProgress>
    fun uninstallModule(moduleId, version?): Boolean
    fun isModuleInstalled(moduleId): Boolean
    fun getModuleEnvironment(moduleId): Map<String, String>

    // 构建
    fun executeBuild(projectPath, buildCommand, moduleIds): Flow<BuildProgress>

    // 项目
    suspend fun createProject(projectName, template, outputDir): Result<ProjectConfig>

    // 终端
    fun openTerminal(moduleIds): TerminalSession

    // 语言 SDK（新增）
    fun getLanguageSdk(languageId): LanguageSdkInfo?
    fun getLanguageSdks(): List<LanguageSdkInfo>

    // 语法补全上下文（新增）
    fun resolveCompletionContext(filePath, moduleIds): CompletionContext
}

data class LanguageSdkInfo(
    val languageId: String,       // "python", "rust", "go", ...
    val packageName: String,      // "env-python", "env-rust", ...
    val version: String,          // "3.11.8"
    val homeDir: String,          // SDK 安装根目录
    val binaryPaths: Map<String, String>,  // 工具名 → 绝对路径
    val includePaths: List<String>,        // 头文件/源码搜索路径
    val libraryPaths: List<String>,        // 库搜索路径
    val envVars: Map<String, String>,      // 完整环境变量
    val ideTasks: List<String>             // 可用 IDE 任务
)

data class CompletionContext(
    val languageId: String,
    val sdkInfo: LanguageSdkInfo?,
    val additionalIncludePaths: List<String>,
    val environment: Map<String, String>
)
```

---

## 支持的语言映射

| 语言 | 语言ID | 包名 | 入口二进制 | IDE 任务 |
| :--- | :--- | :--- | :--- | :--- |
| Java | java | env-jdk | java, javac, jar, javadoc, jdb | java-compile/run/debug/javadoc |
| Kotlin | kotlin | env-jdk | java, javac | java-compile/run/debug |
| Python | python | env-python | python3, pip3 | python-run, pip-install/freeze, venv-create |
| JavaScript | javascript | env-node | node, npm, npx | node-run, npm-install/run-script/test |
| TypeScript | typescript | env-node | node, npm, npx, tsc | node-run, npm-install/run-script/test |
| C | c | env-cc | clang, clang++, ar, nm, objdump, strip | c-compile, c++-compile, cmake-build, make-build |
| C++ | cpp | env-cc | clang, clang++ | c++-compile, cmake-build, make-build |
| Rust | rust | env-rust | rustc, cargo, rustfmt, clippy-driver | cargo-build/run/test/doc/fmt/clippy |
| Go | go | env-go | go, gofmt, goimports, gopls | go-build/run/test/mod-tidy/mod-download/fmt/vet |

---

## 风险与应对

| 风险 | 影响 | 应对 |
| :--- | :--- | :--- |
| 模块二进制交叉编译失败 | 高 | 使用 GitHub Actions 多阶段构建 + NDK 工具链 |
| GPL 许可证污染 | 高 | ✅ 已完成 jackpal 清除；Git 使用 GPL-2.0 但仅作为可选包 |
| 仓库体积膨胀 | 中 | 模块包托管在 GitHub Releases，不在仓库内 |
| 本地构建不可用 | 中 | ✅ 依赖 GitHub Actions CI；本地仅做代码编辑 |
