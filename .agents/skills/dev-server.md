# Dev Server Skill — OrderMate

Prepares the Android development environment before running instrumented tests.
Warms up the Gradle daemon, verifies an emulator/device is reachable, and
resolves dependencies so test runs start quickly.

## Quick Commands

### Warm Gradle Daemon

```bash
./gradlew --daemon help
```

### Check Connected Devices

```bash
adb devices
# Expected: at least one line ending in "device" (not "offline")
```

### Start Emulator (if none connected)

```bash
# List available AVDs
emulator -list-avds

# Start an AVD in the background
emulator -avd <AVD_NAME> -no-window -no-audio &

# Wait for boot
adb wait-for-device shell getprop sys.boot_completed
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null)" != "1" ]; do
  echo "Waiting for emulator..."; sleep 3
done
echo "Emulator ready"
```

### Resolve Dependencies

```bash
./gradlew dependencies --configuration debugRuntimeClasspath > /dev/null
echo "Dependencies resolved"
```

## Full Sequence

```bash
./gradlew --daemon help         # warm daemon
adb devices                     # verify device reachable
./gradlew dependencies --configuration debugRuntimeClasspath > /dev/null
echo "Dev environment ready for instrumented tests"
```

## Environment Requirements

| Requirement | Notes |
|-------------|-------|
| `adb` | Android Debug Bridge — part of Android SDK platform-tools |
| `emulator` | Android Emulator — part of Android SDK emulator package |
| AVD created | `avdmanager create avd -n test_avd -k "system-images;android-34;google_apis;x86_64"` |
