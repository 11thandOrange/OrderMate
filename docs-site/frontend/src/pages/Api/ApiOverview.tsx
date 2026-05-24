import { Link } from 'react-router-dom';
import { ApiLayout } from '../../components/Layout/ApiLayout';
import { Copy } from 'lucide-react';

const quickLinks = [
  { method: 'GET', title: 'List Orders', href: '/api/orders#list-orders' },
  { method: 'GET', title: 'Get Order', href: '/api/orders#get-order' },
  { method: 'POST', title: 'Create Order', href: '/api/orders#create-order' },
  { method: 'GET', title: 'List Customers', href: '/api/customers#list-customers' },
  { method: 'GET', title: 'List Payments', href: '/api/payments#list-payments' },
];

const methodColors: Record<string, string> = {
  GET: '#10b981',
  POST: '#3b82f6',
  PUT: '#f59e0b',
  DELETE: '#ef4444',
};

const codeBlockStyle = {
  background: '#1e2028',
  borderRadius: '0.75rem',
  fontFamily: "'Monaco', 'Menlo', monospace",
  fontSize: '13px',
};

const endpoints = [
  { name: 'Orders', href: '/api/orders', description: 'Create, read, update, and delete orders' },
  { name: 'Line Items', href: '/api/line-items', description: 'Manage items within orders' },
  { name: 'Customers', href: '/api/customers', description: 'Access customer information' },
  { name: 'Payments', href: '/api/payments', description: 'View payment transactions' },
  { name: 'Webhooks', href: '/api/webhooks', description: 'Receive real-time notifications' },
];

export function ApiOverview() {
  return (
    <ApiLayout>
      {/* Breadcrumb */}
      <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
        <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
        <span>/</span>
        <span style={{ color: '#9ca3af' }}>API Reference</span>
      </nav>

      {/* Page Title */}
      <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>API Reference</h1>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
        The Clover API allows you to programmatically access and manage merchant data 
        including orders, customers, payments, and more.
      </p>

      {/* Quick Links */}
      <div style={{ marginBottom: '40px', padding: '16px', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px', background: 'rgba(255,255,255,0.05)' }}>
        <h3 style={{ fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#6b7280', marginBottom: '12px' }}>Quick Links</h3>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
          {quickLinks.map((link) => (
            <Link
              key={link.href}
              to={link.href}
              style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px', fontSize: '14px', borderRadius: '8px', color: '#9ca3af', textDecoration: 'none' }}
            >
              <span style={{ fontSize: '12px', padding: '2px 6px', borderRadius: '4px', color: '#ffffff', fontWeight: 500, background: methodColors[link.method] }}>
                {link.method}
              </span>
              <span>{link.title}</span>
            </Link>
          ))}
        </div>
      </div>

      {/* Base URL */}
      <section style={{ marginBottom: '48px' }}>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Base URL</h2>
        <div style={{ overflow: 'hidden', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.1)' }}>
          <div style={{ padding: '16px', background: 'rgba(255,255,255,0.05)' }}>
            <code style={{ fontSize: '18px', color: '#34d399' }}>https://api.clover.com/v3</code>
          </div>
        </div>
        <p style={{ fontSize: '14px', color: '#6b7280', marginTop: '12px' }}>
          For sandbox/development, use: <code style={{ color: '#9ca3af' }}>https://sandbox.dev.clover.com/v3</code>
        </p>
      </section>

      {/* Authentication */}
      <section style={{ marginBottom: '48px' }}>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Authentication</h2>
        <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
          All API requests require authentication using an OAuth 2.0 Bearer token. 
          Include your API token in the Authorization header:
        </p>
        <div style={{ ...codeBlockStyle, overflow: 'hidden' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 16px', borderBottom: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)' }}>
            <span style={{ fontSize: '12px', color: '#6b7280' }}>Authorization Header</span>
            <button style={{ color: '#6b7280', background: 'none', border: 'none', cursor: 'pointer' }}>
              <Copy style={{ width: '16px', height: '16px' }} />
            </button>
          </div>
          <pre style={{ padding: '16px', margin: 0, overflowX: 'auto' }}><code style={{ fontSize: '14px', color: '#d1d5db' }}>{`curl -H "Authorization: Bearer YOUR_API_TOKEN" \\
  https://api.clover.com/v3/merchants/{mId}/orders`}</code></pre>
        </div>
      </section>

      {/* Endpoints */}
      <section style={{ marginBottom: '48px' }}>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Endpoints</h2>
        <div style={{ overflow: 'hidden', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.1)' }}>
          <table style={{ width: '100%', fontSize: '14px', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)' }}>
                <th style={{ textAlign: 'left', padding: '12px 16px', fontWeight: 500, color: '#9ca3af' }}>Resource</th>
                <th style={{ textAlign: 'left', padding: '12px 16px', fontWeight: 500, color: '#9ca3af' }}>Description</th>
              </tr>
            </thead>
            <tbody>
              {endpoints.map((ep, i) => (
                <tr key={ep.name} style={{ borderBottom: i < endpoints.length - 1 ? '1px solid rgba(255,255,255,0.05)' : 'none' }}>
                  <td style={{ padding: '12px 16px' }}>
                    <Link to={ep.href} style={{ color: '#fb923c', textDecoration: 'none', fontWeight: 500 }}>{ep.name}</Link>
                  </td>
                  <td style={{ padding: '12px 16px', color: '#9ca3af' }}>{ep.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Response Format */}
      <section style={{ marginBottom: '48px' }}>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Response Format</h2>
        <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
          All API responses are returned in JSON format. List endpoints return an 
          object with an <code style={{ color: '#fb923c' }}>elements</code> array:
        </p>
        <div style={{ ...codeBlockStyle, overflow: 'hidden' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 16px', borderBottom: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)' }}>
            <span style={{ fontSize: '12px', color: '#6b7280' }}>Example Response</span>
            <button style={{ color: '#6b7280', background: 'none', border: 'none', cursor: 'pointer' }}>
              <Copy style={{ width: '16px', height: '16px' }} />
            </button>
          </div>
          <pre style={{ padding: '16px', margin: 0, overflowX: 'auto' }}><code style={{ fontSize: '14px', color: '#d1d5db' }}>{`{
  "elements": [
    {
      "id": "ABC123",
      "total": 2499,
      "paymentState": "PAID"
    }
  ]
}`}</code></pre>
        </div>
      </section>
    </ApiLayout>
  );
}
