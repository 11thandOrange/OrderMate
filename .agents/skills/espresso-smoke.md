# Espresso Smoke Skill — OrderMate

Run targeted Espresso UI tests for acceptance criteria verification.
Provides commands to execute specific test classes and capture screenshots.

## Quick Commands

### Run All Smoke Tests

```bash
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.orderMate.smoke
```

### Run Specific AC Test

```bash
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.orderMate.smoke.<TestClass>
```

### Capture Screenshots During Test

```kotlin
// In your test method:
fun takeScreenshot(name: String) {
    InstrumentationRegistry.getInstrumentation().uiAutomation
        .takeScreenshot()
        ?.let { bitmap ->
            val file = File("/sdcard/screenshots/${name}.png")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
}
```

### Pull Screenshots from Device

```bash
mkdir -p .smoke-results
adb pull /sdcard/screenshots/ .smoke-results/
ls -la .smoke-results/
```

### Clean Screenshots from Device

```bash
adb shell rm -rf /sdcard/screenshots/
```

## Test Location

Smoke tests live in:
`app/src/androidTest/java/com/orderMate/smoke/`

## Prerequisites

```bash
# Emulator or device must be connected
adb devices | grep -v "List of" | grep "device$" | wc -l
# Must be >= 1
```

## Environment Requirements

| Requirement | Notes |
|-------------|-------|
| Connected device/emulator | `adb devices` must show a device |
| WRITE_EXTERNAL_STORAGE | Needed for screenshots on Android < 10 |
| Android SDK 34 | Target API level |
