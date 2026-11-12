# UnlockedWear

> **Aerospace-Grade Intelligence on Your Wrist**
> Multi-module Wear OS + Android companion ecosystem for Galaxy Watch 8 + Galaxy S25

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Wear OS](https://img.shields.io/badge/Wear_OS-5+-4285F4?logo=wearos)](https://developer.android.com/wear)
[![Samsung SDK](https://img.shields.io/badge/Samsung_Health_Sensor_SDK-1.4.1-1428A0)](https://developer.samsung.com/health)

## Architecture

```
UnlockedWear/
├── shared-protocol/     # Pure Kotlin data contracts, signal processing, math
├── wear-watchface/      # WFF XML declarative watch face (zero executable code)
├── wear-core/           # Wear OS APK — Samsung sensors, Compose UI, Tiles, Complications
└── mobile-hub/          # Android companion — DeepSeek/Gemini AI, Room, Weather, Dashboard
```

## Modules

### `:shared-protocol` — Data Contracts & Signal Processing
- `@Serializable` data models: `ThermalFrame`, `BiometricSnapshot`, `DailyBriefing`, `WeatherState`
- EWMA baseline tracking with z-score anomaly detection
- RMSSD (Root Mean Square of Successive Differences) for HRV
- Ambient-compensated skin temperature: `T_comp = T_skin - β(T_amb - T_ref)`
- Cumulative erythemal UV dose integration (WHO/CIE standard)
- Wearable Data Layer path constants

### `:wear-watchface` — Mission Control Watch Face
- 850-line WFF v2 XML — zero executable code
- 450×450 circular AMOLED design for Galaxy Watch 8
- Native Wear OS weather integration via `[WEATHER.TEMPERATURE]` and `[WEATHER.UV_INDEX]` expressions
- 6 complication slots: Mission Ring, Co-Pilot headline, HR, Temp/Weather fallback, SpO2, Battery
- Aerospace HUD aesthetic: radar reticle rings, cardinal azimuth markers
- AMOLED burn-in protection (<3% lit pixels in ambient)
- 4 color themes: Cyber Cyan, Solar Flare, Orbit Green, Stealth Mono

### `:wear-core` — On-Wrist Execution Engine
**Samsung Health Sensor SDK (Primary)**
- `SamsungSdkInitializer` — Full `HealthTrackingService` lifecycle, SDK_POLICY_ERROR detection
- `UnlockedThermalManager` — IR skin/ambient temp via `SKIN_TEMPERATURE_CONTINUOUS/ON_DEMAND`
- `HeartRateManager` — PPG heart rate + IBI extraction via `HEART_RATE_CONTINUOUS`
- `SpO2Manager` — Blood oxygen via `SPO2_ON_DEMAND`

**Wear OS Native APIs**
- Native Wear OS system weather integration with 1-tap launcher
- `MissionRingComplicationService` — Adaptive outer ring (UV/Health/Focus/Power)
- `CoPilotComplicationService` — DeepSeek AI headline
- `ThermalComplicationService` — Skin temp delta
- `MissionRingTileService` — ProtoLayout glanceable tile
- `DataSyncService` — Wearable Data Layer watch↔phone sync

**Compose UI**
- HomeScreen with rotary crown ring mode switching and Wear OS weather tile
- ThermalDetailScreen with 24h trend chart
- CoPilotScreen with DeepSeek briefing + Gemini voice
- RingSelectorScreen with haptic crown navigation
- HealthDetailScreen with real-time HRV

### `:mobile-hub` — Galaxy S25 Intelligence Hub
**AI Engine**
- `DeepSeekClient` — Morning briefing + on-demand analysis (`response_format: json_object`)
- `GeminiClient` — Official Google Gen AI Client SDK (`com.google.ai.client.generativeai`) using `GenerativeModel`
- `IntelligenceRouter` — Multi-model request routing

**Data & Sync**
- Room database: thermal, biometric, briefing, weather entities
- `WatchDataListenerService` — Receives sensor payloads from watch
- `PhoneToWatchSyncer` — Pushes briefings/weather/ring mode to watch
- `WeatherService` — OpenWeatherMap + Open-Meteo fallback

**Background Workers**
- `MorningBriefingWorker` — Daily 7 AM DeepSeek synthesis
- `WeatherSyncWorker` — 2-hour periodic UV/weather updates

**Compose M3 Dashboard**
- Dashboard, Health (charts), AI (dual-engine chat), Settings screens

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.1.10 |
| Build | Gradle 8.12 + AGP 8.8.2 |
| Watch Sensors | Samsung Health Sensor SDK 1.4.1 |
| Watch UI | Jetpack Compose for Wear OS 1.5.0 |
| Watch Face | Watch Face Format (WFF) v2 XML |
| Phone UI | Jetpack Compose Material 3 |
| Watch↔Phone | Wearable Data Layer API |
| DI | Hilt 2.53.1 |
| Database | Room 2.7.2 |
| Networking | Ktor 3.0.3 |
| Background | WorkManager 2.10.0 |
| AI | DeepSeek API + Google Gemini API |
| Weather | OpenWeatherMap / Open-Meteo |

## Setup

### Prerequisites
- Android Studio Ladybug or later
- JDK 17+
- Galaxy Watch 8 with Samsung Health Developer Mode enabled
- Galaxy S25 (or any Android 9+ phone)

### Samsung Health Developer Mode
1. On your Watch 8: **Settings → Apps → Health Sensor Service**
2. Tap the title approximately **10 times**
3. Enable **Developer Mode**
4. Accept the SDK licence
5. Reboot the watch

### Samsung Health Sensor SDK
1. Download `samsung-health-sensor-api.aar` from [Samsung Developer Portal](https://developer.samsung.com/health/sensor)
2. Place it in `wear-core/libs/samsung-health-sensor-api.aar`

### API Keys
Configure in the mobile app Settings screen:
- **DeepSeek API Key** — [platform.deepseek.com](https://platform.deepseek.com)
- **Gemini API Key** — [aistudio.google.com](https://aistudio.google.com)
- **OpenWeatherMap API Key** — [openweathermap.org](https://openweathermap.org/api)

### Build
```bash
./gradlew :wear-core:assembleDebug      # Watch app
./gradlew :wear-watchface:assembleDebug  # Watch face
./gradlew :mobile-hub:assembleDebug      # Phone companion
```

## License
Copyright © 2026 UnlockedWear. All rights reserved.