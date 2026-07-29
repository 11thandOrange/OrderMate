---
title: "Calendar"
order: 2
summary: "A calendar screen for scheduling and previewing time-based orders, with custom date/time pickers and an event preview dialog."
status: stable
implements:
  workflows:
    - instrumented-tests
  skills: []
  dependencies: []
  integrations:
    - clover-android-sdk
runWith:
  - "Open the Calendar tab to browse scheduled items by date."
  - "Pick a date or time with the custom date picker and date-time picker dialog."
  - "Tap an entry to open the event preview dialog."
tradeoffs:
  - "The calendar uses bespoke picker dialogs (CustomDatePickerFragment, DateTimePickerDialog) rather than the platform pickers, which gives a consistent look at the cost of maintaining custom UI."
notes:
  - kind: note
    body: "Calendar UI is composed of several cooperating fragments and dialogs rather than a single view, so behavior is spread across CalendarFragment and its picker/preview helpers."
---

## What it does

Calendar is OrderMate's scheduling screen. It lets users browse and preview time-based
orders, choose dates and times with custom pickers, and preview an event before acting on
it.

## How it works

The screen is `app/src/main/java/com/orderMate/fragment/CalendarFragment.kt`, supported by
`CustomDatePickerFragment`, `DateTimePickerDialog`, and `EventPreviewDialog`. Instrumented
coverage lives under `app/src/androidTest/java/com/orderMate/fragment/`
(`CalendarFlowTest.kt`).

## Configuration & running

Open the Calendar tab, use the custom date and date-time pickers to select a slot, and tap
an entry to open the event preview dialog.
