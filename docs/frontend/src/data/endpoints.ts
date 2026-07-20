import type { Endpoint } from '../types/api';

export const endpoints: Endpoint[] = [
  // Orders
  {
    id: 'list-orders',
    method: 'GET',
    path: '/v3/merchants/{mId}/orders',
    title: 'List Orders',
    description: 'Retrieves a list of orders for the specified merchant. Orders can be filtered by various criteria including payment status, date range, and custom fields.',
    category: 'Orders',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'filter', in: 'query', type: 'string', required: false, description: 'Filter expression (e.g., `paymentState=PAID`)' },
      { name: 'expand', in: 'query', type: 'string', required: false, description: 'Expand related objects (e.g., `lineItems`, `customers`)' },
      { name: 'limit', in: 'query', type: 'integer', required: false, description: 'Maximum number of results to return', default: '100' },
      { name: 'offset', in: 'query', type: 'integer', required: false, description: 'Number of results to skip', default: '0' },
    ],
    responseSchema: {
      type: 'object',
      properties: {
        elements: { type: 'array', items: { $ref: '#/definitions/Order' } },
        href: { type: 'string' },
      },
    },
    exampleResponse: {
      elements: [
        {
          id: 'ABC123',
          currency: 'USD',
          total: 2499,
          paymentState: 'PAID',
          title: 'Order #1001',
          note: 'Extra napkins please',
          createdTime: 1699900000000,
          modifiedTime: 1699900500000,
          state: 'open',
        },
        {
          id: 'DEF456',
          currency: 'USD',
          total: 1599,
          paymentState: 'OPEN',
          title: 'Order #1002',
          createdTime: 1699901000000,
          modifiedTime: 1699901000000,
          state: 'open',
        },
      ],
      href: 'https://api.clover.com/v3/merchants/{mId}/orders?limit=100',
    },
  },
  {
    id: 'get-order',
    method: 'GET',
    path: '/v3/merchants/{mId}/orders/{orderId}',
    title: 'Get Order',
    description: 'Retrieves a single order by its unique identifier. Use the expand parameter to include related objects like line items, payments, and customers.',
    category: 'Orders',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'orderId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the order' },
      { name: 'expand', in: 'query', type: 'string', required: false, description: 'Expand related objects (e.g., `lineItems`, `payments`, `customers`)' },
    ],
    responseSchema: {
      type: 'object',
      properties: {
        id: { type: 'string' },
        currency: { type: 'string' },
        total: { type: 'integer' },
        paymentState: { type: 'string' },
      },
    },
    exampleResponse: {
      id: 'ABC123',
      currency: 'USD',
      total: 2499,
      paymentState: 'PAID',
      title: 'Order #1001',
      note: 'Extra napkins please',
      createdTime: 1699900000000,
      modifiedTime: 1699900500000,
      state: 'open',
      lineItems: {
        elements: [
          { id: 'LI001', name: 'Burger', price: 1299, quantity: 1 },
          { id: 'LI002', name: 'Fries', price: 499, quantity: 2 },
        ],
      },
    },
  },
  {
    id: 'create-order',
    method: 'POST',
    path: '/v3/merchants/{mId}/orders',
    title: 'Create Order',
    description: 'Creates a new order for the specified merchant. The order can include line items, customer information, and custom notes.',
    category: 'Orders',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
    ],
    requestBody: {
      type: 'object',
      description: 'Order object to create',
      example: {
        state: 'open',
        title: 'New Order',
        note: 'Customer requested extra sauce',
        orderType: { id: 'default' },
      },
    },
    responseSchema: {
      type: 'object',
      properties: {
        id: { type: 'string' },
        href: { type: 'string' },
      },
    },
    exampleResponse: {
      id: 'NEW789',
      href: 'https://api.clover.com/v3/merchants/{mId}/orders/NEW789',
    },
  },
  {
    id: 'update-order',
    method: 'POST',
    path: '/v3/merchants/{mId}/orders/{orderId}',
    title: 'Update Order',
    description: 'Updates an existing order. You can modify the order title, notes, state, and other properties.',
    category: 'Orders',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'orderId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the order' },
    ],
    requestBody: {
      type: 'object',
      description: 'Order fields to update',
      example: {
        note: 'Updated note for the order',
        state: 'locked',
      },
    },
    responseSchema: {
      type: 'object',
      properties: {
        id: { type: 'string' },
      },
    },
    exampleResponse: {
      id: 'ABC123',
      note: 'Updated note for the order',
      state: 'locked',
      modifiedTime: 1699902000000,
    },
  },
  {
    id: 'delete-order',
    method: 'DELETE',
    path: '/v3/merchants/{mId}/orders/{orderId}',
    title: 'Delete Order',
    description: 'Deletes an order. This action cannot be undone. Orders with payments cannot be deleted.',
    category: 'Orders',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'orderId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the order' },
    ],
    responseSchema: {},
    exampleResponse: {},
  },

  // Line Items
  {
    id: 'list-line-items',
    method: 'GET',
    path: '/v3/merchants/{mId}/orders/{orderId}/line_items',
    title: 'List Line Items',
    description: 'Retrieves all line items for a specific order.',
    category: 'Line Items',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'orderId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the order' },
    ],
    responseSchema: {
      type: 'object',
      properties: {
        elements: { type: 'array' },
      },
    },
    exampleResponse: {
      elements: [
        { id: 'LI001', name: 'Burger', price: 1299, quantity: 1 },
        { id: 'LI002', name: 'Fries', price: 499, quantity: 2 },
        { id: 'LI003', name: 'Soda', price: 199, quantity: 1 },
      ],
    },
  },
  {
    id: 'create-line-item',
    method: 'POST',
    path: '/v3/merchants/{mId}/orders/{orderId}/line_items',
    title: 'Create Line Item',
    description: 'Adds a new line item to an existing order.',
    category: 'Line Items',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'orderId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the order' },
    ],
    requestBody: {
      type: 'object',
      description: 'Line item to add',
      example: {
        item: { id: 'ITEM001' },
        quantity: 2,
        note: 'No onions',
      },
    },
    responseSchema: {
      type: 'object',
    },
    exampleResponse: {
      id: 'LI004',
      name: 'Pizza',
      price: 1499,
      quantity: 2,
      note: 'No onions',
    },
  },
  {
    id: 'update-line-item',
    method: 'POST',
    path: '/v3/merchants/{mId}/orders/{orderId}/line_items/{lineItemId}',
    title: 'Update Line Item',
    description: 'Updates the quantity, price, or note of an existing line item.',
    category: 'Line Items',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'orderId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the order' },
      { name: 'lineItemId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the line item' },
    ],
    requestBody: {
      type: 'object',
      description: 'Line item fields to update',
      example: {
        quantity: 3,
        note: 'Extra cheese',
      },
    },
    responseSchema: {
      type: 'object',
    },
    exampleResponse: {
      id: 'LI004',
      name: 'Pizza',
      price: 1499,
      quantity: 3,
      note: 'Extra cheese',
    },
  },
  {
    id: 'delete-line-item',
    method: 'DELETE',
    path: '/v3/merchants/{mId}/orders/{orderId}/line_items/{lineItemId}',
    title: 'Delete Line Item',
    description: 'Removes a line item from an order.',
    category: 'Line Items',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'orderId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the order' },
      { name: 'lineItemId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the line item' },
    ],
    responseSchema: {},
    exampleResponse: {},
  },

  // Customers
  {
    id: 'list-customers',
    method: 'GET',
    path: '/v3/merchants/{mId}/customers',
    title: 'List Customers',
    description: 'Retrieves a list of customers for the merchant. Customers can be filtered by name, email, or phone number.',
    category: 'Customers',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'filter', in: 'query', type: 'string', required: false, description: 'Filter expression' },
      { name: 'limit', in: 'query', type: 'integer', required: false, description: 'Maximum number of results', default: '100' },
    ],
    responseSchema: {
      type: 'object',
      properties: {
        elements: { type: 'array' },
      },
    },
    exampleResponse: {
      elements: [
        {
          id: 'CUST001',
          firstName: 'John',
          lastName: 'Doe',
          emailAddresses: [{ emailAddress: 'john@example.com' }],
          phoneNumbers: [{ phoneNumber: '555-0100' }],
        },
        {
          id: 'CUST002',
          firstName: 'Jane',
          lastName: 'Smith',
          emailAddresses: [{ emailAddress: 'jane@example.com' }],
        },
      ],
    },
  },
  {
    id: 'get-customer',
    method: 'GET',
    path: '/v3/merchants/{mId}/customers/{customerId}',
    title: 'Get Customer',
    description: 'Retrieves a single customer by ID.',
    category: 'Customers',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'customerId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the customer' },
      { name: 'expand', in: 'query', type: 'string', required: false, description: 'Expand related objects' },
    ],
    responseSchema: {
      type: 'object',
    },
    exampleResponse: {
      id: 'CUST001',
      firstName: 'John',
      lastName: 'Doe',
      emailAddresses: [{ emailAddress: 'john@example.com' }],
      phoneNumbers: [{ phoneNumber: '555-0100' }],
      orders: { href: '/v3/merchants/{mId}/customers/CUST001/orders' },
    },
  },
  {
    id: 'create-customer',
    method: 'POST',
    path: '/v3/merchants/{mId}/customers',
    title: 'Create Customer',
    description: 'Creates a new customer record for the merchant.',
    category: 'Customers',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
    ],
    requestBody: {
      type: 'object',
      description: 'Customer object to create',
      example: {
        firstName: 'Alex',
        lastName: 'Rivera',
        emailAddresses: [{ emailAddress: 'alex@example.com' }],
      },
    },
    responseSchema: {
      type: 'object',
    },
    exampleResponse: {
      id: 'CUST003',
      firstName: 'Alex',
      lastName: 'Rivera',
      emailAddresses: [{ emailAddress: 'alex@example.com' }],
    },
  },
  {
    id: 'update-customer',
    method: 'POST',
    path: '/v3/merchants/{mId}/customers/{customerId}',
    title: 'Update Customer',
    description: 'Updates an existing customer\'s contact information.',
    category: 'Customers',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'customerId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the customer' },
    ],
    requestBody: {
      type: 'object',
      description: 'Customer fields to update',
      example: {
        phoneNumbers: [{ phoneNumber: '555-0199' }],
      },
    },
    responseSchema: {
      type: 'object',
    },
    exampleResponse: {
      id: 'CUST001',
      firstName: 'John',
      lastName: 'Doe',
      phoneNumbers: [{ phoneNumber: '555-0199' }],
    },
  },
  {
    id: 'delete-customer',
    method: 'DELETE',
    path: '/v3/merchants/{mId}/customers/{customerId}',
    title: 'Delete Customer',
    description: 'Deletes a customer record. Orders previously linked to this customer are not affected.',
    category: 'Customers',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'customerId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the customer' },
    ],
    responseSchema: {},
    exampleResponse: {},
  },

  // Payments
  {
    id: 'list-payments',
    method: 'GET',
    path: '/v3/merchants/{mId}/payments',
    title: 'List Payments',
    description: 'Retrieves a list of payments for the merchant.',
    category: 'Payments',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'filter', in: 'query', type: 'string', required: false, description: 'Filter expression' },
      { name: 'limit', in: 'query', type: 'integer', required: false, description: 'Maximum results', default: '100' },
    ],
    responseSchema: {
      type: 'object',
      properties: {
        elements: { type: 'array' },
      },
    },
    exampleResponse: {
      elements: [
        {
          id: 'PAY001',
          amount: 2499,
          result: 'SUCCESS',
          cardTransaction: {
            last4: '4242',
            cardType: 'VISA',
          },
          createdTime: 1699900500000,
        },
      ],
    },
  },
  {
    id: 'get-payment',
    method: 'GET',
    path: '/v3/merchants/{mId}/payments/{paymentId}',
    title: 'Get Payment',
    description: 'Retrieves a single payment by its unique identifier.',
    category: 'Payments',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'paymentId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the payment' },
    ],
    responseSchema: {
      type: 'object',
    },
    exampleResponse: {
      id: 'PAY001',
      amount: 2499,
      result: 'SUCCESS',
      cardTransaction: {
        last4: '4242',
        cardType: 'VISA',
      },
      createdTime: 1699900500000,
      order: { id: 'ABC123' },
    },
  },
  {
    id: 'refund-payment',
    method: 'POST',
    path: '/v3/merchants/{mId}/payments/{paymentId}/refunds',
    title: 'Refund Payment',
    description: 'Issues a full or partial refund for a payment. Omit `amount` for a full refund.',
    category: 'Payments',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'paymentId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the payment' },
    ],
    requestBody: {
      type: 'object',
      description: 'Refund details',
      example: {
        amount: 500,
        reason: 'Customer requested partial refund',
      },
    },
    responseSchema: {
      type: 'object',
    },
    exampleResponse: {
      id: 'REF001',
      payment: { id: 'PAY001' },
      amount: 500,
      status: 'SUCCESS',
      createdTime: 1699903000000,
    },
  },
  {
    id: 'void-payment',
    method: 'POST',
    path: '/v3/merchants/{mId}/payments/{paymentId}/voids',
    title: 'Void Payment',
    description: 'Voids a payment before it settles. Once a payment has settled, use Refund Payment instead.',
    category: 'Payments',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'paymentId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the payment' },
    ],
    requestBody: {
      type: 'object',
      description: 'Void reason',
      example: {
        voidReason: 'MERCHANT_INITIATED',
      },
    },
    responseSchema: {
      type: 'object',
    },
    exampleResponse: {
      id: 'PAY001',
      result: 'VOIDED',
      voidReason: 'MERCHANT_INITIATED',
    },
  },

  // Webhooks
  {
    id: 'list-webhooks',
    method: 'GET',
    path: '/v3/merchants/{mId}/webhooks',
    title: 'List Webhooks',
    description: 'Retrieves all webhook subscriptions for the merchant.',
    category: 'Webhooks',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
    ],
    responseSchema: {
      type: 'object',
      properties: {
        elements: { type: 'array' },
      },
    },
    exampleResponse: {
      elements: [
        {
          id: 'WH001',
          url: 'https://myapp.com/webhooks/clover',
          events: ['ORDER_CREATED', 'ORDER_UPDATED', 'PAYMENT_PROCESSED'],
          active: true,
        },
      ],
    },
  },
  {
    id: 'create-webhook',
    method: 'POST',
    path: '/v3/merchants/{mId}/webhooks',
    title: 'Create Webhook',
    description: 'Creates a new webhook subscription to receive real-time notifications.',
    category: 'Webhooks',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
    ],
    requestBody: {
      type: 'object',
      description: 'Webhook configuration',
      example: {
        url: 'https://myapp.com/webhooks/clover',
        events: ['ORDER_CREATED', 'ORDER_UPDATED'],
      },
    },
    responseSchema: {
      type: 'object',
    },
    exampleResponse: {
      id: 'WH002',
      url: 'https://myapp.com/webhooks/clover',
      events: ['ORDER_CREATED', 'ORDER_UPDATED'],
      active: true,
      secret: 'whsec_xxxxxxxxxxxxx',
    },
  },
  {
    id: 'update-webhook',
    method: 'POST',
    path: '/v3/merchants/{mId}/webhooks/{webhookId}',
    title: 'Update Webhook',
    description: 'Updates a webhook subscription\'s URL, subscribed events, or active state.',
    category: 'Webhooks',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'webhookId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the webhook' },
    ],
    requestBody: {
      type: 'object',
      description: 'Webhook fields to update',
      example: {
        events: ['ORDER_CREATED', 'ORDER_UPDATED', 'PAYMENT_PROCESSED'],
        active: false,
      },
    },
    responseSchema: {
      type: 'object',
    },
    exampleResponse: {
      id: 'WH001',
      url: 'https://myapp.com/webhooks/clover',
      events: ['ORDER_CREATED', 'ORDER_UPDATED', 'PAYMENT_PROCESSED'],
      active: false,
    },
  },
  {
    id: 'delete-webhook',
    method: 'DELETE',
    path: '/v3/merchants/{mId}/webhooks/{webhookId}',
    title: 'Delete Webhook',
    description: 'Deletes a webhook subscription. Events already queued for delivery may still be sent.',
    category: 'Webhooks',
    parameters: [
      { name: 'mId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the merchant' },
      { name: 'webhookId', in: 'path', type: 'string', required: true, description: 'Unique identifier of the webhook' },
    ],
    responseSchema: {},
    exampleResponse: {},
  },
];

/**
 * Webhooks are signed with an HMAC-SHA256 signature so you can verify a
 * request actually came from Clover before trusting its payload. The
 * `secret` returned once from Create Webhook (`whsec_...`) is the HMAC key -
 * store it securely; it is never returned again.
 */
export const webhookSignatureVerification = {
  header: 'X-Clover-Signature',
  algorithm: 'HMAC-SHA256',
  description:
    'Compute an HMAC-SHA256 digest of the raw request body using your webhook\'s secret as the key, hex-encode it, and compare it to the X-Clover-Signature header using a constant-time comparison. Reject the request if they don\'t match.',
  verifyExample: `const crypto = require('crypto');

function isValidSignature(rawBody, signatureHeader, secret) {
  const expected = crypto
    .createHmac('sha256', secret)
    .update(rawBody)
    .digest('hex');

  return crypto.timingSafeEqual(
    Buffer.from(expected),
    Buffer.from(signatureHeader)
  );
}`,
};

/**
 * Example payloads for each event type a webhook can subscribe to.
 */
export const webhookEventPayloads: Record<string, object> = {
  ORDER_CREATED: {
    type: 'ORDER_CREATED',
    merchantId: 'MERCH001',
    objectId: 'ABC123',
    ts: 1699900000000,
  },
  ORDER_UPDATED: {
    type: 'ORDER_UPDATED',
    merchantId: 'MERCH001',
    objectId: 'ABC123',
    ts: 1699900500000,
  },
  PAYMENT_PROCESSED: {
    type: 'PAYMENT_PROCESSED',
    merchantId: 'MERCH001',
    objectId: 'PAY001',
    ts: 1699900600000,
  },
};

export const categories = [
  { id: 'orders', title: 'Orders', icon: 'ShoppingBag' },
  { id: 'line-items', title: 'Line Items', icon: 'List' },
  { id: 'customers', title: 'Customers', icon: 'Users' },
  { id: 'payments', title: 'Payments', icon: 'CreditCard' },
  { id: 'webhooks', title: 'Webhooks', icon: 'Webhook' },
];

export const getEndpointsByCategory = (category: string): Endpoint[] => {
  return endpoints.filter(
    (e) => e.category.toLowerCase().replace(' ', '-') === category.toLowerCase()
  );
};

export const getEndpointById = (id: string): Endpoint | undefined => {
  return endpoints.find((e) => e.id === id);
};
