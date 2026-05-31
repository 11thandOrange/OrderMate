# CustomOrderApp


#1 to make the build upload on any store you need to sign he app with the V1 sign key

   
(appSigner path)  sign --ks (path of the keystore file)  --v1-signing-enabled=true --v2-signing-enabled=false --v3-signing-enabled=false
--v1-signer-name Cert (path of the apk to be signed)

generally the path of the appsigner is **Library/Android/sdk/build-tools/31.0.0/apksigner**

e.g.
/Users/manjotsingh/Library/Android/sdk/build-tools/31.0.0/apksigner  sign 
--ks /Users/manjotsingh/SHUBHAM/MY\ PROJECTS/CLOVER_ANDROID_GITHUB/Cert 
--v1-signing-enabled=true --v2-signing-enabled=false --v3-signing-enabled=false
--v1-signer-name Cert /Users/manjotsingh/SHUBHAM/MY\ PROJECTS/CLOVER_ANDROID_GITHUB/app/release/app-release.apk 



**#2** To get the data from the connectors you need to connect the app from the **preview in app market option >> connect the app** >> after this you
will get the data else you will not.



/Users/manjotsingh/Library/Android/sdk/build-tools/31.0.0/apksigner  sign
--ks /Users/manjotsingh/SHUBHAM/MY\ PROJECTS/CLOVER_ANDROID_GITHUB/Cert
--v1-signing-enabled=true --v2-signing-enabled=false --v3-signing-enabled=false
--v1-signer-name Cert /Users/manjotsingh/SHUBHAM/MY\ PROJECTS/CLOVER_ANDROID_GITHUB/app/release/app-release.apk

---

## AI Agent Automation

OrderMate uses [OpenHands](https://app.all-hands.dev) agents to automate the development
workflow. Agents live in `.agents/` and can be triggered manually or via the autonomous pipeline.

### Autonomous Dev Pipeline

Add the `ready-to-implement` label to any GitHub Issue to trigger the full pipeline:

```
Issue labelled "ready-to-implement"
        ↓
  ticket-planner     reads the issue, maps it to the codebase, writes an implementation plan
        ↓
android-implementer  creates a branch, writes Kotlin code, verifies build & tests pass
        ↓
      tester         fills any missing test coverage
        ↓
  build-release      confirms debug build compiles cleanly
        ↓
  ticket-manager     opens a PR linked to the issue
        ↓
   pr-reviewer       self-reviews the PR, posts inline comments, iterates
        ↓
   ci-monitor        waits for GitHub Actions to go green (auto-retries up to 3×)
        ↓
whatsapp-notifier    sends "PR #N is ready for your review" to your phone
```

The pipeline **never** merges to `main`, pushes release tags, or uploads to any store.
The WhatsApp message is the handoff — you review and merge.

### One-Time Setup

**1. Create the `ready-to-implement` label**
```bash
gh label create "ready-to-implement" --color "0075ca" \
  --description "Queued for autonomous implementation"
```

**2. Set up WhatsApp notifications**
- Send `I allow callmebot to send me messages` to **+34 644 76 60 71** on WhatsApp
- You'll receive a personal API key in reply
- Register two secrets in OpenHands → Settings → Secrets:

| Secret | Value |
|--------|-------|
| `WHATSAPP_PHONE` | Your number in international format, no `+` (e.g. `447911123456`) |
| `WHATSAPP_API_KEY` | The key you received from callmebot |

**3. Create the Android CI workflow**

See the template in `.agents/skills/ci-monitor.md` and save it as
`.github/workflows/android-ci.yml`.

**4. Register the automation in OpenHands**

Run the `curl` command in `.agents/automations/autonomous-dev-pipeline.md` with your
`OPENHANDS_API_KEY`.

### Available Agents

| Agent | Description |
|-------|-------------|
| `ordermate-agent` | Main orchestrator — delegates to all sub-agents |
| `ticket-planner` | Reads a GitHub Issue → produces a codebase-aware implementation plan |
| `android-implementer` | Executes the plan → writes Kotlin/Android code on a feature branch |
| `ticket-manager` | Creates, investigates, and updates GitHub Issues and PRs |
| `pr-reviewer` | Reviews PRs, posts inline comments, iterates |
| `tester` | Writes and runs unit, integration, and e2e tests |
| `build-release` | Builds APKs, bumps versions (release ops require human confirmation) |
| `code-auditor` | Scans the codebase for bugs and tech debt |
| `postman-manager` | Creates and runs Postman collections for API testing |
| `changelog-agent` | Generates changelogs from conventional commits |
| `api-spec-generator` | Extracts Retrofit endpoints → TypeScript definitions for the docs site |

### Available Skills

| Skill | Description |
|-------|-------------|
| `whatsapp-notifier` | Sends WhatsApp messages via callmebot or Twilio |
| `ci-monitor` | Polls GitHub Actions checks and surfaces failure logs |
| `docs-deploy` | Deploys the documentation site to GitHub Pages |
| `openapi-extractor` | Extracts API endpoints from Kotlin/Retrofit code |

### What Always Requires Human Approval

- Merging any PR into `main`
- Pushing release tags (`vX.Y.Z`)
- Creating a release build for the Play Store or Firebase 
