# Env Setup Skill — OrderMate

Writes `local.properties` and sets required environment variables before
running or testing the OrderMate Android app. Run this before `dev-server`
or any command that needs the Android SDK.

## Required Secrets

Register these in OpenHands → Settings → Secrets:

| Secret | Description |
|--------|-------------|
| `ANDROID_SDK_ROOT` | Path to Android SDK (e.g., `/opt/android-sdk` or `~/Library/Android/sdk`) |
| `CLOVER_API_KEY` | Clover developer API key for integration tests |
| `CLOVER_MERCHANT_ID` | Test merchant ID for sandbox requests |

## Write local.properties

```bash
# Run from repo root
cat > local.properties << EOF
sdk.dir=${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}
clover.api.key=${CLOVER_API_KEY}
clover.merchant.id=${CLOVER_MERCHANT_ID}
EOF

echo "local.properties written"
cat local.properties
```

## Verify SDK

```bash
${ANDROID_SDK_ROOT}/tools/bin/sdkmanager --version 2>/dev/null \
  || echo "sdkmanager not found — check ANDROID_SDK_ROOT"
```

## CI Mode (no emulator)

For compile/lint checks without an emulator:

```bash
# Skip emulator-dependent tasks
export ANDROID_EMULATOR_SKIP=1
./gradlew assembleDebug -x connectedAndroidTest
```

## Environment Requirements

| Requirement | Notes |
|-------------|-------|
| Java 17+ | Required by Gradle 8+ and AGP 8+ |
| Android SDK 34 | Compile SDK version |
| Gradle | Uses wrapper — no separate install needed |
