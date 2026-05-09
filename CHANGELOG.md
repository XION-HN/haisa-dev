# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
  - Parse semantic versions (major.minor.patch + pre-release + build metadata)
  - Version comparison with pre-release ordering (alpha < beta < rc < stable)
  - Bump operations (major, minor, patch, pre-release)
  - Compatibility checking (`isCompatible`, `satisfiesRequirement`)
  - `findLatestVersion` / `findLatestPreRelease` for module update resolution

- **HaisaEnvironment SDK Facade**: Single entry point for IDE integration

- **Android-Terminal-Emulator Integration**: 28 Java files, package renamed
  - `jackpal.androidterm` → `com.haisa.terminal`
  - `jackpal.androidterm.emulatorview` → `com.haisa.terminal.emulatorview`

- **Unit Tests**: 8 test files, 50+ test cases
  - Models, PathResolver, EnvironmentInjector, ModuleExtractor
  - GitHubReleasesSource, LocalDataSource, ProjectController, VersionManager

- **App Module**: `MainActivity`, `ModuleAdapter`, Material theme, layouts

- **CI/CD**: GitHub Actions workflow
  - Build + test all modules (haisa-sdk, terminal-emulator, app)
  - Auto-generate and commit `repo-index.json` on main push
  - Upload build artifacts and test results

- **Documentation**
  - `docs/ARCHITECTURE.md` — MVC architecture design
  - `docs/DEVELOPMENT_PLAN.md` — 13-week development plan
  - `docs/INTEGRATION.md` — 4 integration methods for 3rd-party projects
  - `docs/repo-index.json` — module index for IDE store

[1.0.0]: https://github.com/XION-HN/haisa-dev/releases/tag/v1.0.0
