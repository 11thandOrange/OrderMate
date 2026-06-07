# Screenshot Smoke Tests — OrderMate

Run Android screenshot tests to verify UI renders correctly.
Screenshots are committed to the PR branch and linked in a PR comment.

## Prerequisites

```bash
# Verify Android SDK and emulator are available
which adb
emulator -list-avds
```

## Approach

1. Build instrumented test APK
2. Start emulator
3. Run screenshot tests
4. Save screenshots to `.smoke-results/`
5. Commit and post PR comment

## Commands

```bash
# Build instrumented tests
./gradlew assembleDebugAndroidTest

# Run screenshot tests (requires test setup)
./gradlew connectedAndroidTest

# Pull screenshots from device
adb pull /sdcard/screenshots/ .smoke-results/ 2>/dev/null || echo "No screenshots"

# List results
ls -la .smoke-results/
```

## Cleanup

```bash
# Remove screenshots before committing
rm -rf .smoke-results/

# Or add to .gitignore
echo ".smoke-results/" >> .gitignore
```