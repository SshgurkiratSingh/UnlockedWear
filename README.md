# UnlockedWear

> **Aerospace-Grade Intelligence and Telemetry Ecosystem**  
> Tailored for the Samsung Galaxy Watch 8 (`SM-L320`) and Samsung Galaxy S25 (`SM-S931B`)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Wear OS](https://img.shields.io/badge/Wear_OS-5%2B_%2F_6-4285F4?logo=wearos)](https://developer.android.com/wear)
[![Watch Face Format](https://img.shields.io/badge/WFF-v2_Declarative-34A853?logo=android)](https://developer.android.com/training/wearables/wff)
[![Samsung SDK](https://img.shields.io/badge/Samsung_Health_Sensor_SDK-1.4.1-1428A0)](https://developer.samsung.com/health)
[![Android](https://img.shields.io/badge/Android-15_API_35-3DDC84?logo=android)](https://developer.android.com)

---

## Project Overview

UnlockedWear is a multi-module wearable intelligence ecosystem designed to utilize the hardware and telemetry capabilities of Samsung's flagship wearable, the Samsung Galaxy Watch 8, in coordination with the Galaxy S25.

The platform provides an instrument-grade aerospace telemetry cockpit on the wrist. It combines an ultra-low-power, zero-executable Watch Face Format (WFF v2) dial with continuous biometric and environmental data processing via the Samsung Health Sensor SDK 1.4.1, synchronized with a dual-engine artificial intelligence companion application powered by DeepSeek and Google Gemini.

```
                           +----------------------------------------+
                           |      GALAXY WATCH 8 (SM-L320)          |
                           |                                        |
                           |   +--------------------------------+   |
                           |   |  :wear-watchface (WFF v2 XML)  |   |
                           |   |  - 3D Gyroscope Parallax       |   |
                           |   |  - Native Samsung Complications|   |
                           |   |  - Inbuilt App Direct Routing  |   |
                           |   |  - Zero Screen Cutoff & Notch  |   |
                           |   +---------------^----------------+   |
                           |                   | System IPC         |
                           |   +---------------v----------------+   |
                           |   |  :wear-core (On-Wrist Engine)  |   |
                           |   |  - Samsung Sensor SDK (PPG/IR) |   |
                           |   |  - Continuous Thermal Tracking │   |
                           |   |  - Tile & Complication Services|   |
                           |   +---------------^----------------+   |
                           +-------------------┼--------------------+
                                               | Bluetooth / Wi-Fi
                                               | Wearable Data Layer
                           +-------------------v--------------------+
                           |       GALAXY S25 (SM-S931B)            |
                           |                                        |
                           |   +--------------------------------+   |
                           |   |  :mobile-hub (Companion App)   |   |
                           |   |  - DeepSeek & Gemini Dual AI   |   |
                           |   |  - 24-Hour Biometric Trends    |   |
                           |   |  - Room Database & Syncer      |   |
                           |   +--------------------------------+   |
                           +----------------------------------------+
```

---

## Core Capabilities

### 1. Instrument-Grade 3D Watch Face (`:wear-watchface`)
- **Zero-Executable Declarative Architecture**: Authored entirely in Watch Face Format (WFF v2), adhering to Wear OS battery preservation standards with less than 3% lit-pixel ratio in Always-On Display (AOD) mode.
- **Hardware Gyroscope Parallax**: Multi-plane depth rendering dynamically tilts the celestial space photography in real time:
  $$\vec{\Delta}(x, y) = \text{clamp}(\text{ANGLE}_{x,y}, -60^\circ, 60^\circ) \times (-0.15)$$
- **Smoked Sapphire Instrument Plates**: 3D floating digital chronometer featuring dual-bevel specular sheen reflections (`#50FFFFFF`) and ambient occlusion drop shadows (`#70000000`).
- **Celestial Wallpaper Deck**: On-watch customizer selection of high-resolution space photography assets:
  - Mars Deep Space Mission
  - Earthrise Orbit
  - Orbital Sunrise
  - Lunar Terminator
  - Orbital Terrain
  - Stealth AMOLED Pure Black

### 2. Native Samsung Complication Architecture
Complication pods integrate directly with official Samsung system providers:
- **PULSE Pod (Bottom-Left)**: Bound to `com.samsung.android.wear.shealth` (`HeartrateComplicationProviderService`). Displays the official Samsung Health heart icon, high-contrast `PULSE` badge, live heart rate reading, and coral baseline accent.
- **POWER Pod (Bottom-Center)**: Bound to the Samsung/Wear OS Battery provider (`BatteryProviderService`). Features a dynamic green ranged progress line scaled to the current battery level.
- **STEPS Pod (Bottom-Right)**: Bound to `com.samsung.android.wear.shealth` (`StepsComplicationProviderService`). Displays live step counts with goal progress scaling toward a 10,000-step target.
- **ATMOSPHERE Pill (Top-Right)**: Bound to `com.samsung.android.watch.weather` (`WeatherComplicationService`). Displays live atmospheric temperatures and weather conditions with built-in fallbacks.
- **CALENDAR Pill (Top-Left)**: Bound to `com.samsung.android.calendar` (`DayAndDateProviderService`). Displays day, date, and schedule status.

### 3. Native Inbuilt Application Routing
Interactive touch targets route directly to Samsung's native system applications without custom intermediary activities:
- **Digital Clock** maps to Samsung Clock and Alarm (`com.samsung.android.watch.alarm`)
- **Chrono Pill** maps to Samsung Stopwatch (`com.samsung.android.watch.stopwatch`)
- **Weather Pill** maps to Samsung Weather (`com.samsung.android.watch.weather`)
- **Date Pill** maps to Samsung Calendar (`com.samsung.android.calendar`)
- **Pulse and Steps Pods** map to Samsung Health (`com.samsung.android.wear.shealth`)
- **Battery Pod** maps to Samsung Battery Settings (`com.samsung.android.batterysavingsettings`)

### 4. Mathematical Circular Bezel Geometry and System Card Clearance
- **Zero Edge Cutoff**: Elements are positioned within circular chord bounds ($W(y) = 2\sqrt{R^2 - dy^2}$ for $R = 225\,\text{px}$). Top capsules reside at $y=42$ within $x \in [92, 358]$, preventing bezel edge clipping.
- **114px Wear OS Overlay Clearance**: Lower pods terminate at $y=336$. The vertical zone from $y=337$ to $y=450$ remains clear so that the floating Wear OS Now Playing media controller, incoming call notifications, and system chips never overlap or obscure metrics.

### 5. Continuous Sensor Tracking Engine (`:wear-core`)
- Direct integration with Samsung Health Sensor SDK 1.4.1 for continuous hardware-level telemetry:
  - **Thermal**: Continuous infrared skin and ambient temperature tracking (`SKIN_TEMPERATURE_CONTINUOUS`).
  - **Cardiovascular**: Continuous PPG heart rate and Inter-Beat Interval (IBI) collection (`HEART_RATE_CONTINUOUS`).
  - **Oxygenation**: On-demand pulse oximetry (`SPO2_ON_DEMAND`).
- Wear OS ProtoLayout Tile Service (`MissionRingTileService`) for glanceable telemetry tiles.

### 6. Dual-Engine Intelligence Companion (`:mobile-hub`)
- **Multi-Model Intelligence Routing**:
  - **DeepSeek API**: Synthesizes structured morning briefings, sleep quality analysis, and workout strain metrics formatted as validated JSON objects.
  - **Google Gemini API**: Facilitates on-demand multi-modal health coaching, natural language queries, and conversational interaction via the official `@google/genai` architecture.
- **Room Persistence**: Stores biometric time-series records, thermal trendlines, and meteorological logs on the Galaxy S25.
- **Bi-Directional Wearable Data Layer**: Synchronizes briefings, alert thresholds, and sensor payloads between devices.

---

## Signal Processing and Mathematical Formulations

### Ambient-Compensated Skin Temperature
To isolate physiological core-temperature variations from ambient environmental shifts:
$$T_{\text{comp}} = T_{\text{skin}} - \beta (T_{\text{ambient}} - T_{\text{ref}})$$

Where:
- $T_{\text{skin}}$: Raw infrared skin temperature
- $T_{\text{ambient}}$: Onboard ambient temperature sensor reading
- $T_{\text{ref}}$: Reference ambient baseline ($24.0^\circ\text{C}$)
- $\beta$: Thermal conductance transfer coefficient ($\approx 0.12$)

### Heart Rate Variability (RMSSD)
Root Mean Square of Successive Differences calculated across validated IBI intervals:
$$\text{RMSSD} = \sqrt{\frac{1}{N-1} \sum_{i=1}^{N-1} (\text{IBI}_{i+1} - \text{IBI}_i)^2}$$

### Dynamic Baseline Tracking (EWMA with Z-Score)
Exponentially Weighted Moving Average for anomaly and trend detection:
$$\mu_t = \alpha \cdot x_t + (1 - \alpha) \cdot \mu_{t-1}$$
$$\sigma_t^2 = \alpha \cdot (x_t - \mu_t)^2 + (1 - \alpha) \cdot \sigma_{t-1}^2$$
$$z = \frac{x_t - \mu_t}{\sigma_t}$$

### Cumulative Erythemal UV Dose Integration
Integration of real-time solar UV index to calculate safe sun exposure margins according to CIE standards:
$$\text{Dose}_{\text{UV}} = \int_0^t \text{UVI}(\tau) \cdot k_{\text{skin}} \, d\tau$$

---

## Project Structure

```
UnlockedWear/
├── shared-protocol/                # Kotlin multiplatform data contracts
│   └── src/main/kotlin/com/unlockedwear/protocol/
│       ├── CognitiveLoad.kt        # Workload and cognitive stress models
│       ├── DataLayerPaths.kt       # Wearable Data Layer URI constants
│       ├── DataModels.kt           # Biometric snapshots and thermal frames
│       ├── ProtocolModels.kt       # Serialization contracts
│       ├── RingMode.kt             # Adaptive ring state definition
│       ├── SignalProcessing.kt     # EWMA, RMSSD, and thermal algorithms
│       └── UvRiskLevel.kt          # CIE erythemal UV classification
│
├── wear-watchface/                 # Declarative Watch Face Format (WFF v2)
│   └── src/main/
│       ├── AndroidManifest.xml     # Watch face service registration
│       └── res/
│           ├── drawable-nodpi/     # 480x480 space photography wallpapers
│           ├── drawable/           # Vector icons (pulse, steps, battery, etc.)
│           ├── raw/
│           │   └── watchface.xml   # Master 3D WFF XML dial definition
│           └── values/
│               ├── colors.xml      # Color palette definitions
│               └── strings.xml     # Complication names and configuration labels
│
├── wear-core/                      # Wear OS application engine
│   └── src/main/kotlin/com/unlockedwear/wear/
│       ├── complications/          # Custom complication providers
│       ├── data/                   # DataStore and Room database
│       ├── presentation/           # Rotary-enabled Wear Compose UI
│       ├── sensors/                # Samsung Health Sensor SDK 1.4.1 managers
│       ├── service/                # DataLayer listener and sync services
│       └── tiles/                  # Glanceable ProtoLayout tiles
│
├── mobile-hub/                     # Android companion for Galaxy S25
│   └── src/main/kotlin/com/unlockedwear/mobile/
│       ├── ai/                     # DeepSeek and Google Gemini clients
│       ├── data/                   # Room database, preferences, repositories
│       ├── sync/                   # Watch-to-phone data listener and syncer
│       ├── ui/                     # Material 3 Compose dashboard and charts
│       └── workers/                # WorkManager background briefing tasks
│
└── Assets/                         # Space wallpapers and visual assets
```

---

## Technology Stack

| Layer | Component | Technologies |
|---|---|---|
| **Dial UI** | Watch Face | Watch Face Format (WFF v2 XML), Hardware Gyroscope Parallax |
| **Watch UI** | Companion App | Jetpack Compose for Wear OS 1.5, Horologist, Wear Material |
| **Watch Sensors** | Telemetry | Samsung Health Sensor SDK 1.4.1 (PPG, Thermal IR, SpO2) |
| **Mobile UI** | Phone Hub | Jetpack Compose Material 3, MPAndroidChart / Compose Canvas |
| **Connectivity** | Watch to Phone | Google Play Services Wearable Data Layer API |
| **AI Synthesis** | Dual Models | DeepSeek API (`deepseek-chat`) + Google Gemini API (`gemini-2.5-flash`) |
| **Architecture** | Patterns | Clean Architecture, MVI, Repository Pattern, Hilt DI |
| **Build System** | Toolchain | Gradle 8.12, AGP 8.8.2, Kotlin 2.1.10, Java 17 |

---

## Setup and Deployment

### Prerequisites
- Android Studio Ladybug (2024.2+) or later
- JDK 17+
- Samsung Galaxy Watch 8 with Wireless Debugging enabled
- Samsung Galaxy S25 (or any Android 9.0+ device)

### Enabling Samsung Health Sensor Developer Mode
To permit third-party access to the Samsung Health Sensor SDK on the Galaxy Watch 8:
1. On the watch, navigate to: Settings -> Apps -> Health Sensor Service.
2. Tap the title "Health Sensor Service" repeatedly (approximately 10 times) until Developer Mode is unlocked.
3. Toggle Developer Mode to ON and accept the SDK license terms.
4. Reboot the watch.

### Build and Deploy via ADB

```bash
# 1. Clone repository
git clone git@github.com:SshgurkiratSingh/UnlockedWear.git
cd UnlockedWear

# 2. Build the Watch Face APK
./gradlew :wear-watchface:assembleDebug

# 3. Connect to Galaxy Watch 8 over Wi-Fi
adb connect <WATCH_IP>:<PORT>

# 4. Install watch face directly to Galaxy Watch 8
adb -s <WATCH_DEVICE_ID> install -r wear-watchface/build/outputs/apk/debug/wear-watchface-debug.apk

# 5. Build and install Mobile Hub companion to Galaxy S25
./gradlew :mobile-hub:installDebug
```