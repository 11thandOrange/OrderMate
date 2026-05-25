import { DocsLayout } from '../components/Layout/DocsLayout';
import { ArrowRight, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';

const codeBlockStyle = {
  background: '#1e2028',
  borderRadius: '0.75rem',
  fontFamily: "'Monaco', 'Menlo', monospace",
  fontSize: '13px',
};

export function GettingStarted() {
  return (
    <DocsLayout>
      {/* Breadcrumb */}
      <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
        <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
        <span>/</span>
        <span style={{ color: '#9ca3af' }}>Getting Started</span>
      </nav>

      {/* Page Title */}
      <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>Getting Started</h1>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
        Get OrderMate up and running on your Clover device in just a few minutes.
      </p>

      {/* Prerequisites */}
      <section style={{ marginBottom: '48px' }}>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Prerequisites</h2>
        <ul style={{ display: 'flex', flexDirection: 'column', gap: '12px', listStyle: 'none', padding: 0, margin: 0 }}>
          <li style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
            <CheckCircle2 style={{ width: '20px', height: '20px', color: '#34d399', flexShrink: 0, marginTop: '2px' }} />
            <span style={{ color: '#d1d5db' }}>A Clover merchant account</span>
          </li>
          <li style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
            <CheckCircle2 style={{ width: '20px', height: '20px', color: '#34d399', flexShrink: 0, marginTop: '2px' }} />
            <span style={{ color: '#d1d5db' }}>Clover device (Station, Mini, Flex, or Mobile)</span>
          </li>
          <li style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
            <CheckCircle2 style={{ width: '20px', height: '20px', color: '#34d399', flexShrink: 0, marginTop: '2px' }} />
            <span style={{ color: '#d1d5db' }}>Active internet connection</span>
          </li>
        </ul>
      </section>

      {/* Installation */}
      <section style={{ marginBottom: '48px' }}>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Installation</h2>
        
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <div style={{ padding: '24px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px' }}>
            <h3 style={{ fontSize: '18px', fontWeight: 500, color: '#ffffff', marginBottom: '12px' }}>
              1. Install from Clover App Market
            </h3>
            <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
              The easiest way to install OrderMate is through the Clover App Market.
            </p>
            <a
              href="https://www.clover.com/appmarket/apps/WWTF1AKT87VJ8"
              target="_blank"
              rel="noopener noreferrer"
              style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '8px 16px', background: '#f97316', color: '#ffffff', fontWeight: 500, borderRadius: '8px', textDecoration: 'none' }}
            >
              Open Clover App Market
              <ArrowRight style={{ width: '16px', height: '16px' }} />
            </a>
          </div>

          <div style={{ padding: '24px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px' }}>
            <h3 style={{ fontSize: '18px', fontWeight: 500, color: '#ffffff', marginBottom: '12px' }}>
              2. Grant Permissions
            </h3>
            <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
              OrderMate requires the following permissions to function:
            </p>
            <ul style={{ display: 'flex', flexDirection: 'column', gap: '8px', listStyle: 'none', padding: 0, margin: 0, color: '#d1d5db' }}>
              <li>• <strong>Orders</strong> - Read and write access to orders</li>
              <li>• <strong>Customers</strong> - Read and write access to customer data</li>
              <li>• <strong>Inventory</strong> - Read access to inventory items</li>
              <li>• <strong>Payments</strong> - Read access to payment information</li>
            </ul>
          </div>

          <div style={{ padding: '24px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px' }}>
            <h3 style={{ fontSize: '18px', fontWeight: 500, color: '#ffffff', marginBottom: '12px' }}>
              3. Launch OrderMate
            </h3>
            <p style={{ color: '#9ca3af', margin: 0 }}>
              After installation, find OrderMate in your Clover app drawer and tap to launch. 
              The app will sync with your existing orders automatically.
            </p>
          </div>
        </div>
      </section>

      {/* Configuration */}
      <section style={{ marginBottom: '48px' }}>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Initial Configuration</h2>
        <p style={{ color: '#9ca3af', marginBottom: '24px' }}>
          After launching OrderMate for the first time, you'll be guided through the setup process:
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', gap: '16px' }}>
            <div style={{ width: '32px', height: '32px', borderRadius: '50%', background: 'rgba(249, 115, 22, 0.2)', color: '#f97316', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600, flexShrink: 0 }}>
              1
            </div>
            <div>
              <h4 style={{ color: '#ffffff', fontWeight: 500, marginBottom: '4px' }}>Set your notification preferences</h4>
              <p style={{ color: '#9ca3af', fontSize: '14px', margin: 0 }}>
                Configure SMS and email notifications for order updates.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '16px' }}>
            <div style={{ width: '32px', height: '32px', borderRadius: '50%', background: 'rgba(249, 115, 22, 0.2)', color: '#f97316', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600, flexShrink: 0 }}>
              2
            </div>
            <div>
              <h4 style={{ color: '#ffffff', fontWeight: 500, marginBottom: '4px' }}>Customize your widgets</h4>
              <p style={{ color: '#9ca3af', fontSize: '14px', margin: 0 }}>
                Set up custom fields and widgets to capture additional order information.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '16px' }}>
            <div style={{ width: '32px', height: '32px', borderRadius: '50%', background: 'rgba(249, 115, 22, 0.2)', color: '#f97316', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600, flexShrink: 0 }}>
              3
            </div>
            <div>
              <h4 style={{ color: '#ffffff', fontWeight: 500, marginBottom: '4px' }}>Configure calendar settings</h4>
              <p style={{ color: '#9ca3af', fontSize: '14px', margin: 0 }}>
                Set your business hours and calendar view preferences.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* API Access */}
      <section style={{ marginBottom: '48px' }}>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>API Access</h2>
        <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
          To use the Clover API with OrderMate, you'll need to obtain an API token from your Clover developer account.
        </p>

        <div style={{ ...codeBlockStyle, overflow: 'hidden' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 16px', borderBottom: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)' }}>
            <span style={{ fontSize: '12px', color: '#6b7280' }}>Test API Connection</span>
          </div>
          <pre style={{ padding: '16px', margin: 0, overflowX: 'auto' }}><code style={{ fontSize: '14px', color: '#d1d5db' }}>{`# Set your API credentials
export CLOVER_API_KEY="your_api_key_here"
export CLOVER_MERCHANT_ID="your_merchant_id"

# Test your connection
curl -H "Authorization: Bearer $CLOVER_API_KEY" \\
  "https://api.clover.com/v3/merchants/$CLOVER_MERCHANT_ID"`}</code></pre>
        </div>
      </section>

      {/* Next Steps */}
      <section style={{ padding: '24px', background: 'rgba(249, 115, 22, 0.05)', border: '1px solid rgba(249, 115, 22, 0.2)', borderRadius: '12px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Next Steps</h2>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
          <Link
            to="/features/orders"
            style={{ padding: '16px', background: 'rgba(255,255,255,0.05)', borderRadius: '8px', textDecoration: 'none' }}
          >
            <h3 style={{ color: '#ffffff', fontWeight: 500, marginBottom: '4px' }}>Order Management →</h3>
            <p style={{ fontSize: '14px', color: '#9ca3af', margin: 0 }}>Learn how to manage orders effectively</p>
          </Link>
          <Link
            to="/api/orders"
            style={{ padding: '16px', background: 'rgba(255,255,255,0.05)', borderRadius: '8px', textDecoration: 'none' }}
          >
            <h3 style={{ color: '#ffffff', fontWeight: 500, marginBottom: '4px' }}>API Reference →</h3>
            <p style={{ fontSize: '14px', color: '#9ca3af', margin: 0 }}>Explore the API documentation</p>
          </Link>
        </div>
      </section>
    </DocsLayout>
  );
}
