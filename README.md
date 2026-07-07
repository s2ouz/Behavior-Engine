# Behavior Engine — v0.1.0 Foundation

A Visual Behavior Engine for Android. This is the **architecture foundation** phase: no AI,
no screen capture, no Accessibility Service, no OCR, no automation. Just the scaffolding every
later phase will build on.

## Opening the project

This project was generated outside Android Studio, so the Gradle wrapper's binary launcher
(`gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`) is not included — binary files
can't be authored as text. `gradle/wrapper/gradle-wrapper.properties` already pins the intended
version (Gradle 8.9). Regenerate the launcher one of two ways:

1. Open the project in a recent Android Studio (Ladybird/Meerkat or newer) — it detects the
   missing wrapper and offers to generate it automatically on sync.
2. Or, with a local Gradle install: `gradle wrapper --gradle-version 8.9` from the project root.

Requires JDK 17 and Android SDK Platform 35 (installed via Android Studio's SDK Manager).

## Why this package structure

`com.behaviorengine` is organized as a single app module with package-level boundaries rather
than Gradle sub-modules. Multi-module Gradle setups add real build-time value once several
teams or truly independent build targets exist; at Foundation phase it would only add
ceremony (inter-module API surfaces, dependency graphs) around code that doesn't have anything
to be independent from yet. Package boundaries keep the same separation of concerns and can be
promoted to Gradle modules later without a rewrite, once a package's boundary is proven stable.

```
com.behaviorengine
├── core
│   ├── common        // App-wide infra: AppConstants, LoggerManager, ConfigManager
│   ├── data           // Clean Architecture data layer (empty — nothing to back yet)
│   ├── domain         // Clean Architecture domain layer — engine contracts live here
│   └── presentation   // Screens + ViewModels (splash, home, settings)
├── engine             // EngineManager's real implementation — the future orchestrator
├── vision             // (future) screen capture / frame acquisition
├── recognition        // (future) OCR + visual element recognition
├── world              // (future) structured "what's on screen" model
├── behavior
│   ├── rules          // (future) conditions over world state
│   ├── actions        // (future) effects a rule can trigger
│   └── feedback       // (future) outcomes of executed actions
├── memory             // (future) persisted history for learning to train on
├── learning           // (future) adapts rules/decisions over time
├── automation         // (future) executes actions against the device
├── accessibility      // (future) AccessibilityService integration
├── services           // (future) Android Service hosts (foreground service, etc.)
├── settings           // User preference model + DataStore prep (no persistence yet)
├── utils              // Small pure-function helpers (e.g. time formatting)
├── di                 // Hilt modules
├── navigation         // Navigation Compose graph + route definitions
└── ui/theme           // Compose dark theme, typography, color tokens
```

The `vision → recognition → world → behavior → memory → learning → automation → accessibility`
packages read top-to-bottom as the intended data pipeline of the engine: capture the screen,
recognize what's on it, model it, decide what to do, remember what happened, learn from it, and
act — each package is a placeholder for exactly one pipeline stage. `core` holds the four
Clean Architecture layers (common/data/domain/presentation) that cut across every stage.
Packages with no functional code yet contain a single `package-info.kt` documenting what will
live there and why, so the intended shape of the project is visible without writing filler
classes.

## Core foundation

- **`EngineManager`** (`core.domain.engine`) — the lifecycle contract (`start()`, `stop()`,
  `pause()`, `resume()`, plus an observable `StateFlow<EngineState>`). Framework-free by design,
  so the domain layer never depends on how the engine is actually implemented.
- **`EngineManagerImpl`** (`engine`) — the real implementation, bound to the interface via Hilt
  (`di/EngineModule.kt`). Today it only manages `EngineStatus` transitions and a running-time
  clock, enough to prove the state → ViewModel → Compose pipeline end-to-end; future phases wire
  vision/recognition/world/behavior initialization into `start()`/`stop()` here.
- **`EngineState`** — immutable snapshot (`status`, `currentPhase`, `runningTimeMillis`,
  `version`, plus a `reserved` map for fields future phases need without reshaping consumers).
- **`EngineStatus`** — `OFFLINE / STARTING / RUNNING / PAUSED / STOPPING / ERROR`.
- **`EngineEvent`** — sealed class for one-shot occurrences (`Started`, `Stopped`, `Paused`,
  `Resumed`, `Error`), declared but not yet emitted anywhere — no subsystem exists yet that has
  anything interesting to raise.
- **`LoggerManager`** (`core.common`) — the only class allowed to call Timber directly, so a
  future crash-reporting or remote-log tree only needs to change in one place.
- **`ConfigManager`** (`core.common`) — placeholder for engine-internal runtime configuration,
  intentionally separate from `settings.AppSettings`, which is user-facing preferences.
- **`AppConstants`** (`core.common`) — app version (from `BuildConfig`), project name, engine
  version, current phase label, debug flag. The one place these values are allowed to live.

## UI

Jetpack Compose, dark theme only (`ui/theme`), Navigation Compose with three routes
(`Splash → Home → Settings`). Only Home is functional: it shows the engine status, current
phase, running time (while running), and Start/Stop buttons wired to `HomeViewModel`, which
adapts `EngineManager`'s `StateFlow` to Compose via `collectAsState()`. Settings is a
placeholder screen; Splash exists only to prove the nav graph itself works.

## Dependency injection

Hilt, `SingletonComponent` scope. `LoggerManager` and `ConfigManager` use `@Inject constructor`
directly; `di/EngineModule.kt` binds `EngineManager` → `EngineManagerImpl`; `di/AppModule.kt`
provides the (currently unused) settings `DataStore<Preferences>`. `HomeViewModel` is a
`@HiltViewModel` obtained via `hiltViewModel()` in Compose.

## Settings foundation

`settings.AppSettings` models `theme` (`ThemeMode`), `debugModeEnabled`, `loggingEnabled`, and
reserved `AiSettings` / `AccessibilitySettings` blocks for future phases.
`settings.SettingsManager` holds this in memory today; the DataStore instance is already
injected into it so a future phase only has to add read/write logic in one place — no
persistence is wired up yet, per this phase's scope.

## What's deliberately not here

AI/ML, screen capture, Accessibility Service behavior, OCR, and automation execution are all
out of scope for this phase. Their packages exist (see the pipeline above) so later phases have
an agreed home to land in, not a repo-wide refactor to make room.
