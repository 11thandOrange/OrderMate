# Env Setup Skill — {{REPO_NAME}}

Writes the environment file from OpenHands secrets before running or testing
the {{REPO_NAME}} app. Run this before `dev-server` or any command that needs
live credentials.

## Required Secrets

Register these in OpenHands → Settings → Secrets:

{{SECRETS_NEEDED}}

## Write Environment File

```bash
# Run from repo root
# Replace with the correct env file path and variable names for {{REPO_NAME}}
cat > <ENV_FILE_PATH> << EOF
<SECRET_1>=${<SECRET_1>}
<SECRET_2>=${<SECRET_2>}
EOF

echo "Environment file written"
cat <ENV_FILE_PATH> | sed 's/=.*/=<hidden>/'
```

## Verify Connection

```bash
# Run a quick sanity check that the environment is correctly set
# e.g. for a database: attempt a connection
# e.g. for an SDK: call a status endpoint
```

## Cleanup

The env file is listed in `.gitignore`. Never commit it.

```bash
git check-ignore -v <ENV_FILE_PATH>
```
