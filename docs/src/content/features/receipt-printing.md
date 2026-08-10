---
title: Receipt Printing
order: 2
summary: Schedules receipt printing a configurable number of days and minutes before an order's due date, using AlarmManager and the Clover Printer SDK.
status: stable
implements:
  workflows: []
  skills: []
  dependencies: []
  integrations: [clover-api]
runWith:
  - "Runs on-device via ReceiptPrintScheduler; alarms fire a BroadcastReceiver that prints through the Clover Printer SDK on Clover hardware."
tradeoffs:
  - "Uses AlarmManager (exact alarms) for reliable background execution on Clover devices; print times already in the past are skipped rather than fired immediately."
notes:
  - kind: note
    body: "Scheduling is driven by per-merchant Advanced Settings (receipt days + minutes before due date), resolved through SettingsManager."
---

## What it does
Receipt Printing automatically prints an order's receipt ahead of its due date so kitchen/prep staff get tickets on time. It schedules a print job `receiptDays` days and `receiptMinutes` minutes before the order's due date, then prints to the kitchen/order printer via the Clover Printer SDK.

## How it works
`ReceiptPrintScheduler` (`app/src/main/java/com/orderMate/services/ReceiptPrintScheduler.kt`) reads the receipt-lead settings from `SettingsManager`, computes the print time with `calculatePrintTime(dueDate, days, minutes)`, and — if that time is still in the future — registers an exact `AlarmManager` alarm. When the alarm fires, a `BroadcastReceiver` prints the order through the Clover Printer SDK (`PrinterConnector` / `StaticOrderPrintJob`) using the merchant's `CloverAccount`.

## Scheduling entry point
Print scheduling is coordinated by `ScheduledTaskManager.scheduleTasksForOrder()`, which is called after an order note is saved. It resolves the order's due date via `OrderDueDateResolver` (a 3-priority resolution using cached widgets) and, when receipt printing is enabled in Advanced Settings, hands off to `ReceiptPrintScheduler`. Time-calculation and printer-selection logic is covered by `ReceiptPrintSchedulerTest` (JUnit), including edge cases such as past due dates and zero lead values.
