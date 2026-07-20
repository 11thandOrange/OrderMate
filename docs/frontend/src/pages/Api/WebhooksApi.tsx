import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiLayout } from '../../components/Layout/ApiLayout';
import { EndpointDoc } from '../../components/ApiReference/EndpointDoc';
import { RequestBuilder } from '../../components/ApiReference/RequestBuilder';
import { CodeBlock } from '../../components/ApiReference/CodeBlock';
import { getEndpointsByCategory, webhookSignatureVerification, webhookEventPayloads } from '../../data/endpoints';
import type { Endpoint, HttpMethod } from '../../types/api';

const methodColors: Record<HttpMethod, string> = {
  GET: '#10b981',
  POST: '#3b82f6',
  PUT: '#f59e0b',
  DELETE: '#ef4444',
  PATCH: '#a78bfa',
};

export function WebhooksApi() {
  const categoryEndpoints = getEndpointsByCategory('webhooks');
  const [activeEndpointId, setActiveEndpointId] = useState<string | undefined>(categoryEndpoints[0]?.id);
  const activeEndpoint: Endpoint | undefined = categoryEndpoints.find((e) => e.id === activeEndpointId);

  return (
    <ApiLayout rightPanel={activeEndpoint ? <RequestBuilder endpoint={activeEndpoint} /> : undefined}>
      {/* Breadcrumb */}
      <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
        <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
        <span>/</span>
        <Link to="/api" style={{ color: '#6b7280', textDecoration: 'none' }}>API Reference</Link>
        <span>/</span>
        <span style={{ color: '#9ca3af' }}>Webhooks</span>
      </nav>

      {/* Page Title */}
      <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>Webhooks</h1>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
        Subscribe to real-time notifications when orders, payments, or other resources change, instead of polling the API.
      </p>

      {/* On This Page */}
      <div style={{ marginBottom: '40px', padding: '16px', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px', background: 'rgba(255,255,255,0.05)' }}>
        <h3 style={{ fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#6b7280', marginBottom: '12px' }}>On this page</h3>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
          {categoryEndpoints.map((ep) => (
            <a
              key={ep.id}
              href={`#${ep.id}`}
              onClick={() => setActiveEndpointId(ep.id)}
              style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px', fontSize: '14px', borderRadius: '8px', color: '#9ca3af', textDecoration: 'none' }}
            >
              <span style={{ fontSize: '12px', padding: '2px 6px', borderRadius: '4px', color: '#ffffff', fontWeight: 500, background: methodColors[ep.method] }}>
                {ep.method}
              </span>
              <span>{ep.title}</span>
            </a>
          ))}
          <a href="#signature-verification" style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px', fontSize: '14px', borderRadius: '8px', color: '#9ca3af', textDecoration: 'none' }}>
            <span style={{ fontSize: '12px', padding: '2px 6px', borderRadius: '4px', color: '#ffffff', fontWeight: 500, background: '#6b7280' }}>DOCS</span>
            <span>Verifying signatures</span>
          </a>
          <a href="#event-payloads" style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px', fontSize: '14px', borderRadius: '8px', color: '#9ca3af', textDecoration: 'none' }}>
            <span style={{ fontSize: '12px', padding: '2px 6px', borderRadius: '4px', color: '#ffffff', fontWeight: 500, background: '#6b7280' }}>DOCS</span>
            <span>Event payloads</span>
          </a>
        </div>
      </div>

      {/* Endpoints */}
      <div className="space-y-16">
        {categoryEndpoints.map((ep) => (
          <div key={ep.id} onMouseEnter={() => setActiveEndpointId(ep.id)}>
            <EndpointDoc endpoint={ep} />
          </div>
        ))}
      </div>

      {/* Signature verification */}
      <section id="signature-verification" className="mt-16 scroll-mt-24">
        <h2 className="text-2xl font-bold text-white mb-4">Verifying webhook signatures</h2>
        <p className="text-gray-400 mb-4">
          Every webhook request includes an <code className="text-orange-400">{webhookSignatureVerification.header}</code> header,
          an {webhookSignatureVerification.algorithm} digest of the raw request body keyed with the <code className="text-orange-400">secret</code> returned
          once from <a href="#create-webhook" className="text-orange-400 underline">Create Webhook</a> (the <code className="text-orange-400">whsec_...</code> value).
          {' '}{webhookSignatureVerification.description}
        </p>
        <CodeBlock code={webhookSignatureVerification.verifyExample} language="javascript" title="Verify a webhook signature (Node.js)" />
      </section>

      {/* Event payloads */}
      <section id="event-payloads" className="mt-16 scroll-mt-24">
        <h2 className="text-2xl font-bold text-white mb-4">Event payloads</h2>
        <p className="text-gray-400 mb-4">
          The request body delivered to your webhook URL is small - it identifies what changed, not the full object.
          Fetch the full resource (e.g. <a href="/api/orders" className="text-orange-400 underline">Get Order</a>) using <code className="text-orange-400">objectId</code> if you need more than the event type and timestamp.
        </p>
        <div className="space-y-6">
          {Object.entries(webhookEventPayloads).map(([event, payload]) => (
            <div key={event}>
              <h3 className="text-sm font-medium text-gray-400 mb-2 font-mono">{event}</h3>
              <CodeBlock code={JSON.stringify(payload, null, 2)} language="json" />
            </div>
          ))}
        </div>
      </section>
    </ApiLayout>
  );
}
