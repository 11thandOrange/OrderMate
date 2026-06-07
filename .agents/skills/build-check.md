# Build Check Skill — OrderMate

Verify the Android/Kotlin codebase builds correctly for production.

## Build Commands

The Gradle build defines the canonical build:

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease

# Type check Kotlin
./gradlew compileDebugKotlin
./gradlew compileReleaseKotlin
```

## Prerequisites

```bash
# Verify we're in OrderMate repo
gh repo view --json nameWithOwner -q '.nameWithOwner'
# Should output: 11thandOrange/OrderMate

# Check ANDROID_HOME is set
echo "ANDROID_HOME: $ANDROID_HOME"

# Check Java version
java -version 2>&1 | head -1
```

## Process

### Step 1: Clean Previous Builds

```bash
# Clean build directories
./gradlew clean

# Remove any stale artifacts
rm -rf app/build/ .gradle/ 2>/dev/null

echo "Cleaned previous builds"
```

### Step 2: Run Kotlin Compilation

```bash
# Type check Kotlin (faster than full build)
./gradlew compileDebugKotlin 2>&1 | tail -50

# Check for compilation errors
if [ $? -ne 0 ]; then
  echo "Kotlin compilation failed"
  exit 1
fi

echo "Kotlin compilation successful"
```

### Step 3: Run Unit Tests

```bash
# Run all unit tests
./gradlew test 2>&1 | tail -50

# Check test results
if [ $? -ne 0 ]; then
  echo "Tests failed"
  exit 1
fi

echo "All unit tests passed"
```

### Step 4: Build Debug APK

```bash
# Build debug APK
./gradlew assembleDebug 2>&1 | tail -30

# Check if APK was created
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
  echo "Debug APK built successfully"
  ls -lh app/build/outputs/apk/debug/app-debug.apk
else
  echo "Debug APK not found"
  exit 1
fi
```

### Step 5: Verify APK Contents

```bash
# Verify APK has expected contents
unzip -l app/build/outputs/apk/debug/app-debug.apk | head -20

# Check for required files
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -E "classes|resources|AndroidManifest"
```

## Verification Checklist

- [ ] Kotlin compilation succeeds (no errors)
- [ ] All unit tests pass
- [ ] Debug APK builds successfully
- [ ] APK contains expected components

## Common Issues

| Issue | Solution |
|-------|----------|
| `Unresolved reference` | Check imports, verify dependencies |
| `Cannot find symbol` | Run `./gradlew clean` then rebuild |
| `Resource not found` | Check `res/` directory structure |
| `Test failures` | Fix tests before proceeding |

## Output

- Debug APK at `app/build/outputs/apk/debug/app-debug.apk`
- All tests passing
- Kotlin compilation clean

## Next Step

Pass to `ticket-manager` agent to create PR.