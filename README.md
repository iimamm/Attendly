# Attendly 📍

> **Smart Geo-Fenced Attendance System for Android**  
> *Package:* `com.appsbase.attendly`

---

## 1. Project Title and Description

**Attendly** is a native Android application engineered to provide a high-accuracy, location-aware attendance system. It allows employees or administrators to define an office geofence directly on an interactive Google Map and enables workers to validate and mark their attendance only when physically present within a **50-meter radius** of the designated workplace and within defined **office working hours (09:00 AM – 06:00 PM)**.

### Core Capabilities:
- **Interactive Geofence Configuration**: Drag the map or tap the GPS button to target coordinates and establish or update workplace boundaries.
- **50-Meter Geofence Enforcement**: Visual circle overlay with dynamic proximity calculations using the Haversine formula.
- **Real-Time Distance Feedback**: Live updates providing precise distance indicators (e.g., *"You are 120m away from the office"* or *"You are inside the office geofence (22m away)"*).
- **Time Window Validation**: Enforces check-ins strictly during business hours with clear visual indicators.
- **Simulation Control Engine**: Built-in 3-dot overflow menu allowing evaluators to bypass working hours for off-hours testing, view attendance logs, or reset all preferences.
- **Production Architecture**: Modern Jetpack Compose UI, Layered MVI architecture, Kotlin Flow reactive streams, Dagger Hilt dependency injection, and Jetpack DataStore persistence.

---

## 2. Project Structure & Architectural Approach

Attendly adheres to **Clean Architecture** principles and the **Model-View-Intent (MVI)** architectural pattern with **Unidirectional Data Flow (UDF)**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           UI LAYER (Jetpack Compose)                        │
│   • AttendanceScreen: Interactive GoogleMap, Distance Banner, Action Buttons │
│   • TopAppBar & Overflow Menu: Simulation controls, History, Reset Dialogs  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                   ▲                   │  Dispatches Intents
       Observes UI │                   ▼
       StateFlow   │  ┌───────────────────────────────────────────────────────┐
                   │  │                VIEWMODEL (MVI Orchestrator)           │
                   └──┤  • AttendanceViewModel (StateFlow<AttendanceState>)   │
                      │  • One-off side effects via SharedFlow<AttendanceEffect>│
                      └────────────────────────┬──────────────────────────────┘
                                               │ Executes
                                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                DOMAIN LAYER                                 │
│   • Use Cases:                                                              │
│     - ValidateAttendanceEligibilityUseCase (50m geofence & work hours check)│
│     - MarkAttendanceUseCase (Attendance creation & validation)              │
│     - SaveOfficeLocationUseCase (Office coordinates update)                 │
│   • Pure Utilities:                                                         │
│     - GeoFenceCalculator (Pure Kotlin Haversine algorithm, 50m threshold)   │
│     - TimeValidator (Working shift window verification)                     │
│   • Models: OfficeLocation, AttendanceRecord, LocationModel, SimulationConfig│
│   • Repository Interfaces: AttendanceRepository                             │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Implements
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                 DATA LAYER                                  │
│   • Repositories: AttendanceRepositoryImpl                                  │
│   • Local Storage: Jetpack DataStore Preferences (AttendanceDataStore)      │
│   • Hardware / Sensors: FusedLocationProviderClient (LocationTracker)       │
│   • Time Source: Injectable TimeProvider (SystemTimeProvider)               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Architectural Classes:
1. **`AttendanceState`**: An immutable data class representing the single source of truth for the UI—holding current GPS coordinates, saved office coordinates, target drag location, distance in meters, eligibility status, today's attendance record, and simulation flags.
2. **`AttendanceIntent`**: Sealed interface defining all possible user actions (`SaveOfficeLocationClicked`, `MarkAttendanceClicked`, `MapCameraMoved`, `ToggleTimeSimulation`, `ConfirmResetAll`).
3. **`AttendanceViewModel`**: The state holder that processes intents, interacts with domain use cases, and publishes the unified state stream (`StateFlow`) and side-effects (`SharedFlow`).
4. **`AttendanceRepositoryImpl` & `AttendanceDataStore`**: Reactive persistence layer managing local coordinates, attendance logs, and simulation configurations via Kotlin Flow.

---

## 3. Generative AI Usage

Generative AI (Google Antigravity & Gemini) was utilized systematically throughout the design, implementation, and quality assurance phases of the project:

### How AI was Applied:
1. **Architectural Modeling & Planning**: Defining a clean MVI contract and ensuring clear isolation between the production business logic and evaluation simulation features before starting implementation.
2. **Haversine Algorithm Optimization**: Formulating a pure-Kotlin spherical distance calculation with zero Android framework dependencies, ensuring millisecond-level JVM unit testing without mocks.
3. **Automated Test Scenarios**: Generating comprehensive boundary conditions for geofencing (inside, exact 50m boundary, outside) and time windows (at shift start, before shift, after shift, and simulation bypass).
4. **Build & Compatibility Troubleshooting**: Diagnosing AGP 9.4+ Gradle configurations, KSP annotation processors, and Kotlin compiler flags.

### Key Prompts Used During Development:
- *"Draft a comprehensive implementation plan for an Android Geo-Fenced Attendance system using Jetpack Compose, MVI, Kotlin Flow, and DataStore, ensuring complete separation between simulation and production code."*
- *"Implement a pure Kotlin Haversine distance calculator that computes distances in meters without importing android.location.Location for clean unit testability."*
- *"Generate a comprehensive unit test suite covering 50m geofence edge cases, shift boundaries (09:00 AM – 06:00 PM), and MVI ViewModel intent-state transitions using Turbine and Coroutines Test."*

---

## 4. How to Run

### Prerequisites:
- Android Studio Ladybug / Meerkat or newer.
- JDK 17 or higher configured as Gradle JVM.
- Android SDK 35 (compileSdk: 35, minSdk: 24).
- Google Maps API key (recommended for map tiles rendering).

### Step-by-Step Setup:
1. **Clone the repository:**
   ```bash
   git clone <REPO_URL>
   cd attendly
   ```

2. **Configure Google Maps API Key:**
   Open or create `local.properties` in the project root directory and add your key:
   ```properties
   MAPS_API_KEY=YOUR_ACTUAL_GOOGLE_MAPS_API_KEY
   ```
   *(Note: The build system automatically injects this key into `AndroidManifest.xml` via `manifestPlaceholders` so that secrets are never exposed in Git.)*

3. **Run Unit Tests:**
   ```bash
   ./gradlew test
   ```

4. **Build and Install Debug APK:**
   ```bash
   ./gradlew installDebug
   ```

5. **Build Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```
   The compiled APK will be located at:
   `app/build/outputs/apk/release/app-release-unsigned.apk`  
   *(Debug APK is at: `app/build/outputs/apk/debug/app-debug.apk`)*

---

## 5. Screen Highlights & Features Demonstration

### 1. Setup Phase
- On launch, the interactive Google Map centers on the user's current GPS location.
- Users can drag the map to reposition the workplace pin or tap the floating location button to snap back to their current position.
- If the office location is unset, the primary setup button displays **"Set Office Location"**, and the proximity banner prompts the user to configure their office.

### 2. Validation & Proximity Feedback Phase
- Once set, the office location is rendered with a prominent pin and a shaded **50-meter radius circle overlay**.
- The real-time distance card calculates the user's exact proximity:
  - **In Range**: *"You are inside the office geofence (22 m away)"* (Green badge).
  - **Out of Range**: *"You are 120 m away from the office"* (Amber badge).
- The **"Mark Attendance"** button dynamically validates both location and working hours (09:00 AM – 06:00 PM). If ineligible, a contextual explanation informs the user why check-in is unavailable.

### 3. Simulation & Testing Menu (Top Right 3-Dot Icon)
- **Bypass Working Hours (Simulate)**: Allows evaluators to test attendance marking at any time of day. Displays a bright `SIMULATION MODE` badge on the app bar.
- **View Attendance History**: Opens a Material 3 bottom sheet showing all saved attendance timestamps, coordinates, and recorded distances.
- **Reset Everything**: Clears saved office coordinates, attendance history, and simulation flags back to initial state.

---

## 6. Testing Summary

| Test Suite | Coverage Area | Status |
| :--- | :--- | :--- |
| `GeoFenceCalculatorTest` | Haversine distance, <50m inside, exact 50m boundary, >50m outside | ✅ PASSED |
| `TimeValidatorTest` | Standard shift window, edge minutes (08:59, 09:00, 18:00, 18:01), simulation bypass | ✅ PASSED |
| `ValidateAttendanceEligibilityUseCaseTest` | Office unset, already marked, out-of-range, out-of-hours, eligible states | ✅ PASSED |
| `AttendanceViewModelTest` | MVI intent dispatching, location tracking, camera animation, simulation toggling | ✅ PASSED |

---

## 7. Deliverables
- **Source Code**: Fully modularized and clean git history on `master`.
- **Documentation**: Comprehensive `docs/PLAN.md` and `README.md`.
- **Binaries**:
  - Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
  - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
