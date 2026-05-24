import { Link } from 'react-router-dom';
import { ApiLayout } from '../../components/Layout/ApiLayout';
import { Play, Copy } from 'lucide-react';

const orderAttributes = [
  { name: 'id', type: 'string', description: 'Unique identifier for the order' },
  { name: 'currency', type: 'string', description: 'Three-letter ISO currency code' },
  { name: 'total', type: 'integer', description: 'Total amount in cents' },
  { name: 'paymentState', type: 'string', description: 'OPEN, PAID, PARTIALLY_PAID, etc.' },
  { name: 'title', type: 'string', description: 'Order title/name' },
];

const quickLinks = [
  { method: 'GET', title: 'List Orders', href: '#list-orders' },
  { method: 'GET', title: 'Get Order', href: '#get-order' },
  { method: 'POST', title: 'Create Order', href: '#create-order' },
  { method: 'PUT', title: 'Update Order', href: '#update-order' },
  { method: 'DELETE', title: 'Delete Order', href: '#delete-order' },
];

const methodColors: Record<string, string> = {
  GET: '#10b981',
  POST: '#3b82f6',
  PUT: '#f59e0b',
  DELETE: '#ef4444',
};

// Code block styles matching mockup exactly
const codeBlockStyle = {
  background: '#1e2028',
  borderRadius: '0.75rem',
  fontFamily: "'Monaco', 'Menlo', monospace",
  fontSize: '13px',
};

function TryItOutPanel() {
  return (
    <>
      <h3 style={{ fontSize: '18px', fontWeight: 600, color: '#ffffff', marginBottom: '24px' }}>Try it out</h3>
      
      {/* Endpoint URL */}
      <div style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.1)', fontFamily: 'monospace', fontSize: '14px', background: 'rgba(255,255,255,0.05)' }}>
          <span style={{ padding: '2px 6px', borderRadius: '4px', fontSize: '12px', fontWeight: 700, background: '#10b981', color: '#ffffff' }}>GET</span>
          <span style={{ color: '#9ca3af', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>/v3/merchants/{'{mId}'}/orders</span>
        </div>
      </div>

      {/* Input Fields */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '24px' }}>
        <div>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: '#9ca3af', marginBottom: '8px' }}>API Key</label>
          <input 
            type="text" 
            placeholder="Enter your Clover API key" 
            style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', fontSize: '14px', color: '#ffffff', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)', outline: 'none' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: '#9ca3af', marginBottom: '8px' }}>Merchant ID</label>
          <input 
            type="text" 
            placeholder="Enter merchant ID" 
            style={{ width: '100%', padding: '8px 12px', borderRadius: '8px', fontSize: '14px', color: '#ffffff', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)', outline: 'none' }}
          />
        </div>
      </div>

      {/* Try it button */}
      <button style={{ width: '100%', padding: '10px 16px', background: '#f97316', color: '#ffffff', fontWeight: 500, borderRadius: '8px', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
        <Play style={{ width: '16px', height: '16px' }} />
        Try it
      </button>

      {/* Code Examples */}
      <div style={{ marginTop: '32px' }}>
        <h4 style={{ fontSize: '14px', fontWeight: 500, color: '#9ca3af', marginBottom: '12px' }}>Code Examples</h4>
        <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
          <button style={{ padding: '6px 12px', fontSize: '14px', color: '#ffffff', borderRadius: '8px', background: 'rgba(255,255,255,0.1)', border: 'none', cursor: 'pointer' }}>cURL</button>
          <button style={{ padding: '6px 12px', fontSize: '14px', color: '#9ca3af', borderRadius: '8px', background: 'transparent', border: 'none', cursor: 'pointer' }}>Python</button>
        </div>
        <div style={{ ...codeBlockStyle, padding: '16px' }}>
          <pre style={{ margin: 0 }}><code style={{ color: '#d1d5db', fontSize: '12px' }}><span style={{ color: '#a78bfa' }}>curl</span> <span style={{ color: '#34d399' }}>"https://api.clover.com/v3/merchants/{'{mId}'}/orders"</span> \{'\n'}  -H <span style={{ color: '#34d399' }}>"Authorization: Bearer TOKEN"</span></code></pre>
        </div>
      </div>
    </>
  );
}

export function OrdersApi() {
  return (
    <ApiLayout rightPanel={<TryItOutPanel />}>
      {/* Breadcrumb */}
      <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
        <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
        <span>/</span>
        <Link to="/api" style={{ color: '#6b7280', textDecoration: 'none' }}>API Reference</Link>
        <span>/</span>
        <span style={{ color: '#9ca3af' }}>Orders</span>
      </nav>

      {/* Page Title */}
      <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>Orders</h1>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
        The Orders API allows you to create, read, update, and delete orders for a merchant.
        Orders contain line items, customer information, and payment details.
      </p>

      {/* On This Page */}
      <div style={{ marginBottom: '40px', padding: '16px', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px', background: 'rgba(255,255,255,0.05)' }}>
        <h3 style={{ fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#6b7280', marginBottom: '12px' }}>On this page</h3>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
          {quickLinks.map((link) => (
            <a 
              key={link.href}
              href={link.href} 
              style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px', fontSize: '14px', borderRadius: '8px', color: '#9ca3af', textDecoration: 'none' }}
            >
              <span style={{ fontSize: '12px', padding: '2px 6px', borderRadius: '4px', color: '#ffffff', fontWeight: 500, background: methodColors[link.method] }}>
                {link.method}
              </span>
              <span>{link.title}</span>
            </a>
          ))}
        </div>
      </div>

      {/* The Order Object */}
      <section style={{ marginBottom: '48px' }}>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>The Order Object</h2>
        <div style={{ overflow: 'hidden', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.1)' }}>
          <table style={{ width: '100%', fontSize: '14px', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)' }}>
                <th style={{ textAlign: 'left', padding: '12px 16px', fontWeight: 500, color: '#9ca3af' }}>Attribute</th>
                <th style={{ textAlign: 'left', padding: '12px 16px', fontWeight: 500, color: '#9ca3af' }}>Type</th>
                <th style={{ textAlign: 'left', padding: '12px 16px', fontWeight: 500, color: '#9ca3af' }}>Description</th>
              </tr>
            </thead>
            <tbody>
              {orderAttributes.map((attr, i) => (
                <tr key={attr.name} style={{ borderBottom: i < orderAttributes.length - 1 ? '1px solid rgba(255,255,255,0.05)' : 'none' }}>
                  <td style={{ padding: '12px 16px', fontFamily: 'monospace', color: '#fb923c' }}>{attr.name}</td>
                  <td style={{ padding: '12px 16px', color: '#9ca3af' }}>{attr.type}</td>
                  <td style={{ padding: '12px 16px', color: '#9ca3af' }}>{attr.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* List Orders Endpoint */}
      <section id="list-orders" style={{ marginBottom: '48px', scrollMarginTop: '80px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <span style={{ padding: '4px 8px', borderRadius: '4px', fontSize: '12px', fontWeight: 700, background: '#10b981', color: '#ffffff' }}>GET</span>
          <code style={{ fontSize: '14px', color: '#9ca3af', fontFamily: 'monospace' }}>/v3/merchants/{'{mId}'}/orders</code>
        </div>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>List Orders</h2>
        <p style={{ color: '#9ca3af', marginBottom: '24px' }}>
          Retrieves a list of orders for the specified merchant.
        </p>

        {/* Parameters */}
        <div style={{ marginBottom: '24px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Parameters</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ padding: '16px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                <code style={{ color: '#fb923c', fontFamily: 'monospace' }}>mId</code>
                <span style={{ fontSize: '12px', color: '#6b7280' }}>string</span>
                <span style={{ fontSize: '12px', padding: '2px 6px', borderRadius: '4px', background: 'rgba(239, 68, 68, 0.2)', color: '#f87171' }}>required</span>
              </div>
              <p style={{ fontSize: '14px', color: '#9ca3af', margin: 0 }}>Unique identifier of the merchant</p>
            </div>
            <div style={{ padding: '16px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                <code style={{ color: '#fb923c', fontFamily: 'monospace' }}>limit</code>
                <span style={{ fontSize: '12px', color: '#6b7280' }}>integer</span>
              </div>
              <p style={{ fontSize: '14px', color: '#9ca3af', margin: 0 }}>Maximum results to return (default: 100)</p>
            </div>
          </div>
        </div>

        {/* Response */}
        <div style={{ marginBottom: '24px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Response</h3>
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
        </div>
      </section>
    </ApiLayout>
  );
}
