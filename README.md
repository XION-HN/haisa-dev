# Haisa Dev Environment

> Android 模块化跨语言开发平台 - 让移动设备成为真正的开发工作站

---

## 项目概述

Haisa Dev 是一个基于 **Android-Terminal-Emulator** 构建的模块化开发环境，为 Haisa IDE 系列提供底层运行时支持。采用 **MVC 架构** 设计，支持通过在线模块商店按需下载 C/C++、Java、Python、Node.js 等语言运行环境。

### 核心特性
- 🧩 模块化设计：按需下载语言运行时，最小化安装体积
- 📦 GitHub Releases 分发：预编译资源包，全球 CDN 加速
- 🏗️ MVC 架构：清晰的职责分离，易于扩展和维护
- 🔧 基于 Android-Terminal-Emulator：成熟的终端模拟器核心
- 📱 IDE 深度集成：与 Haisa IDE 系列无缝协作

---

## 快速开始

### 环境要求
- Android 8.0+ (API 26+)
- 最低 RAM: 2GB
- 存储空间: 200MB（基础环境）+ 按需下载模块

### 集成方式

```groovy
// build.gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.haisa-dev:haisa-dev:latest'
}
```

---

## 项目结构 (MVC 架构)

```
haisa-dev/
├── app/                          # 演示应用 (Demo App)
│   ├── src/main/
│   │   ├── java/com/haisa/dev/
│   │   │   ├── HaisaApp.java     # Application 基类
│   │   │   ├── MainActivity.kt   # 主界面
│   │   │   ├── HaisaDevManager.kt       # 开发环境管理器 (Facade)
│   │   │   ├── model/
│   │   │   │   ├── ModuleConfig.kt      # 模块配置数据
│   │   │   │   ├── ModuleRepository.kt  # 模块仓库接口
│   │   │   │   └── data/
│   │   │   │       ├── LocalDataSource.kt       # 本地模块数据源
│   │   │   │       └── GitHubReleasesSource.kt  # GitHub 远程数据源
│   │   │   ├── view/
│   │   │   │   ├── ModuleStoreFragment.kt    # 模块商店界面
│   │   │   │   ├── TerminalFragment.kt       # 终端界面
│   │   │   │   ├── ProjectTemplateFragment.kt # 项目模板选择
│   │   │   │   └── adapter/
│   │   │   │       └── ModuleAdapter.kt      # 模块列表适配器
│   │   │   ├── controller/
│   │   │   │   ├── ModuleController.kt      # 模块管理控制器
│   │   │   │   ├── TerminalController.kt    # 终端控制器
│   │   │   │   └── BuildController.kt     # 构建控制器
│   │   │   └── service/
│   │   │       ├── ModuleDownloadService.kt  # 模块下载服务
│   │   │       └── EnvironmentService.kt   # 环境注入服务
│   │   └── res/
│   │       └── layout/
│   └── build.gradle
│
├── haisa-sdk/                    # SDK 库（供其他项目集成）
│   ├── src/main/java/com/haisa/sdk/
│   │   ├── HaisaEnvironment.kt      # 对外暴露的核心 API
│   │   ├── engine/
│   │   │   ├── ModuleEngine.kt          # 模块引擎
│   │   │   ├── TerminalEngine.kt        # 终端引擎
│   │   │   └── BuildEngine.kt         # 构建引擎
│   │   ├── interface/
│   │   │   ├── OnModuleInstallListener.kt     # 模块安装回调
│   │   │   ├── OnEnvironmentReadyListener.kt  # 环境就绪回调
│   │   │   └── OnBuildProgressListener.kt     # 构建进度回调
│   │   └── util/
│   │       ├── ModuleExtractor.kt       # 模块解压工具
│   │       ├── EnvironmentInjector.kt   # 环境注入工具
│   │       └── PathResolver.kt          # 路径解析工具
│
├── terminal-emulator/              # 核心终端模拟器 (基于 Android-Terminal-Emulator 修改)
│   ├── emulator/
│   │   ├── TerminalSession.kt      # 终端会话管理
│   │   ├── TerminalEmulator.kt     # 终端模拟器核心
│   │   └── TermSessionProvider.kt  # 会话提供者
│   └── view/
│       └── HaisaTerminalView.kt    # 自定义终端视图
│
├── modules/                        # 模块定义与构建脚本
│   ├── env-base/
│   ├── env-cc/
│   ├── env-jdk/
│   ├── env-python/
│   ├── env-node/
│   └── env-git/
│
├── .github/
│   └── workflows/
│       └── build-modules.yml       # CI/CD 流水线
│
├── docs/                           # 详细文档
│   ├── ARCHITECTURE.md             # 架构设计文档
│   ├── INTEGRATION.md              # 集成方案文档
│   ├── DEVELOPMENT_PLAN.md         # 开发计划流程表
│   └── API_REFERENCE.md            # API 参考手册
│
├── scripts/                        # 构建与部署脚本
│   ├── build-all.sh
│   ├── build-module.sh
│   └── release.sh
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 开发计划流程表

详见 [DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md)

---

## 集成方案

详见 [INTEGRATION.md](docs/INTEGRATION.md)

---

## 许可证

MIT License - 详见 [LICENSE](LICENSE)
