---
title: "Settings"
order: 3
summary: "App and profile settings for OrderMate, covering the main settings screen and the profile settings screen."
status: stable
implements:
  workflows:
    - instrumented-tests
  skills: []
  dependencies: []
  integrations:
    - clover-android-sdk
runWith:
  - "Open Settings to adjust app-level preferences."
  - "Open Profile Settings to manage the merchant/profile details."
tradeoffs:
  - "Settings is split into a general SettingsFragment and a dedicated ProfileSettingsFragment, keeping profile concerns separate from app preferences at the cost of two screens to navigate."
notes:
  - kind: note
    body: "Profile details are tied to the connected Clover merchant, so some settings reflect state from the Clover SDK rather than local-only preferences."
---

## What it does

Settings is where users manage OrderMate's app-level preferences and their profile
details. It is split into a general settings screen and a profile settings screen.

## How it works

The screens are `app/src/main/java/com/orderMate/fragment/SettingsFragment.kt` and
`ProfileSettingsFragment.kt`. They are covered by the instrumented UI test
`app/src/androidTest/java/com/orderMate/fragment/SettingsFlowTest.kt`.

## Configuration & running

Open Settings for app preferences, and Profile Settings to manage profile/merchant
details.
