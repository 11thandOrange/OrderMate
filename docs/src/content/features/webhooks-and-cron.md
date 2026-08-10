---
title: Webhooks & Cron
order: 3
summary: Firebase Cloud Functions receive Clover lifecycle webhooks and (planned) run scheduled report jobs; on-device schedulers drive per-order notification and print tasks.
status: beta
implements:
  workflows: []
  skills: []
  dependencies: []
  integrations: [firebase, clover-api, fcm]
runWith:
  - "The Clover webhook receiver runs as a Firebase Cloud Function (functions/src/webhooks/cloverWebhook.ts); on-device task scheduling runs in the Android app via ScheduledTaskManager."
tradeoffs:
  - "Merchant enrichment (name/email/storeName) is left blank on install: OrderMate is a native Android-only Clover app with no server-side REST client, so only the on-device app can obtain a per-merchant Clover token to fill those fields."
notes:
  - kind: warning
    body: "Every non-verification webhook POST must carry a valid X-Clover-Auth header matching CLOVER_AUTH_CODE, or it is rejected with 401. The one-time verification handshake is intentionally not auth-gated."
  - kind: note
    body: "Event writes are idempotent: buildEventId() derives a deterministic, Firebase-key-safe id so a Clover retry updates the same event record instead of creating a duplicate."
---

## What it does
This feature covers OrderMate's server-side and scheduled automation. A Firebase Cloud Function ingests Clover app-lifecycle webhooks (install, uninstall, subscription change) and records them in the Realtime Database; the same backend plan (see `WEBHOOK_CRON_IMPLEMENTATION.md`) covers a scheduled weekly-report cron job. On the device, per-order notification and receipt-print tasks are scheduled around each order's due date.

## Clover webhook handler
`cloverWebhook` (`functions/src/webhooks/cloverWebhook.ts`) is an `functions.https.onRequest` endpoint. `GET` is a health check; non-`POST` returns 405. A `POST` carrying `verificationCode` completes Clover's one-time dashboard handshake (not auth-gated, since it runs before `CLOVER_AUTH_CODE` is configured). Every other `POST` must pass `isAuthorizedEvent()` — an `x-clover-auth` header equal to `CLOVER_AUTH_CODE` — or it is rejected with 401. Authorized events in Clover's `{ appId, merchants }` format are processed per-merchant and per-update: app events (`A`) map `CREATE`/`DELETE`/`UPDATE` to install / uninstall / subscription-change handlers, which write `merchantInfo`, `subscription`, and deterministically-keyed `events` records. Failures are isolated per update; if any update fails the function returns 500 so Clover re-delivers, and idempotent writes make the retry safe. A legacy `{ merchantId, type }` payload is also supported for manual testing.

## Cron & on-device scheduling
The weekly-report cron job (Issue #94) is specified in `WEBHOOK_CRON_IMPLEMENTATION.md` as a scheduled Cloud Function that aggregates per-merchant activity for reporting. On-device, `ScheduledTaskManager.scheduleTasksForOrder()` schedules email/SMS notifications (via `NotificationScheduler`) and receipt prints (via `ReceiptPrintScheduler`) using each order's resolved due date, gated by the merchant's Advanced Settings.
