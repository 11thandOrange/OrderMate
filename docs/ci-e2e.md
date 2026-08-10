# CI: End-to-End (E2E) & Coverage

OrderMate consumes the shared **agent-ops reusable E2E pipeline** via a thin
caller workflow. OrderMate does not own any emulator/runner plumbing itself —
it only passes inputs.

## Where things live

| Piece | Path | Owner |
|-------|------|-------|
| Caller workflow | `.github/workflows/e2e-pipeline.yml` | OrderMate (this repo) |
| Reusable pipeline | `11thandOrange/agent-ops/.github/workflows/e2e-pipeline-reusable.yml@main` | agent-ops (shared) |
| Critical-flow manifest (starter) | `app/e2e-coverage.yaml` | OrderMate |
| JaCoCo coverage task | `app/build.gradle.kts` (`jacocoTestReport`) | OrderMate |

## What is shared vs. Android-specific

**Shared (owned by the agent-ops reusable workflow):** KVM setup, AVD creation
and caching, Android SDK install, emulator boot + input-ready wait, emulator
teardown, test-artifact upload, and the PR status comment. We get all of this
for free by calling the reusable workflow — we do not reimplement it here.

**Android-specific (passed as inputs from our caller):**

- `working_directory: app` — the Android app module (root module is `:app`).
- `test_command: "./gradlew connectedDebugAndroidTest"` — the standard Gradle
  task that runs instrumented (Espresso) tests on a connected device/emulator.
- `needs_emulator: true` — instrumented tests need a running Android emulator,
  so the reusable spins one up.
- `emulator_api_level: 30` — the AVD API level the emulator boots.
- `record_video` — optional screen recording, off by default; can be toggled
  via `workflow_dispatch`.
- `coverage_manifest_path` — currently **empty** (see below).

## Triggering

The pipeline runs automatically on pull requests targeting `main`, and can be
run manually via **Actions → e2e-pipeline → Run workflow** (`workflow_dispatch`),
where you may enable `record_video`.

## Critical-flow coverage manifest

`app/e2e-coverage.yaml` is a **starter** manifest listing OrderMate's key user
flows (order list browse/filter, order detail + notes, calendar scheduling,
customer notifications, settings/profile). It is **not yet wired in**:
`coverage_manifest_path` in the caller is intentionally empty because no real
instrumented tests exist yet — only the default `ExampleInstrumentedTest`
boilerplate. Wiring the manifest before the flows are covered by tests would
fail the coverage gate.

### How to add instrumented tests and enable the manifest

1. Add Espresso instrumented tests under
   `app/src/androidTest/java/...` that exercise each flow named in
   `app/e2e-coverage.yaml`.
2. Confirm they pass locally: `./gradlew connectedDebugAndroidTest`.
3. Enable the manifest by pointing the caller at it — edit
   `.github/workflows/e2e-pipeline.yml`:

   ```yaml
   coverage_manifest_path: app/e2e-coverage.yaml
   ```
4. Open a PR; the reusable pipeline's coverage gate will then verify each
   listed flow has a corresponding passing test.

## Unit-test coverage (JaCoCo)

To support the `ordermate-dev` coverage gate (`coverage_type=jacoco`),
`app/build.gradle.kts` applies the `jacoco` plugin and defines a
`jacocoTestReport` task. It:

- depends on `testDebugUnitTest` (runs the debug unit tests to produce
  execution data), and
- emits an XML report at
  `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`.

Run it locally with:

```bash
./gradlew :app:jacocoTestReport
```

The addition is purely additive — it registers a new task and sets the JaCoCo
tool version; it does not change any existing app, source, or build logic.
