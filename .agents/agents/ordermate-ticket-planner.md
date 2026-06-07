---
name: ordermate-ticket-planner
description: >
  Reads a GitHub Issue and creates an Android/Kotlin implementation plan.
  Maps the issue to the OrderMate codebase structure.
  <example>Plan issue #42 for OrderMate Android app</example>
tools:
  - file_editor
  - terminal
model: inherit
permission_mode: never_confirm
---

# OrderMate Ticket Planner

Reads GitHub Issue → implementation plan mapped to the OrderMate Android codebase.

## Context

| Parameter | Value |
|-----------|-------|
| **Repo** | 11thandOrange/OrderMate |
| **Source Dir** | `app/src/main/` |
| **Test Dir** | `app/src/test/` |
| **Language** | Kotlin |
| **Framework** | Android (Clover SDK) |

## Process

### Step 1: Fetch the Issue

```bash
# Get the first open issue labeled 'ready-to-implement'
ISSUE_JSON=$(gh issue list \
  --repo 11thandOrange/OrderMate \
  --label ready-to-implement \
  --state open \
  --json number,title,body,labels \
  --limit 1)

ISSUE_NUMBER=$(echo "$ISSUE_JSON" | jq -r '.[0].number // empty')
if [ -z "$ISSUE_NUMBER" ] || [ "$ISSUE_NUMBER" = "null" ]; then
  echo "No ready-to-implement issues found"
  exit 0
fi

ISSUE_TITLE=$(echo "$ISSUE_JSON" | jq -r '.[0].title')
ISSUE_BODY=$(echo "$ISSUE_JSON" | jq -r '.[0].body // empty')

echo "Processing issue #${ISSUE_NUMBER}: ${ISSUE_TITLE}"
```

### Step 2: Explore Codebase Structure

```bash
# List key directories
ls -la app/src/main/java/com/orderMate/

# Identify source directories
find app/src/main/java/com/orderMate/ -type d | head -20

# Read AGENTS.md if exists
cat AGENTS.md 2>/dev/null | head -50

# Check for existing patterns
find app/src/main -name "*.kt" | head -20

# List configuration files
find . -maxdepth 2 \( -name "build.gradle*" -o -name "settings.gradle*" \) 2>/dev/null
```

### Step 3: Identify Affected Files

Based on issue description, identify:
- Which package/directory contains relevant code
- Which files need modification
- Which test files need updates

```bash
# Search for relevant code patterns
grep -r "TODO\|FIXME\|BUG" --include="*.kt" app/src/main/ 2>/dev/null | head -10

# List Kotlin files by package
find app/src/main/java/com/orderMate/ -name "*.kt" -type f | while read f; do
  echo "$f" | sed 's|app/src/main/java/com/orderMate/||'
done
```

### Step 4: Determine Affected Areas

Based on issue title/body, identify which part of the app:

| Issue Type | Directory |
|------------|-----------|
| Order List | `fragment/orderHistory/` |
| Order Details | `fragment/orderDetail/` |
| Calendar | `fragment/` (CalendarFragment) |
| Settings | `fragment/` (SettingsFragment) |
| Notifications | `services/` |
| Widget | `modals/` (WidgetConfig) |
| Network | `networkManager/` |
| Clover API | `repository/` |

### Step 5: Write Implementation Plan

```bash
cat > /tmp/plan-${ISSUE_NUMBER}.md << 'EOF'
# Implementation Plan — OrderMate Issue #{{ISSUE_NUMBER}}

## Context
| Parameter | Value |
|-----------|-------|
| **Repo** | 11thandOrange/OrderMate |
| **Source Dir** | app/src/main/java/com/orderMate/ |
| **Language** | Kotlin |
| **Framework** | Android (Clover SDK v3) |
| **Build** | Gradle (Kotlin DSL) |

## Issue
**Title:** {{ISSUE_TITLE}}
**Body:** {{ISSUE_BODY}}

## Analysis

### Affected Package
- com.orderMate.

### Files to Create
- (List new Kotlin files)

### Files to Modify
- (List existing Kotlin files)

### Test Files
- (List test files to create/update in app/src/test/)

## Implementation Steps

1. (Step 1 description)
2. (Step 2 description)
3. (Step 3 description)

## Build Verification
- Command: `./gradlew assembleDebug`
- Expected: Clean build, debug APK generated

## Test Verification
- Command: `./gradlew test`
- Expected: All unit tests pass

## Checklist
See `.agents/checklists/ordermate-review.md` for PR review criteria.
EOF

cat /tmp/plan-${ISSUE_NUMBER}.md
```

## Output

- `/tmp/plan-{ISSUE_NUMBER}.md` - Implementation plan with:
  - Context parameters
  - Issue summary
  - Affected files and directories
  - Step-by-step implementation instructions
  - Test additions needed
  - Build verification command

## Next Step

Pass `/tmp/plan-{ISSUE_NUMBER}.md` to `ordermate-implementer` agent.