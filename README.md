# Behavior Engine — v0.3.0 Runtime Foundation

A Visual Behavior Engine for Android. v0.1.0 built the architecture shell, v0.2.0 built the
engine's internal machinery (lifecycle, tick loop, modules, events). This phase makes the engine
a real Android *background* engine — a foreground Service, a session concept, performance
timing, and health reporting — still with no AI, no screen capture, no Accessibility Service,
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

## Package structure

Unchanged from v0.1.0/v0.2.0 — this phase adds files, not new packages, with one exception:
`services/` now holds a real `EngineService` instead of only a `package-info.kt` placeholder.

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
├── services           // EngineService (foreground host); future AccessibilityService lives here too
├── settings           // User preference model + DataStore prep (no persistence yet)
├── utils              // Small pure-function helpers (e.g. time formatting)
├── di                 // Hilt modules
├── navigation         // Navigation Compose graph + route definitions
└── ui/theme           // Compose dark theme, typography, color tokens
```

## Why EngineManager split into three collaborators

v0.2.0's `EngineManagerImpl` did everything itself: validated transitions, drove the tick loop,
fanned out to modules. That was fine when "control the engine" meant one thing. This phase adds
a second, orthogonal responsibility — owning a real Android Service — and rather than bolt it
onto an already-busy class, the tick-loop mechanics moved out into their own component:

- **`RuntimeController`** — everything v0.2.0's `EngineManagerImpl` used to do: fan out
  `EngineModule` lifecycle calls via `ModuleRegistry`, drive `EngineLoop`/`EngineClock` in
  lockstep, validate every transition through `EngineLifecycleManager`, and now also wrap
  startup/tick timing through `PerformanceTimer`. Its behavior is unchanged from v0.2.0 — it was
  moved, not rewritten.
- **`EngineServiceConnection`** — a framework-free handle (`isConnected: StateFlow<Boolean>`,
  `connect()`, `disconnect()`) onto the real `EngineService`. No Android types appear in the
  interface; only its `engine`-package implementation knows it's actually starting/stopping an
  Android Service.
- **`EngineManager` / `EngineManagerImpl`** — now a thin facade (~35 lines) coupling the two:
  `initialize()` delegates to `RuntimeController.initialize()` and, only if that succeeds,
  calls `EngineServiceConnection.connect()`; `reset()` mirrors that in reverse. `start/pause/
  resume/stop` are pure one-line delegations to `RuntimeController` — the Service's lifetime is
  tied to the *session* (Initialize → Reset), not to whether the tick loop is currently running.

`EngineManager` is still the *only* class allowed to call `EngineServiceConnection.connect()` /
`disconnect()` — not a ViewModel, not `EngineService` itself — matching this phase's spec.

## EngineService

A real Android foreground Service (`services/EngineService.kt`) with **no business logic**: it
holds no reference to `RuntimeController` or `ModuleRegistry`. The tick loop already runs on its
own coroutine scope regardless of any Android component's lifecycle (true since v0.2.0); what it
doesn't survive on its own is the *process* being killed once there's no visible Activity. This
Service exists purely to promote that process to the foreground with a persistent, honest
notification, so the engine keeps ticking while the app is backgrounded. `onStartCommand`
promotes to foreground immediately and returns `START_NOT_STICKY` — restarting after a process
kill is a decision only `EngineManager.initialize()` should make, not something the platform
should do unprompted while the lifecycle state machine doesn't know it happened. Uses the
`specialUse` foreground service type (API 34+) since none of the platform's predefined
categories (`dataSync`, `mediaPlayback`, etc.) honestly describe "run an app's own background
computation loop."

## EngineSession

`core.domain.engine.EngineSession` (sessionId, start time, elapsed time, status, tick, fps,
reserved stats) identifies *which run this is*, as opposed to `EngineState`'s "what is the
engine doing." `EngineStateStoreImpl` mints a new UUID the moment the engine leaves OFFLINE for
INITIALIZING and clears it the moment it returns to OFFLINE — never on a mere `stop()` — by
reacting to `EngineEvent.LifecycleChanged` off the `EventBus`, the same event-driven pattern
`EngineObserver` already used in v0.2.0.

## EngineStateStore

The single source of truth every UI component observes (`engineState`, `session`, `health`,
`performance`). `HomeViewModel` previously read `EngineManager.engineState` directly; now
`EngineManager` only exposes actions, and every observable flow comes from here instead — "no UI
class should own engine state" per this phase's spec.

## PerformanceTimer

Measures three things `EngineClock` doesn't: startup duration (one call, `initialize()`), and
last/average tick duration (how long a tick's own CPU work takes, as opposed to `EngineClock`'s
FPS, which measures wall-clock time *between* ticks). Runtime duration is deliberately *not*
independently re-measured — `RuntimeControllerImpl` feeds it `EngineClock`'s own uptime each
tick via `recordRuntimeDuration()`, so the two numbers can never disagree. "No optimization
required yet" per spec: this only collects metrics, nothing reacts to a slow tick.

## EngineHealthMonitor

Combines `EngineLifecycleManager`, `EngineServiceConnection`, `EngineObserver`, and
`ModuleRegistry` into one `EngineHealthSnapshot` (engine alive, runtime active, service
connected, lifecycle valid, module count, error/warning counts). Purely reporting — no automatic
recovery, per spec; a future phase deciding whether to back off automation after repeated errors
would read from here rather than re-deriving these checks itself.

## UI

Home screen shows Engine Status (the raw `EngineStatus` name), Runtime Status (a friendlier
Active/Paused/Idle derived from it), Service Status (Connected/Disconnected from
`EngineHealthSnapshot`), Session ID (first 8 characters, or "—"), Running Time, Tick Count, and
Average Tick Time — the same six buttons as v0.2.0 (Initialize/Start/Pause/Resume/Stop/Reset),
each still enabled only for its legal transition.

## Dependency injection

`di/EngineDiModule.kt` now binds twelve interfaces total (five new this phase:
`RuntimeController`, `EngineServiceConnection`, `PerformanceTimer`, `EngineHealthMonitor`,
`EngineStateStore`). The dependency graph is a strict DAG — `EngineManager` depends on
`RuntimeController`/`EngineServiceConnection`/`EngineStateStore`; `EngineStateStore` depends on
`EngineHealthMonitor`/`PerformanceTimer`; nothing depends back up — so Hilt resolves it without
needing `@Provides` methods beyond the `@Binds` declarations already established in v0.2.0.

## What's deliberately not here

AI/ML, screen capture, Accessibility Service behavior, OCR, and automation execution are all
still out of scope. So is a real `EngineModule` implementation, and so is a runtime
`POST_NOTIFICATIONS` permission request flow — the permission is declared in the manifest (so
the foreground notification *can* display), but requesting it from the user is a future
Settings/permissions phase's concern, not a runtime-foundation one.
