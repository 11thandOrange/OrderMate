import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiLayout } from '../../components/Layout/ApiLayout';
import { EndpointDoc } from '../../components/ApiReference/EndpointDoc';
import { RequestBuilder } from '../../components/ApiReference/RequestBuilder';
import { getEndpointsByCategory } from '../../data/endpoints';
import type { Endpoint, HttpMethod } from '../../types/api';

const methodColors: Record<HttpMethod, string> = {
  GET: '#10b981',
  POST: '#3b82f6',
  PUT: '#f59e0b',
  DELETE: '#ef4444',
  PATCH: '#a78bfa',
};

interface ApiCategoryPageProps {
  categorySlug: string; // matches the id in data/endpoints.ts `categories`, e.g. 'line-items'
  title: string;
  description: string;
}

export function ApiCategoryPage({ categorySlug, title, description }: ApiCategoryPageProps) {
  const categoryEndpoints = getEndpointsByCategory(categorySlug);
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
        <span style={{ color: '#9ca3af' }}>{title}</span>
      </nav>

      {/* Page Title */}
      <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>{title}</h1>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>{description}</p>

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
    </ApiLayout>
  );
}
