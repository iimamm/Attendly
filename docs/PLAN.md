# Attendly — Implementation Plan (as built)

This document is the executable plan the project was built from, updated to match what actually shipped. It started as a refinement of the raw requirement draft in [draft_plan.md](draft_plan.md) — ambiguities resolved, stack and layer rules locked — before any code was generated. The build then worked through the five steps below in order, passing each acceptance check before advancing. The commit that closed each step is noted next to it, and the changes made after the plan was locked are listed at the end.

## Status

**Complete and working.** `./gradlew test assembleRelease` is green (25 unit tests, four suites), and the full end-to-end flow — set office → in range → mark attendance → history → reset — was verified on an emulator. Screenshots of every state are in the [README](../README.md#5-screenshots).

## Product rules (source of truth)

- The office location is chosen by dragging an in-card Google Map; a **50 m geofence** circle is drawn around it.
- Attendance is markable only when: **office set → inside 50 m → within 09:00–18:00 (both boundaries inclusive) → not already marked today**. A null GPS fix with the office set counts as `OUTSIDE_GEOFENCE`.
- A **simulation toggle** (overflow menu) bypasses the time rule; a `SIMULATION MODE` badge shows in the top bar while active.
- **Reset Everything** (confirm dialog) clears office location, attendance history, and the simulation flag from storage.
- Check-in is once per calendar day; history lists records newest-first.
- **Hardware failure handling:** if location permission is missing, an amber banner offers a re-request; if location services are off while permission is granted, a second banner offers *Enable GPS* (opens system location settings). Both flags are re-checked every time the app resumes.

## Conventions

- Single module `:app`; packages `core/`, `data/`, `di/`, `domain/`, `ui/`.
- No Android framework imports under `domain/` (pure Kotlin, plain-JUnit testable).
- No hardcoded UI strings — everything in `strings.xml`; ViewModel messages are `@StringRes`.
- Reactive only: `StateFlow` / `SharedFlow` + Kotlin Flow (no LiveData, no callbacks).
- Time and clock access only through the injected `TimeProvider`.
- Core library desugaring enabled, so `java.time` is available on all supported API levels (24+).

## Architecture blueprint

```
┌────────────────────────────────────────────────────────────────────────────┐
│                          UI LAYER (Jetpack Compose)                         │
│  • AttendanceScreen — TopAppBar (back arrow, SIMULATION badge, overflow    │
│    menu), office-context card (Google Map, center pin, coords pill, FAB),  │
│    distance ring, status chip, check-in section, permission banner         │
│  • AttendanceHistoryBottomSheet · ResetConfirmationDialog                  │
└──────────────────────────────┬──────────────────────────────────────────────┘
                               │ AttendanceIntent (user actions)
                               ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     PRESENTATION (MVI ORCHESTRATOR)                         │
│  AttendanceViewModel — reduces intents, collects repository flows and the  │
│  GPS stream, recalculates eligibility on every state change                │
└──────────────────────────────┬──────────────────────────────────────────────┘
                               │ invokes use cases
                               ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                        DOMAIN LAYER (pure Kotlin)                           │
│  • ValidateAttendanceEligibilityUseCase (office → marked → 50 m → hours)   │
│  • MarkAttendanceUseCase · SaveOfficeLocationUseCase                       │
│  • GeoFenceCalculator (Haversine, 50 m) · TimeValidator (09:00–18:00)      │
│  • AttendanceRepository interface · domain models                          │
└──────────────────────────────┬──────────────────────────────────────────────┘
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

Data flows back up the same chain: **Data → Domain → ViewModel → UI**. The UI observes `AttendanceState` (`StateFlow`) and renders it as a pure function of state; one-off side effects (`AttendanceEffect`: snackbars, map camera animation) travel through a `SharedFlow`. The cycle runs one way only — Intent → ViewModel → State → Render — so any screen state is reproducible from `AttendanceState` alone, and each layer is testable in isolation.

## Tech stack (as built)

| Tool | Version |
| :--- | :--- |
| Kotlin / KSP | 2.2.10 / 2.2.10-2.0.2 |
| AGP / Gradle | 9.4.1 / 9.6.0 (JDK 17) |
| Compose BOM / Activity Compose | 2026.02.01 / 1.10.0 |
| Hilt (+ navigation-compose) | 2.60.1 (+ 1.2.0) |
| DataStore Preferences | 1.1.2 |
| Maps Compose / Play Services Maps / Location | 4.4.1 / 19.0.0 / 21.3.0 |
| Core library desugaring | 2.1.5 |
| JUnit4 / Turbine / coroutines-test / MockK | 4.13.2 / 1.2.0 / 1.10.1 / 1.13.14 |

SDK: `minSdk 24`, `compileSdk` / `targetSdk 36`.

## The five build steps

### Step 1 — Project skeleton & DI · `ee76796` ✅

Created `gradle/libs.versions.toml` (versions above), root and `app/build.gradle.kts` (reads `MAPS_API_KEY` from `local.properties` — with a `local.properties.example` template — into `manifestPlaceholders` + a `BuildConfig` field), `AndroidManifest.xml` (INTERNET, ACCESS_NETWORK_STATE, ACCESS_FINE/COARSE_LOCATION; `com.google.android.geo.API_KEY` meta-data; launcher `MainActivity`), `AttendlyApplication` (`@HiltAndroidApp`), `MainActivity` (edge-to-edge, hosts `AttendanceScreen` in `AttendlyTheme`), `di/AppModule` (binds `SystemTimeProvider` → `TimeProvider`; provides `@IoDispatcher`/`@MainDispatcher`), `di/LocationModule` (`FusedLocationProviderClient` + tracker binding), `di/RepositoryModule`, `ui/theme/Color.kt` + `Theme.kt` (light/dark schemes, no dynamic color), seeded `res/values/strings.xml`, `app/proguard-rules.pro`.

**Acceptance (passed):** `./gradlew assembleDebug` succeeded with an empty Compose screen.

### Step 2 — Domain layer (pure Kotlin) · `a399bea` ✅

- `domain/model/Models.kt` — `LocationModel(lat, lng, accuracy)`, `OfficeLocation(lat, lng, isSet)`, `AttendanceRecord(id, timestamp, lat, lng, distanceMeters)`, `SimulationConfig(bypassTimeValidation)`, `AttendanceStatus` enum.
- `domain/repository/AttendanceRepository.kt` — flows: `officeLocation`, `attendanceHistory`, `todayAttendance`, `simulationConfig`; suspends: `saveOfficeLocation`, `markAttendance(location, distanceMeters)`, `resetAllData`, `setTimeBypassSimulation`.
- `domain/util/GeoFenceCalculator.kt` — object; `GEOFENCE_RADIUS_METERS = 50.0`; `calculateDistanceMeters(...): Int` (Haversine, Earth radius 6,371,000 m); `isWithinGeofence(distance, radius)` (≤ counts as inside).
- `domain/util/TimeValidator.kt` — `@Singleton` with injected `TimeProvider`; `isWithinWorkHours(current, start 09:00, end 18:00, bypassSimulation)`, inclusive at both ends, bypass short-circuits true; `getWorkHoursFormatted()` → `"hh:mm a"` pair.
- Use cases — `ValidateAttendanceEligibilityUseCase` returns `AttendanceEligibility(status, distanceMeters, isEligible)` with precedence `OFFICE_NOT_SET → ALREADY_MARKED → OUTSIDE_GEOFENCE → OUTSIDE_TIME_WINDOW → ELIGIBLE`; `MarkAttendanceUseCase` re-validates and returns `AttendanceRecord?` (null = rejected, no exception strings); `SaveOfficeLocationUseCase` returns `Result<Unit>`.

**Acceptance (passed):** compiles; `grep -r "import android" app/src/main/java/com/appsbase/attendly/domain/` returns nothing.

### Step 3 — Data layer · `a399bea` (same commit as Step 2) ✅

- `data/local/AttendanceDataStore.kt` — Preferences DataStore named `attendly_prefs`; keys `office_latitude`/`office_longitude` (Double), `office_is_set` (Boolean), `attendance_history` (JSON array of `{id, timestamp, lat, lng, dist}`, newest-first insert at index 0), `bypass_time_simulation` (Boolean); flows map `IOException` → empty defaults and run their mapping on the injected `@IoDispatcher` via `flowOn`; `clearAll()` wipes everything.
- `data/repository/AttendanceRepositoryImpl.kt` — delegates to the DataStore; `todayAttendance` derives from history filtered to `[startOfDay, startOfDay + 24 h)` of `timeProvider.now()`; `markAttendance` builds the record with a `UUID` id and `timeProvider.currentTimeMillis()`.
- `data/location/LocationTracker.kt` — interface (`locationUpdates: Flow<LocationModel>`, `suspend getCurrentLocation(): LocationModel?`, `hasLocationPermission()`, `isGpsEnabled()`) + `DefaultLocationTracker`: permission-guarded; GPS state via a `LocationManager` provider check (GPS **or** network provider enabled); stream via `callbackFlow` with `LocationRequest(PRIORITY_HIGH_ACCURACY, 3000 ms, min 1500 ms)`; one-shot `getCurrentLocation` with a cancellation token cancelled if the calling coroutine is cancelled, and a `Task.awaitTask()` helper that resumes `null` on failure.

**Acceptance (passed):** Hilt graph completed — `./gradlew assembleDebug` with the repository bound.

### Step 4 — Presentation (MVI + Compose UI) · `05315c9`, refined in `6bc4402` / `fe1f5eb` / `7426673` ✅

`ui/attendance/AttendanceContract.kt`:

- `AttendanceState` — `currentLocation`, `officeLocation` (the saved geofence), `targetOfficeLocation` (the map-center draft pin the user aims before saving), `distanceMeters`, `eligibilityStatus`, `isWithinGeofence`, `todayAttendance`, `attendanceHistory`, `simulationConfig`, `checkInWindow`, `hasLocationPermission`, `isGpsEnabled`, `isMarkingAttendance`, `isSavingOffice`, `showHistorySheet`, `showResetConfirmDialog`.
- `AttendanceIntent` — `RefreshLocationState` (ON_RESUME) and `PauseLocationTracking` (ON_STOP) dispatched from lifecycle effects so GPS collection pauses while backgrounded, `PermissionResultReceived(isGranted)`, `MapCameraMoved(center)` (deduped in the ViewModel), `CenterMapOnCurrentLocation`, `SaveOfficeLocationClicked`, `MarkAttendanceClicked`, `ToggleTimeSimulation`, `ShowHistorySheet(show)`, `ShowResetConfirmDialog(show)`, `ConfirmResetAll`.
- `AttendanceEffect` — `ShowSnackbar(@StringRes messageRes)`, `AnimateMapCamera(location)`.

`ui/attendance/AttendanceViewModel.kt`: init sets `checkInWindow` (e.g. `"09:00 AM – 06:00 PM"`), `hasLocationPermission`, and `isGpsEnabled`, and starts tracking if permitted; `RefreshLocationState` re-reads both flags on resume and restarts tracking when it is not running; `PauseLocationTracking` cancels GPS collection on ON_STOP (and cancels tracking too if permission is revoked); collects the four repository flows plus the GPS stream (one-shot fix first; on the first fix with the office unset, target = current location); `recalculateEligibility()` after every change; on mark failure maps status → string res (`OFFICE_NOT_SET → attendance_disabled_unset_reason`, `OUTSIDE_GEOFENCE → hint_out_of_range`, `OUTSIDE_TIME_WINDOW → attendance_disabled_time_reason`, `ALREADY_MARKED → attendance_already_marked`); reset clears storage, sets the office null and the target back to the current location.

`ui/attendance/AttendanceScreen.kt` keeps only the ViewModel wiring (permission launcher, camera effects, resume refresh) and delegates rendering to a stateless `AttendanceContent(state, cameraPositionState, snackbarHostState, onBack, onIntent, onRequestPermission)`:

- **TopAppBar** — back arrow (finishes the activity), left-aligned navy title "Attendance", `SIMULATION MODE` badge, overflow menu (bypass toggle, history with record count, reset).
- **Office-context card** — 150 dp interactive `GoogleMap` (50 m circle + azure office marker via `rememberMarkerState`; camera zoom 17, +0.5 on office load, centers on the **first** GPS fix only so it never fights user drags), 36 dp center targeting pin, monospace coordinates pill centered above the pin, 40 dp my-location FAB bottom-end, Set/Update Office outlined button (56 dp). In preview/inspection mode the map renders a static placeholder (`LocalInspectionMode`).
- **Distance ring** — 176 dp `Canvas` arc, grey track + accent sweep (green in range / red out), sweep = `1f` in range else `(distance / 200f).coerceIn(0.1f, 1f)`, center `"<n>m"` (or `—`) + `AWAY`.
- **Status chip** — `OFFICE UNSET` (grey) / `MARKED TODAY` / `IN RANGE` (green) / `OUT OF RANGE` (red); a hint line beneath mirrors `eligibilityStatus`.
- **Check-in section** — transparent (screen background shows through), framed only by a stronger dashed rounded border; header icon lock (locked) / lock-open (eligible) / check (marked); Mark Attendance enabled only when `ELIGIBLE`; caption `AVAILABLE <window>` / `SIMULATION: ANY TIME` / `MARKED TODAY AT <hh:mm a>`.
- **Guidance banners** (one shared composable): permission banner (re-request via `RequestMultiplePermissions`, any-granted) and GPS-off banner (`LocationOff` icon, *Enable GPS* opens `Settings.ACTION_LOCATION_SOURCE_SETTINGS`), the latter shown when permission is granted but location services are off.
- Widgets live in `components/`, one file per major widget: `AttendanceTopBar.kt`, `GuidanceBanners.kt`, `OfficeContextCard.kt`, `ProximityWidgets.kt`, `MarkAttendanceSection.kt`, `AttendanceHistoryBottomSheet.kt`, `AttendanceDialogs.kt`. Widgets take narrow parameters (booleans, counts, the data they render) rather than the whole `AttendanceState`, so a GPS tick doesn't recompose unchanged widgets.
- `@Preview` cases render the whole screen without a device: office unset, set-but-out-of-range, set-and-in-range (eligible), and set-and-marked-today.

**Acceptance (passed):** app runs on an emulator; unset, out-of-range, eligible, out-of-hours (with simulation), and marked-today states all render; permission and GPS-off banners appear/disappear with system state on resume; camera targeting works.

### Step 5 — Tests, verification & docs · `ec2d2ea` ✅

`testutil/` fakes — `FakeAttendanceRepository` (StateFlow-backed), `FakeLocationTracker` (configurable permission, GPS flag + `MutableStateFlow` location, call counter), `FakeTimeProvider` (fixed clock with setters) — and four suites (25 tests):

- `GeoFenceCalculatorTest` (5) — same point = 0, < 50 m, exact 50 m boundary, > 50 m, custom radius.
- `TimeValidatorTest` (5) — 08:59 / 09:00 / 18:00 / 18:01, bypass flag.
- `ValidateAttendanceEligibilityUseCaseTest` (7) — full status matrix incl. null location and simulation bypass.
- `AttendanceViewModelTest` (8) — intent→state transitions with `StandardTestDispatcher`, incl. GPS disabled/re-enabled refresh and pause/resume of location tracking.

Finished with `README.md` (title/description, structure & MVI classes, AI usage, how to run, screenshots).

**Acceptance (passed):** `./gradlew test assembleRelease` green; manual emulator pass of set-office → mark-attendance → history → reset.

## Changes made after the plan was locked

Each of these was reviewed, recompiled, re-tested, and verified on the emulator before being kept:

| Commit | Change |
| :--- | :--- |
| `647e956` | Removed the "undo today" menu option, keeping Reset Everything; docs and tests updated to match. |
| `6bc4402` | Redesigned the screen to the card-based layout (from an attached design mock), added GPS-off handling, streamlined the docs, and deleted dead code left from the template. |
| `fbd4662` | Updated the launcher icons and adjusted Gradle settings for better compatibility. |
| `6c9984e` | Ignored the `.idea` folder and removed it from git history. |
| `fe1f5eb` | Added previews for every state case, retitled the top bar, and refined the map and check-in visuals. |
| `afdfa06` | Enabled core library desugaring (for `java.time` on minSdk 24) and updated dependencies. |
| `7426673` | Split the screen into per-widget component files: top bar, guidance banners, mark-attendance section. |
| `932ddc3` | Made location tracking lifecycle-aware: GPS collection pauses on ON_STOP and resumes on ON_RESUME. |
| `3736b0c` | Annotated the UI state and the models it holds with `@Stable` so Compose can skip recomposition. |

## Definition of Done

- [x] All five step acceptance checks pass.
- [x] `./gradlew test assembleRelease` — 25 tests green, both APKs build.
- [x] No `import android.*` under `domain/`; no hardcoded UI strings.
- [x] Emulator E2E: set office → in range → mark → MARKED TODAY state → history → reset returns to a clean state.
- [x] `README.md` contains all five required sections, screenshots included.
