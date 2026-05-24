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
  GET: 'bg-emerald-500',
  POST: 'bg-blue-500',
  PUT: 'bg-amber-500',
  DELETE: 'bg-red-500',
};

function TryItOutPanel() {
  return (
    <>
      <h3 className="text-lg font-semibold text-white mb-6">Try it out</h3>
      
      {/* Endpoint URL */}
      <div className="mb-6">
        <div className="flex items-center gap-2 px-3 py-2 rounded-lg border border-white/10 font-mono text-sm" style={{ background: 'rgba(255,255,255,0.05)' }}>
          <span className="px-1.5 py-0.5 rounded text-xs font-bold bg-emerald-500 text-white">GET</span>
          <span className="text-gray-400 truncate">/v3/merchants/{'{mId}'}/orders</span>
        </div>
      </div>

      {/* Input Fields */}
      <div className="space-y-4 mb-6">
        <div>
          <label className="block text-sm font-medium text-gray-400 mb-2">API Key</label>
          <input 
            type="text" 
            placeholder="Enter your Clover API key" 
            className="w-full px-3 py-2 rounded-lg text-sm text-white placeholder-gray-500 border border-white/10 focus:outline-none focus:ring-2 focus:ring-orange-500/50"
            style={{ background: 'rgba(255,255,255,0.05)' }}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-400 mb-2">Merchant ID</label>
          <input 
            type="text" 
            placeholder="Enter merchant ID" 
            className="w-full px-3 py-2 rounded-lg text-sm text-white placeholder-gray-500 border border-white/10 focus:outline-none focus:ring-2 focus:ring-orange-500/50"
            style={{ background: 'rgba(255,255,255,0.05)' }}
          />
        </div>
      </div>

      {/* Try it button */}
      <button className="w-full py-2.5 px-4 bg-orange-500 hover:bg-orange-600 text-white font-medium rounded-lg transition flex items-center justify-center gap-2">
        <Play className="w-4 h-4" />
        Try it
      </button>

      {/* Code Examples */}
      <div className="mt-8">
        <h4 className="text-sm font-medium text-gray-400 mb-3">Code Examples</h4>
        <div className="flex gap-2 mb-4">
          <button className="px-3 py-1.5 text-sm text-white rounded-lg" style={{ background: 'rgba(255,255,255,0.1)' }}>cURL</button>
          <button className="px-3 py-1.5 text-sm text-gray-400 hover:text-white rounded-lg">Python</button>
        </div>
        <div className="p-4 rounded-xl text-xs font-mono" style={{ background: '#1e2028' }}>
          <pre className="text-gray-300"><code><span className="text-purple-400">curl</span> <span className="text-emerald-400">"https://api.clover.com/v3/merchants/{'{mId}'}/orders"</span> \{'\n'}  -H <span className="text-emerald-400">"Authorization: Bearer TOKEN"</span></code></pre>
        </div>
      </div>
    </>
  );
}

export function OrdersApi() {
  return (
    <ApiLayout rightPanel={<TryItOutPanel />}>
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-gray-500 mb-6">
        <Link to="/" className="hover:text-white transition">Docs</Link>
        <span>/</span>
        <Link to="/api" className="hover:text-white transition">API Reference</Link>
        <span>/</span>
        <span className="text-gray-400">Orders</span>
      </nav>

      {/* Page Title */}
      <h1 className="text-4xl font-bold text-white mb-4">Orders</h1>
      <p className="text-lg text-gray-400 mb-10 leading-relaxed">
        The Orders API allows you to create, read, update, and delete orders for a merchant.
        Orders contain line items, customer information, and payment details.
      </p>

      {/* On This Page */}
      <div className="mb-10 p-4 border border-white/10 rounded-xl" style={{ background: 'rgba(255,255,255,0.05)' }}>
        <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-3">On this page</h3>
        <div className="grid grid-cols-2 gap-2">
          {quickLinks.map((link) => (
            <a 
              key={link.href}
              href={link.href} 
              className="flex items-center gap-2 px-3 py-2 text-sm rounded-lg text-gray-400 hover:text-white transition"
              style={{ background: 'transparent' }}
              onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.05)'}
              onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
            >
              <span className={`text-xs px-1.5 py-0.5 rounded text-white font-medium ${methodColors[link.method]}`}>
                {link.method}
              </span>
              <span>{link.title}</span>
            </a>
          ))}
        </div>
      </div>

      {/* The Order Object */}
      <section className="mb-12">
        <h2 className="text-2xl font-semibold text-white mb-4">The Order Object</h2>
        <div className="overflow-hidden rounded-xl border border-white/10">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10" style={{ background: 'rgba(255,255,255,0.05)' }}>
                <th className="text-left px-4 py-3 font-medium text-gray-400">Attribute</th>
                <th className="text-left px-4 py-3 font-medium text-gray-400">Type</th>
                <th className="text-left px-4 py-3 font-medium text-gray-400">Description</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {orderAttributes.map((attr) => (
                <tr key={attr.name}>
                  <td className="px-4 py-3 font-mono text-orange-400">{attr.name}</td>
                  <td className="px-4 py-3 text-gray-400">{attr.type}</td>
                  <td className="px-4 py-3 text-gray-400">{attr.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* List Orders Endpoint */}
      <section id="list-orders" className="mb-12 scroll-mt-20">
        <div className="flex items-center gap-3 mb-4">
          <span className="px-2 py-1 rounded text-xs font-bold bg-emerald-500 text-white">GET</span>
          <code className="text-sm text-gray-400 font-mono">/v3/merchants/{'{mId}'}/orders</code>
        </div>
        <h2 className="text-2xl font-semibold text-white mb-4">List Orders</h2>
        <p className="text-gray-400 mb-6">
          Retrieves a list of orders for the specified merchant.
        </p>

        {/* Parameters */}
        <div className="mb-6">
          <h3 className="text-lg font-semibold text-white mb-4">Parameters</h3>
          <div className="space-y-3">
            <div className="p-4 rounded-lg border border-white/10" style={{ background: 'rgba(255,255,255,0.05)' }}>
              <div className="flex items-center gap-2 mb-1">
                <code className="text-orange-400 font-mono">mId</code>
                <span className="text-xs text-gray-500">string</span>
                <span className="text-xs px-1.5 py-0.5 rounded" style={{ background: 'rgba(239, 68, 68, 0.2)', color: '#f87171' }}>required</span>
              </div>
              <p className="text-sm text-gray-400">Unique identifier of the merchant</p>
            </div>
            <div className="p-4 rounded-lg border border-white/10" style={{ background: 'rgba(255,255,255,0.05)' }}>
              <div className="flex items-center gap-2 mb-1">
                <code className="text-orange-400 font-mono">limit</code>
                <span className="text-xs text-gray-500">integer</span>
              </div>
              <p className="text-sm text-gray-400">Maximum results to return (default: 100)</p>
            </div>
          </div>
        </div>

        {/* Response */}
        <div className="mb-6">
          <h3 className="text-lg font-semibold text-white mb-4">Response</h3>
          <div className="rounded-xl overflow-hidden" style={{ background: '#1e2028' }}>
            <div className="flex items-center justify-between px-4 py-2 border-b border-white/10" style={{ background: 'rgba(255,255,255,0.05)' }}>
              <span className="text-xs text-gray-500">Example Response</span>
              <button className="text-gray-500 hover:text-white">
                <Copy className="w-4 h-4" />
              </button>
            </div>
            <pre className="p-4 overflow-x-auto text-sm"><code className="text-gray-300">{`{
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
