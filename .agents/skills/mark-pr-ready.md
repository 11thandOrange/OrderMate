# Mark PR Ready Skill — OrderMate

Remove draft status from an OrderMate pull request, making it ready for review.

## Command

```bash
gh pr ready <PR_NUMBER> --repo 11thandOrange/OrderMate
```

## Verify

```bash
gh pr view <PR_NUMBER> --repo 11thandOrange/OrderMate --json isDraft -q '.isDraft'
# Must return: false
```

## Prerequisites

```bash
[ -n "$GITHUB_TOKEN" ] && echo "set" || echo "GITHUB_TOKEN missing"
```
