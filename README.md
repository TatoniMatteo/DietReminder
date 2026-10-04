# Diet Reminder

Diet Reminder is a native Android application for managing weekly dietary plans, tracking ingredients, scheduling hydration reminders, and showing upcoming meals in a home-screen widget.

Built with Kotlin, Jetpack Compose, Material 3, Room Database, and Jetpack Glance.

## Overview

Diet Reminder helps organize structured meal plans. Users can manage multiple weekly diets, inspect aggregated ingredients, configure local meal and hydration notifications, and see the next scheduled meal from the Android Home Screen.

## Core Capabilities

### 1. Dietary Schedule Management
- **Multi-Diet Support:** Create, duplicate, activate, and manage multiple weekly diet configurations.
- **Data Export & Import:** Import and export diet plans in structured JSON format (`.dr`).
- **Daily Timeline:** View daily scheduled meals categorized by meal types (*Breakfast*, *Lunch*, *Dinner*, etc.) and custom times.
- **Detailed Course Breakdown:** Organize meals into flexible courses (*First course*, *Main course*, *Side dish*) containing specific food items and quantities.

### 2. Centralized Ingredient Management
- **Aggregated Weekly Table:** Automatically computes and groups all food items across the active diet plan.
- **Expandable Detail View:** Tap any ingredient entry to inspect its exact occurrence across days of the week, meal types, courses, and quantities.
- **Real-Time Autocomplete:** Suggests previously entered food items during meal configuration to prevent duplicate entries and typos.

### 3. Hydration Reminders
- **Customizable Alert Windows:** Configure the days, time windows (e.g. 08:00 to 22:00), and interval for hydration notifications.
- **Local Scheduling:** Hydration alerts are scheduled on the device; this feature does not track the amount of water consumed.

### 4. Home Screen Widget (Jetpack Glance)
- **Native Glance Widget:** Displays the next upcoming scheduled meal directly on the home screen.
- **Background Refresh:** WorkManager periodically refreshes the widget, while AlarmManager schedules meal and hydration notifications.

### 5. Customization & System Integration
- **Material You Dynamic Colors:** Integrates with system dynamic color palettes on Android 12+.
- **Theme Support:** Supports System, Light, and Dark themes.
- **Localization:** Native support for Italian and English.
- **Developer Options:** Unlock developer tools by tapping the version row in Settings seven times.

## User Interface Screenshots

|                   Weekly Schedule                   |                   Diets Management                    |                      Ingredients List                       |
|:---------------------------------------------------:|:-----------------------------------------------------:|:-----------------------------------------------------------:|
| ![Weekly Schedule](docs/images/screenshot_week.png) | ![Diets Management](docs/images/screenshot_diets.png) | ![Ingredients List](docs/images/screenshot_ingredients.png) |

|                   Hydration Reminders                   |                     Settings                     |                    Home Screen Widget                    |
|:---------------------------------------------------------:|:------------------------------------------------:|:--------------------------------------------------------:|
| ![Hydration Reminders](docs/images/screenshot_hydration.png) | ![Settings](docs/images/screenshot_settings.png) | ![Home Screen Widget](docs/images/screenshot_widget.png) |

## Technical Architecture

The application uses MVVM (Model-View-ViewModel), Jetpack Compose, Room, and repository interfaces to keep UI, persistence, and application logic separate. Hilt provides dependencies. Database access is local; the repository boundaries are designed to allow a server-backed implementation in the future.

- **Language:** Kotlin 2.4.20
- **UI Framework:** Jetpack Compose with Material 3 (1.4.0)
- **Database:** Room 2.8.5 with KSP 2.3.12 (Type Converters, Foreign Key Constraints with CASCADE deletion)
- **App Widget:** Jetpack Glance 1.2.0
- **Background Work:** WorkManager 2.12.0 & AlarmManager
- **Navigation:** Type-Safe Navigation Compose with Kotlinx Serialization
- **Dependency Injection:** Hilt 2.60.1
- **Testing Infrastructure:** JUnit 4, Robolectric 4.17, Compose UI tests, and screen robots

### Environment Specifications
- **Compile SDK:** 37
- **Target SDK:** 37
- **Min SDK:** 34
- **Gradle runtime:** JDK 21
- **Java source/target compatibility:** Java 17
- **Gradle Wrapper:** 9.8.0
- **Android Gradle Plugin:** 9.4.1

### Build and Test

Build the debug APK:

```bash
./gradlew assembleDebug
```

Run the JVM unit tests (including Robolectric tests):

```bash
./gradlew testDebugUnitTest
```

Run the instrumented tests on a connected device or emulator with API 34 or newer:

```bash
./gradlew connectedDebugAndroidTest
```

### Local Development

Open the project in Android Studio or use the Gradle wrapper commands above. Android SDK 37 and JDK 21 are required by the configured toolchain. The debug APK is produced under `app/build/outputs/apk/debug/`.

### Repository and Offline Strategy

- Repository code is grouped by role under `data/repository`:
  - `contracts/` contains the public repository interfaces and read/write contracts (`DietRepository.kt`, `ShoppingListRepository.kt`, `ConfigRepository.kt`, `VersionPolicyRepository.kt`).
  - `room/` contains the Room-backed implementations currently used by the app.
  - `cache/` exposes read-only repository adapters over Room for data that will be server-owned in a future backend.
  - `delegating/` selects the repository used for reads and rejects writes to server-owned data while offline.
  - `aggregate/` combines related repository contracts; `fake/` contains in-memory repositories for tests.
  - `github/` contains the GitHub implementation used only to retrieve the update policy.
  - A server repository is not implemented yet.

#### What Works Offline

- The app shows an offline service screen with **Retry** and **Continue offline** actions. A mandatory obsolete-version screen cannot be bypassed by continuing offline.
- Shared-data writes stay blocked while the update policy is being checked at startup, so a slow connection cannot briefly allow changes before offline status is known.
- Once in the app, an offline banner indicates that some operations are unavailable. The app reads diets and shopping lists from the local Room database.
- Writes to diets, meals, imports, and shopping lists are disabled in the UI and rejected by repository boundaries. These writes are not queued for later synchronization.
- `AppConfig` preferences are device-local Room data. Appearance, language, developer mode, default meal times, global meal reminders, and hydration-reminder settings can be changed while offline. They are not uploaded to a server.
- Alarm registration and its registry are local device operations, so notification scheduling does not depend on server access.
- Per-diet data, including settings stored on a diet, remains part of the shared diet data and cannot be changed in offline read-only mode.
- The developer mode can still be enabled offline by tapping the version row in Settings seven times.

#### Current Backend and Synchronization Limits

All diet, meal, ingredient, shopping-list, and configuration persistence currently uses Room. The repository interfaces and cache/delegation layers prepare for a future Spring Boot server, but there is no server repository, account-based multi-device sync, offline write queue, or server-to-Room live sync yet. The names `onlineRepository` and `localCacheRepository` describe the intended boundary; they do not mean that diet or shopping-list data currently comes from a server. In the current app, the online path is also Room, with offline mode enforcing read-only behavior for shared data.

The only current GitHub request is for `version-policy.json`. Its repository uses a local 24-hour cache and a bundled asset fallback when the network is unavailable. GitHub is not used for user data.

WorkManager is currently used for widget refresh, not data synchronization. A future server sync should use network-constrained unique work, apply versioned and idempotent updates transactionally, preserve deletion markers, and avoid replacing valid cache data after partial failures. Kafka, if introduced, should remain server-side rather than being consumed directly by the Android app.
