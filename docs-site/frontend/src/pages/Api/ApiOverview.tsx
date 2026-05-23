import { Link } from 'react-router-dom';
import { ApiLayout } from '../../components/Layout/ApiLayout';
import { CodeBlock } from '../../components/ApiReference/CodeBlock';
import { 
  ShoppingBag, 
  Users, 
  CreditCard, 
  Webhook, 
  List,
  ArrowRight,
  Key,
  Globe
} from 'lucide-react';

export function ApiOverview() {
  const categories = [
    {
      title: 'Orders',
      description: 'Create, read, update, and delete orders',
      icon: ShoppingBag,
      href: '/api/orders',
      color: 'text-emerald-400',
      bgColor: 'bg-emerald-400/10',
    },
    {
      title: 'Line Items',
      description: 'Manage items within orders',
      icon: List,
      href: '/api/line-items',
      color: 'text-blue-400',
      bgColor: 'bg-blue-400/10',
    },
    {
      title: 'Customers',
      description: 'Access customer information',
      icon: Users,
      href: '/api/customers',
      color: 'text-purple-400',
      bgColor: 'bg-purple-400/10',
    },
    {
      title: 'Payments',
      description: 'View payment transactions',
      icon: CreditCard,
      href: '/api/payments',
      color: 'text-amber-400',
      bgColor: 'bg-amber-400/10',
    },
    {
      title: 'Webhooks',
      description: 'Receive real-time notifications',
      icon: Webhook,
      href: '/api/webhooks',
      color: 'text-red-400',
      bgColor: 'bg-red-400/10',
    },
  ];

  return (
    <ApiLayout>
      <div className="max-w-3xl">
        {/* Breadcrumb */}
        <div className="flex items-center gap-2 text-sm text-gray-500 mb-8">
          <Link to="/" className="hover:text-white">Docs</Link>
          <span>/</span>
          <span className="text-gray-300">API Reference</span>
        </div>

        <h1 className="text-4xl font-bold text-white mb-4">API Reference</h1>
        <p className="text-xl text-gray-400 mb-8">
          The Clover API allows you to programmatically access and manage merchant data 
          including orders, customers, payments, and more.
        </p>

        {/* Base URL */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-4 flex items-center gap-2">
            <Globe className="w-6 h-6 text-om-orange" />
            Base URL
          </h2>
          <div className="p-4 bg-white/5 border border-white/10 rounded-lg">
            <code className="text-lg text-emerald-400">https://api.clover.com/v3</code>
          </div>
          <p className="text-sm text-gray-500 mt-2">
            For sandbox/development, use: <code className="text-gray-400">https://sandbox.dev.clover.com/v3</code>
          </p>
        </section>

        {/* Authentication */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-4 flex items-center gap-2">
            <Key className="w-6 h-6 text-om-orange" />
            Authentication
          </h2>
          <p className="text-gray-400 mb-4">
            All API requests require authentication using an OAuth 2.0 Bearer token. 
            Include your API token in the Authorization header:
          </p>
          <CodeBlock
            code={`curl -H "Authorization: Bearer YOUR_API_TOKEN" \\
  https://api.clover.com/v3/merchants/{mId}/orders`}
            language="bash"
            title="Authorization Header"
          />
        </section>

        {/* Rate Limits */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-4">Rate Limits</h2>
          <div className="p-4 bg-white/5 border border-white/10 rounded-lg">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-400">
                  <th className="pb-3">Tier</th>
                  <th className="pb-3">Limit</th>
                  <th className="pb-3">Window</th>
                </tr>
              </thead>
              <tbody className="text-gray-300">
                <tr className="border-t border-white/5">
                  <td className="py-3">Standard</td>
                  <td className="py-3">16 requests</td>
                  <td className="py-3">Per second</td>
                </tr>
                <tr className="border-t border-white/5">
                  <td className="py-3">Burst</td>
                  <td className="py-3">50 requests</td>
                  <td className="py-3">Per second (short bursts)</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        {/* API Categories */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-6">Endpoints</h2>
          <div className="grid gap-4">
            {categories.map((category) => (
              <Link
                key={category.href}
                to={category.href}
                className="group flex items-center gap-4 p-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all"
              >
                <div className={`p-3 rounded-lg ${category.bgColor}`}>
                  <category.icon className={`w-6 h-6 ${category.color}`} />
                </div>
                <div className="flex-1">
                  <h3 className="text-lg font-medium text-white group-hover:text-om-orange transition-colors">
                    {category.title}
                  </h3>
                  <p className="text-sm text-gray-400">{category.description}</p>
                </div>
                <ArrowRight className="w-5 h-5 text-gray-500 group-hover:text-om-orange transition-colors" />
              </Link>
            ))}
          </div>
        </section>

        {/* Response Format */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-4">Response Format</h2>
          <p className="text-gray-400 mb-4">
            All API responses are returned in JSON format. List endpoints return an 
            object with an <code className="text-gray-300">elements</code> array:
          </p>
          <CodeBlock
            code={`{
  "elements": [
    {
      "id": "ABC123",
      "total": 2499,
      "paymentState": "PAID"
    }
  ],
  "href": "https://api.clover.com/v3/merchants/{mId}/orders?limit=100"
}`}
            language="json"
            title="List Response Format"
          />
        </section>

        {/* Error Handling */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-4">Error Handling</h2>
          <p className="text-gray-400 mb-4">
            The API uses standard HTTP status codes to indicate success or failure:
          </p>
          <div className="space-y-2">
            <div className="flex items-center gap-3 p-3 bg-white/5 rounded-lg">
              <span className="px-2 py-0.5 text-xs font-medium rounded bg-emerald-500/20 text-emerald-400">200</span>
              <span className="text-gray-300">OK - Request succeeded</span>
            </div>
            <div className="flex items-center gap-3 p-3 bg-white/5 rounded-lg">
              <span className="px-2 py-0.5 text-xs font-medium rounded bg-blue-500/20 text-blue-400">201</span>
              <span className="text-gray-300">Created - Resource created successfully</span>
            </div>
            <div className="flex items-center gap-3 p-3 bg-white/5 rounded-lg">
              <span className="px-2 py-0.5 text-xs font-medium rounded bg-amber-500/20 text-amber-400">400</span>
              <span className="text-gray-300">Bad Request - Invalid parameters</span>
            </div>
            <div className="flex items-center gap-3 p-3 bg-white/5 rounded-lg">
              <span className="px-2 py-0.5 text-xs font-medium rounded bg-amber-500/20 text-amber-400">401</span>
              <span className="text-gray-300">Unauthorized - Invalid or missing API token</span>
            </div>
            <div className="flex items-center gap-3 p-3 bg-white/5 rounded-lg">
              <span className="px-2 py-0.5 text-xs font-medium rounded bg-red-500/20 text-red-400">404</span>
              <span className="text-gray-300">Not Found - Resource doesn't exist</span>
            </div>
            <div className="flex items-center gap-3 p-3 bg-white/5 rounded-lg">
              <span className="px-2 py-0.5 text-xs font-medium rounded bg-red-500/20 text-red-400">429</span>
              <span className="text-gray-300">Too Many Requests - Rate limit exceeded</span>
            </div>
          </div>
        </section>
      </div>
    </ApiLayout>
  );
}
