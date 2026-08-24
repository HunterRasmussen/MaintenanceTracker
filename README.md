# MaintenanceTracker

An Android app for tracking vehicle maintenance: cars, maintenance records with receipt
photos, PDF report export, and local backup/restore. Fully offline — the app requests no
`INTERNET` permission, so nothing you enter ever leaves your device except through a backup
file you explicitly export.

There's no Play Store or F-Droid listing. This is a build-it-yourself, sideload-it-yourself
app — see below.

## Building from source

**Prerequisites:**

- A JDK 17+ with a full JDK (not just a JRE) — you need `jlink` on your `PATH`, which the
  JRE-only package some distros install by default does not include. If you already have
  Android Studio installed, its bundled JBR works and is the easiest fix:
  ```bash
  export JAVA_HOME=/path/to/android-studio/jbr   # e.g. on Linux, often under the Android Studio install dir
  ```
  If a build fails with `jlink executable ... does not exist`, this is why — point
  `JAVA_HOME` at a real JDK (Android Studio's bundled one, Temurin, etc.) and retry.
- The Android SDK (command-line tools are enough; Android Studio also works and is the
  simplest way to get a working SDK set up). Set `sdk.dir` in a `local.properties` file at
  the repo root, or export `ANDROID_HOME`.

**Build an installable debug APK:**

```bash
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

**Build and install directly to a connected device in one step:**

1. On your phone: Settings → About phone → tap "Build number" 7 times to enable Developer
   options, then Settings → System → Developer options → enable USB debugging.
2. Plug the phone in via USB and accept the "Allow USB debugging?" prompt on the phone
   screen.
3. From the repo root:
   ```bash
   ./gradlew installDebug
   ```

This builds the APK and installs it over ADB in one step — no need to manually copy the
file or enable "install from unknown sources," since ADB installs bypass that Play
Protect gate. The app will show up as "MaintenanceTracker" on the device.

## Updating

Re-running `./gradlew installDebug` after pulling new commits reinstalls in place and keeps
your existing data (Room DB + receipt photos live in app-private storage, untouched by
reinstalling the same package/signature).

## Backing up your data

Settings → Backup, inside the app, exports a `.zip` (your data + receipt photos) to
wherever you choose via the system file picker. Importing merges a backup file's contents
into whatever's already on the device rather than overwriting it.
