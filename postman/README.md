# OrderMate Postman Testing Guide

## Overview

This guide explains how to test OrderMate's Firebase API endpoints using Postman.

## Setup

### 1. Import the Collection

1. Open Postman
2. Click **Import** button
3. Select `OrderMate_API.postman_collection.json` from this folder
4. The collection will be added to your workspace

### 2. Configure Environment Variables

The collection uses these variables (set in Collection Variables):

| Variable | Description | Example |
|----------|-------------|---------|
| `firebaseUrl` | Firebase Realtime Database URL | `https://ordermate-53077-default-rtdb.firebaseio.com` |
| `functionsUrl` | Cloud Functions URL | `https://us-central1-ordermate-53077.cloudfunctions.net` |
| `merchantId` | Clover Merchant ID to test with | `ABCD1234567890` |
| `employeeId` | Employee ID for profile tests | `EMP123456` |
| `widgetId` | Widget ID for widget tests | `widget_123` |
| `referralId` | Referral ID for referral tests | `ref_123` |

### 3. Firebase Authentication (if rules enabled)

If Firebase rules require authentication, add an `auth` query parameter to requests:
```
{{firebaseUrl}}/path.json?auth=YOUR_DATABASE_SECRET
```

## Testing Endpoints

### Referrals API

**Endpoint**: `merchants/{merchantId}/referrals`

#### Save Referral
- **Method**: `PUT`
- **URL**: `{{firebaseUrl}}/merchants/{{merchantId}}/referrals/{{referralId}}.json`
- **Payload**:
```json
{
  "id": "ref_test1234",
  "partnerName": "TestPartner",
  "submittedAt": 1714766133000,
  "submittedBy": "employee_123"
}
```
- **Expected Response**: `200 OK` with the saved data
- **Behavior**: Creates/updates a referral entry. In the app, only Owners can submit referrals.

#### Get Referrals
- **Method**: `GET`
- **URL**: `{{firebaseUrl}}/merchants/{{merchantId}}/referrals.json`
- **Expected Response**: 
  - `200 OK` with object containing referral(s)
  - `null` if no referrals exist

### Profiles API

**Endpoint**: `merchants/{merchantId}/profiles`

#### Save Employee Profile
- **Method**: `PUT`
- **URL**: `{{firebaseUrl}}/merchants/{{merchantId}}/profiles/{{employeeId}}.json`
- **Payload**:
```json
{
  "color": "#3C4B80",
  "avatar": "👨‍🍳"
}
```
- **Expected Response**: `200 OK` with the saved profile
- **Behavior**: Saves employee's theme color and avatar selection

#### Get Employee Profile
- **Method**: `GET`
- **URL**: `{{firebaseUrl}}/merchants/{{merchantId}}/profiles/{{employeeId}}.json`
- **Expected Response**: 
  - `200 OK` with profile object `{"color": "#...", "avatar": "..."}`
  - `null` if profile doesn't exist

### Widget Configuration API

**Endpoint**: `merchants/{merchantId}/widgets`

#### Get All Widgets
- **Method**: `GET`
- **URL**: `{{firebaseUrl}}/merchants/{{merchantId}}/widgets.json`
- **Expected Response**: Object with `itemLevel` and `orderLevel` arrays

#### Save Item Widget
- **Method**: `PUT`
- **URL**: `{{firebaseUrl}}/merchants/{{merchantId}}/widgets/itemLevel/{{index}}.json`
- **Payload**:
```json
{
  "id": "widget_category",
  "type": "SINGLE_SELECT",
  "label": "Category",
  "isEnabled": true,
  "showInFilter": true,
  "order": 0,
  "options": [
    {"id": "opt_1", "label": "Pickup", "value": "pickup"},
    {"id": "opt_2", "label": "Delivery", "value": "delivery"}
  ]
}
```

### Cloud Functions (Webhooks)

**Endpoint**: `{{functionsUrl}}/cloverWebhook`

#### Health Check
- **Method**: `GET`
- **Expected Response**: `200 OK` with text "OK"

#### Test App Installed Event
- **Method**: `POST`
- **Payload**:
```json
{
  "appId": "ORDERMATE_APP_ID",
  "merchants": {
    "TEST_MERCHANT_ID": [
      {
        "objectId": "A:ORDERMATE_APP_ID",
        "type": "CREATE",
        "ts": 1714766133000
      }
    ]
  }
}
```
- **Expected Response**: `200 OK`
- **Behavior**: Creates merchant entry in Firebase with install timestamp

## Common Issues

### 1. 401 Unauthorized
- Firebase rules may require authentication
- Add `?auth=YOUR_TOKEN` to the URL

### 2. 404 Not Found
- Check that `merchantId` is set correctly
- Verify the path is correct

### 3. CORS Errors (Browser)
- Use Postman desktop app instead of web version
- Or test from the Android app directly

## Verifying Data

After making requests, verify data in Firebase Console:
1. Go to https://console.firebase.google.com/
2. Select the OrderMate project
3. Navigate to Realtime Database
4. Browse to `merchants/{merchantId}/` to see your test data

## Cleanup

To clean up test data:
1. Use DELETE requests to remove test entries
2. Or manually delete in Firebase Console

---

*Last updated: May 2026 - #81 QA Documentation*
