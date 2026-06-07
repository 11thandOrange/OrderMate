# Dev Server Skill — OrderMate

Starts the Android build/watch mode for the OrderMate app. Used by
`ordermate-implementer` for manual verification during development.

## Prerequisites

```bash
# Verify ANDROID_HOME is set
echo "ANDROID_HOME: $ANDROID_HOME"

# Verify Gradle wrapper exists
[ -f "./gradlew" ] || echo "⚠️  gradlew missing"

# Check Java version
java -version 2>&1 | head -1
```

## Start Dev Server (Watch Mode)

```bash
# Start Gradle watch mode (rebuilds on file changes)
./gradlew assembleDebug --watch
```

Or start emulator and install:
```bash
# Start Android emulator (requires emulator to be configured)
emulator -avd <avd_name> &

# Wait for emulator to boot
adb wait-for-device

# Install debug APK
./gradlew installDebug
```

## Quick Verification

```bash
# Build debug APK
./gradlew assembleDebug

# Check APK was created
ls -la app/build/outputs/apk/debug/
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| ANDROID_HOME not set | Set environment variable to Android SDK path |
| Gradle not found | Run `./gradlew` instead of `gradle` |
| Build fails | Run `./gradlew clean` first |