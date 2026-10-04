# Attendly - Implementation & Architecture Plan

## 1. Project Overview & Objectives
**Attendly** (`com.appsbase.attendly`) is a modern, production-ready Native Android application demonstrating a high-accuracy **Geo-Fenced Attendance System**. Built with Kotlin, Jetpack Compose, Kotlin Flow, MVI architecture, and Google Maps SDK, the app allows users to define their workplace location, observe real-time proximity feedback, validate attendance within a 50-meter geo-fence and defined work hours, and seamlessly test various real-world scenarios via an isolated simulation control system.

---

## 2. Architectural Blueprint (Layered MVI Pattern)

The project adheres to a strict **Unidirectional Data Flow (UDF)** and **Layered MVI (Model-View-Intent)** architecture:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           UI LAYER (Jetpack Compose)                        │
│   • AttendanceScreen (Map, Real-time Distance Card, Action Buttons)         │
│   • TopAppBar with Overflow Action Menu (Simulation, History, Reset)        │
│   • History BottomSheet / Confirmation Dialogs                              │
└──────────────────────────────────────┬──────────────────────────────────────┘
                   ▲                   │  Emits User Intents (User Actions)
      Observes UI  │                   ▼
      State Flow   │  ┌───────────────────────────────────────────────────────┐
                   │  │                 VIEWMODEL (MVI Orchestrator)          │
                   └──┤  • AttendanceViewModel (StateFlow<AttendanceState>)   │
                      │  • One-off Single Events (SharedFlow<AttendanceEffect>)│
                      └────────────────────────┬──────────────────────────────┘
                                               │ Calls Domain / Use Cases
                                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                DOMAIN LAYER                                 │
│   • Use Cases:                                                              │
│     - CalculateDistanceUseCase (Haversine & Android Location)               │
│     - ValidateGeofenceUseCase (50m threshold check)                         │
│     - ValidateAttendanceTimeUseCase (Working hours window check)            │
│     - MarkAttendanceUseCase & UndoAttendanceUseCase                         │
│   • Domain Models: OfficeLocation, AttendanceRecord, GeoFenceStatus,        │
│     TimeValidationStatus, SimulationConfig                                  │
│   • Contracts / Interfaces: AttendanceRepository, LocationTracker           │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Implements Interfaces
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                 DATA LAYER                                  │
│   • Repositories:                                                           │
│     - AttendanceRepositoryImpl (Coordinates, Attendance Records)            │
│     - SimulationRepositoryImpl (Bypass time, override distance/status)      │
│   • Data Sources:                                                           │
│     - Local DataStore (Preferences DataStore for reactive storage)          │
│     - FusedLocationProviderClient (Reactive GPS location tracking Flow)     │
│     - System Clock / Time Provider (Injectable for testability)             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Clean Separation Between Feature & Simulation
- **Production Engine**: Strict GPS fetching, 50m Haversine distance threshold calculation, strict working-hours validation (e.g., 09:00 AM – 06:00 PM).
- **Simulation Engine**: Isolated `SimulationManager` / `SimulationRepository` injected into UseCases and ViewModel.
  - Allows bypassing the time window for off-hours evaluation.
  - Allows undoing today's attendance mark for repeated testing.
  - Allows full reset of local DataStore state.
  - Clear visual indicator ("Simulation Mode Active") when any override is enabled.

---

## 3. High-Level Technical Stack
- **Language & Runtime**: Kotlin 2.2.x, JVM 17.
- **UI Toolkit**: Jetpack Compose with Material 3 Design System.
- **State Management**: Kotlin Coroutines & Kotlin Flow (`StateFlow`, `SharedFlow`).
- **Dependency Injection**: Dagger Hilt 2.5x.
- **Local Storage**: Jetpack DataStore (Preferences).
- **Maps & Location**: Google Maps Compose SDK (`maps-compose`), Google Play Services Location (`FusedLocationProviderClient`).
- **Secrets Management**: Google Maps API Key injected via `local.properties` (never committed to VCS).
- **Testing**: JUnit4, Turbine (Flow testing), MockK / Fake implementations, Kotlinx Coroutines Test.

---

## 4. Implementation Breakdown (4-Step Flow)

### Step 1: Foundation, Build Configuration & Core Architecture Setup
1. **Dependency & Plugin Setup**:
   - Configure Hilt Android Gradle Plugin and KSP in `libs.versions.toml` and `build.gradle.kts`.
   - Add dependencies: Hilt, Google Maps Compose, Play Services Location, DataStore Preferences, Lifecycle Runtime Compose, Turbine, MockK.
   - Configure Maps API Key injection: read `MAPS_API_KEY` from `local.properties` into `manifestPlaceholders` in `app/build.gradle.kts` and declare in `AndroidManifest.xml`.
2. **Design System & Localization**:
   - Establish Material 3 Theme (Colors, Typography, Shapes) supporting dynamic colors and consistent styling matching the design.
   - Comprehensive `strings.xml` extraction for all UI labels, distance indicators, error states, and simulation dialogs.
3. **Application & DI Skeleton**:
   - Create `AttendlyApplication` annotated with `@HiltAndroidApp`.
   - Setup Core Hilt Modules (`AppModule`, `DataStoreModule`, `LocationModule`).

### Step 2: Domain, Data & Simulation Layers
1. **Hardware & Location Tracker**:
   - Implement `LocationTracker` with `FusedLocationProviderClient` emitting real-time GPS coordinates as a Kotlin `Flow<Location>`.
   - Handle permission states, GPS disabled states, and hardware failure fallbacks gracefully.
2. **Local Storage with DataStore**:
   - Implement `AttendanceDataStore` managing office coordinates (`latitude`, `longitude`), address string, marked attendance history (timestamps and coordinates), and simulation toggles.
3. **Domain Use Cases & Validators**:
   - `GeoFenceCalculator`: Accurate distance calculation (50m threshold) with distance formatting (meters/km).
   - `TimeValidator`: Working-hours evaluation (e.g., 09:00 - 18:00) with injectable clock for testability.
   - `SimulationManager`: Separate debug controller handling "Bypass Time", "Undo Attendance", and "Factory Reset".
4. **Repository Implementation**:
   - `AttendanceRepository` implementing reactive retrieval and atomic updates using Kotlin Flow.

### Step 3: Presentation Layer (MVI) & Jetpack Compose UI
1. **MVI Architecture Setup**:
   - Define `AttendanceState`: Current user location, office location, distance in meters, isWithinGeofence, isWithinTimeWindow, isAttendanceMarked, simulation active flag, loading & error states.
   - Define `AttendanceIntent`: `SetOfficeLocation`, `UpdateOfficeLocation`, `MarkAttendance`, `MapMoved(LatLng)`, `ToggleTimeSimulation`, `UndoAttendance`, `ResetAllData`, `RequestLocationPermission`.
   - Define `AttendanceEffect`: Navigation events, Toast/Snackbar messages, error alerts.
   - Implement `AttendanceViewModel` managing state transitions and business logic triggers.
2. **AttendanceScreen Composable**:
   - **Top Section / TopAppBar**: App title and three-dot overflow menu for Simulation actions (Bypass Time, Undo Attendance, View History, Reset All).
   - **Interactive Google Map**:
     - Draggable center marker for office targeting.
     - 50-meter radius visual circle overlay around the office location.
     - Current user location marker/pin with GPS floating action button.
   - **Distance Feedback & Status Banner**:
     - Real-time indicator ("You are 120m away from the office", "You are within office range (22m)").
     - Time validation prompt when outside working hours.
     - Prompt to set office location if coordinates are uninitialized.
   - **Primary Action Controls**:
     - "Set Office Location" / "Update Office Location" dynamic button.
     - "Mark Attendance" button (strictly enabled only when inside 50m AND within work hours / simulated).
     - Success state showing today's attendance timestamp.
3. **History Sheet & Simulation Modals**:
   - Modal BottomSheet displaying list of marked attendance records.
   - Confirmation dialogs for undoing attendance and resetting data.

### Step 4: Unit Testing, Release Build Verification & Documentation
1. **Unit Testing**:
   - `GeoFenceCalculatorTest`: Test inside radius (<50m), exact boundary (50m), outside radius (>50m).
   - `TimeValidatorTest`: Test valid work hours, before shift, after shift, and simulation bypass flag.
   - `AttendanceViewModelTest`: Validate MVI intent processing, state emissions, and effect dispatching using Turbine and Coroutines Test.
   - `AttendanceRepositoryTest`: Validate local persistence, data integrity, and flow streams.
2. **Build Verification**:
   - Verify lint checks, debug build, and release APK compilation via `./gradlew assembleRelease` / `./gradlew assembleDebug`.
3. **Documentation**:
   - Produce a comprehensive, developer-friendly `README.md` containing:
     - Project Title & Overview.
     - Architectural approach (Layered MVI, Flow, Hilt, DataStore).
     - Generative AI usage & prompt documentation.
     - Setup & run instructions (including `local.properties` Maps API key configuration).
     - Architecture diagrams and feature screenshots/GIFs.

---

## 5. Security & Best Practices
- **No API Keys in VCS**: Google Maps API key loaded through Gradle from `local.properties` via `manifestPlaceholders`.
- **Battery & GPS Optimization**: Location updates managed reactively, pausing updates when screen is inactive/disposed.
- **Robust Error Recovery**: Clear messaging when location services or GPS permissions are denied.
- **Production-Ready Code Quality**: Clean package structure (`di`, `data`, `domain`, `ui`), explicit typing, immutability in states, zero hardcoded strings.
