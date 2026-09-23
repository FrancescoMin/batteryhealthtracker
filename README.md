<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher.xml" width="96" height="96" alt="Battery Health Tracker Logo" />
</p>

<h1 align="center">Battery Health Tracker (Oplus Edition)</h1>

<p align="center">
  <b>Advanced hardware-level battery health, BMS telemetry, and degradation tracker for Oppo, OnePlus, and Realme devices. Powered by Shizuku.</b>
</p>

<p align="center">
  <a href="https://github.com/RikkaApps/Shizuku"><img src="https://img.shields.io/badge/Shizuku-Required%20%2F%20Supported-blue?style=flat-square&logo=android" alt="Shizuku Supported" /></a>
  <img src="https://img.shields.io/badge/Android-14%2B-green?style=flat-square&logo=android" alt="Android 14+" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Display-120%20Hz%20Fluid-brightgreen?style=flat-square" alt="120 Hz Fluid" />
  <img src="https://img.shields.io/badge/Theme-AMOLED%20Pure%20Black-black?style=flat-square" alt="AMOLED Pure Black" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-orange?style=flat-square" alt="License" />
  <img src="https://img.shields.io/badge/PRs-Welcome-brightgreen?style=flat-square" alt="PRs Welcome" />
</p>

---

## 📌 Overview

On modern Android devices—particularly within the **Oplus ecosystem (Oppo, OnePlus, Realme / ColorOS, OxygenOS, Realme UI)**—the operating system restricts and abstracts critical battery metrics. Standard Android battery APIs typically return coarse percentage estimates, rounded cycle counts, or hidden health statistics.

**Battery Health Tracker** bridges this gap. By utilizing **[Shizuku](https://shizuku.rikka.app/)** to execute elevated unprivileged shell transactions (no root required), the app directly interrogates the kernel Battery Management System (BMS) hardware nodes (`/sys/class/oplus_chg/battery/` and `/sys/class/power_supply/battery/`).

This delivers accurate, real-time electrochemical diagnostic data: true State of Health (SOH), cycle counts, internal cell resistance (ESR), cell temperature, thermal capacity compensation (**IEC 61960**), charging IC safety fault registers, and live wattage.

---

## 📸 Screenshots

<table align="center">
  <tr>
    <td align="center" width="33%"><b>AMOLED Pure Black</b></td>
    <td align="center" width="33%"><b>Light Theme</b></td>
    <td align="center" width="33%"><b>Hardware Diagnostics</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/dashboard_amoled.png" width="100%" alt="AMOLED Dashboard" /></td>
    <td><img src="docs/screenshots/dashboard_light.png" width="100%" alt="Light Dashboard" /></td>
    <td><img src="docs/screenshots/hardware_diagnostics.png" width="100%" alt="Hardware Diagnostics" /></td>
  </tr>
  <tr>
    <td align="center" width="33%"><b>Health Trend & Projection</b></td>
    <td align="center" width="33%"><b>Device Presets & Rated mAh</b></td>
    <td align="center" width="33%"><b>Theme Settings</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/health_trend_chart.png" width="100%" alt="Health Trend Chart" /></td>
    <td><img src="docs/screenshots/device_preset_dialog.png" width="100%" alt="Device Presets Dialog" /></td>
    <td><img src="docs/screenshots/theme_selection_dialog.png" width="100%" alt="Theme Dialog" /></td>
  </tr>
</table>

---

## ✨ Key Features

### 🔬 Low-Level BMS & Kernel Telemetry (via Shizuku)
- **Direct Fuel Gauge Interrogation:** Reads physical register values directly from `/sys/class/oplus_chg/battery/` and `/sys/class/power_supply/battery/`.
- **True State of Health (SOH):** Real electrochemical capacity measured against calibrated nominal factory design capacity.
- **Accurate Cycle Counter:** Reads cumulative charge cycles tracked by the physical fuel gauge IC rather than software battery stats resets.
- **Coulomb Counter Integration & BMS Drift Detection:** Tracks synchronization health between the battery management IC and open-circuit voltage (OCV) curves to guide calibration when partial charges cause measurement drift.

### ⚡ True Full Charge vs Display 100%
- **Constant Current (CC) vs Constant Voltage (CV) Tracking:** Detects whether Android's display "100%" is merely the end of the CC phase (~88%–92% chemical saturation) or whether the battery has reached **True Full Saturation** through CV tapering ($I_{\text{now}} \le 80\text{ mA}$).
- Direct raw monitoring of the `/sys/class/oplus_chg/battery/chip_soc` register.

### 🌡️ IEC 61960 Thermal Compensation
- **Normalized Capacity at 25°C:** Normalizes measured full-charge capacity against ambient and battery temperature variations using the international electrochemical standard:
  $$C_{25^\circ\text{C}} = \frac{C_{\text{fcc}}}{1.0 + 0.006 \times (T_{\text{batt}} - 25.0)}$$
- Eliminates seasonal fluctuations (e.g., apparent lower capacity in winter at 15°C vs higher in summer at 35°C), providing an objective indicator of true molecular cell wear.

### 🛡️ BMS Safety & Hardware Protection Registers
- **Silicon-Level Health Monitoring:** Inspects physical protection flags in real time:
  - `short_c_hw_status`: Short-circuit detection across power stages.
  - `short_ic_otp_status`: Hardware over-temperature protection (OTP) of the charge controller.
  - `subboard_temp_err`: Integrity and health of the USB-C sub-board thermal sensors.
- Displays an instant security badge (**SAFE** vs **ALERT**) to verify charging subsystem integrity.

### 📈 Internal Resistance (ESR) & Cell Impedance
- Estimates real-time internal resistance ($\text{m}\Omega$) based on instantaneous $\frac{\Delta V}{I}$ under active discharge and charge loads.
- Educates the user regarding normal electrochemical polarization overpotentials during high-speed SuperVOOC charging.

### 📊 Health History, Projection & Charts
- **Smooth Canvas Trend Visualization:** Interactive linear chart tracking SOH over time with reference lines at 100%, 90%, and the critical 80% industrial battery replacement threshold.
- **Cycle Life Projection:** Estimates remaining charge cycles before reaching 80% capacity based on the user's historical degradation rate.
- **Safe History Management:** Multi-selection deletion, persistent Trash bin, full restoration, and JSON/CSV export.

### 🚀 120 Hz Native Fluidity & Battery-Saving Design
- **Ultra-Fluid UI:** Fully optimized for high-refresh-rate displays (120 Hz) with zero-allocation composables, stable keys, and strict 8.33 ms frame budget compliance.
- **Color-Matched System Bars:** Status bar and navigation bar seamlessly blend with the application header banner for a professional, unified aesthetic.
- **Four Display Theme Modes:**
  1. **System Default:** Follows OS-level light/dark configuration.
  2. **Light Theme:** Classic, clean Material 3 design.
  3. **Dark Theme:** Balanced dark tones designed to reduce eye strain in low-light environments.
  4. **AMOLED Pure Black (`#000000`):** Turns off OLED sub-pixels completely, eliminating screen power draw across background regions.

### 🌐 Multilingual
- Fully localized in **English**, **Italian (Italiano)**, and **Spanish (Español)**.

---

## 📱 Supported Devices & Verified Database

The app includes an extensive, verified database of factory **rated (nominal)** and **typical** capacities for single-cell and serial dual-cell (1S/2S) architectures across the Oplus ecosystem:

| Manufacturer | Model Series | Battery Architecture | Typical / Rated Capacity |
| :--- | :--- | :--- | :--- |
| **Oppo** | Find X9 Pro | Silicon-Carbon Dual-Cell | 7500 mAh / 7290 mAh (28.13 Wh / 27.34 Wh) |
| **Oppo** | Find X9 | Silicon-Carbon Dual-Cell | 7025 mAh / 6840 mAh (26.35 Wh / 25.65 Wh) |
| **Oppo** | Find X8 / X8 Pro | Silicon-Carbon Dual-Cell | 5630–5910 mAh (Typical) |
| **Oppo** | Find X7 / X7 Ultra | Dual-Cell Serial | 5000 mAh / 4860–4880 mAh |
| **Oppo** | Reno 16 (Global/EU) | High-Density Single-Cell | 6000 mAh / 5820 mAh (22.5 Wh / 21.83 Wh) |
| **Oppo** | Reno 16 (China) | Silicon-Carbon | 6700 mAh / 6490 mAh |
| **Oppo** | Reno 15 / 15 Pro | High-Density Single-Cell | 6500 mAh / 6335 mAh (25.48 Wh / 24.84 Wh) |
| **Oppo** | Reno 14 / 14 Pro | High-Density Single-Cell | 6000–6200 mAh / 5840–6060 mAh |
| **OnePlus** | OnePlus 15 | Serial Dual-Cell | 7300 mAh (2×3650) / 7150 mAh (2×3575) |
| **OnePlus** | OnePlus 13 / 13R | Silicon-Carbon Dual-Cell | 6000 mAh / 5840 mAh |
| **OnePlus** | OnePlus 12 / 12R | Dual-Cell Serial | 5400–5500 mAh / 5260–5360 mAh |
| **OnePlus** | OnePlus 11 / 10 Pro | Dual-Cell Serial | 5000 mAh / 4880 mAh |
| **OnePlus** | OnePlus Open | Dual-Cell Foldable | 4805 mAh / 4680 mAh |
| **Realme** | GT 8 Pro | Dual-Cell Serial | 7000 mAh (2×3500) / 6850 mAh (2×3425) |
| **Realme** | GT 7 Pro (Global/EU) | Dual-Cell Serial | 6500 mAh (2×3250) / 6310 mAh (2×3155) |
| **Realme** | GT 6 / GT 5 / Neo | Dual-Cell Serial | 5240–5500 mAh (Typical) |

*Manual rated capacity override is also supported for custom or unlisted models.*

---

## 🛠️ How It Works (Technical Architecture)

```mermaid
flowchart LR
    A[Oplus Kernel Sysfs Nodes] -->|Elevated Read| B[Shizuku Service]
    B -->|IPC Binder| C[BatteryRepository]
    C -->|Normalizes Telemetry| D[BatteryViewModel]
    D -->|StateFlow| E[Jetpack Compose UI]
    D -->|Persistent History| F[Room SQLite Database]
    D -->|Periodic Sampling| G[WorkManager Background Task]
```

1. **Permission Layer:** When granted access through **Shizuku**, the application accesses system-level shell commands to read standard and vendor-specific sysfs nodes that are normally blocked by SELinux from third-party app access.
2. **Data Extraction:** Reads nodes including:
   - `/sys/class/oplus_chg/battery/mcu_vote_soc` & `chip_soc`
   - `/sys/class/oplus_chg/battery/batt_rm` (Remaining chemical capacity in mAh)
   - `/sys/class/oplus_chg/battery/batt_fcc` (Full charge capacity in mAh)
   - `/sys/class/oplus_chg/battery/batt_cc` (BMS cycle count)
   - `/sys/class/oplus_chg/battery/short_c_hw_status`, `short_ic_otp_status`, `subboard_temp_err`
   - `/sys/class/power_supply/battery/current_now`, `voltage_now`, `temp`
3. **Data Processing:**
   - Adapts to vendor sign conventions (discharging vs charging current).
   - Computes State of Health: $\text{SOH} = \frac{C_{\text{fcc}}}{C_{\text{rated}}} \times 100\%$.
   - Applies temperature normalization following IEC 61960 standards.
4. **Reactive UI:** Built entirely in **Jetpack Compose (Material 3)**, reactive flows dynamically render real-time changes without unnecessary recompositions.

---

## 📥 Installation & Setup

### Prerequisites
1. An **Oppo, OnePlus, or Realme** device running Android 14 or higher.
2. **Shizuku** installed and running:
   - Download Shizuku from [Google Play](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or [GitHub Releases](https://github.com/RikkaApps/Shizuku/releases).
   - Start Shizuku via **Wireless Debugging** (no computer required after initial setup) or via **Root** (if rooted).

### App Setup
1. Download the latest `app-debug.apk` (or release APK) from the [Releases](https://github.com/FrancescoMin/batteryhealthtracker/releases) section.
2. Install the APK on your device.
3. Open **Battery Health Tracker**.
4. When prompted, tap **"Authorize Shizuku"** and allow permission in the Shizuku prompt.
5. All hardware telemetry and accurate battery health metrics will immediately populate!

---

## 🔨 Building from Source

### Requirements
- **JDK 17** or **JDK 21**
- **Android SDK 34** (compileSdk 34)
- **Gradle 8.7+**

### Steps
1. Clone this repository:
   ```bash
   git clone https://github.com/FrancescoMin/batteryhealthtracker.git
   cd batteryhealthtracker
   ```
2. Build the debug APK using Gradle wrapper:
   ```bash
   # On Linux / macOS:
   ./gradlew assembleDebug

   # On Windows (PowerShell):
   .\gradlew.bat assembleDebug
   ```
3. Install directly to your connected device via ADB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔒 Privacy & Security

- **100% Offline:** The app does not request `android.permission.INTERNET`. Zero network calls, zero data leaves your device.
- **Zero Trackers / Telemetry:** No Google Analytics, Firebase, Crashlytics, or third-party advertising SDKs.
- **Open Source:** Full source code is open and auditable.
- **Local Storage Only:** Historical measurements are stored in a local on-device SQLite database via Android Room.

---

## 🤝 Contributing

Contributions, device profile additions, translations, and feature suggestions are welcome!

1. Fork the project.
2. Create your feature branch (`git checkout -b feature/NewDevicePreset`).
3. Commit your changes (`git commit -m "Add verified battery preset for Device X"`).
4. Push to the branch (`git push origin feature/NewDevicePreset`).
5. Open a Pull Request.

---

## 📄 License

Distributed under the **Apache License, Version 2.0**. See `LICENSE` for more information.

---

<p align="center">
  <sub>Built with ❤️ for Android enthusiasts, battery health purists, and the Shizuku community.</sub>
</p>