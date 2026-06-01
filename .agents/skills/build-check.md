# Build Check Skill — OrderMate

Verify the OrderMate Android app compiles cleanly and passes lint.
Run after all tests pass, before opening a PR. Does not build a release APK
or run instrumented tests (that's `tester`'s job).

## Build Commands

```bash
# Compile check (debug variant, no emulator needed)
./gradlew assembleDebug

# Lint check
./gradlew lint

# Both in sequence
./gradlew assembleDebug lint
```

## Step-by-Step

### Step 1 — Verify Gradle Wrapper

```bash
./gradlew --version
```

### Step 2 — Compile

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

### Step 3 — Lint

```bash
./gradlew lint 2>&1 | grep -E "^(ERROR|WARNING|BUILD)" | head -30
```

Check lint output at `app/build/reports/lint-results-debug.html`.

### Step 4 — Report

```markdown
## Build Check Results

- **Compile (assembleDebug):** ✅ SUCCESS / ❌ FAILED
- **Lint:** ✅ 0 errors / ⚠️ N warnings / ❌ N errors
- **APK size:** X MB (debug)
```

## Troubleshooting

**`SDK location not found`:** Run `env-setup` skill first to write `local.properties`
**`Duplicate class`:** Check for dependency version conflicts in `build.gradle.kts`
**Lint errors blocking build:** Check `app/build/reports/lint-results-debug.html`

## Environment Requirements

| Requirement | Version |
|-------------|---------|
| Java | 17+ |
| Android Gradle Plugin | 8+ |
| Compile SDK | 34 |
