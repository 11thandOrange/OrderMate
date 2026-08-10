---
title: Order Management
order: 1
summary: View, filter, and manage Clover POS orders on-device, with full paginated order history backed by the Clover REST v3 Orders API.
status: stable
implements:
  workflows: []
  skills: []
  dependencies: []
  integrations: [clover-api, firebase]
runWith:
  - "Runs on-device as part of the OrderMate Android app; order data comes from the Clover SDK v3 OrderConnector and the Clover REST v3 Orders API."
tradeoffs:
  - "The on-device Clover OrderConnector only serves a retention-windowed local cache, so older history is fetched separately from the Clover REST Orders API, which paginates independently of the device cache."
notes:
  - kind: note
    body: "CloverOrdersApi returns a raw ResponseBody rather than a deserialized model, because com.clover.sdk.v3.order.Order is JSONObject-backed (not Gson-reflectable); CloverRepository.loadOlderOrders parses the JSON and constructs Order instances the way the SDK itself does."
---

## What it does
Order Management is OrderMate's core surface: it lists, filters, and manages a merchant's Clover POS orders. Orders visible on the device come from the Clover SDK v3 `OrderConnector` via `MyApp.getAllOrders()`, which serves a retention-windowed local cache. To show history beyond that window, the app supplements the cache with Clover's cloud Orders API.

## How it works
`CloverOrdersApi` (a Retrofit interface under `app/src/main/java/com/orderMate/networkManager`) exposes `GET v3/merchants/{merchantId}/orders` with `limit`/`offset` query params and an `Authorization` header, enabling pagination independent of the local device cache. Because the REST base URL and bearer token both come from `com.clover.sdk.util.CloverAuth.authenticate()` per call (they can differ by merchant environment and the token can be refreshed), `CloverOrdersApiClient.create(baseUrl)` builds a fresh client per call rather than caching a singleton. Responses are returned as a raw `ResponseBody`; `CloverRepository.loadOlderOrders` parses the JSON and constructs `Order` objects the same way the SDK does.

## Filtering & UI
Order filtering, categorization, and the redesigned order list/detail views are covered by an extensive JVM/JUnit unit-test suite under `app/src/test/**/*.kt` (filter utils, view models, adapters, and order-detail dialogs), so filtering and display logic is exercised without a device.
