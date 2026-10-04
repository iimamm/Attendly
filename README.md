# Attendly 📍

## 1. Project Title and Description

**Attendly** is a native Android application for **geo-fenced attendance marking**. Users define their office location on an interactive Google Map, and attendance can be marked only when they are physically within a **50-meter radius** (Haversine distance) of that office **and** inside working hours (**09:00 AM – 06:00 PM**, both boundaries inclusive). Check-in is allowed once per calendar day, and a simulation toggle lets evaluators test the flow outside working hours.

Core capabilities:

- **Office geofence setup** — drag the in-card map (or tap the my-location button) to target coordinates, then save them; a 50 m circle is drawn around the office.
- **Live proximity feedback** — real-time distance shown as a progress ring with an `IN RANGE / OUT OF RANGE` status chip and a contextual hint.
- **Check-in gate** — the Mark Attendance button unlocks only when: office set → inside 50 m → within working hours → not already marked today.
- **Simulation mode** — overflow-menu toggle that bypasses the working-hours check for evaluation (badged as `SIMULATION MODE`).
- **History & reset** — bottom sheet listing all attendance records (coordinates + recorded distance), and a confirm-dialog "Reset Everything".
- **Permission & GPS handling** — in-app banners when location permission is missing (with re-request) or when location services are off (with an *Enable GPS* shortcut to system settings); both are re-checked every time the app resumes.

## 2. Project Structure / Approaches

The project adheres to a strict **Unidirectional Data Flow (UDF)** and **Layered MVI (Model-View-Intent)** architecture: the Compose UI dispatches user actions as `AttendanceIntent` to `AttendanceViewModel`, which reduces them into a single immutable `AttendanceState` (`StateFlow`) and emits one-off `AttendanceEffect` side effects (`SharedFlow` — snackbars, camera animation); the UI never mutates state directly. Business rules live in framework-free use cases (`ValidateAttendanceEligibility`, `MarkAttendance`, `SaveOfficeLocation`); data comes from a DataStore-backed repository and a FusedLocationProviderClient location tracker bound via Hilt.

```
User action ──AttendanceIntent──▶ AttendanceViewModel ──▶ Use Cases (domain) ──▶ Repository (data)
     ▲                                  │                                        │
     └──── AttendanceState (StateFlow) ──┘        AttendanceEffect (SharedFlow) ◀─┘
```

The cycle always runs one way — **Intent → ViewModel → State → Render** — so every screen state is reproducible from the state class alone.

```
app/src/main/java/com/appsbase/attendly/
├── AttendlyApplication.kt      # @HiltAndroidApp
├── MainActivity.kt             # single screen, edge-to-edge
├── core/time/                  # TimeProvider abstraction (testable clock)
├── data/
│   ├── local/                  # AttendanceDataStore (Preferences DataStore)
│   ├── location/               # DefaultLocationTracker (FusedLocationProviderClient)
│   └── repository/             # AttendanceRepositoryImpl
├── di/                         # Hilt modules (dispatchers, tracker, repository)
├── domain/
│   ├── model/                  # OfficeLocation, AttendanceRecord, SimulationConfig, …
│   ├── repository/             # AttendanceRepository interface
│   ├── usecase/                # ValidateAttendanceEligibility, MarkAttendance, SaveOfficeLocation
│   └── util/                   # GeoFenceCalculator (Haversine), TimeValidator
└── ui/
    ├── attendance/             # AttendanceScreen, AttendanceViewModel, MVI contract, components
    └── theme/                  # Material 3 color schemes (light + dark)
```

Eligibility precedence: `OFFICE_NOT_SET → ALREADY_MARKED → OUTSIDE_GEOFENCE → OUTSIDE_TIME_WINDOW → ELIGIBLE`. The step-by-step build plan behind this structure is in [docs/PLAN.md](docs/PLAN.md).

## 3. Generative AI Usage

This project was built with an AI-agent workflow (Google Antigravity & Gemini) — not one-shot generation, but a **draft → refine plan → generate → review → iterate** loop. The actual sequence, with the real artifacts:

**Step 1 — Draft requirement.** Started from the raw task brief containing the objective, general requirements, and the functional prompt for the geo-fenced attendance screen — kept verbatim here: [docs/draft_plan.md](docs/draft_plan.md).

**Step 2 — Refine and lock the plan.** The draft was fed to the AI to refine into an executable plan: ambiguities were resolved (simulation separated from production code, eligibility precedence, once-per-day check-in, Maps key via `local.properties` → manifest placeholder), the stack and layer rules were fixed, and each of the five build steps got file lists and acceptance checks. The locked plan: [docs/PLAN.md](docs/PLAN.md).

**Step 3 — Agent generates code from the plan.** The agent then implemented the plan step by step — skeleton & DI → domain → data → MVI/UI → tests & docs — running each step's acceptance check before advancing. This produced the four feature commits in the git history (`feat(setup)`, `feat(data-domain)`, `feat(ui)`, `feat(test-docs)`).

**Step 4 — Review and iterate with practical prompts.** After each build, the project was reviewed and adjusted with short, concrete change prompts; every change was recompiled, re-tested (23 unit tests), and verified on an emulator before being kept. The real iterative prompts:

- *"Remove the undo today option from the context menu, keeping reset everything, and update docs and tests."* — menu simplification after reviewing the simulation options.
- *"Read the whole codebase. Based on the attached Compose design mock, restyle the attendance screen; keep things simple, readable, and production-ready; remove unnecessary code, comments, and files; make the documentation concise and well structured. Do not auto-commit."* — full UI redesign to the card/ring layout plus a dead-code cleanup pass (unused state, intents, template files, stale docs all deleted).
- *"On the top bar keep the title on the left side, and add a back button icon with arrow."* — small UI adjustment, confirmed working on the emulator by tapping the back arrow.

Anything that did not survive review was deleted rather than patched around — the plan and this README were themselves rewritten twice from review feedback.

## 4. How to Run

**Prerequisites:** Android Studio, JDK 17, Android SDK 35, a Google Maps API key, and a device/emulator with Google Play services (Android 7.0 / API 24 or newer).

1. **Clone the repository:**
   ```bash
   git clone <REPO_URL>
   cd attendly
   ```
2. **Configure the Maps key** — copy [`local.properties.example`](local.properties.example) to `local.properties` (git-ignored) and fill in your key (injected into the manifest at build time, never committed to VCS):
   ```bash
   cp local.properties.example local.properties
   ```
   ```properties
   MAPS_API_KEY=YOUR_ACTUAL_GOOGLE_MAPS_API_KEY
   ```
3. **Run the app:**
   ```bash
   ./gradlew installDebug        # build & install on a connected device/emulator
   ```
4. **Run the unit tests** (24 tests across four suites):
   ```bash
   ./gradlew test
   ```

| Suite | Tests | Covers |
| :--- | :--- | :--- |
| `GeoFenceCalculatorTest` | 5 | Haversine distance; inside / exact 50 m boundary / outside; custom radius |
| `TimeValidatorTest` | 5 | Shift boundaries (08:59, 09:00, 18:00, 18:01) and simulation bypass |
| `ValidateAttendanceEligibilityUseCaseTest` | 7 | Full eligibility matrix (unset office, already marked, out of range, out of hours, bypass, eligible) |
| `AttendanceViewModelTest` | 7 | MVI intent→state transitions: permissions, GPS disabled/re-enabled refresh, map targeting, save office, mark attendance, simulation toggle, reset |

5. **Build a release APK** (output: `app/build/outputs/apk/release/`):
   ```bash
   ./gradlew assembleRelease
   ```

**Troubleshooting:** a blank/beige map usually means the Maps key is missing from `local.properties` or the device has no network; an amber *GPS is Disabled* banner means device location services are off — tap **Enable GPS** or enable location in quick settings; distance stays `—` until the first GPS fix arrives (grant location permission when prompted).

## 5. Screenshots

*(Screenshots of the running application — to be added.)*

| Office setup (map targeting) | Out of range | In range / Marked today | Simulation mode & menu |
| :---: | :---: | :---: | :---: |
| _todo_ | _todo_ | _todo_ | _todo_ |
