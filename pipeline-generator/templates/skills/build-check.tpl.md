# Build Check Skill — {{REPO_NAME}}

Verify {{REPO_NAME}} builds cleanly. Run after all tests pass, before opening a PR.

## Build Commands

```bash
{{BUILD_CMD}}
```

## Step-by-Step

### Step 1 — Install Dependencies (if needed)

```bash
# Run any install steps required by {{STACK}}
```

### Step 2 — Run Build

```bash
{{BUILD_CMD}} 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` or `built in Xs`

### Step 3 — Report

```markdown
## Build Check Results — {{REPO_NAME}}

- **Command:** `{{BUILD_CMD}}`
- **Status:** ✅ SUCCESS / ❌ FAILED
- **Duration:** Xs
```

## Common Failures

| Error | Cause | Fix |
|-------|-------|-----|
| Missing env file | env-setup not run | Run `env-setup` skill first |
| Compilation error | Code error | Fix the error in the source file |
| Test failure blocking build | Tests run as part of build | Fix failing tests |

## Never Do

- Run release/production builds
- Push to `main`
