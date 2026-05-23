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
];

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
