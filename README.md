# Behavior Engine — v0.5.0 Core Prototype Freeze

A Visual Behavior Engine for Android. v0.1.0 built the architecture shell, v0.2.0 built the
engine's internal machinery, v0.3.0 made it a real Android background engine. **This phase adds
no new user-facing features.** It validates, stabilizes, and freezes everything built so far —
a permanent diagnostics/validation layer, a cleanup pass, and this document — as the official
baseline every future phase builds on. Still no AI, no screen capture, no Accessibility Service,
no OCR, and no automation.

## Opening the project

This project was generated outside Android Studio, so the Gradle wrapper's binary launcher
(`gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`) is not included — binary files
can't be authored as text. `gradle/wrapper/gradle-wrapper.properties` already pins the intended
version (Gradle 8.9). Regenerate the launcher one of two ways:

1. Open the project in a recent Android Studio (Ladybird/Meerkat or newer) — it detects the
   missing wrapper and offers to generate it automatically on sync.
2. Or, with a local Gradle install: `gradle wrapper --gradle-version 8.9` from the project root.

Requires JDK 17 and Android SDK Platform 35 (installed via Android Studio's SDK Manager).

## Architecture at a glance

```mermaid
graph TD
    Screen["HomeScreen (Compose)"] -->|collectAsState| VM["HomeViewModel"]
    VM -->|actions| EM["EngineManager (thin facade)"]
    VM -->|state| Store["EngineStateStore"]

    EM --> RC["RuntimeController"]
    EM --> SC["EngineServiceConnection"]
    SC -.controls.-> Svc["EngineService (foreground host)"]

    RC --> LM["EngineLifecycleManager (FSM)"]
    RC --> Clock["EngineClock"]
    RC --> Loop["EngineLoop"]
    RC --> Registry["ModuleRegistry (EngineModule contract)"]

    Store --> LM
    Store --> Clock
    Store --> Health["EngineHealthMonitor"]
    Store --> Perf["PerformanceTimer"]

    Diag["EngineDiagnosticsManager"] --> Metrics["EngineMetrics"]
    Diag --> Health
    Diag --> Validator["EngineValidator"]
    Diag --> Observer["EngineObserver"]
    EM -.self-check on initialize.-> Diag

    LM --> Bus["EventBus"]
    Registry --> Bus
    Bus --> Observer
    Health --> Bus
```

Every node except `HomeScreen`/`HomeViewModel`/`EngineService` is a framework-free interface in
`core/domain/engine/` with its real implementation in `engine/`, bound together in
`di/EngineDiModule.kt`. `HomeViewModel` only ever depends on `EngineManager` (to act) and
`EngineStateStore` (to observe) — it never reaches into `RuntimeController`, `EngineClock`, or
any other internal collaborator directly.

## Package structure

Unchanged since v0.1.0 — this phase adds files, not new packages:

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
├── behavior           // (future) rules / actions / feedback
├── memory             // (future) persisted history for learning to train on
├── learning           // (future) adapts rules/decisions over time
├── automation         // (future) executes actions against the device
├── accessibility      // (future) AccessibilityService integration
├── services           // EngineService (foreground host); future AccessibilityService lives here too
├── settings           // User preference model + DataStore prep (no persistence yet)
├── utils              // Small pure-function helpers (time/number formatting)
├── di                 // Hilt modules
├── navigation         // Navigation Compose graph + route definitions
└── ui/theme           // Compose dark theme, typography, color tokens
```

## What this phase added: the validation layer

Three new components, all genuinely functional (none are stubs), none exposed as new UI:

- **`EngineMetrics`** — an on-demand, pull-based snapshot (`snapshot(): EngineMetricsSnapshot`)
  combining every numeric signal (`EngineClock` timing, `PerformanceTimer` profiling,
  `EngineHealthMonitor` counts) into one object. Deliberately *not* a `StateFlow`: it exists for
  one-off consumers (a diagnostics dump, a log line, a future test assertion) that just want
  "the numbers, right now" without subscribing to anything — `EngineStateStore` already covers
  the continuously-observed case.
- **`EngineValidator`** — runtime self-checks encoding invariants the engine relies on: no two
  registered modules sharing an id, an active module always present in the full module list, the
  configured tick rate resolving to a positive interval, and `EngineHealthMonitor` never
  reporting the runtime active while the engine isn't alive or the lifecycle is in `ERROR`. Most
  of these hold by construction today (no real `EngineModule` exists yet to violate the registry
  invariant) — their value is as a **regression guard**, not evidence something is currently
  broken.
- **`EngineDiagnosticsManager`** — bundles `EngineMetrics` + `EngineHealthMonitor` +
  `EngineValidator` + `EngineObserver` into one `DiagnosticsReport`. `EngineManagerImpl.initialize()`
  calls it once automatically after a successful boot and logs the result — this is the only new
  code path this phase adds, and it's a self-check on an *existing* action, not a new feature.

## Cleanup performed this phase

An audit pass (full read of all ~55 Kotlin files) found and fixed:

- **Duplicated `CoroutineScope(SupervisorJob() + Dispatchers.Default)` construction** across four
  singletons (`EngineLoopImpl`, `EngineObserverImpl`, `EngineStateStoreImpl`,
  `EngineHealthMonitorImpl`) — replaced with one `@EngineCoroutineScope`-qualified scope provided
  once in `di/AppModule.kt` and injected into all four. Never explicitly cancelled, by design:
  it's `@Singleton` in `SingletonComponent`, living exactly as long as the process does, the same
  reasoning Android's own guidance gives for an application-wide scope.
- **A real Locale bug**: `RuntimeControllerImpl`'s per-tick fps log used `"%.1f".format(...)`
  without pinning `Locale.US`, unlike every other formatted value in the app — on a device whose
  default locale uses `,` as a decimal separator, this could have logged fps as e.g. `9,8`
  instead of `9.8`. Fixed by routing through the new `utils/NumberFormatter`, alongside the
  existing `utils/TimeFormatter`.
- **A duplicated `MILLIS_PER_SECOND = 1000L` constant**, independently declared in `TickRate.kt`
  and `EngineClockImpl.kt` (plus an unnamed `1000` literal in `TimeFormatter.kt`) — consolidated
  into `AppConstants.MILLIS_PER_SECOND`, the designated single source of truth for exactly this
  kind of value.
- **Duplicated `EngineStatus` button-eligibility logic**, independently hardcoded in both
  `HomeScreen.kt` (button `enabled` state) and `RuntimeControllerImpl.reset()` (the enforcement
  guard) — extracted into six pure predicates (`EngineStatus.canInitialize()`, `canStart()`,
  `canPause()`, `canResume()`, `canStop()`, `canReset()`) shared by both call sites.
- **Excessive per-tick logging**: `RuntimeControllerImpl` logged an fps line on *every* tick,
  meaning 120 logcat writes/sec at `TickRate.FPS_120`. Throttled to once per second regardless of
  configured tick rate — the `EventBus` publish (used by `EngineObserver` and any future
  subscriber) still carries every tick's full-resolution reading.
- **An inconsistent log tag**: `EngineLifecycleManagerImpl` used `"EngineLifecycle"` while every
  other `*Impl` class's tag drops only the `Impl` suffix (`"ModuleRegistry"`,
  `"RuntimeController"`, `"EngineServiceConnection"`) — renamed to `"EngineLifecycleManager"` for
  consistency.
- **Missing KDoc** added to eight public classes that had none (`EngineClockImpl`,
  `EngineHealthMonitorImpl`, `EngineLoopImpl`, `EngineServiceConnectionImpl`, `EventBusImpl`,
  `ModuleRegistryImpl`, `PerformanceTimerImpl`, `MainActivity`).
- **An unused import** in `di/AppModule.kt` (`AppConstants`, referenced only from a KDoc link,
  which doesn't count as a use to the compiler) — the KDoc link now uses a fully-qualified
  reference instead, matching the pattern used elsewhere in the codebase.

Explicitly reviewed and left unchanged: `settings/SettingsManager.kt`'s
`TODO(future phase): persist to and read from dataStore` — this is v0.1.0's deliberate, accurate,
still-true deferred-persistence marker, not leftover debugging code.

## Thread safety notes

- `ModuleRegistryImpl` synchronizes all reads/writes of its module map on a private lock, since
  `RuntimeControllerImpl` reads it from the tick loop while a future module could register
  itself from a different thread.
- `PerformanceTimerImpl`'s tick counters are plain `var`s, safe only because `measureTick` is
  exclusively called from the tick loop's single sequential coroutine (never two ticks
  concurrently) and `measureStartup` can only run once per session, before any tick is reachable.
  Documented in the class's KDoc as an assumption a future phase must re-check if ticks ever
  start overlapping.
- Every other piece of shared engine state is a `StateFlow`/`SharedFlow`, whose updates are
  inherently atomic; no other manual synchronization is needed or present.

## What's deliberately not here

AI/ML, screen capture, Accessibility Service behavior, OCR, and automation execution are all
still out of scope. So is a real `EngineModule` implementation, a runtime `POST_NOTIFICATIONS`
permission request flow, and any UI surface for the new diagnostics layer — all three remain
future work, unchanged from v0.3.0's scope. **This version is the frozen Core Prototype**; future
specifications build only on top of it, not by reshaping it.
