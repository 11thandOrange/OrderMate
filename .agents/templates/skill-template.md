# Skill Title — OrderMate

One sentence: what this skill does for OrderMate and when to use it.

Stack context: Kotlin/Android (`app/`), Gradle build system,
`docs/frontend/` React/TypeScript site.

## Prerequisites

```bash
[ -n "$GITHUB_TOKEN" ] && echo "set" || echo "missing"
```

## Quick Commands

```bash
# The most common invocation
./gradlew <task>
```

## Step-by-Step

### Step 1 — [Action]

```bash
# commands
```

### Step N — Verify

```bash
./gradlew <check-task> 2>&1 | grep "BUILD"
```

## Environment Requirements

| Requirement | Version |
|-------------|---------|
| Java | 17+ |
| Android SDK | 34 |
| Gradle | via wrapper |

## Troubleshooting

**Problem:** `SDK location not found`
**Fix:** Run `env-setup` skill first

**Problem:** Gradle daemon not responding
**Fix:** `./gradlew --stop` then retry
