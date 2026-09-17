# Diet Reminder

Native Android app for managing multiple weekly diets and exposing the next meal through a Glance home-screen widget. Includes a smart hydration reminder system.

## Stack

- Kotlin 2.4.20
- Android Gradle Plugin 9.4.0
- Gradle 9.7.1
- Jetpack Compose (Material 3 1.4.0)
- Room 2.8.5
- KSP 2.3.12
- Glance 1.2.0
- WorkManager 2.11.2
- compileSdk 37 / targetSdk 36
- minSdk 34
- JDK 17

## Features

- **Multiple Diets**: Create and manage different dietary plans.
- **Weekly Editor**: Detailed weekly meal configuration.
- **Next Meal Widget**: Home-screen widget powered by Jetpack Glance showing the next scheduled meal.
- **Hydration Reminders**: Configurable water intake alerts with custom time windows and intervals.
- **Dynamic Theming**: Support for Material You dynamic colors (Android 12+) and light/dark modes.
- **Type-Safe Navigation**: Modern navigation architecture using Kotlin Serialization.
- **Deep Linking**: Direct access to specific screens via URI patterns.

## Build

Open the project in Android Studio and use the included Gradle wrapper.

```bash
./gradlew assembleDebug
```
