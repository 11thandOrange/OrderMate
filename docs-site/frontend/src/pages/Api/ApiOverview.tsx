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
  GET: 'bg-emerald-500',
  POST: 'bg-blue-500',
  PUT: 'bg-amber-500',
  DELETE: 'bg-red-500',
};

export function ApiOverview() {
  return (
    <ApiLayout>
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-gray-500 mb-6">
        <Link to="/" className="hover:text-white transition">Docs</Link>
        <span>/</span>
        <span className="text-gray-400">API Reference</span>
      </nav>

      {/* Page Title */}
      <h1 className="text-4xl font-bold text-white mb-4">API Reference</h1>
      <p className="text-lg text-gray-400 mb-10 leading-relaxed">
        The Clover API allows you to programmatically access and manage merchant data 
        including orders, customers, payments, and more.
      </p>

      {/* On This Page / Quick Links */}
      <div className="mb-10 p-4 border border-white/10 rounded-xl" style={{ background: 'rgba(255,255,255,0.05)' }}>
        <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-3">Quick Links</h3>
        <div className="grid grid-cols-2 gap-2">
          {quickLinks.map((link) => (
            <Link
              key={link.href}
              to={link.href}
              className="flex items-center gap-2 px-3 py-2 text-sm rounded-lg text-gray-400 hover:text-white hover:bg-white/5 transition"
            >
              <span className={`text-xs px-1.5 py-0.5 rounded text-white font-medium ${methodColors[link.method]}`}>
                {link.method}
              </span>
              <span>{link.title}</span>
            </Link>
          ))}
        </div>
      </div>

      {/* Base URL */}
      <section className="mb-12">
        <h2 className="text-2xl font-semibold text-white mb-4">Base URL</h2>
        <div className="overflow-hidden rounded-xl border border-white/10">
          <div className="p-4" style={{ background: 'rgba(255,255,255,0.05)' }}>
            <code className="text-lg text-emerald-400">https://api.clover.com/v3</code>
          </div>
        </div>
        <p className="text-sm text-gray-500 mt-3">
          For sandbox/development, use: <code className="text-gray-400">https://sandbox.dev.clover.com/v3</code>
        </p>
      </section>

      {/* Authentication */}
      <section className="mb-12">
        <h2 className="text-2xl font-semibold text-white mb-4">Authentication</h2>
        <p className="text-gray-400 mb-4">
          All API requests require authentication using an OAuth 2.0 Bearer token. 
          Include your API token in the Authorization header:
        </p>
        <div className="rounded-xl overflow-hidden" style={{ background: '#1e2028' }}>
          <div className="flex items-center justify-between px-4 py-2 border-b border-white/10" style={{ background: 'rgba(255,255,255,0.05)' }}>
            <span className="text-xs text-gray-500">Authorization Header</span>
            <button className="text-gray-500 hover:text-white">
              <Copy className="w-4 h-4" />
            </button>
          </div>
          <pre className="p-4 overflow-x-auto text-sm"><code className="text-gray-300">{`curl -H "Authorization: Bearer YOUR_API_TOKEN" \\
  https://api.clover.com/v3/merchants/{mId}/orders`}</code></pre>
        </div>
      </section>

      {/* Endpoints */}
      <section className="mb-12">
        <h2 className="text-2xl font-semibold text-white mb-4">Endpoints</h2>
        <div className="overflow-hidden rounded-xl border border-white/10">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/10" style={{ background: 'rgba(255,255,255,0.05)' }}>
                <th className="text-left px-4 py-3 font-medium text-gray-400">Resource</th>
                <th className="text-left px-4 py-3 font-medium text-gray-400">Description</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              <tr>
                <td className="px-4 py-3">
                  <Link to="/api/orders" className="text-orange-400 hover:text-orange-300 font-medium">Orders</Link>
                </td>
                <td className="px-4 py-3 text-gray-400">Create, read, update, and delete orders</td>
              </tr>
              <tr>
                <td className="px-4 py-3">
                  <Link to="/api/line-items" className="text-orange-400 hover:text-orange-300 font-medium">Line Items</Link>
                </td>
                <td className="px-4 py-3 text-gray-400">Manage items within orders</td>
              </tr>
              <tr>
                <td className="px-4 py-3">
                  <Link to="/api/customers" className="text-orange-400 hover:text-orange-300 font-medium">Customers</Link>
                </td>
                <td className="px-4 py-3 text-gray-400">Access customer information</td>
              </tr>
              <tr>
                <td className="px-4 py-3">
                  <Link to="/api/payments" className="text-orange-400 hover:text-orange-300 font-medium">Payments</Link>
                </td>
                <td className="px-4 py-3 text-gray-400">View payment transactions</td>
              </tr>
              <tr>
                <td className="px-4 py-3">
                  <Link to="/api/webhooks" className="text-orange-400 hover:text-orange-300 font-medium">Webhooks</Link>
                </td>
                <td className="px-4 py-3 text-gray-400">Receive real-time notifications</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      {/* Response Format */}
      <section className="mb-12">
        <h2 className="text-2xl font-semibold text-white mb-4">Response Format</h2>
        <p className="text-gray-400 mb-4">
          All API responses are returned in JSON format. List endpoints return an 
          object with an <code className="text-orange-400">elements</code> array:
        </p>
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
      </section>
    </ApiLayout>
  );
}
