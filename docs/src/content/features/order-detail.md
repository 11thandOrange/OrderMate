---
title: "Order Detail"
order: 1
summary: "The order-detail and checkout screen: view an order pulled from the Clover SDK, refresh it, and share it with the customer by email or SMS."
status: stable
implements:
  workflows:
    - instrumented-tests
  skills: []
  dependencies: []
  integrations:
    - clover-android-sdk
    - messagebird-conversations
runWith:
  - "Open an order from the order list; OrderDetailFragment receives the Order through the navigation Bundle."
  - "Pull to refresh to re-fetch the order via AppsConnector/getOrderConnector()."
  - "Share the order with the customer over email or SMS through the Conversations API."
tradeoffs:
  - "The Order is passed in as a Clover SDK type via a Bundle rather than re-fetched by id on open, so the screen depends on the caller handing it a valid Order and refreshes on demand."
  - "Customer messaging goes through the external Conversations API, which is the only network dependency on this screen; everything else is in-process Clover data."
notes:
  - kind: note
    body: "Most of the order data here comes from the Clover Android SDK in-process (AIDL), not a REST call. The only REST surface on this screen is the message-share action."
---

## What it does

Order Detail is OrderMate's order-detail and checkout screen. It shows a single order,
lets the user refresh it, and shares it with the customer by email or SMS.

## How it works

The screen lives under `app/src/main/java/com/orderMate/fragment/orderDetail/`.
`OrderDetailFragment` reads its order from a `Bundle`-passed `Order` (a Clover Android
SDK type) and refreshes through `AppsConnector` / `getOrderConnector()`. The email/SMS
share action posts to the MessageBird Conversations API
(`POST /workspaces/{merchantId}/channels/{channelId}/messages`).

It is covered by the instrumented UI test
`app/src/androidTest/java/com/orderMate/fragment/orderDetail/OrderDetailFlowTest.kt`.

## Configuration & running

Open an order from the list to land on this screen; the order arrives via the navigation
`Bundle`. Refresh to re-fetch from the Clover connector, and use the share action to send
the order to the customer over the Conversations channel.
