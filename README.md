# Launcher3 QuickLaunch

An Xposed/LSPosed module that lets you press **Enter** in Launcher3's app drawer search to instantly launch the first search result.

This replicates the behavior proposed in [LineageOS Gerrit patch #489879](https://review.lineageos.org/c/LineageOS/android_packages_apps_Launcher3/+/489879) without needing to compile a custom ROM.

## Requirements
* **Root Access**
* **LSPosed Framework** 
  * *Recommended:* [JingMatrix/Vector 2.0](https://github.com/JingMatrix/Vector)
* **Tested on:** Launcher3 on LineageOS 23.2

## Installation & Usage
1. Download the latest `QuickLaunch-vX.X.apk` from the [Releases page](https://github.com/hddq/launcher3-quicklaunch/releases).
2. Install the APK on your device.
3. Open your LSPosed manager (or Vector manager) and go to the **Modules** tab.
4. Enable **Launcher3 QuickLaunch**.
5. The system framework or Launcher3 should be automatically checked in the module scope.
6. **Force stop** the Launcher3 process (or simply reboot your device) to apply the hook.
7. Open your app drawer, type an app name, and hit Enter!

## Building from source
```bash
./gradlew assembleDebug
```
