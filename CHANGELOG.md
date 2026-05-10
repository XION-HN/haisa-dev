# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-05-10

### Added

- **Module Store UI (Phase 3)**: Full Fragment-based Module Store with search, SwipeRefreshLayout, and Material CardView module cards with category chips and dependency counts
- **Module Detail Page**: Shows module info, version, size, install status, dependency list, install/uninstall actions with real-time progress
- **Navigation Component**: Jetpack Navigation graph with store -> detail navigation and up button handling
- **ModuleStoreViewModel**: MVVM architecture with LiveData state management for module list, search, install progress
- **ModuleListAdapter**: ListAdapter with DiffUtil for efficient RecyclerView updates
- **BuildTaskQueue (6.4)**: Build task queue with sequential execution, task status tracking, cancel support, and StateFlow-based queue state observation
- **IDE Plugin API (7.3)**: `HaisaIdeApi` interface defining the full IDE integration contract — module management, build execution, project creation, terminal sessions, and project lifecycle listeners
- **HaisaIdeApiImpl**: Reference implementation of the IDE Plugin API backed by HaisaEnvironment
- **ViewBinding**: Enabled in app build.gradle for type-safe view access
- **Tests**: BuildTaskQueueTest (4 cases), HaisaIdeApiTest (3 cases)

### Fixed

- **BuildEngine**: Removed `Class.forName("android.util.Log")` reflection — now uses pure `System.err` for test compatibility
- **BuildEngineTest**: Rewrote to use block-body `@Test` methods to avoid JUnit4 `InvalidTestClassError`
- **ModuleRepositoryTest**: Switched to `MockitoJUnitRunner.Silent` to avoid `UnnecessaryStubbingException`
- **termexec.c**: Removed `#include <linux/ptmx.h>` which is not available in Android NDK
- **ParcelFileDescriptorCompat**: Fixed `fd.toInt()` — now uses reflection to extract int file descriptor from `FileDescriptor` object

## [1.1.0] - 2026-05-10

### Added

- **Real Module Installation**: `ModuleRepositoryImpl.installModule()` now actually downloads module archives from GitHub Releases via OkHttp, extracts ZIP archives to the module directory, validates structure via `ModuleExtractor`, and reports real-time progress
- **Module Dependency Resolution**: Installation automatically resolves and installs dependencies before the target module
- **BuildEngine**: Full implementation that spawns shell processes with injected module environments, captures stdout/stderr in real-time via Flow, and reports build lifecycle (PREPARING -> COMPILING -> TESTING -> PACKAGING -> FINISHED/FAILED)
- **Project Generator**: `HaisaEnvironment.createProject()` now creates actual project scaffolding on disk with template-appropriate files (Python, Node.js, C/C++, Android/Kotlin, Rust, Go)
- **TerminalActivity**: Complete terminal UI integration with EmulatorView, PTY management via `ParcelFileDescriptorCompat`, module environment injection, and shell process lifecycle management
- **ParcelFileDescriptorCompat**: Utility for opening PTMX, getting PTS name, and granting access for terminal sessions
- **Terminal JNI Native Library**: Full C implementation of `jackpal-termexec2` with `createSubprocessInternal`, `waitFor`, and `sendSignal` native methods, plus CMakeLists.txt for NDK build
- **Module Manifests**: All 6 module manifests now have proper `env_vars` (with `{{install_dir}}`/`{{old_path}}` templates), `dependencies`, `entry_binaries`, `package_id`, and `ide_integrations`
- **Environment from Manifest**: `getModuleEnvironment()` now reads `manifest.json` and applies `resolveEnvVars()` template substitution instead of hardcoded values
- **Tests**: BuildEngineTest (4 cases), ModuleRepositoryTest (7 cases)
- **Gradle Wrapper**: `gradlew` script and `gradle-wrapper.jar` now included in the repository

### Fixed

- **CI Pipeline**: Removed `|| echo` failure suppression - build/test failures now correctly fail the CI
- **CI Pipeline**: Added lint check step, removed fragile ad-hoc wrapper download in favor of committed wrapper
- **Robolectric Dependency**: Added `org.robolectric:robolectric:4.12.1` and `androidx.test:core:1.5.0` to haisa-sdk test dependencies
- **Uninstall in MainActivity**: Now actually calls SDK uninstall instead of just showing a toast
- **Terminal Menu**: `openTerminal()` now launches the real TerminalActivity instead of being commented out
- **OkHttp MockWebServer**: Added to haisa-sdk test dependencies for network layer testing

## [1.0.0] - 2026-05-09

### Added

- **MVC Architecture**: Full Model-View-Controller skeleton
- **Model**: `Models.kt` — `SemanticVersion`, `ModuleManifest`, `ModuleInfo`, `ModuleRelease`, `RepoIndex`
- **Data**: `LocalDataSource` — SharedPreferences persistence for module metadata
- **Network**: `GitHubReleasesSource` — Retrofit + Gson GitHub API client
- **Repository**: `ModuleRepository` interface + `ModuleRepositoryImpl`
- **Service**: `ModuleManager` (singleton), `NetworkProvider` (Retrofit singleton)
- **Controller**: `ModuleController`, `BuildController`, `ProjectController`
- **Utility**: `PathResolver`, `EnvironmentInjector`, `ModuleExtractor`, `VersionManager`
- **VersionManager**: Full SemVer 2.0 support
- **HaisaEnvironment SDK Facade**: Single entry point for IDE integration
- **Android-Terminal-Emulator Integration**: 28 Java files, package renamed
- **Unit Tests**: 8 test files, 50+ test cases
- **App Module**: `MainActivity`, `ModuleAdapter`, Material theme, layouts
- **CI/CD**: GitHub Actions workflow
- **Documentation**: ARCHITECTURE.md, DEVELOPMENT_PLAN.md, INTEGRATION.md, repo-index.json

[1.2.0]: https://github.com/XION-HN/haisa-dev/releases/tag/v1.2.0
[1.1.0]: https://github.com/XION-HN/haisa-dev/releases/tag/v1.1.0
[1.0.0]: https://github.com/XION-HN/haisa-dev/releases/tag/v1.0.0
