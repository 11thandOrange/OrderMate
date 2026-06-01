# Dev Server Skill — {{REPO_NAME}}

Starts the {{REPO_NAME}} app in development mode. Run after `env-setup`.

## Prerequisites

`env-setup` skill has been run (env file exists with valid secrets).

## Startup Sequence

### Step 1 — Install Dependencies

```bash
# Install all dependencies for {{REPO_NAME}}
# (stack-specific: npm install, gradle dependencies, etc.)
```

### Step 2 — Start the App

```bash
# Start {{REPO_NAME}} in dev mode
# (stack-specific: shopify app dev, ./gradlew, npm run dev, etc.)
```

### Step 3 — Verify the App is Running

```bash
# Verify the expected port is listening
# or check logs for "ready" / "started" message
```

## Stopping the App

```bash
# Kill the dev server process
ps aux | grep "<process name>" | grep -v grep | awk '{print $2}' | xargs kill
```

## Ports

| Service | Port |
|---------|------|
| App | <PORT> |
