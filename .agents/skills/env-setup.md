# Env Setup Skill — OrderMate

Sets up the Android/Kotlin development environment for OrderMate.

## Prerequisites

```bash
# Verify we're in OrderMate repo
gh repo view --json nameWithOwner -q '.nameWithOwner'
# Should output: 11thandOrange/OrderMate

# Check Java version (Android requires JDK 11+)
java -version 2>&1 | head -1

# Check Gradle
./gradlew --version 2>&1 | head -5
```

## Required Secrets

Register these in OpenHands → Settings → Secrets:

| Secret | Description |
|--------|-------------|
| `FIREBASE_CONFIG` | Firebase configuration JSON |
| `CLOVER_APP_ID` | Clover SDK App ID |
| `CLOVER_APP_SECRET` | Clover SDK App Secret |

## Step 1: Verify Project Structure

```bash
# Check key files exist
ls -la build.gradle.kts settings.gradle.kts gradle.properties

# Verify source directories
ls -la app/src/main/java/com/orderMate/
ls -la app/src/test/java/com/orderMate/
```

## Step 2: Configure Firebase (if needed)

```bash
# Check if google-services.json exists
if [ ! -f "app/google-services.json" ]; then
  echo "google-services.json not found"
  echo "If Firebase is needed, add it to app/"
fi

# Or use mock config for testing
if [ -f "app/google-services.json" ]; then
  echo "Firebase config found"
fi
```

## Step 3: Verify Dependencies

```bash
# Check Gradle wrapper
./gradlew --version

# Resolve dependencies
./gradlew dependencies --configuration debugRuntimeClasspath 2>&1 | tail -20
```

## Step 4: Create Local Properties (if needed)

```bash
# Create local.properties with SDK path
if [ ! -f "local.properties" ]; then
  echo "sdk.dir=$ANDROID_HOME" > local.properties
  echo "Created local.properties"
fi

cat local.properties
```

## Step 5: Clean and Build

```bash
# Clean previous builds
./gradlew clean

# Verify build works
./gradlew assembleDebug --dry-run 2>&1 | tail -10
```

## Verify Setup

```bash
# Run a simple check
./gradlew tasks --group=build 2>&1 | grep -E "assemble|build|compile" | head -10

# Should show:
# - assembleDebug
# - assembleRelease
# - compileDebugKotlin
# - compileReleaseKotlin
```

## Common Issues

| Issue | Solution |
|-------|----------|
| `ANDROID_HOME not set` | Set ANDROID_HOME environment variable |
| `Gradle sync failed` | Run `./gradlew clean`, then `./gradlew --refresh-dependencies` |
| `Kotlin version mismatch` | Check `gradle.properties` for `kotlinVersion` |
| `Firebase config missing` | Add `google-services.json` to `app/` directory |

## Never Do

- Do NOT commit `google-services.json` with real keys
- Do NOT commit `local.properties` with real SDK paths
- Do NOT commit `.gradle` cache directories