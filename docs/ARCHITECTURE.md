# Haisa Des 架构设计文档

> 版本: v1.0  
> 日期: 2026-05-09

---

## 系统架构总览

```
┌─────────────────────────────────────────────────────────┐
│                     用户界面层 (View)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ ModuleStore  │  │   Terminal   │  │ BuildPanel   │  │
│  │  Fragment    │  │  Fragment    │  │  Fragment    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────┤
│                    控制器层 (Controller)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ ModuleCtrl   │  │ TerminalCtrl │  │  BuildCtrl   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────┤
│                     业务层 (Service)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ ModuleMgr    │  │ TerminalEng  │  │ BuildEngine  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────┤
│                     数据层 (Model)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │LocalDataSrc  │  │  RemoteSrc   │  │ ModuleConfig │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────┤
│                      核心组件                             │
│  ┌──────────────────────────────────────────────────┐  │
│  │        Android-Terminal-Emulator (已修改)         │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │  │
│  │  │TermSession  │  │  TermEmul   │  │TermView │ │  │
│  │  └─────────────┘  └─────────────┘  └─────────┘ │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## MVC 架构详解

### Model 层

负责数据管理与持久化。

```
model/
├── ModuleConfig.kt          # 模块配置数据类
├── InstalledModule.kt       # 已安装模块数据类
├── ModuleManifest.kt        # 模块元数据解析 (manifest.json)
├── data/
│   ├── LocalDataSource.kt   # 本地数据源 (SQLite/SharedPref)
│   ├── RemoteDataSource.kt  # 远程数据源 (GitHub API)
│   └── CacheManager.kt      # 缓存管理 (LRU/磁盘)
└── repository/
    ├── ModuleRepository.kt       # 模块仓库接口
    └── ModuleRepositoryImpl.kt   # 模块仓库实现
```

### View 层

负责 UI 展示与用户交互。

```
view/
├── ModuleStoreFragment.kt      # 模块商店界面
├── TerminalFragment.kt         # 终端界面
├── BuildPanelFragment.kt         # 构建面板
├── ProjectTemplateFragment.kt  # 项目模板选择
├── adapter/
│   ├── ModuleAdapter.kt        # 模块列表适配器
│   └── ProgressAdapter.kt      # 进度显示适配器
└── widget/
    ├── HaisaTerminalView.kt    # 自定义终端视图
    └── DownloadProgressBar.kt  # 下载进度条
```

### Controller 层

负责业务逻辑调度，连接 View 与 Model。

```
controller/
├── ModuleController.kt       # 模块管理控制器
│   └── 处理: 安装/卸载/切换/搜索
├── TerminalController.kt     # 终端控制器
│   └── 处理: 创建会话/执行命令/环境注入
├── BuildController.kt        # 构建控制器
│   └── 处理: 构建任务调度/日志回显/错误处理
└── EnvironmentController.kt  # 环境控制器
    └── 处理: 变量注入/路径解析/版本匹配
```

---

## 模块生命周期

```
┌───────────┐
│  未安装   │
└─────┬─────┘
      │ 用户点击安装
      ▼
┌───────────┐     ┌───────────┐
│  下载中   │────>│  下载失败  │
└─────┬─────┘     └───────────┘
      │
      ▼
┌───────────┐
│  解压中   │
└─────┬─────┘
      │
      ▼
┌───────────┐     ┌───────────┐
│  验证中   │────>│  验证失败  │
└─────┬─────┘     └───────────┘
      │
      ▼
┌───────────┐
│  已就绪   │
└───────────┘
      │
      │ 用户切换版本
      ▼
┌───────────┐
│  切换中   │
└─────┬─────┘
      │
      ▼
┌───────────┐
│  已就绪   │
└───────────┘
```

---

## 关键设计决策

### 1. 为什么选择 Android-Terminal-Emulator?

- **成熟稳定**: 多年维护，社区活跃
- **功能完整**: 支持 VT100/VT220/VT320 终端序列
- **易于集成**: 模块化的 `TermSession` 和 `TermView`
- **原生支持**: Java 实现，无需额外 Native 库

### 2. 为什么使用 GitHub Releases 作为分发平台?

- **免费可靠**: GitHub 提供全球 CDN 加速
- **版本管理**: 天然支持语义化版本控制
- **API 丰富**: REST API 支持列表查询和下载
- **开源友好**: 社区开发者容易参与

### 3. 模块依赖如何解决?

```
env-jdk (目标)
├── dependency: env-base 1.0.0
└── manifest.json 声明:
    {
      "dependencies": [
        {"id": "env-base", "version": ">=1.0.0"}
      ]
    }

安装流程:
1. 解析 env-jdk 的 manifest.json
2. 发现依赖 env-base 1.0.0
3. 递归检查 env-base 的依赖 (无)
4. 按拓扑排序下载: env-base -> env-jdk
5. env-base 解压到 modules/env-base/1.0.0/
6. env-jdk 解压到 modules/env-jdk/17.0.8/
7. 生成版本锁定文件 version_locks.json
```

---

## 性能优化策略

| 优化点 | 策略 | 实现 |
| :--- | :--- | :--- |
| 下载加速 | 断点续传 + 多线程 | OkHttp Range 请求 + 并发下载器 |
| 磁盘 I/O | 异步解压 + 进度回调 | Kotlin Coroutines + Flow |
| 内存占用 | 按需加载 + 智能清理 | LRU 缓存 + 定期扫描旧版本 |
| 启动速度 | 模块懒加载 | 仅在首次使用时初始化模块 |
| 网络请求 | CDN 缓存 + 本地索引 | GitHub Pages 托管 repo-index.json |

---

## 安全设计

```
1. 沙盒隔离
   └── 所有模块安装在 App 私有目录
   └── /data/data/<pkg>/files/modules/

2. 签名校验 (规划中)
   └── 模块包 .tar.xz 包含 .sha256 校验文件
   └── 下载后校验完整性

3. 权限最小化
   └── 不需要 Root 权限
   └── 不需要网络权限以外的特殊权限

4. 代码签名 (规划中)
   └── GPG 公钥验证模块作者身份
```

---

## 扩展接口

如需添加新语言支持，实现以下接口：

```kotlin
interface LanguagePlugin {
    val moduleId: String
    val supportedProjectTypes: List<String>
    
    fun getTemplate(projectType: String): ProjectTemplate
    fun getBuildCommand(projectType: String): String
    fun getRequiredModules(): List<String>
}

class PythonPlugin : LanguagePlugin {
    override val moduleId = "env-python"
    override val supportedProjectTypes = listOf("flask", "django", "script")
    
    override fun getTemplate(projectType: String): ProjectTemplate {
        return when (projectType) {
            "flask" -> FlaskTemplate()
            "django" -> DjangoTemplate()
            else -> ScriptTemplate()
        }
    }
}
```
