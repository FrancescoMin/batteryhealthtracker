# 📱 Community Device Compatibility & Telemetry Matrix

This document tracks verified hardware compatibility, kernel sysfs driver support, and community test results for **Battery Health Tracker** across Oppo, OnePlus, Realme, and other Android devices.

---

## 🌟 How Compatibility Works in Battery Health Tracker

Unlike conventional battery monitors that rely solely on Android's public `BatteryManager` (which only provides smoothed percentages and instantaneous coulomb counters), Battery Health Tracker directly interrogates physical hardware registers via **[Shizuku](https://shizuku.rikka.app/)** (UID 2000 shell).

### Telemetry Tiers:
1. **Tier 1 — OPlus Kernel Sysfs & Dynamic Fuel-Gauge (Shizuku Required):**
   - Reads directly from `/sys/class/oplus_chg/battery/` (`normal_batt_soh`, `batt_soh`, `batt_qmax`, `batt_fcc`, `batt_rm`, `bcc_parms`, `aging_ffc_data`).
   - Captures genuine BMS degradation %, individual dual-cell voltage balance, internal resistance (ESR), and safety fault registers.
2. **Tier 2 — Android 14+ HAL Interface (`BATTERY_PROPERTY_CYCLE_COUNT`):**
   - Automatically queries Android 14 HAL property 7 when sysfs cycle count nodes are isolated or renamed.
3. **Tier 3 — Universal Android BatteryManager Fallback:**
   - Graceful fallback for non-OPlus devices or when Shizuku is not running, with transparent warning badges explaining that capacity is based on instantaneous coulomb counting.

---

## 📊 Hardware Verification Status Matrix

| Manufacturer | Commercial Model | Codename / Board | SoC Family | Battery Architecture | Tested SOH Source | Cycle Source | Dual-Cell Balance | Compatibility Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Oppo** | **Reno 14 (EU)** | `CPH2737` / `OP5F02L1` | Dimensity 7300 | 1S Single-Cell (5840 mAh) | `normal_batt_soh` (95%) | `battery_cc` (321) | N/A (1S) | 🟢 **100% Verified (Shizuku)** |
| **Realme** | **GT 7T (EU)** | `RMX5085` / `RE6090L1` | Dimensity 8400 | 2S Dual-Cell (6850 mAh) | `battery_log_content` (99%) | `battery_cc` (259) | `bcc_parms` (Δ 1 mV) | 🟢 **100% Verified (Shizuku)** |
| **OnePlus** | **OnePlus 13** | `PJZ110` / `CPH2653` | Snapdragon 8 Elite | 2S Dual-Cell (5840 mAh) | `oplus_mms` / Dynamic SOH | `oplus_mms/cycle_count` | `bcc_parms` / dual-cell | 🟢 **100% Verified (Shizuku)** |
| **OnePlus** | OnePlus 13R | `CPH2645` | Snapdragon 8 Gen 3 | 2S Dual-Cell (5840 mAh) | To verify | To verify | To verify | 🤝 Help wanted |
| **OnePlus** | OnePlus 12 / 12R | `CPH2573` / `CPH2609` | Snapdragon 8 Gen 3 / 2 | 2S Dual-Cell (5400 mAh) | To verify | `battery_cycle` | To verify | 🤝 Help wanted |
| **OnePlus** | Nord 5 (Global/IN) | `CPH2707` | Snapdragon 8s Gen 3 | 2S Dual-Cell (6650 mAh) | Direct Sysfs | Sysfs BMS | To verify | ⚠️ Requires Root (SELinux) |
| **Realme** | GT 7 Pro | `RMX5010` | Snapdragon 8 Elite | 2S Dual-Cell (6310 mAh) | To verify | `battery_cc` | To verify | 🤝 Help wanted |
| **Realme** | GT 6 / GT 6T | `RMX3851` / `RMX3853` | Snapdragon 8s Gen 3 / 7+ Gen 3 | 2S Dual-Cell (5360 mAh) | To verify | `battery_cc` | To verify | 🤝 Help wanted |
| **Oppo** | Find X8 / X8 Pro | `CPH2651` / `CPH2659` | Dimensity 9400 | 2S Dual-Cell (5630–5910 mAh) | To verify | `battery_cc` | To verify | 🤝 Help wanted |

---

## 🧪 How to Test and Submit Your Device

We warmly welcome community testing! To verify your device and help the project:

1. **Install Shizuku & Battery Health Tracker:**
   - Install and start [Shizuku](https://shizuku.rikka.app/) via Wireless Debugging.
   - If using ColorOS / OxygenOS / Realme UI, make sure to bypass permission restrictions as described in [OPPO_REALME_ONEPLUS_SHIZUKU_GUIDE.md](../OPPO_REALME_ONEPLUS_SHIZUKU_GUIDE.md).
2. **Open the Built-in Diagnostic Console:**
   - Open Battery Health Tracker.
   - Tap the terminal icon (`>_`) in the top app bar or tap the *Data source* pill.
   - Tap **"Copy Report"** to copy the complete Markdown diagnostic report to your clipboard.
3. **Submit the Report:**
   - Open the **[Device Compatibility Report Form](https://github.com/FrancescoMin/batteryhealthtracker/issues/new?template=device_compatibility.yml)** on GitHub.
   - Fill in your device details, check which metrics are working, and paste the clipboard report.

---

## ⚖️ Legal & Diagnostic Disclaimer

Battery Health Tracker is an independent, community-driven diagnostic tool. It is **not** an official software utility certified by device manufacturers (Oppo, OnePlus, Realme, or others). Data displayed may differ from manufacturer service equipment due to sensor tolerances and proprietary closed firmware routines.
