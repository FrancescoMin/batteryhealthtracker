# 📱 Community Device Compatibility & Telemetry Matrix

This document tracks verified hardware compatibility, kernel sysfs driver support, and the complete device capacity catalog (**IEC 61960**) for **Battery Health Tracker** across Oppo, OnePlus, Realme, Samsung Galaxy, and other Android devices.

---

## 🌟 How Compatibility Works in Battery Health Tracker

Unlike conventional battery monitors that rely solely on Android's public `BatteryManager` (which only provides smoothed percentages and instantaneous coulomb counters), Battery Health Tracker directly interrogates physical hardware registers via **[Shizuku](https://shizuku.rikka.app/)** (UID 2000 shell).

### Telemetry Tiers:
1. **Tier 1 — OPlus Kernel Sysfs & Dynamic Fuel-Gauge (Shizuku Required):**
   - Reads directly from `/sys/class/oplus_chg/battery/` (`normal_batt_soh`, `batt_soh`, `batt_qmax`, `batt_fcc`, `batt_rm`, `bcc_parms`, `aging_ffc_data`).
   - Captures genuine BMS degradation %, individual dual-cell voltage balance, internal resistance (ESR), and safety fault registers.
2. **Tier 1.5 — Samsung Galaxy Sec Battery Subsystem & EFS (Shizuku Required):**
   - Reads directly from `/sys/class/power_supply/battery/` (`battery_cycle`, `batt_capacity_max`, `batt_asoc`, `batt_slate_mode`).
   - Captures calibrated ASOC %, learned battery capacity, and dynamic ESR steps without requiring OEM permission bypasses.
3. **Tier 2 — Android 14+ HAL Interface (`BATTERY_PROPERTY_CYCLE_COUNT`):**
   - Automatically queries Android 14 HAL property 7 when sysfs cycle count nodes are isolated or renamed.
4. **Tier 3 — Universal Android BatteryManager Fallback:**
   - Graceful fallback for generic devices or when Shizuku is not running, with transparent warning badges explaining that capacity is based on instantaneous coulomb counting.

---

## 📊 Hardware Verification Status Matrix

These devices have undergone end-to-end hardware testing. All core telemetry metrics (SOH, FCC/Learned Capacity, Cycle Count, Temperature, Voltage, and Power) are verified accurate against native hardware logs.

| Manufacturer | Commercial Model | Codename / Board | SoC Family | Battery Architecture | Tested SOH Source | Cycle Source | Dual-Cell Balance | Compatibility Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Oppo** | **Reno 14 (EU)** | `CPH2737` / `OP5F02L1` | Dimensity 7300 | 1S Single-Cell (5840 mAh) | `normal_batt_soh` (95%) | `battery_cc` (321) | N/A (1S) | 🟢 **100% Verified (Shizuku)** |
| **Oppo** | **Reno 14 Pro (Global/IN)** | `CPH2739` / `OP5F05L1` | Dimensity 8450 | 1S Single-Cell (6060 mAh) | `normal_batt_soh` (100%) | `battery_cycle` (104) | N/A (1S) | 🟢 **100% Verified (Shizuku)** |
| **Oppo** | **Reno 13 5G (Global/IN)** | `CPH2689` / `OP5E9EL1` | Dimensity 8350 | 1S Single-Cell (5450 mAh) | `normal_batt_soh` (100%) | `battery_cycle` (338) | N/A (1S) | 🟢 **100% Verified (Shizuku)** |
| **Realme** | **GT 7T (EU)** | `RMX5085` / `RE6090L1` | Dimensity 8400 | 2S Dual-Cell (6850 mAh) | `battery_log_content` (99%) | `battery_cc` (259) | `bcc_parms` (Δ 1 mV) | 🟢 **100% Verified (Shizuku)** |
| **OnePlus** | **OnePlus 13** | `PJZ110` / `CPH2653` | Snapdragon 8 Elite | 2S Dual-Cell (5840 mAh) | `oplus_mms` / Dynamic SOH | `cycle_count` | `bcc_parms` / dual-cell | 🟢 **100% Verified (Shizuku)** |
| **Samsung** | **Galaxy Tab S9** | `SM-X710` / `SM-X716B` | Snapdragon 8 Gen 2 | 1S Single-Cell (8160 mAh) | Sec Battery / HAL / EFS | `battery_cycle` / HAL | N/A (1S) | 🟢 **100% Verified (Shizuku)** |

---

## 📚 Complete Device Presets & Battery Capacity Catalog

All models below are pre-configured in Battery Health Tracker's automatic preset detection engine. If your device is not listed, you can manually set your rated capacity in **Settings**.

### 1. OnePlus
| Model | Battery Architecture | Typical Capacity | Rated Capacity (IEC 61960) | Status |
| :--- | :--- | :--- | :--- | :--- |
| **OnePlus 13** | Silicon-Carbon Dual-Cell | 6000 mAh | 5840 mAh | 🟢 **100% Verified** |
| OnePlus 13R | Silicon-Carbon Dual-Cell | 6000 mAh | 5840 mAh | 🤝 Help wanted |
| OnePlus 15 | Serial Dual-Cell | 7300 mAh (2×3650) | 7150 mAh (2×3575) | 🤝 Help wanted |
| OnePlus 12 | Dual-Cell Serial | 5400 mAh | 5260 mAh | 🤝 Help wanted |
| OnePlus 12R | Dual-Cell Serial | 5500 mAh | 5360 mAh | 🤝 Help wanted |
| OnePlus 11 | Dual-Cell Serial | 5000 mAh | 4880 mAh | 🤝 Help wanted |
| OnePlus 10 Pro | Dual-Cell Serial | 5000 mAh | 4880 mAh | 🤝 Help wanted |
| OnePlus Nord 5 (Global/IN)* | Dual-Cell Serial | 6800 mAh | 6650 mAh | ⚠️ Requires Root (SELinux) |
| OnePlus Nord 5 (EU/UK)* | Dual-Cell Serial | 5200 mAh | 5200 mAh | 🤝 Help wanted |
| OnePlus Nord 4 | Dual-Cell Serial | 5500 mAh | 5360 mAh | 🤝 Help wanted |
| OnePlus Nord 3 | Dual-Cell Serial | 5000 mAh | 4880 mAh | 🤝 Help wanted |
| OnePlus Nord CE 4 / CE 4 Lite | Dual-Cell Serial | 5500 mAh | 5360 mAh | 🤝 Help wanted |
| OnePlus Open | Dual-Cell Foldable | 4805 mAh | 4680 mAh | 🤝 Help wanted |

*\*Note on Nord 5: OxygenOS SELinux policies strictly isolate Qualcomm sysfs nodes from standard shell UID 2000; direct BMS readings require root (`su`).*

### 2. Oppo
| Model | Battery Architecture | Typical Capacity | Rated Capacity (IEC 61960) | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Oppo Reno 14 (EU)** | High-Density Single-Cell | 6000 mAh | 5840 mAh | 🟢 **100% Verified** |
| **Oppo Reno 14 Pro 5G** | High-Density Single-Cell | 6200 mAh | 6060 mAh | 🟢 **100% Verified** |
| **Oppo Reno 13 5G** | High-Density Single-Cell | 5600 mAh | 5450 mAh | 🟢 **100% Verified** |
| Oppo Find X9 Ultra | High-Density Single-Cell | 7050 mAh | 6890 mAh | 🤝 Help wanted |
| Oppo Find X9 Pro | Silicon-Carbon Dual-Cell | 7500 mAh | 7290 mAh | 🤝 Help wanted |
| Oppo Find X9 | Silicon-Carbon Dual-Cell | 7025 mAh | 6840 mAh | 🤝 Help wanted |
| Oppo Find X8 / X8 Pro | Silicon-Carbon Dual-Cell | 5630–5910 mAh | 5480–5750 mAh | 🤝 Help wanted |
| Oppo Find X7 / X7 Ultra | Dual-Cell Serial | 5000 mAh | 4860–4880 mAh | 🤝 Help wanted |
| Oppo Reno 16 (Global/EU) | High-Density Single-Cell | 6000 mAh | 5820 mAh | 🤝 Help wanted |
| Oppo Reno 16 (China) | Silicon-Carbon Single-Cell | 6700 mAh | 6490 mAh | 🤝 Help wanted |
| Oppo Reno 15 / 15 Pro | High-Density Single-Cell | 6200–6500 mAh | 6040–6335 mAh | 🤝 Help wanted |
| Oppo Reno 10 Pro / 11 Pro | Dual-Cell Serial | 4600 mAh | 4440 mAh | 🤝 Help wanted |

### 3. Realme
| Model | Battery Architecture | Typical Capacity | Rated Capacity (IEC 61960) | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Realme GT 7T (EU)** | Dual-Cell Serial | 7000 mAh (2×3500) | 6850 mAh (2×3425) | 🟢 **100% Verified** |
| Realme GT 8 Pro | Dual-Cell Serial | 7000 mAh (2×3500) | 6850 mAh (2×3425) | 🤝 Help wanted |
| Realme GT 7 Pro (Global/EU/CN) | Dual-Cell Serial | 6500 mAh (2×3250) | 6310 mAh (2×3155) | 🤝 Help wanted |
| Realme GT 7 Pro (India) | Dual-Cell Serial | 5800 mAh | 5660 mAh | 🤝 Help wanted |
| Realme GT 7 | Dual-Cell Serial | 7000 mAh (2×3500) | 6850 mAh (2×3425) | 🤝 Help wanted |
| Realme GT 6 / GT 6T / GT 5 Pro | Dual-Cell Serial | 5400–5500 mAh | 5260–5360 mAh | 🤝 Help wanted |
| Realme 14 Pro+ | Silicon-Carbon Single-Cell | 6000 mAh | 5850 mAh | 🤝 Help wanted |
| Realme 13 Pro+ | High-Density Single-Cell | 5200 mAh | 5050 mAh | 🤝 Help wanted |
| Realme 12 Pro+ | High-Density Single-Cell | 5000 mAh | 4880 mAh | 🤝 Help wanted |

### 4. Samsung Galaxy (Smartphones & Tablets)
| Model Series | Battery Architecture | Typical Capacity | Rated Capacity (IEC 61960) | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Galaxy Tab S9** | High-Capacity Single-Cell | 8400 mAh | 8160 mAh | 🟢 **100% Verified** |
| Galaxy Tab S9+ | High-Capacity Single-Cell | 10090 mAh | 9800 mAh | 🤝 Help wanted |
| Galaxy Tab S9 Ultra | High-Capacity Single-Cell | 11200 mAh | 10880 mAh | 🤝 Help wanted |
| Galaxy Tab S8 / S8+ / S8 Ultra | High-Capacity Single-Cell | 8000–11200 mAh | 7760–10880 mAh | 🤝 Help wanted |
| Galaxy S24 Ultra | High-Density Single-Cell | 5000 mAh | 4855 mAh | 🤝 Help wanted |
| Galaxy S24+ | High-Density Single-Cell | 4900 mAh | 4755 mAh | 🤝 Help wanted |
| Galaxy S24 | High-Density Single-Cell | 4000 mAh | 3880 mAh | 🤝 Help wanted |
| Galaxy S23 / S23+ / S23 Ultra | High-Density Single-Cell | 3900–5000 mAh | 3785–4855 mAh | 🤝 Help wanted |
| Galaxy S22 / S22+ / S22 Ultra | High-Density Single-Cell | 3700–5000 mAh | 3590–4855 mAh | 🤝 Help wanted |
| Galaxy Z Fold 6 / Fold 5 | Dual-Cell Asymmetric | 4400 mAh | 4275 mAh | 🤝 Help wanted |
| Galaxy Z Flip 6 / Flip 5 | Dual-Cell Asymmetric | 4000 mAh | 3887 mAh | 🤝 Help wanted |
| Galaxy A55 / A54 / A35 | High-Density Single-Cell | 5000 mAh | 4905 mAh | 🤝 Help wanted |

---

## 🧪 How to Test and Submit Your Device

We warmly welcome community testing! To verify your device and help the project:

1. **Install Shizuku & Battery Health Tracker:**
   - Install and start [Shizuku](https://shizuku.rikka.app/) via Wireless Debugging.
   - If using ColorOS / OxygenOS / Realme UI, make sure to bypass permission restrictions as described in [OPPO_REALME_ONEPLUS_SHIZUKU_GUIDE.md](../OPPO_REALME_ONEPLUS_SHIZUKU_GUIDE.md).
   - On Samsung Galaxy devices, Shizuku works immediately out-of-the-box without permission toggles.
2. **Open the Built-in Diagnostic Console:**
   - Open Battery Health Tracker.
   - Tap the terminal icon (`>_`) in the top app bar or tap the *Data source* pill.
   - Tap **"Copy Report"** to copy the complete Markdown diagnostic report to your clipboard.
3. **Submit the Report:**
   - Open the **[Device Compatibility Report Form](https://github.com/FrancescoMin/batteryhealthtracker/issues/new?template=device_compatibility.yml)** on GitHub.
   - Fill in your device details, check which metrics are working, and paste the clipboard report.

---

## ⚖️ Legal & Diagnostic Disclaimer

Battery Health Tracker is an independent, community-driven diagnostic tool. It is **not** an official software utility certified by device manufacturers (Oppo, OnePlus, Realme, Samsung, or others). Data displayed may differ from manufacturer service equipment due to sensor tolerances and proprietary closed firmware routines.
