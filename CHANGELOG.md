# 📜 Version History & Changelog

All notable changes and technical milestones of **Battery Health Tracker** are documented in this file.

---

### [v1.9] - Current Stable Release
* **Expanded Hardware Coverage:** Added verified hardware profiles for Oppo Reno 14 Pro 5G (`CPH2739` / Dimensity 8450), OnePlus 13 (`PJZ110` / Snapdragon 8 Elite), and Samsung Galaxy S25+ (`SM-S936B`).
* **Home Screen App Widgets:** Introduced Glance-style battery widgets with real-time SOH and cycle counters that auto-refresh on charging and power events.
* **Live BMS Power Streaming:** Added real-time wattage animations with instantaneous charge rate tracking.
* **Anti-Static ESR Filter:** Implemented dynamic rejection filters for static equilibrium float voltages on MediaTek platforms (preventing distortion from 4540 mV CV float nodes).
* **Internationalization:** Added German (`de`) localization.
* **Diagnostic Console:** Integrated shell inspector overlay (`>_`) with 1-click Markdown GitHub bug report generator.

---

### [v1.8]
* **Samsung EFS & Sec-Battery Subsystem:** Deep integration with Samsung fuel-gauge calibration blocks, prioritizing raw chemical EFS ASOC over smoothed One UI display stats.
* **Nothing / CMF Architecture:** Tailored support for Nothing OS and CMF Phone 2 Pro (NT03 battery architecture) with dynamic Coulomb Counter recalibration.
* **Flexible Work Scheduler:** Configurable background telemetry sampling intervals (24h, 2 days, 7 days) synchronized to target execution hours.
* **Internationalization:** Added French (`fr`) localization.

---

### [v1.7]
* **WorkManager Background Engine:** Automated periodic snapshot sampling via Android `WorkManager` with zero persistent background service overhead.
* **True Full-Charge Disconnect Logging:** Automated broadcast receiver logging battery state upon 100% full charge unplug events.
* **Safety & Thermal Alarms:** High-priority system notification dispatcher for critical cell overheat conditions (> 42°C).

---

### [v1.6]
* **Multi-Vendor Expansion:** First multi-vendor milestone adding support for Samsung Galaxy flagships and tablets (Tab S9, Galaxy S24/S23 series).
* **Internationalization:** Added Spanish (`es`) localization.
* **SELinux Fallback Pipeline:** Dual-path querying adapting between vendor-specific sysfs nodes and Android 14+ HAL properties.

---

### [v1.5]
* **Room Database & Local Persistence:** SQLite integration storing comprehensive snapshot telemetry history on-device.
* **Snapshot Trash Bin:** Multi-item soft-deletion management, batch restoration, and permanent purge.
* **Backup & Portability:** Complete JSON and CSV snapshot export/import engine with cryptographic sanitization.

---

### [v1.4]
* **Dynamic Step ESR Sampling:** Revolutionary $\Delta V / \Delta I$ step sampling across active load variations ($\ge 150\text{ mA}$) eliminating reliance on uncalibrated static OCV.
* **Pure AMOLED Black Theme:** True `#000000` dark theme for OLED power efficiency.
* **Internationalization:** Added Italian (`it`) localization.

---

### [v1.3]
* **Interactive Catmull-Rom Bezier Canvas:** Smooth cubic spline degradation visualization with real-time scrub timeline and haptic feedback ticks.
* **Degradation Projections:** Mathematical linear and polynomial degradation projections for 6 and 12-month cell health.

---

### [v1.2]
* **Dual-Cell SuperVOOC Telemetry:** Real-time individual cell monitoring (`cell0Volt`, `cell1Volt`) with series balance calculation ($\Delta\text{ mV}$).
* **Silicon Protection Monitoring:** Real-time cutoff under-voltage threshold monitoring (`vbat_uv`).

---

### [v1.1]
* **IEC 61960 Thermal Compensation:** Temperature-normalized capacity calculations across thermal swings.
* **120 Hz Fluid Display:** Dynamic refresh rate manager locking 120 Hz mode on supported LTPO displays.
* **Android Power Profile Reflection:** Extraction of nominal design capacity from Android framework `power_profile.xml`.

---

### [v1.0] - Initial Launch
* **Elevated Shell Telemetry (via Shizuku):** Direct BMS querying under `UID 2000 shell` without requiring root permissions.
* **OPlus Kernel Sysfs Integration:** Native support for Oppo, OnePlus, and Realme devices running ColorOS and Realme UI.
* **Core Metrics:** Dynamic State of Health (SOH) and non-volatile hardware cycle counter tracking.
