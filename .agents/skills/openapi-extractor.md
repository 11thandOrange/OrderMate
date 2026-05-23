# OpenAPI Extractor Skill

Extract API endpoint definitions from Kotlin/Retrofit code and generate OpenAPI-compatible
TypeScript definitions for the OrderMate documentation site.

## Quick Reference

### Find Retrofit Interfaces
```bash
find app/src -name "*.kt" -exec grep -l "@GET\|@POST\|@PUT\|@DELETE\|@PATCH" {} \;
```

### Extract All Endpoints
```bash
grep -n -A 5 "@GET\|@POST\|@PUT\|@DELETE\|@PATCH" app/src/main/java/com/orderMate/networkManager/*.kt
```

### Check for API Changes
```bash
git diff HEAD~1 --name-only | grep -E "\.kt$" | xargs grep -l "@GET\|@POST" 2>/dev/null
```

## Kotlin to TypeScript Mapping

| Kotlin | TypeScript |
|--------|------------|
| `String` | `string` |
| `Int`, `Long` | `number` |
| `Boolean` | `boolean` |
| `Double`, `Float` | `number` |
| `List<T>` | `T[]` |
| `Map<K, V>` | `Record<K, V>` |
| `Any` | `unknown` |
| `T?` (nullable) | `T \| null` |
| `Unit` | `void` |

## Retrofit Annotation Patterns

### Path Parameter
```kotlin
@GET("v3/merchants/{mId}/orders/{orderId}")
suspend fun getOrder(
    @Path("mId") merchantId: String,
    @Path("orderId") orderId: String
): Response<Order>
```

### Query Parameter
```kotlin
@GET("v3/merchants/{mId}/orders")
suspend fun listOrders(
    @Query("filter") filter: String?,
    @Query("limit") limit: Int?,
    @Query("offset") offset: Int?
): Response<OrdersResponse>
```

### Request Body
```kotlin
@POST("v3/merchants/{mId}/orders")
suspend fun createOrder(
    @Path("mId") merchantId: String,
    @Body request: CreateOrderRequest
): Response<Order>
```

### Headers
```kotlin
@Headers("Content-Type: application/json")
@PUT("v3/merchants/{mId}/orders/{orderId}")
suspend fun updateOrder(...)
```

## Generated TypeScript Schema

```typescript
// docs-site/frontend/src/data/endpoints.ts

import { Endpoint, Parameter } from '../types/api';

export const ordersEndpoints: Endpoint[] = [
  {
    id: 'orders-get',
    method: 'GET',
    path: '/v3/merchants/{mId}/orders/{orderId}',
    title: 'Get Order',
    description: 'Retrieves a single order by ID.',
    parameters: [
      {
        name: 'mId',
        type: 'string',
        required: true,
        description: 'Merchant ID',
        location: 'path',
      },
      {
        name: 'orderId',
        type: 'string',
        required: true,
        description: 'Order ID',
        location: 'path',
      },
    ],
    requestBody: null,
    responseExample: {
      id: 'ABCD1234',
      currency: 'USD',
      total: 2500,
      state: 'open',
      createdTime: 1699900000000,
    },
  },
];
```

## Clover API Endpoint Patterns

### Orders
- `GET /v3/merchants/{mId}/orders` - List orders
- `GET /v3/merchants/{mId}/orders/{orderId}` - Get order
- `POST /v3/merchants/{mId}/orders` - Create order
- `PUT /v3/merchants/{mId}/orders/{orderId}` - Update order
- `DELETE /v3/merchants/{mId}/orders/{orderId}` - Delete order

### Line Items
- `GET /v3/merchants/{mId}/orders/{orderId}/line_items` - List line items
- `POST /v3/merchants/{mId}/orders/{orderId}/line_items` - Add line item
- `DELETE /v3/merchants/{mId}/orders/{orderId}/line_items/{lineItemId}` - Remove line item

### Payments
- `GET /v3/merchants/{mId}/orders/{orderId}/payments` - List payments
- `POST /v3/merchants/{mId}/orders/{orderId}/payments` - Create payment

### Customers
- `GET /v3/merchants/{mId}/customers` - List customers
- `GET /v3/merchants/{mId}/customers/{customerId}` - Get customer
- `POST /v3/merchants/{mId}/customers` - Create customer

## Extraction Script

```bash
#!/bin/bash
# extract-endpoints.sh

OUTPUT_FILE="docs-site/frontend/src/data/extracted-endpoints.json"

echo "Extracting API endpoints from Kotlin source..."

# Find all files with Retrofit annotations
FILES=$(find app/src -name "*.kt" -exec grep -l "@GET\|@POST\|@PUT\|@DELETE\|@PATCH" {} \;)

echo "Found files with API definitions:"
echo "$FILES"

# Extract endpoint info
for file in $FILES; do
    echo "Processing: $file"
    grep -B 1 -A 10 "@GET\|@POST\|@PUT\|@DELETE\|@PATCH" "$file"
done

echo "Extraction complete. Review output and update endpoints.ts"
```

## Response Example Templates

### List Response
```json
{
  "elements": [...],
  "href": "string"
}
```

### Single Object Response
```json
{
  "id": "string",
  "createdTime": 0,
  "modifiedTime": 0
}
```

### Error Response
```json
{
  "message": "string",
  "details": "string"
}
```

## Validation Checklist

After extraction, verify:

- [ ] All endpoints have unique IDs
- [ ] Path parameters match function parameters
- [ ] Required/optional correctly identified
- [ ] Response examples are realistic
- [ ] Descriptions are clear and accurate
