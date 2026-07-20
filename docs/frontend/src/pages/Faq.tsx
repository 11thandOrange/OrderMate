import { Link } from 'react-router-dom';
import { DocsLayout } from '../components/Layout/DocsLayout';

const faqs: { question: string; answer: React.ReactNode }[] = [
  {
    question: 'Is there a sandbox environment for testing?',
    answer: (
      <>
        Yes - point requests at <code style={{ color: '#fb923c' }}>https://sandbox.dev.clover.com/v3</code> instead
        of <code style={{ color: '#fb923c' }}>https://api.clover.com/v3</code> while developing. See{' '}
        <Link to="/api" style={{ color: '#f97316' }}>API Reference</Link>.
      </>
    ),
  },
  {
    question: 'Can I test the API without a real Clover account?',
    answer: 'Yes - the "Try it" panel on each endpoint page returns realistic mock data when you don\'t have live credentials configured, so you can see request/response shapes before connecting a real merchant.',
  },
  {
    question: "What happens to an order's payments if I delete it?",
    answer: (
      <>
        You can't - <Link to="/api/orders#delete-order" style={{ color: '#f97316' }}>Delete Order</Link> refuses to delete
        an order that has payments attached. Void or refund the payment first via{' '}
        <Link to="/api/payments#void-payment" style={{ color: '#f97316' }}>Void Payment</Link> or{' '}
        <Link to="/api/payments#refund-payment" style={{ color: '#f97316' }}>Refund Payment</Link>.
      </>
    ),
  },
  {
    question: 'When should I void a payment vs. refund it?',
    answer: 'Void before the payment settles (typically same-day); refund after it has settled. Voiding a settled payment will fail - use a refund instead.',
  },
  {
    question: 'How do I know which webhook events are supported?',
    answer: (
      <>
        See <Link to="/api/webhooks#event-payloads" style={{ color: '#f97316' }}>Event payloads</Link> on the Webhooks
        reference page for the full list with example payloads.
      </>
    ),
  },
  {
    question: 'My webhook signature check is failing - what should I check first?',
    answer: 'Verify the HMAC over the raw, unparsed request body - not a re-serialized version of the parsed JSON, which can produce a different byte sequence (key ordering, whitespace) and a mismatched signature. See Setting up Webhooks in Guides.',
  },
  {
    question: 'Does OrderMate work on all Clover devices?',
    answer: 'Yes - Station, Mini, Flex, and Mobile are all supported. See Getting Started for installation.',
  },
];

export function Faq() {
  return (
    <DocsLayout>
      <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
        <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
        <span>/</span>
        <span style={{ color: '#9ca3af' }}>FAQ</span>
      </nav>
      <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>Frequently Asked Questions</h1>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
        Common questions about installing OrderMate and using the API.
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {faqs.map((faq) => (
          <div key={faq.question} style={{ padding: '20px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#ffffff', marginBottom: '8px' }}>{faq.question}</h3>
            <p style={{ fontSize: '14px', color: '#9ca3af', margin: 0, lineHeight: 1.6 }}>{faq.answer}</p>
          </div>
        ))}
      </div>
    </DocsLayout>
  );
}
