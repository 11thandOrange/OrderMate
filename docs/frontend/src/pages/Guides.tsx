import { Link, useParams } from 'react-router-dom';
import { DocsLayout } from '../components/Layout/DocsLayout';
import { ShoppingBag, Webhook, AlertTriangle } from 'lucide-react';
import { CodeBlock } from '../components/ApiReference/CodeBlock';

const iconColors: Record<string, string> = {
  orders: '#34d399',
  webhooks: '#60a5fa',
  errors: '#f87171',
};

const icons: Record<string, React.ElementType> = {
  orders: ShoppingBag,
  webhooks: Webhook,
  errors: AlertTriangle,
};

const guides: Record<string, { title: string; description: string }> = {
  orders: {
    title: 'Working with Orders',
    description: 'A walkthrough of the full order lifecycle: create, add line items, take payment, and update status.',
  },
  webhooks: {
    title: 'Setting up Webhooks',
    description: 'Subscribe to real-time order and payment events instead of polling the API.',
  },
  errors: {
    title: 'Error Handling',
    description: 'How to handle the error responses the API returns, and how to make retries safe.',
  },
};

function GuideBody({ guide }: { guide: string }) {
  if (guide === 'orders') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '48px' }}>
        <section>
          <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>1. Create the order</h2>
          <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
            Start with an empty order via <Link to="/api/orders#create-order" style={{ color: '#f97316' }}>Create Order</Link>.
            Orders are created empty - you add line items separately, which lets you build up an order
            incrementally as a customer adds items.
          </p>
          <CodeBlock
            language="bash"
            code={`curl -X POST "https://api.clover.com/v3/merchants/{mId}/orders" \\
  -H "Authorization: Bearer $CLOVER_API_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{"state": "open", "title": "New Order"}'`}
          />
        </section>

        <section>
          <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>2. Add line items</h2>
          <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
            Add each item with <Link to="/api/line-items#create-line-item" style={{ color: '#f97316' }}>Create Line Item</Link>,
            referencing the order returned from step 1. Use <Link to="/api/line-items#update-line-item" style={{ color: '#f97316' }}>Update Line Item</Link> to
            change quantity or attach a note (e.g. "no onions") without removing and re-adding it.
          </p>
          <CodeBlock
            language="bash"
            code={`curl -X POST "https://api.clover.com/v3/merchants/{mId}/orders/{orderId}/line_items" \\
  -H "Authorization: Bearer $CLOVER_API_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{"item": {"id": "ITEM001"}, "quantity": 2}'`}
          />
        </section>

        <section>
          <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>3. Track payment state</h2>
          <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
            Payments are created by the Clover device at checkout, not through this API - you read them via{' '}
            <Link to="/api/payments" style={{ color: '#f97316' }}>Get Payment</Link> or by expanding an order
            with <code style={{ color: '#fb923c' }}>?expand=payments</code>. Watch <code style={{ color: '#fb923c' }}>paymentState</code> on
            the order (<code style={{ color: '#fb923c' }}>OPEN</code>, <code style={{ color: '#fb923c' }}>PAID</code>,{' '}
            <code style={{ color: '#fb923c' }}>PARTIALLY_PAID</code>) rather than polling payments directly, or subscribe to
            the <code style={{ color: '#fb923c' }}>PAYMENT_PROCESSED</code> webhook event - see the Webhooks guide.
          </p>
        </section>

        <section>
          <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>4. Update or close the order</h2>
          <p style={{ color: '#9ca3af', margin: 0 }}>
            Use <Link to="/api/orders#update-order" style={{ color: '#f97316' }}>Update Order</Link> to change the note or
            lock the order once it's finalized. Orders with payments attached can't be deleted - if an order was created
            in error before any payment, use <Link to="/api/orders#delete-order" style={{ color: '#f97316' }}>Delete Order</Link> instead.
          </p>
        </section>
      </div>
    );
  }

  if (guide === 'webhooks') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '48px' }}>
        <section>
          <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>1. Register a webhook</h2>
          <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
            Call <Link to="/api/webhooks#create-webhook" style={{ color: '#f97316' }}>Create Webhook</Link> with the URL you want
            events delivered to and the event types you care about. The response includes a{' '}
            <code style={{ color: '#fb923c' }}>secret</code> field (<code style={{ color: '#fb923c' }}>whsec_...</code>) - save it
            immediately, it is never shown again.
          </p>
          <CodeBlock
            language="bash"
            code={`curl -X POST "https://api.clover.com/v3/merchants/{mId}/webhooks" \\
  -H "Authorization: Bearer $CLOVER_API_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{"url": "https://myapp.com/webhooks/clover", "events": ["ORDER_CREATED", "PAYMENT_PROCESSED"]}'`}
          />
        </section>

        <section>
          <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>2. Verify every request</h2>
          <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
            Never trust a webhook payload without verifying its signature first - anyone who guesses your endpoint URL can
            POST to it. See{' '}
            <Link to="/api/webhooks#signature-verification" style={{ color: '#f97316' }}>Verifying webhook signatures</Link>{' '}
            on the Webhooks reference page for the full HMAC check.
          </p>
        </section>

        <section>
          <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>3. Respond fast, process async</h2>
          <p style={{ color: '#9ca3af', margin: 0 }}>
            Return a <code style={{ color: '#fb923c' }}>2xx</code> as soon as you've verified and durably queued the event
            (e.g. to a database row or job queue). Do the actual work - sending a confirmation email, updating your own
            order records - after responding. A slow endpoint that does synchronous work before responding risks
            delivery being marked as failed and retried, which can process the same event twice.
          </p>
        </section>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '48px' }}>
      <section>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>HTTP status codes</h2>
        <div style={{ overflow: 'hidden', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.1)' }}>
          <table style={{ width: '100%', fontSize: '14px', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)' }}>
                <th style={{ textAlign: 'left', padding: '12px 16px', fontWeight: 500, color: '#9ca3af' }}>Status</th>
                <th style={{ textAlign: 'left', padding: '12px 16px', fontWeight: 500, color: '#9ca3af' }}>Meaning</th>
                <th style={{ textAlign: 'left', padding: '12px 16px', fontWeight: 500, color: '#9ca3af' }}>What to do</th>
              </tr>
            </thead>
            <tbody>
              {[
                ['400', 'Bad Request', 'Fix the request body/params - retrying unchanged will fail again'],
                ['401', 'Unauthorized', 'The API token is missing, invalid, or expired - refresh it'],
                ['403', 'Forbidden', 'The token is valid but lacks permission for this merchant/resource'],
                ['404', 'Not Found', "The merchant, order, or other resource ID doesn't exist"],
                ['429', 'Too Many Requests', 'Back off and retry with exponential backoff'],
                ['5xx', 'Server Error', 'Safe to retry with backoff; if it persists, contact support'],
              ].map(([code, meaning, action], i, arr) => (
                <tr key={code} style={{ borderBottom: i < arr.length - 1 ? '1px solid rgba(255,255,255,0.05)' : 'none' }}>
                  <td style={{ padding: '12px 16px', fontFamily: 'monospace', color: '#fb923c' }}>{code}</td>
                  <td style={{ padding: '12px 16px', color: '#ffffff' }}>{meaning}</td>
                  <td style={{ padding: '12px 16px', color: '#9ca3af' }}>{action}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Error response shape</h2>
        <p style={{ color: '#9ca3af', marginBottom: '16px' }}>
          Error responses share a consistent shape so you can branch on <code style={{ color: '#fb923c' }}>message</code> without
          parsing HTML or plain text:
        </p>
        <CodeBlock
          language="json"
          code={JSON.stringify({ message: 'Invalid merchant ID', type: 'invalid_request' }, null, 2)}
        />
      </section>

      <section>
        <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>Making retries safe</h2>
        <p style={{ color: '#9ca3af', margin: 0 }}>
          <Link to="/api/orders#create-order" style={{ color: '#f97316' }}>Create Order</Link> and{' '}
          <Link to="/api/line-items#create-line-item" style={{ color: '#f97316' }}>Create Line Item</Link> don't accept a
          client-supplied idempotency key today - a retried POST after a timed-out request can create a duplicate. If you
          need to retry safely, check for an existing order/line item with matching contents before creating a new one,
          rather than retrying blindly.
        </p>
      </section>
    </div>
  );
}

export function Guides() {
  const { guide } = useParams<{ guide?: string }>();

  if (!guide) {
    return (
      <DocsLayout>
        <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
          <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
          <span>/</span>
          <span style={{ color: '#9ca3af' }}>Guides</span>
        </nav>
        <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>Guides</h1>
        <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
          Task-focused walkthroughs that tie multiple endpoints together.
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {Object.entries(guides).map(([key, value]) => {
            const Icon = icons[key];
            return (
              <Link
                key={key}
                to={`/guides/${key}`}
                style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '16px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px', textDecoration: 'none' }}
              >
                <div style={{ padding: '12px', borderRadius: '8px', background: 'rgba(255,255,255,0.05)' }}>
                  <Icon style={{ width: '24px', height: '24px', color: iconColors[key] }} />
                </div>
                <div style={{ flex: 1 }}>
                  <h3 style={{ fontSize: '18px', fontWeight: 500, color: '#ffffff', marginBottom: '4px' }}>{value.title}</h3>
                  <p style={{ fontSize: '14px', color: '#9ca3af', margin: 0 }}>{value.description}</p>
                </div>
              </Link>
            );
          })}
        </div>
      </DocsLayout>
    );
  }

  const content = guides[guide];
  if (!content) {
    return (
      <DocsLayout>
        <div style={{ textAlign: 'center', padding: '48px 0' }}>
          <h1 style={{ fontSize: '24px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>Guide not found</h1>
          <Link to="/guides" style={{ color: '#f97316', textDecoration: 'underline' }}>Back to Guides</Link>
        </div>
      </DocsLayout>
    );
  }

  const Icon = icons[guide];

  return (
    <DocsLayout>
      <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
        <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
        <span>/</span>
        <Link to="/guides" style={{ color: '#6b7280', textDecoration: 'none' }}>Guides</Link>
        <span>/</span>
        <span style={{ color: '#9ca3af' }}>{content.title}</span>
      </nav>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '24px' }}>
        <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(255,255,255,0.05)' }}>
          <Icon style={{ width: '32px', height: '32px', color: iconColors[guide] }} />
        </div>
        <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', margin: 0 }}>{content.title}</h1>
      </div>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '48px', lineHeight: 1.6 }}>{content.description}</p>
      <GuideBody guide={guide} />
    </DocsLayout>
  );
}
