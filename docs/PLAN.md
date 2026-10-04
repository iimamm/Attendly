# Attendly — Implementation Plan (Agent-Oriented)

Executable plan to build **Attendly**, a geo-fenced attendance app for Android, from an empty project. Work through the steps in order; each step lists the files to create, the implementation details that matter, and an acceptance check. Do not advance until the check passes.

> This plan was produced by refining the original requirement draft in [draft_plan.md](draft_plan.md) and then locking the scope, stack, and layer rules before any code was generated.

## Product Rules (source of truth)

- The office location is chosen by dragging an in-card Google Map; a **50 m geofence** circle is drawn around it.
- Attendance is markable only when: **office set → inside 50 m → within 09:00–18:00 (both boundaries inclusive) → not already marked today**. A null GPS fix with office set counts as `OUTSIDE_GEOFENCE`.
- A **simulation toggle** (overflow menu) bypasses the time rule; a `SIMULATION MODE` badge shows in the top bar while active.
- **Reset Everything** (confirm dialog) clears office location, attendance history, and the simulation flag from storage.
- Check-in is once per calendar day; history lists records newest-first.
- **Hardware failure handling:** if location permission is missing, an amber banner offers a re-request; if location services (GPS) are off while permission is granted, a second banner offers *Enable GPS* (opens system location settings). Both flags are re-checked every time the app resumes.

## Conventions

- Single module `:app`; packages `core/`, `data/`, `di/`, `domain/`, `ui/`.
- No Android framework imports under `domain/` (pure Kotlin, plain-JUnit testable).
- No hardcoded UI strings — everything in `strings.xml`; ViewModel messages are `@StringRes`.
- Reactive only: `StateFlow` / `SharedFlow` + Kotlin Flow (no LiveData, no callbacks).
- Time and clock access only through the injected `TimeProvider`.

## Architecture Blueprint

The project adheres to a strict **Unidirectional Data Flow (UDF)** and **Layered MVI (Model-View-Intent)** architecture:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                          UI LAYER (Jetpack Compose)                         │
│  • AttendanceScreen — TopAppBar (back arrow, SIMULATION badge, overflow    │
│    menu), office-context card (Google Map, center pin, coords pill, FAB),  │
│    distance ring, status chip, check-in section, permission banner         │
│  • AttendanceHistoryBottomSheet · ResetConfirmationDialog                  │
└──────────────────────────────┬─────────────────────────────────────────────┘
                               │ AttendanceIntent (user actions)
                               ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     PRESENTATION (MVI ORCHESTRATOR)                         │
│  AttendanceViewModel — reduces intents, collects repository flows and the  │
│  GPS stream, recalculates eligibility on every state change                │
└──────────────────────────────┬─────────────────────────────────────────────┘
                               │ invokes use cases
                               ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                        DOMAIN LAYER (pure Kotlin)                           │
│  • ValidateAttendanceEligibilityUseCase (office → marked → 50 m → hours)   │
│  • MarkAttendanceUseCase · SaveOfficeLocationUseCase                       │
│  • GeoFenceCalculator (Haversine, 50 m) · TimeValidator (09:00–18:00)      │
│  • AttendanceRepository interface · domain models                          │
└──────────────────────────────┬─────────────────────────────────────────────┘
                               │ implements interface
                               ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                                DATA LAYER                                   │
│  • AttendanceRepositoryImpl                                                │
│  • AttendanceDataStore (Preferences DataStore: office, history, flags)     │
│  • DefaultLocationTracker (FusedLocationProviderClient)                    │
│  • SystemTimeProvider (injectable clock)                                   │
└────────────────────────────────────────────────────────────────────────────┘
```

Return path (same chain, upward): **Data → Domain → ViewModel → UI**. The UI observes `AttendanceState` (`StateFlow`) and renders it as a pure function of state; one-off side effects (`AttendanceEffect`: snackbars, map camera animation) are emitted through a `SharedFlow`. The cycle always runs one way — Intent → ViewModel → State → Render — so any screen state is reproducible from `AttendanceState` alone, and each layer is testable in isolation.

## Tech Stack (fixed versions)

| Tool | Version |
| :--- | :--- |
| Kotlin / KSP | 2.2.10 / 2.2.10-2.0.2 |
| AGP / Gradle | 9.4.1 / 9.6.0 (JDK 17) |
| Compose BOM / Activity Compose | 2026.02.01 / 1.10.0 |
| Hilt (+ navigation-compose 1.2.0) | 2.60.1 |
| DataStore Preferences | 1.1.2 |
| Maps Compose / Play Services Location | 4.4.1 / 21.3.0 |
| JUnit4 / Turbine / coroutines-test / MockK | 4.13.2 / 1.2.0 / 1.10.1 / 1.13.14 |

SDK: `minSdk 24`, `compileSdk`/`targetSdk 35`.

## Steps

### Step 1 — Project skeleton & DI
Create: `gradle/libs.versions.toml` (versions above), root and `app/build.gradle.kts` (read `MAPS_API_KEY` from `local.properties` — ship a `local.properties.example` template — → `manifestPlaceholders` + `BuildConfig` field), `AndroidManifest.xml` (INTERNET, ACCESS_NETWORK_STATE, ACCESS_FINE/COARSE_LOCATION; `com.google.android.geo.API_KEY` meta-data; launcher `MainActivity`), `AttendlyApplication` (`@HiltAndroidApp`), `MainActivity` (edge-to-edge, hosts `AttendanceScreen` in `AttendlyTheme`), `di/AppModule` (bind `SystemTimeProvider` → `TimeProvider`; provide `@IoDispatcher`/`@MainDispatcher`), `di/LocationModule` (`FusedLocationProviderClient` + tracker binding), `di/RepositoryModule`, `ui/theme/Color.kt` + `Theme.kt` (light/dark schemes, no dynamic color), seed `res/values/strings.xml`, `app/proguard-rules.pro`.

**Acceptance:** `./gradlew assembleDebug` succeeds with an empty Compose screen.

### Step 2 — Domain layer (pure Kotlin)
Create:
- `domain/model/Models.kt` — `LocationModel(lat, lng, accuracy)`, `OfficeLocation(lat, lng, isSet)`, `AttendanceRecord(id, timestamp, lat, lng, distanceMeters)`, `SimulationConfig(bypassTimeValidation)`, `AttendanceStatus` enum.
- `domain/repository/AttendanceRepository.kt` — flows: `officeLocation`, `attendanceHistory`, `todayAttendance`, `simulationConfig`; suspends: `saveOfficeLocation`, `markAttendance(location, distanceMeters)`, `resetAllData`, `setTimeBypassSimulation`.
- `domain/util/GeoFenceCalculator.kt` — object; `GEOFENCE_RADIUS_METERS = 50.0`; `calculateDistanceMeters(...): Int` (Haversine, EARTH_RADIUS 6_371_000 m); `isWithinGeofence(distance, radius)` (≤ is inside).
- `domain/util/TimeValidator.kt` — `@Singleton` with injected `TimeProvider`; `isWithinWorkHours(current, start=09:00, end=18:00, bypassSimulation)` inclusive at both ends, bypass short-circuits true; `getWorkHoursFormatted()` → `"hh:mm a"` pair.
- Use cases — `ValidateAttendanceEligibilityUseCase` returns `AttendanceEligibility(status, distanceMeters, isEligible)` with precedence `OFFICE_NOT_SET → ALREADY_MARKED → OUTSIDE_GEOFENCE → OUTSIDE_TIME_WINDOW → ELIGIBLE`; `MarkAttendanceUseCase` re-validates and returns `AttendanceRecord?` (null = rejected, no exception strings); `SaveOfficeLocationUseCase` returns `Result<Unit>`.

**Acceptance:** compiles; `grep -r "import android" app/src/main/java/com/appsbase/attendly/domain/` returns nothing.

### Step 3 — Data layer
Create:
- `data/local/AttendanceDataStore.kt` — Preferences DataStore named `attendly_prefs`; keys `office_latitude`/`office_longitude` (Double), `office_is_set` (Boolean), `attendance_history` (JSON array, objects `{id, timestamp, lat, lng, dist}`, newest-first insert at index 0), `bypass_time_simulation` (Boolean); flows map IOException → empty defaults; `clearAll()` wipes everything.
- `data/repository/AttendanceRepositoryImpl.kt` — delegates to the DataStore; `todayAttendance` derives from history filtered to `[startOfDay, startOfDay + 24 h)` of `timeProvider.now()`; `markAttendance` builds the record with `UUID` id and `timeProvider.currentTimeMillis()`.
- `data/location/LocationTracker.kt` — interface (`locationUpdates: Flow<LocationModel>`, `suspend getCurrentLocation(): LocationModel?`, `hasLocationPermission()`, `isGpsEnabled()`) + `DefaultLocationTracker`: permission-guarded; GPS state via `LocationManager` provider check (GPS **or** network provider enabled); stream via `callbackFlow` with `LocationRequest(PRIORITY_HIGH_ACCURACY, 3000 ms, min 1500 ms)`; one-shot `getCurrentLocation` with a cancellation token and a `Task.awaitTask()` helper that resumes `null` on failure.

**Acceptance:** Hilt graph completes — `./gradlew assembleDebug` with the repository bound.

### Step 4 — Presentation (MVI + Compose UI)
Create `ui/attendance/AttendanceContract.kt`:
- `AttendanceState` — `currentLocation`, `officeLocation`, `targetOfficeLocation`, `distanceMeters`, `eligibilityStatus`, `isWithinGeofence`, `todayAttendance`, `attendanceHistory`, `simulationConfig`, `checkInWindow`, `hasLocationPermission`, `isGpsEnabled`, `isMarkingAttendance`, `isSavingOffice`, `showHistorySheet`, `showResetConfirmDialog`.
- `AttendanceIntent` — `RefreshLocationState` (dispatched from `LifecycleResumeEffect` on every app resume), `PermissionResultReceived(isGranted)`, `MapCameraMoved(center)`, `CenterMapOnCurrentLocation`, `SaveOfficeLocationClicked`, `MarkAttendanceClicked`, `ToggleTimeSimulation`, `ShowHistorySheet(show)`, `ShowResetConfirmDialog(show)`, `ConfirmResetAll`.
- `AttendanceEffect` — `ShowSnackbar(@StringRes messageRes)`, `AnimateMapCamera(location)`.

Create `ui/attendance/AttendanceViewModel.kt`: init sets `checkInWindow` (e.g. `"09:00 AM – 06:00 PM"`), `hasLocationPermission`, and `isGpsEnabled`, starts tracking if permitted; `RefreshLocationState` re-reads both flags on resume and restarts tracking only when a flag flipped (cancels tracking if permission was revoked); collects the four repository flows plus the GPS stream (one-shot fix first; on first fix with office unset, target = current location); `recalculateEligibility()` after every change; camera-idle dispatches `MapCameraMoved`; on mark failure maps status → string res (`OFFICE_NOT_SET → attendance_disabled_unset_reason`, `OUTSIDE_GEOFENCE → hint_out_of_range`, `OUTSIDE_TIME_WINDOW → attendance_disabled_time_reason`, `ALREADY_MARKED → attendance_already_marked`); reset clears storage, sets office null and target = current location.

Create `ui/attendance/AttendanceScreen.kt` (card-based design). `AttendanceScreen` keeps only the ViewModel wiring (permission launcher, camera effects, resume refresh) and delegates rendering to a stateless `AttendanceContent(state, cameraPositionState, snackbarHostState, onBack, onIntent, onRequestPermission)`:
- `TopAppBar` — back arrow (finishes the activity), left-aligned navy title "Attendance", `SIMULATION MODE` badge, overflow menu (bypass toggle, history with record count, reset).
- **Office-context card** — 150 dp interactive `GoogleMap` (50 m circle + azure office marker; camera zoom 17, +0.5 on office load), 36 dp center targeting pin, monospace coordinates pill horizontally centered directly above the pin, 40 dp my-location FAB bottom-end, Set/Update Office outlined button (56 dp). In preview/inspection mode the map renders a static placeholder (`LocalInspectionMode`).
- **Distance ring** — 176 dp `Canvas` arc, track grey + accent (green in range / red out), sweep = `1f` in range else `(distance / 200f).coerceIn(0.1f, 1f)`, center `"<n>m"` (or `—`) + `AWAY`.
- **Status chip** — `OFFICE UNSET` (grey) / `MARKED TODAY` / `IN RANGE` (green) / `OUT OF RANGE` (red); status hint line beneath mirrors `eligibilityStatus`.
- **Check-in section** — transparent (screen background shows through; no fill) framed only by a stronger dashed rounded border; header icon lock (locked) / lock-open (eligible) / check (marked); Mark Attendance button enabled only when `ELIGIBLE`; caption `AVAILABLE <window>` / `SIMULATION: ANY TIME` / `MARKED TODAY AT <hh:mm a>`.
- Guidance banners (one shared composable): permission banner (re-request via `RequestMultiplePermissions`, any-granted) and GPS-off banner (`LocationOff` icon, *Enable GPS* opens `Settings.ACTION_LOCATION_SOURCE_SETTINGS`), shown when permission is granted but location services are off; `components/AttendanceHistoryBottomSheet.kt`; `components/AttendanceDialogs.kt` (reset confirmation).
- Four `@Preview` cases render the whole screen without a device: office unset, set-but-out-of-range, set-and-in-range (eligible), set-and-marked-today.

**Acceptance:** app runs on an emulator; unset, out-of-range, eligible, out-of-hours (with simulation), and marked-today states all render; permission and GPS-off banners appear/disappear with system state on resume; camera targeting works.

### Step 5 — Tests, verification & docs
Create `testutil/` fakes — `FakeAttendanceRepository` (StateFlow-backed), `FakeLocationTracker` (configurable permission, GPS flag + `MutableStateFlow` location), `FakeTimeProvider` (fixed clock with setters) — and suites (24 tests):
- `GeoFenceCalculatorTest` (5) — same point = 0, <50 m, exact 50 m boundary, >50 m, custom radius.
- `TimeValidatorTest` (5) — 08:59 / 09:00 / 18:00 / 18:01, bypass flag.
- `ValidateAttendanceEligibilityUseCaseTest` (7) — full status matrix incl. null location and simulation bypass.
- `AttendanceViewModelTest` (7) — intent→state transitions with `StandardTestDispatcher`, incl. GPS disabled/re-enabled refresh.

Finish with `README.md` (title/description, structure & MVI classes, AI usage, how to run, screenshots).

**Acceptance:** `./gradlew test assembleRelease` green; manual emulator pass of set-office → mark-attendance → history → reset.

## Definition of Done

- [ ] All five step acceptance checks pass.
- [ ] `./gradlew test assembleRelease` — 24 tests green, both APKs build.
- [ ] No `import android.*` under `domain/`; no hardcoded UI strings.
- [ ] Emulator E2E: set office → in range → mark → MARKED TODAY state → history → reset returns to clean state.
- [ ] `README.md` contains all five required sections, screenshots included.
