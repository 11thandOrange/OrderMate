import type { NavItem } from '../types/api';

export const navigation: NavItem[] = [
  {
    title: 'Getting Started',
    href: '/getting-started',
    children: [
      { title: 'Introduction', href: '/getting-started' },
      { title: 'Installation', href: '/getting-started/installation' },
      { title: 'Authentication', href: '/getting-started/authentication' },
    ],
  },
  {
    title: 'Features',
    href: '/features',
    children: [
      { title: 'Order Management', href: '/features/orders' },
      { title: 'Calendar View', href: '/features/calendar' },
      { title: 'Notifications', href: '/features/notifications' },
      { title: 'Widgets', href: '/features/widgets' },
      { title: 'Custom Fields', href: '/features/custom-fields' },
    ],
  },
  {
    title: 'API Reference',
    href: '/api',
    children: [
      { title: 'Overview', href: '/api' },
      { title: 'Orders', href: '/api/orders' },
      { title: 'Line Items', href: '/api/line-items' },
      { title: 'Customers', href: '/api/customers' },
      { title: 'Payments', href: '/api/payments' },
      { title: 'Webhooks', href: '/api/webhooks' },
    ],
  },
  {
    title: 'Guides',
    href: '/guides',
    children: [
      { title: 'Working with Orders', href: '/guides/orders' },
      { title: 'Setting up Webhooks', href: '/guides/webhooks' },
      { title: 'Error Handling', href: '/guides/errors' },
    ],
  },
  {
    title: 'Resources',
    href: '/resources',
    children: [
      { title: 'Changelog', href: '/changelog' },
      { title: 'FAQ', href: '/faq' },
      { title: 'Support', href: '/support' },
    ],
  },
];

export const apiNavigation: NavItem[] = [
  {
    title: 'Orders',
    href: '/api/orders',
    children: [
      { title: 'List Orders', href: '/api/orders#list-orders' },
      { title: 'Get Order', href: '/api/orders#get-order' },
      { title: 'Create Order', href: '/api/orders#create-order' },
      { title: 'Update Order', href: '/api/orders#update-order' },
      { title: 'Delete Order', href: '/api/orders#delete-order' },
    ],
  },
  {
    title: 'Line Items',
    href: '/api/line-items',
    children: [
      { title: 'List Line Items', href: '/api/line-items#list-line-items' },
      { title: 'Create Line Item', href: '/api/line-items#create-line-item' },
    ],
  },
  {
    title: 'Customers',
    href: '/api/customers',
    children: [
      { title: 'List Customers', href: '/api/customers#list-customers' },
      { title: 'Get Customer', href: '/api/customers#get-customer' },
    ],
  },
  {
    title: 'Payments',
    href: '/api/payments',
    children: [
      { title: 'List Payments', href: '/api/payments#list-payments' },
    ],
  },
  {
    title: 'Webhooks',
    href: '/api/webhooks',
    children: [
      { title: 'List Webhooks', href: '/api/webhooks#list-webhooks' },
      { title: 'Create Webhook', href: '/api/webhooks#create-webhook' },
    ],
  },
];
