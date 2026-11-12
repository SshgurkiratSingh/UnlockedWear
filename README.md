# UnlockedWear

> **Aerospace-Grade Intelligence & Telemetry Ecosystem**  
> Tailored for the **Samsung Galaxy Watch 8** (`SM-L320`) and **Galaxy S25** (`SM-S931B`)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Wear OS](https://img.shields.io/badge/Wear_OS-5%2B_%2F_6-4285F4?logo=wearos)](https://developer.android.com/wear)
[![Watch Face Format](https://img.shields.io/badge/WFF-v2_Declarative-34A853?logo=android)](https://developer.android.com/training/wearables/wff)
[![Samsung SDK](https://img.shields.io/badge/Samsung_Health_Sensor_SDK-1.4.1-1428A0)](https://developer.samsung.com/health)
[![Android](https://img.shields.io/badge/Android-15_API_35-3DDC84?logo=android)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

---

## 🛰️ Project Overview

**UnlockedWear** is a multi-module wearable intelligence ecosystem designed to unlock the hardware and telemetry potential of Samsung's latest flagship wearable—the **Samsung Galaxy Watch 8**—in tandem with the **Galaxy S25**. 

Rather than relying on flat watchfaces or fragmented fitness trackers, UnlockedWear delivers an authentic, **instrument-grade aerospace telemetry cockpit**. It pairs an ultra-low-power, zero-executable **Watch Face Format (WFF v2)** dial with continuous biometric and environmental processing via the **Samsung Health Sensor SDK 1.4.1**, all synchronized with a dual-engine AI companion app driven by **DeepSeek** and **Google Gemini**.

```
                           ┌────────────────────────────────────────┐
                           │      GALAXY WATCH 8 (SM-L320)          │
                           │                                        │
                           │   ┌────────────────────────────────┐   │
                           │   │  :wear-watchface (WFF v2 XML)  │   │
                           │   │  - 3D Gyroscope Parallax       │   │
                           │   │  - Native Samsung Complications│   │
                           │   │  - Inbuilt App Direct Routing  │   │
                           │   │  - Zero Screen Cutoff & Notch  │   │
                           │   └───────────────▲────────────────┘   │
                           │                   │ System IPC         │
                           │   ┌───────────────▼────────────────┐   │
                           │   │  :wear-core (On-Wrist Engine)  │   │
                           │   │  - Samsung Sensor SDK (PPG/IR) │   │
                           │   │  - Continuous Thermal Tracking │   │
                           │   │  - Tile & Complication Services│   │
                           │   └───────────────▲────────────────┘   │
                           └───────────────────┼────────────────────┘
                                               │ Bluetooth / Wi-Fi
                                               │ Wearable Data Layer
                           ┌───────────────────▼────────────────────┐
                           │       GALAXY S25 (SM-S931B)            │
                           │                                        │
                           │   ┌────────────────────────────────┐   │
                           │   │  :mobile-hub (Companion App)   │   │
                           │   │  - DeepSeek & Gemini Dual AI   │   │
                           │   │  - 24-Hour Biometric Trends    │   │
                           │   │  - Room Database & Syncer      │   │
                           │   └────────────────────────────────┘   │
                           └────────────────────────────────────────┘
```

---

## ⚡ Core Capabilities

### 1. Instrument-Grade 3D Watch Face (`:wear-watchface`)
- **Zero-Executable Declarative Architecture**: Authored entirely in **Watch Face Format (WFF v2)**, meeting strict Wear OS battery preservation standards with <3% lit-pixel ratio in Always-On Display (AOD) mode.
- **Hardware Gyroscope Parallax**: Multi-plane depth rendering dynamically tilts the celestial space photography in real time:
  $$\vec{\Delta}(x, y) = \text{clamp}(\text{ANGLE}_{x,y}, -60^\circ, 60^\circ) \times (-0.15)$$
- **Smoked Sapphire Instrument Plates**: 3D floating digital chronometer with dual-bevel specular sheen reflections (`#50FFFFFF`) and soft ambient occlusion drop shadows (`#70000000`).
- **Celestial Wallpaper Deck**: In-watch customizer selection of high-resolution space imagery from assets:
  - *Mars Deep Space Mission*
  - *Earthrise Orbit*
  - *Orbital Sunrise*
  - *Lunar Terminator*
  - *Orbital Terrain*
  - *Stealth AMOLED Pure Black*

### 2. Native Samsung Complication Architecture
All complication pods connect to authentic Samsung system providers rather than custom debug cards:
- **PULSE Pod (Bottom-Left)**: Bound to `com.samsung.android.wear.shealth` (`HeartrateComplicationProviderService`). Displays the official Samsung Health heart icon, bold `PULSE` badge, live BPM, and coral glow baseline.
- **POWER Pod (Bottom-Center)**: Bound to Samsung/Wear OS Battery provider (`BatteryProviderService`). Features dynamic green ranged progress line scaled dynamically to current battery percentage.
- **STEPS Pod (Bottom-Right)**: Bound to `com.samsung.android.wear.shealth` (`StepsComplicationProviderService`). Features live step count with goal progress fill toward 10,000 steps.
- **ATMOSPHERE Pill (Top-Right)**: Bound to `com.samsung.android.watch.weather` (`WeatherComplicationService`). Displays live temperature and condition with built-in fallbacks.
- **CALENDAR Pill (Top-Left)**: Bound to `com.samsung.android.calendar` (`DayAndDateProviderService`). Displays day and date with schedule awareness.

### 3. Native Inbuilt App Routing (Zero Custom Debug Overrides)
Every touch target routes directly to Samsung's native system applications:
- **Digital Clock** $\rightarrow$ Samsung Clock & Alarm (`com.samsung.android.watch.alarm`)
- **Chrono Pill** $\rightarrow$ Samsung Stopwatch (`com.samsung.android.watch.stopwatch`)
- **Weather Pill** $\rightarrow$ Samsung Weather (`com.samsung.android.watch.weather`)
- **Date Pill** $\rightarrow$ Samsung Calendar (`com.samsung.android.calendar`)
- **Pulse & Steps Pods** $\rightarrow$ Samsung Health (`com.samsung.android.wear.shealth`)
- **Battery Pod** $\rightarrow$ Samsung Battery Settings (`com.samsung.android.batterysavingsettings`)

### 4. Mathematical Circular Bezel Geometry & Notch Clearance
- **Zero Edge Cutoff**: Elements are placed strictly inside circular chord bounds ($W(y) = 2\sqrt{R^2 - dy^2}$ for $R = 225\,\text{px}$). Top capsules sit at $y=42$ within $x \in [92, 358]$, completely eliminating bezel clipping.
- **$114\,\text{px}$ Wear OS Overlay Clearance**: Lower pods terminate at $y=336$. The entire vertical zone from $y=337$ to $y=450$ is reserved so that the floating **Now Playing media controller**, incoming call banners, and system notification chips never collide with or obscure metrics.

### 5. Continuous Sensor Tracking Engine (`:wear-core`)
- Direct integration with **Samsung Health Sensor SDK 1.4.1** for hardware-level telemetry:
  - **Thermal**: Continuous infrared skin and ambient temperature tracking (`SKIN_TEMPERATURE_CONTINUOUS`).
  - **Cardiovascular**: Continuous PPG heart rate and Inter-Beat Interval (IBI) collection (`HEART_RATE_CONTINUOUS`).
  - **Oxygenation**: On-demand pulse oximetry (`SPO2_ON_DEMAND`).
- Wear OS ProtoLayout Tile Service (`MissionRingTileService`) for glanceable telemetry tiles.

### 6. Dual-Engine Intelligence Companion (`:mobile-hub`)
- **Multi-Model Intelligence Router**:
  - **DeepSeek API**: Synthesizes structured morning briefings, sleep quality analysis, and workout strain metrics with JSON object formatting.
  - **Google Gemini API**: Handles on-demand multi-modal health coaching, natural language queries, and voice interaction using the official `@google/genai` client architecture.
- **Room Persistence**: Stores biometric time-series data, thermal trendlines, and weather history on the Galaxy S25.
- **Bi-Directional Wearable Data Layer**: Transparent synchronization of briefings, alert thresholds, and sensor payloads.

---

## 📐 Signal Processing & Mathematical Formulations

### Ambient-Compensated Skin Temperature
To isolate true physiological core-temperature variations from external ambient shifts:
$$T_{\text{comp}} = T_{\text{skin}} - \beta (T_{\text{ambient}} - T_{\text{ref}})$$
Where:
- $T_{\text{skin}}$ = Raw IR sensor skin temperature
- $T_{\text{ambient}}$ = Onboard ambient temperature sensor reading
- $T_{\text{ref}}$ = Reference ambient baseline ($24.0^\circ\text{C}$)
- $\beta$ = Thermal conductance transfer coefficient ($\approx 0.12$)

### Heart Rate Variability (RMSSD)
Root Mean Square of Successive Differences calculated across validated IBI intervals:
$$\text{RMSSD} = \sqrt{\frac{1}{N-1} \sum_{i=1}^{N-1} (\text{IBI}_{i+1} - \text{IBI}_i)^2}$$

### Dynamic Baseline Tracking (EWMA with Z-Score)
Exponentially Weighted Moving Average for anomaly detection:
$$\mu_t = \alpha \cdot x_t + (1 - \alpha) \cdot \mu_{t-1}$$
$$\sigma_t^2 = \alpha \cdot (x_t - \mu_t)^2 + (1 - \alpha) \cdot \sigma_{t-1}^2$$
$$z = \frac{x_t - \mu_t}{\sigma_t}$$

### Cumulative Erythemal UV Dose Integration
Integration of real-time solar UV index to calculate safe sun exposure margins:
$$\text{Dose}_{\text{UV}} = \int_0^t \text{UVI}(\tau) \cdot k_{\text{skin}} \, d\tau$$

---

## 📂 Project Structure

```
UnlockedWear/
├── shared-protocol/                # Pure Kotlin multiplatform data contracts
│   └── src/main/kotlin/com/unlockedwear/protocol/
│       ├── CognitiveLoad.kt        # Workload and cognitive stress models
│       ├── DataLayerPaths.kt       # Wearable Data Layer URI constants
│       ├── DataModels.kt           # Biometric snapshots and thermal frames
│       ├── ProtocolModels.kt       # Serialization contracts
│       ├── RingMode.kt             # Adaptive ring state enum
│       ├── SignalProcessing.kt     # EWMA, RMSSD, thermal algorithms
│       └── UvRiskLevel.kt          # CIE erythemal UV classification
│
├── wear-watchface/                 # Declarative Watch Face Format (WFF v2)
│   └── src/main/
│       ├── AndroidManifest.xml     # Watch face service registration
│       └── res/
│           ├── drawable-nodpi/     # 480×480 space photography wallpapers
│           ├── drawable/           # Vector icons (pulse, steps, battery, etc.)
│           ├── raw/
│           │   └── watchface.xml   # Master 3D WFF XML dial definition
│           └── values/
│               ├── colors.xml      # Color palette (Cyber Cyan, Solar Orange, etc.)
│               └── strings.xml     # Complication names and config labels
│
├── wear-core/                      # Wear OS application engine
│   └── src/main/kotlin/com/unlockedwear/wear/
│       ├── complications/          # Custom complication providers
│       ├── data/                   # DataStore and Room database
│       ├── presentation/           # Rotary-enabled Wear Compose UI
│       ├── sensors/                # Samsung Health Sensor SDK 1.4.1 managers
│       ├── service/                # DataLayer listener & sync services
│       └── tiles/                  # Glanceable ProtoLayout tiles
│
├── mobile-hub/                     # Android companion for Galaxy S25
│   └── src/main/kotlin/com/unlockedwear/mobile/
│       ├── ai/                     # DeepSeek & Google Gemini dual-engine clients
│       ├── data/                   # Room database, preferences, repositories
│       ├── sync/                   # Watch-to-phone data listener & syncer
│       ├── ui/                     # Material 3 Compose dashboard & charts
│       └── workers/                # WorkManager background briefing tasks
│
└── Assets/                         # Space wallpapers and visual assets
```

---

## 🛠️ Technology Stack

| Layer | Component | Technologies |
|---|---|---|
| **Dial UI** | Watch Face | Watch Face Format (WFF v2 XML), Hardware Gyroscope Parallax |
| **Watch UI** | Companion App | Jetpack Compose for Wear OS 1.5, Horologist, Wear Material |
| **Watch Sensors** | Telemetry | Samsung Health Sensor SDK 1.4.1 (PPG, Thermal IR, SpO2) |
| **Mobile UI** | Phone Hub | Jetpack Compose Material 3, MPAndroidChart / Compose Canvas |
| **Connectivity** | Watch $\leftrightarrow$ Phone | Google Play Services Wearable Data Layer API |
| **AI Synthesis** | Dual Models | DeepSeek API (`deepseek-chat`) + Google Gemini API (`gemini-2.5-flash`) |
| **Architecture** | Patterns | Clean Architecture, MVI, Repository Pattern, Hilt DI |
| **Build System** | Toolchain | Gradle 8.12, AGP 8.8.2, Kotlin 2.1.10, Java 17 |

---

## 🚀 Setup & Deployment

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or later
- **JDK 17+**
- **Samsung Galaxy Watch 8** with Wireless Debugging enabled
- **Samsung Galaxy S25** (or any Android 9.0+ device)

### Enabling Samsung Health Sensor Developer Mode
To allow third-party access to the Samsung Health Sensor SDK on your Galaxy Watch 8:
1. On the watch, navigate to: **Settings $\rightarrow$ Apps $\rightarrow$ Health Sensor Service**.
2. Tap the title **"Health Sensor Service"** repeatedly (~10 times) until the Developer Mode prompt appears.
3. Toggle **Developer Mode** to **ON** and accept the SDK license terms.
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

---

## 📜 License

```
Copyright 2026 Gurkirat Singh / UnlockedWear

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```