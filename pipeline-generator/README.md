# PR Finisher Pipeline Generator

Generates the complete `.agents/` ecosystem for a new repo and wires the
**ticket → PR → WhatsApp** pipeline in the same standardized shape used by
OrderMate and BusyBuddy_v2.

## What it produces for a new repo

| Type | Count | Files |
|------|-------|-------|
| Agents | 10–15 | orchestrator, ticket-planner, implementer, tester, pr-reviewer, smoke-tester, build-release, code-auditor, docs-agent, site-deployer, api-spec-generator, changelog-agent, docs-writer, postman-manager, secondary-implementer |
| Skills | 4–5 | env-setup, build-check, dev-server, smoke-runner, openapi-extractor |
| Automations | 2–3 | autonomous-dev-pipeline, complex-logic-pipeline, docs-agent-automation |
| CI Workflows | 2–3 | android-ci.yml or node-ci.yml, deploy-docs.yml |
| GitHub Labels | 2 | `ready-to-implement`, `complex-logic` |
| OpenHands Automations | 2–3 | Registered via API |

## Pipeline manifest

Every skill appends a structured JSON entry to:
```
/tmp/pipeline-manifest-${REPO_SLUG}.json
```

The manifest records exactly which files were generated at each step.
Both confirmation gates read from the manifest — never from terminal history.

## Installation

These files are **user-level** agents. Install them in your OpenHands
user-level `.agents/` directory (separate from any repo):

```bash
cp -r pipeline-generator/agents/*   ~/.agents/agents/
cp -r pipeline-generator/skills/*   ~/.agents/skills/
cp -r pipeline-generator/templates  ~/.agents/templates/
```

## Usage

Invoke the `pipeline-generator` agent in OpenHands:

```
Set up the pipeline for https://github.com/org/new-repo
```

The generator will ask 12 questions about the new repo, then:
1. Render all agent, skill, workflow, and automation files
2. **Confirm** before pushing as a draft PR
3. Wait for the PR to be merged
4. **Confirm** before registering 2–3 OpenHands automations
5. Create GitHub labels
6. Verify everything end-to-end
7. Send a WhatsApp notification when live

## File structure

```
pipeline-generator/
├── agents/
│   └── pipeline-generator.md          ← the orchestrator agent
├── skills/
│   ├── repo-intake.md                 ← collects repo facts → /tmp/intake.json
│   ├── agent-template-renderer.md     ← renders 10-15 agent .md files
│   ├── skill-template-renderer.md     ← renders 4-5 skill .md files
│   ├── workflow-renderer.md           ← renders 2-3 CI workflow .yml files
│   ├── smoke-scaffold.md              ← creates smoke test infrastructure
│   ├── docs-scaffold.md               ← creates docs/ site skeleton
│   ├── automation-builder.md          ← renders automations + registration script
│   ├── gradle-config-patcher.md       ← patches build.gradle.kts (Android only)
│   ├── label-creator.md               ← creates GitHub labels
│   └── pipeline-verifier.md           ← verifies everything + WhatsApp ping
└── templates/
    ├── agents/    (15 .tpl.md files)
    ├── skills/    (5 .tpl.md files)
    ├── automations/ (3 .tpl.md files)
    └── workflows/ (3 .tpl.yml files)
```

## Supported stacks

| Stack | CI workflow | Smoke type | Implementer agents |
|-------|------------|------------|-------------------|
| `android-kotlin` | `android-ci.yml` | Espresso | `implementer` + `docs-frontend-implementer` |
| `node-shopify` | `node-ci.yml` | Playwright | `busybuddy-implementer` + `shopify-extension-implementer` |
| Other | None generated | As specified | Generic |

## User-level skills referenced (not generated — must already exist)

| Skill | Used in |
|-------|---------|
| `ticket-manager` | every `autonomous-dev-pipeline` |
| `ci-monitor` | every `autonomous-dev-pipeline` |
| `mark-pr-ready` | every pipeline tail |
| `whatsapp-notifier` | every pipeline tail |
| `docs-deploy` | `site-deployer` |
| `approach-planner/implementer/reviewer` | `complex-logic-pipeline` |
| `submit-winning-approach` | `complex-logic-pipeline` |
