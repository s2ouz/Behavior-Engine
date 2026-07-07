# Behavior Engine — v0.2.0 Core Infrastructure

A Visual Behavior Engine for Android. v0.1.0 built the architecture shell; this phase builds a
real engine skeleton behind it — lifecycle, tick loop, module system, events, clock, config —
still with no AI, no screen capture, no Accessibility Service, no OCR, and no automation.

## Opening the project

This project was generated outside Android Studio, so the Gradle wrapper's binary launcher
(`gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`) is not included — binary files
can't be authored as text. `gradle/wrapper/gradle-wrapper.properties` already pins the intended
version (Gradle 8.9). Regenerate the launcher one of two ways:

1. Open the project in a recent Android Studio (Ladybird/Meerkat or newer) — it detects the
   missing wrapper and offers to generate it automatically on sync.
2. Or, with a local Gradle install: `gradle wrapper --gradle-version 8.9` from the project root.

Requires JDK 17 and Android SDK Platform 35 (installed via Android Studio's SDK Manager).

## Package structure

Unchanged from v0.1.0 — this phase adds files, not new packages:

```
com.behaviorengine
├── core
│   ├── common        // App-wide infra: AppConstants, LoggerManager, ConfigManager
│   ├── data           // Clean Architecture data layer (empty — nothing to back yet)
│   ├── domain         // Clean Architecture domain layer — every engine contract lives here
│   └── presentation   // Screens + ViewModels (splash, home, settings)
├── engine             // Concrete implementations of every core.domain.engine interface
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

`vision → recognition → world → behavior → memory → learning → automation → accessibility`
are still empty (each holds a `package-info.kt` explaining its future role) — this phase is
entirely about the engine's *own* machinery in `core.domain.engine` / `engine`, not those
subsystems. A future Vision/Rule/Learning/etc. module will be an `EngineModule` implementation
registered with `ModuleRegistry`, but none is implemented yet.

## The engine skeleton

Every piece below lives as a framework-free interface in `core.domain.engine` with its real
implementation in `engine`, bound together in `di/EngineDiModule.kt` — the same
interface-in-domain, implementation-in-engine split v0.1.0 established for `EngineManager`,
just applied consistently to every new subsystem instead of bolting them onto one class.

- **`EngineLifecycleManager`** — owns the state machine. `EngineStatus` now has eleven states
  (`OFFLINE, INITIALIZING, READY, STARTING, RUNNING, PAUSING, PAUSED, RESUMING, STOPPING,
  STOPPED, ERROR`) instead of v0.1.0's six, modeling transitions as a validated table rather
  than trusting callers: `transitionTo()` returns `false` and leaves state untouched for any
  move not in the table (e.g. `RUNNING -> INITIALIZING` is always rejected). `forceError()` is
  the one deliberate bypass, for when a module has already thrown and validation is moot.

- **`EngineClock`** — tick count, instantaneous FPS (measured from real wall-clock gaps between
  ticks, not just an echo of the configured rate), elapsed-since-last-tick, running time (zeroed
  on stop), and uptime (persists across stop/restart, cleared only on full reset). Kept separate
  from `EngineLoop` deliberately: the loop is just "fires periodically, cancellable," the clock
  is the stateful record of what happened.

- **`EngineLoop`** — the "permanent" tick engine, without a literal `while(true)`: it loops on
  `while (isActive)` inside a cancellable coroutine, so stopping it is just cancelling that
  coroutine. Configurable via `TickRate` (`FPS_10` default, `FPS_30/60/120` prepared for when
  module work per tick actually costs something).

- **`EngineModule` / `ModulePriority` / `ModuleRegistry`** — the plugin contract every future
  subsystem (Vision, Rules, Learning, Memory, Actions, Feedback) will implement
  (`initialize/start/update/pause/resume/stop/release`), a five-tier priority
  (`CRITICAL > HIGH > NORMAL > LOW > BACKGROUND`) controlling init/start order, and the registry
  that holds them. No module exists yet — `EngineManagerImpl` fans lifecycle calls out to
  whatever's registered, which today is nothing.

- **`EventBus` / `EngineEvent`** — Flow-based pub/sub (`SharedFlow`, not a hand-rolled listener
  list — "subscribe" is `events.collect{}`, "unsubscribe" is cancelling that collector's job).
  `EngineEvent` covers lifecycle changes, module events, warnings, errors, and per-tick
  performance readings.

- **`EngineObserver`** — a standing `EventBus` subscriber that folds the raw event stream into
  a running `EngineObserverSnapshot` (last lifecycle change, error/warning counts, last error,
  last performance reading). Distinct from `EngineState`: that's "what's happening right now"
  for the Home screen; this is "what has happened so far," for a future diagnostics screen or
  a crash reporter.

- **`EngineError`** — sealed hierarchy (`InitializationError`, `ModuleError`,
  `ConfigurationError`, `RuntimeError`, `UnknownError`). Every exception a module throws during
  a lifecycle call is caught by `EngineManagerImpl`, wrapped into a `ModuleError` naming which
  module failed, and forces `ERROR` via `EngineLifecycleManager.forceError` — a broken future
  module can't crash the app or leave the engine stuck mid-transition.

- **`EngineConfig`** — `targetTickRate`, `debugEnabled`, `loggingEnabled`,
  `performanceMonitorEnabled`, `autoStart`, and reserved `aiEnabled`/`accessibilityEnabled`/
  `visionEnabled` flags, held by `ConfigManager`. Only `targetTickRate` is actually consulted
  this phase (by `EngineManagerImpl.start()`); the rest are declared so later phases don't
  reshape this data class, the same pattern `settings.AppSettings` used for AI/accessibility.

- **`EngineManager` / `EngineManagerImpl`** — the orchestrator gluing all of the above together
  behind the same simple contract the ViewModel already depended on
  (`initialize/start/pause/resume/stop/reset` + `StateFlow<EngineState>`), so growing the engine
  internally never required touching `HomeViewModel`'s dependency. `EngineState` now also
  carries `currentTick`, `currentFps`, `uptimeMillis`, and `loadedModules` alongside v0.1.0's
  `status`/`currentPhase`/`runningTimeMillis`/`version`.

- **`LoggerManager`** gained a `performance()` level (tagged `.../Perf`, used once per tick).
  File logging is still deferred — every log call already funnels through this one class, so
  that's a single new tree planted in `init()` whenever a future phase needs it.

## UI

Home screen now shows status, current tick, current FPS, running time, and loaded modules
(empty today — "None" — since no module is registered), with six buttons mapped one-to-one onto
legal transitions: **Initialize** (enabled only OFFLINE), **Start** (READY), **Pause**
(RUNNING), **Resume** (PAUSED), **Stop** (RUNNING or PAUSED), **Reset** (STOPPED or ERROR).
Buttons that don't correspond to a legal transition from the current state are simply disabled,
rather than relying on `EngineLifecycleManager` to reject a tap the UI should never have
allowed in the first place.

## Dependency injection

`di/EngineDiModule.kt` binds all seven new interfaces to their `engine` package
implementations. It's deliberately *not* named `EngineModule.kt` — that name now belongs to
`core.domain.engine.EngineModule`, the plugin contract, and reusing it for the Hilt module would
make every import ambiguous to a reader even though the compiler can tell them apart by package.

## What's deliberately not here

AI/ML, screen capture, Accessibility Service behavior, OCR, and automation execution are all
still out of scope. So is a real `EngineModule` implementation — Vision, Rules, Learning, Memory,
Actions, and Feedback modules will register with `ModuleRegistry` in later phases; this phase
only guarantees the registry, the priority system, and the lifecycle fan-out are already correct
and tested against *some* module before the first real one shows up.
