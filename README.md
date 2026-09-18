<p align="center"><img width="100" alt="WhatsApp Image 2026-09-19 at 2 39 19 AM" src="https://github.com/user-attachments/assets/d78c968c-6d50-48f7-ac6f-259de681ce77" /></p>



<h1 align="center">DeviceInfo</h1>
<p align="center">Native Android device-information dashboard — hardware specs, system info, and diagnostics in one clean interface.</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Architecture-MVVM-1F3864?style=flat" />
  <img src="https://img.shields.io/badge/Koin-DI-orange?style=flat" />
  <img src="https://img.shields.io/badge/Offline-100%25-brightgreen?style=flat" />
</p>

DeviceInfo gathers and presents a device's technical specifications — hardware, battery, memory, storage, network, display, and sensors — through Android's system APIs, so users don't have to dig through Settings or juggle multiple diagnostic apps. Runs fully offline, no backend required.

---

## 📱 Screenshots

  <img width="200" alt="DeviceInfo screenshot" src="https://github.com/user-attachments/assets/cd07add4-166a-4b05-a582-883ce76753ca" />
  <img width="200" alt="DeviceInfo screenshot" src="https://github.com/user-attachments/assets/26b558bf-9a12-4dcf-a8bc-938a57ff0ccd" />
  <img width="200" alt="DeviceInfo screenshot" src="https://github.com/user-attachments/assets/75de498d-4d61-47d8-833b-3bcc29b61f3d" />
  <img width="200" alt="DeviceInfo screenshot" src="https://github.com/user-attachments/assets/e1c3a4b5-f644-4b6b-96ac-7023ece5310b" />
  <img width="200" alt="DeviceInfo screenshot" src="https://github.com/user-attachments/assets/cf32c849-0ff4-427d-ae52-3ea6dfc7a76e" />
  <img width="200" alt="DeviceInfo screenshot" src="https://github.com/user-attachments/assets/d19605aa-31ce-434c-b8fb-41418122d885" />
  <img width="200" alt="DeviceInfo screenshot" src="https://github.com/user-attachments/assets/cc75dc6a-9e89-4e07-b0f5-56cc9eae37a6" />
  <img width="200" alt="DeviceInfo screenshot" src="https://github.com/user-attachments/assets/f373e0f2-a9d7-49d3-b684-d13d8394416e" />



---

## 🔍 What It Shows

| Category | Details |
|---|---|
| 📱 **Device** | Manufacturer, model, name, Android version/SDK, build & product info |
| ⚙️ **Hardware** | CPU info, supported architectures, core count, hardware identifiers |
| 🔋 **Battery** | Percentage, charging status/state, health, technology, temperature, voltage |
| 💾 **RAM / Memory** | Total, available, used RAM, memory utilization, low-memory status |
| 💽 **Storage** | Total, available, used storage and utilization |
| 📡 **Network** | Wi-Fi info, connection type/status, IP & interface details |
| 🖥️ **Display** | Resolution, dimensions, density, DPI, refresh rate |
| 🧭 **Sensors** | Detects and lists available sensors (accelerometer, gyroscope, proximity, light, etc.) — doesn't assume every device has the same hardware |

All data is retrieved through Android framework APIs (`Build`, `BatteryManager`, `ActivityManager`, `StatFs`, `DisplayMetrics`, `SensorManager`, `ConnectivityManager`, `WifiManager`, `PackageManager`) — no root access required.

---

## 🌍 Multi-Language Support

Fully localized UI via Android's resource system:

- 🇬🇧 English
- 🇵🇰 Urdu
- 🇸🇦 Arabic

Users can switch languages while keeping the same functionality and interface structure.

## 🎨 Theme Support

- ☀️ Light Theme
- 🌙 Dark Theme

---

## 🏗️ Architecture

MVVM keeps the UI independent from device-data collection logic:

```
Activity / Fragment
       │
       ▼
   ViewModel
       │
       ▼
Repository / Data Layer
       │
       ▼
Android System APIs
       │
       ▼
Device Hardware & System Info
```

- **ViewModel** — preserves UI-related data across configuration changes
- **LiveData** — exposes device info from ViewModel to UI in a lifecycle-aware way
- **Coroutines** — collects device data asynchronously without blocking the main thread
- **Koin** — provides dependencies via modules instead of manual instantiation in Activities/ViewModels

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM
- **UI:** XML Layouts
- **State Handling:** LiveData / ViewModel
- **Async:** Kotlin Coroutines
- **Dependency Injection:** Koin
- **Build System:** Gradle

---

## 🔐 Permissions & Privacy

DeviceInfo is a local diagnostic tool — it doesn't upload device information to a remote server just to display it. Only the permissions actually required (e.g. for network-related info) are requested, following Android's modern privacy guidelines.

---

## 🚀 Getting Started

```bash
git clone https://github.com/Basit-Ali-android-Developer/DeviceInfo.git
```

1. Open the project in **Android Studio**
2. Let Gradle sync finish
3. Run on an emulator or physical device — no backend or account setup required

---

<p align="center"><i>Built and maintained by <a href="https://github.com/Basit-Ali-android-Developer">Basit Ali</a></i></p>
