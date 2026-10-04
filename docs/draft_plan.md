# Attendly — Draft Requirement (Original Prompt)

> The raw requirement draft, kept as-is, that was fed to the AI before refinement. The refined and locked plan lives in [PLAN.md](PLAN.md).

**Project Name:** Attendly (package: com.appsbase.attendly)

## Objective

The primary goal of this project is to assess the proficiency in Native Android, state management using Kotlin Flow, API integration, and adherence to software development best practices.

## General Requirements

- **State Management:** robust solution Kotlin Flow.
- **Architecture:** Layered Architecture MVI, for DI use Hilt.
- **Local Storage:** DataStore.
- **Error Handling:** Graceful handling of permissions and hardware failures.
- **UI:** Jetpack Compose with a scalable design system.
- **Proper Language/string management system**

## Task 1: Geo-Fenced Attendance System — (Native Android)

The goal is to implement a location-aware attendance trigger with high accuracy. Create AttendanceScreen where the user will set office location and later on can provide attendance (on the same screen).

- **Setup Phase:** A button to "Set Office Location". This must fetch the current GPS coordinates and save them locally.
- **Validation Phase:** The "Mark Attendance" button should only be enabled/functional if the user's current location is within a 50-meter radius of the saved coordinates.
- **Feedback:** Show a real-time distance indicator (e.g., "You are 120m away from the office").
- **UI:** Please refer to the following screenshot for building the UI for this task with jetpack compose.

## Functional Prompt

- UI will be like attached image as is.
- **Architecture:** UI -> intent/state -> Viewmodel -> (if something reusable or have complex logic create useCase / if not skip useCase) -> Repository -> Remote Service/Storage
- **Storage:** Use DataStore to save and retrieve local data properly.
- **Map:** Use Google Map (do not put map api key in codebase, use local.properties setup as manifest placeholder), user can drag map to change his office location or use current location button, (by default on initial launch it will on current location).
- If office location is not set then, set office location button will show, if office location set then will show update office location button.
- If office location is not set prompt to set office location on the ui elements section of mark attendance as per UI.
- Also check time validation before enabling and applying mark attendance.
- On top bar add a three dot button, to open a context menu, where all simulation will be available, like within time, if user try to test the project on any time, will be prompt to test purpose only, also keep undo attendance on context menu, reset everything on context menu, also a menu to show saved marked attendance. (Make separation between simulation and actual feature code)
- Also include core logic's unit test, skip ui test for now.
- Overall do not need over engineering or over complex solution, keep things simple and effective and scalable production ready.
