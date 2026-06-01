# Smoke Runner Skill — {{REPO_NAME}}

Run {{SMOKE_TYPE}} smoke tests, capture AC screenshots, and report results.

## Install (once per sandbox)

```bash
# {{SMOKE_TYPE}} install command
```

## Run All Smoke Tests

```bash
# {{SMOKE_TYPE}} run command
```

## Run Specific Test

```bash
# {{SMOKE_TYPE}} run specific test file
```

## Screenshot Capture and Pull

```bash
mkdir -p .smoke-results/
# Pull screenshots from device/browser to .smoke-results/
ls -la .smoke-results/
```

## Success Indicators

All tests exit 0. `.smoke-results/` contains one PNG per AC checkpoint.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| No device connected | Emulator not running | Start emulator / check prerequisites |
| Test timeout | Slow startup | Increase timeout in test config |
| Missing screenshots | Screenshot path wrong | Check screenshot directory path |
