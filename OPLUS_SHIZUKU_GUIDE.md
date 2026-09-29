# 📱 Shizuku Setup & Troubleshooting Guide for OPPO, Realme & OnePlus
*(ColorOS, Realme UI & OxygenOS)*

> **Target Devices:** OPPO, Realme, and OnePlus smartphones running ColorOS 12–16, Realme UI 3.0–6.0, or OxygenOS 12–16 (Android 12, 13, 14, 15, and 16).

---

## 📌 Why OPlus Devices Require Special Attention

Devices manufactured by the **OPlus group (OPPO, OnePlus, Realme)** include a proprietary security subsystem embedded in ColorOS, Realme UI, and OxygenOS. 

When you start Shizuku via **Wireless Debugging**, the local Android `adbd` daemon runs under the `shell` user (`UID 2000`). By default, the OPlus system monitors and blocks inter-process Binder communication (`Binder IPC`) between the `shell` user and third-party user applications (such as **Battery Health Tracker**).

When this occurs, Shizuku displays the following error:
> 🔴 **"The permission of ADB is limited"** *(Italian: "L'autorizzazione di ADB è limitata" / "Devi fare un passo in più")*

Until this restriction is lifted, client apps cannot bind to Shizuku, and hardware battery telemetry cannot be extracted.

---

## ⚠️ Common Trap: "Disable child process restrictions" vs "Disable permission monitoring"

In Developer Options, there are two distinct settings that sound similar but do completely different things:

| Setting Name | Category | Scope & Purpose |
| :--- | :--- | :--- |
| **Disable child process restrictions**<br>*(Disattiva limitazioni per i processi secondari)* | System / Debugging | **AOSP standard (Android 12+):** Prevents Android's Phantom Process Killer from terminating apps running multiple background threads/sub-processes (e.g., Termux). **Does NOT fix the ADB permission error.** |
| **Disable permission monitoring**<br>*(Disabilita monitoraggio autorizzazioni)* | **Apps (At the very bottom)** | **Proprietary OPlus feature:** Removes the security firewall blocking Binder IPC transactions between `shell` and third-party apps. **This is the setting required by Shizuku.** |

---

## 🛠️ Step-by-Step Resolution

### Method 1: Wireless Debugging (On-Device, No PC)

#### Step 1: Find the Hidden Setting
1. Open **Settings** (*Impostazioni*) > **Additional settings / System settings** (*Impostazioni di sistema*) > **Developer options** (*Opzioni sviluppatore*).
2. Scroll **all the way down to the very bottom** of the page to the **Apps** (*Applicazioni*) section.
3. Look for **"Disable permission monitoring"** (*Disabilita monitoraggio autorizzazioni*) and toggle it **ON**.
4. *Variant on newer ColorOS 14/15 / Realme UI 5/6:* If not found under that name, look for **"Disable system optimization"** (*Disattiva ottimizzazione di sistema*).

#### Step 2: The Language Trick (For European EEA Firmware)
On many European firmware builds (e.g., Realme GT 7T `RMX5085EEA` or Reno 14 EU):
- If the system language is set to **Italian** or other non-English languages, the "Disable permission monitoring" toggle is frequently **hidden by the manufacturer's localized UI layout**.
- **Fix:**
  1. Go to **Settings > System settings > Language & region**.
  2. Temporarily switch the primary language to **English (United States)**.
  3. Reopen **Developer options** and scroll to the bottom: the **"Disable permission monitoring"** switch will now be visible.
  4. Toggle it **ON**.
  5. You can now safely switch your language back to Italian.

#### Step 3: Refresh ADB & Apply Settings
Changes to permission monitoring often do not take effect dynamically until ADB restarts:
1. In Developer Options, toggle **USB Debugging** (and **Wireless Debugging**) **OFF** and then **ON** again.
2. If Shizuku still shows the warning, perform a **quick reboot** of the phone.
3. Open Shizuku and tap **Start** under Wireless Debugging.

---

### Method 2: The Infallible PC / USB Method (100% Recommended)

If Wireless Debugging continues to be blocked by your firmware or you don't have access to Wi-Fi, starting Shizuku via **PC (USB cable)** completely bypasses the local wireless restrictions.

1. Connect your phone to your computer via USB cable.
2. Ensure **USB Debugging** (*Debug USB*) is enabled in Developer Options and authorize the computer prompt on your phone screen.
3. On your computer, open a terminal (PowerShell, Command Prompt, or Terminal) and execute:

   ```bash
   adb devices
   ```
   *(Ensure your device is listed with `device`, not `unauthorized`).*

4. **Unlock permission monitoring directly via ADB (Optional / Recommended):**
   ```bash
   adb shell settings put global direct_control_permission_monitoring 0
   ```

5. **Start the Shizuku server directly:**
   ```bash
   # Standard Shizuku starter script:
   adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
   ```
   *Or direct binary invocation (useful if storage access is restricted):*
   ```bash
   adb shell $(pm path moe.shizuku.privileged.api | sed -e 's/package://' -e 's/base.apk/lib\/arm64\/libshizuku.so/')
   ```

6. The Shizuku server will immediately launch under `UID 2000 (shell)` without any permission limitations.

---

## 🔋 Preventing Background Termination on OPlus Devices

ColorOS and Realme UI have aggressive battery optimization daemons (`oplus_chg`, `Athena`, and `Powerkeeper`) that may terminate Shizuku or Battery Health Tracker background services after several hours.

To keep telemetry and automatic sampling active:

1. **Enable Phantom Process Killer bypass:**
   - In Developer Options, enable **"Disable child process restrictions"** (*Disattiva limitazioni per i processi secondari*).
2. **Configure Shizuku App Permissions:**
   - Long press the **Shizuku** app icon > **App info** (*Informazioni app*).
   - Go to **Battery usage** (*Utilizzo batteria*) and enable:
     - **Allow background activity** (*Consenti attività in background*)
     - **Allow auto-launch** (*Consenti avvio automatico*)
3. **Configure Battery Health Tracker:**
   - Repeat the same steps for **Battery Health Tracker** to ensure 24h periodic snapshots and 100% full-charge disconnect sampling can execute reliably in the background via `WorkManager`.

---

## 📋 Quick Troubleshooting Checklist

| Symptom | Root Cause | Solution |
| :--- | :--- | :--- |
| **"ADB permission is limited" banner in Shizuku** | OPlus Binder IPC firewall is active. | Enable *"Disable permission monitoring"* at bottom of Dev Options, or start Shizuku once via PC USB. |
| **"Disable permission monitoring" is missing** | Hidden in non-English UI or renamed. | Switch phone language to English (US) temporarily, or look for *"Disable system optimization"*. |
| **Shizuku stops every time phone reboots** | Normal Android security architecture. | Wireless/USB ADB sessions terminate on reboot. Restart Shizuku via Wireless Debugging or PC script. |
| **Shizuku stops after a few hours of screen off** | OPlus background battery cleaner. | Set Shizuku battery to *"Don't optimize"* and enable *"Disable child process restrictions"*. |
| **Battery Health Tracker says "Shizuku not running"** | Server was killed or permission not granted. | Open Shizuku, verify it says *"Shizuku is running"*, then tap *"Authorized applications"* and ensure Battery Health Tracker is toggled ON. |

---

*Related Documentation:*  
- [Main README](README.md)  
- [Official Shizuku Documentation](https://shizuku.rikka.app/)
