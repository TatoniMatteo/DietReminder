# Diet Reminder (Memo Pasti)

[Italiano](#diet-reminder-memo-pasti---italiano) | [English](#diet-reminder---english)

---

# Diet Reminder (Memo Pasti) - Italiano

Diet Reminder è un'applicazione Android nativa progettata per la gestione centralizzata dei piani alimentari settimanali, il tracciamento degli ingredienti, il monitoraggio dell'idratazione giornaliera e l'integrazione con widget per la schermata Home.

Sviluppata con Kotlin, Jetpack Compose, Material 3, Room Database e Jetpack Glance.

## Panoramica

Diet Reminder offre una soluzione completa per organizzare programmi alimentari strutturati. Consente agli utenti di configurare piani dietetici multipli, consultare un riepilogo settimanale degli ingredienti con dettaglio dei pasti, configurare fasce orarie per i promemoria dell'idratazione e monitorare il prossimo pasto pianificato direttamente dalla schermata Home.

## Funzionalità Principali

### 1. Gestione dei Piani Alimentari
- **Supporto Multi-Dieta:** Crea, duplica, attiva e gestisci molteplici configurazioni dietetiche settimanali.
- **Esportazione e Importazione Dati:** Importa ed esporta i piani alimentari in formato JSON strutturato (`.dr`).
- **Programma Giornaliero:** Visualizza i pasti del giorno categorizzati per tipologia (*Colazione*, *Spuntino*, *Pranzo*, *Cena*, ecc.) ed orario personalizzato.
- **Dettaglio Portate:** Organizza i pasti in portate flessibili (*Primo*, *Secondo*, *Contorno*) contenenti alimenti e quantità specifiche.

### 2. Gestione Centralizzata degli Ingredienti
- **Tabella Settimanale Aggregata:** Calcola e raggruppa automaticamente tutti gli alimenti presenti nella dieta attiva.
- **Vista Dettagliata ed Espandibile:** Seleziona un ingrediente per ispezionare le occorrenze esatte nei giorni della settimana, nei pasti, nelle portate e nelle relative quantità.
- **Autocompletamento in Tempo Reale:** Suggerisce gli alimenti già inseriti durante la configurazione dei pasti per evitare duplicati ed errori di digitazione.

### 3. Monitoraggio dell'Idratazione
- **Contatore Acqua Giornaliero:** Traccia il consumo giornaliero di bicchieri d'acqua.
- **Fasce Orarie Configurabili:** Imposta finestre temporali specifiche per le notifiche (es. dalle 08:00 alle 22:00) e intervalli di promemoria.

### 4. Widget Schermata Home (Jetpack Glance)
- **Widget Nativo Glance:** Visualizza il prossimo pasto programmato direttamente sulla schermata Home dello smartphone.
- **Sincronizzazione in Background:** Gestita tramite WorkManager ed AlarmManager per aggiornamenti affidabili in tempo reale.

### 5. Personalizzazione e Integrazione di Sistema
- **Colori Dinamici Material You:** Integrazione con la palette dei colori dinamici di sistema su Android 12+.
- **Supporto Temi:** Supporta i temi di Sistema, Chiaro e Scuro.
- **Localizzazione:** Supporto nativo completo per Italiano e Inglese.

## Interfaccia Utente

|                   Programma Settimanale                   |                   Gestione Diete                    |                      Lista Ingredienti                       |
|:---------------------------------------------------------:|:---------------------------------------------------:|:------------------------------------------------------------:|
| ![Programma Settimanale](docs/images/screenshot_week.png) | ![Gestione Diete](docs/images/screenshot_diets.png) | ![Lista Ingredienti](docs/images/screenshot_ingredients.png) |

|                     Monitoraggio Idratazione                      |                     Impostazioni                     |                    Widget Home Screen                    |
|:-----------------------------------------------------------------:|:----------------------------------------------------:|:--------------------------------------------------------:|
| ![Monitoraggio Idratazione](docs/images/screenshot_hydration.png) | ![Impostazioni](docs/images/screenshot_settings.png) | ![Widget Home Screen](docs/images/screenshot_widget.png) |

## Architettura Tecnica

L'applicazione segue le linee guida architetturali Android basate su MVVM (Model-View-ViewModel), Repository pattern e Clean Domain Layer.

- **Linguaggio:** Kotlin 2.4.20
- **UI Framework:** Jetpack Compose con Material 3 (1.4.0)
- **Database:** Room 2.8.5 con KSP 2.3.12 (Type Converters, Foreign Key Constraints con eliminazione CASCADE)
- **App Widget:** Jetpack Glance 1.2.0
- **Esecuzione in Background:** WorkManager 2.11.2 & AlarmManager
- **Navigazione:** Navigation Compose Type-Safe con Kotlinx Serialization
- **Infrastruttura di Test:** JUnit4, Robolectric 4.14, Compose UI Test Rules & Robot Pattern

### Specifiche di Ambiente
- **Compile SDK:** 37
- **Target SDK:** 36
- **Min SDK:** 34
- **Compatibilità Java:** JDK 17
- **Versione Gradle:** 9.7.1 (Android Gradle Plugin 9.4.0)

---

[Torna su (Italiano)](#diet-reminder-memo-pasti---italiano) | [Switch to English](#diet-reminder---english)

---

# Diet Reminder - English

Diet Reminder is a native Android application designed for managing weekly dietary plans, centralizing ingredient tracking, monitoring daily hydration, and providing home-screen widget integration.

Built with Kotlin, Jetpack Compose, Material 3, Room Database, and Jetpack Glance.

## Overview

Diet Reminder provides an end-to-end solution for organizing structured meal plans. It enables users to configure multi-diet schedules, view aggregated weekly ingredients with detailed meal breakdowns, configure hydration reminder windows, and track upcoming meals directly from the Android Home Screen.

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

### 3. Hydration Tracker
- **Daily Water Counter:** Track daily water glass consumption.
- **Customizable Alert Windows:** Configure specific notification windows (e.g. 08:00 to 22:00) and reminder intervals.

### 4. Home Screen Widget (Jetpack Glance)
- **Native Glance Widget:** Displays the next upcoming scheduled meal directly on the home screen.
- **Background Synchronization:** Powered by WorkManager and AlarmManager for reliable real-time updates.

### 5. Customization & System Integration
- **Material You Dynamic Colors:** Integrates with system dynamic color palettes on Android 12+.
- **Theme Support:** Supports System, Light, and Dark themes.
- **Localization:** Full native support for Italian and English.

## User Interface Screenshots

|                   Weekly Schedule                   |                   Diets Management                    |                      Ingredients List                       |
|:---------------------------------------------------:|:-----------------------------------------------------:|:-----------------------------------------------------------:|
| ![Weekly Schedule](docs/images/screenshot_week.png) | ![Diets Management](docs/images/screenshot_diets.png) | ![Ingredients List](docs/images/screenshot_ingredients.png) |

|                     Hydration Tracker                      |                     Settings                     |                    Home Screen Widget                    |
|:----------------------------------------------------------:|:------------------------------------------------:|:--------------------------------------------------------:|
| ![Hydration Tracker](docs/images/screenshot_hydration.png) | ![Settings](docs/images/screenshot_settings.png) | ![Home Screen Widget](docs/images/screenshot_widget.png) |

## Technical Architecture

The application follows Android architectural best practices using MVVM (Model-View-ViewModel), a Repository pattern, and a clean domain layer.

- **Linguaggio:** Kotlin 2.4.20
- **UI Framework:** Jetpack Compose with Material 3 (1.4.0)
- **Database:** Room 2.8.5 with KSP 2.3.12 (Type Converters, Foreign Key Constraints with CASCADE deletion)
- **App Widget:** Jetpack Glance 1.2.0
- **Background Work:** WorkManager 2.11.2 & AlarmManager
- **Navigation:** Type-Safe Navigation Compose with Kotlinx Serialization
- **Testing Infrastructure:** JUnit4, Robolectric 4.14, Compose UI Test Rules & Robot Pattern

### Environment Specifications
- **Compile SDK:** 37
- **Target SDK:** 36
- **Min SDK:** 34
- **Java Compatibility:** JDK 17
- **Gradle Version:** 9.7.1 (Android Gradle Plugin 9.4.0)
