import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiLayout } from '../../components/Layout/ApiLayout';
import { EndpointDoc, RequestBuilder } from '../../components/ApiReference';
import { endpoints } from '../../data/endpoints';

export function OrdersApi() {
  const orderEndpoints = endpoints.filter((e) => e.category === 'Orders');
  const [selectedEndpoint, setSelectedEndpoint] = useState(orderEndpoints[0]);

  return (
    <ApiLayout
      rightPanel={
        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-white">Try it out</h3>
          <RequestBuilder endpoint={selectedEndpoint} />
        </div>
      }
    >
      <div className="max-w-3xl">
        {/* Breadcrumb */}
        <div className="flex items-center gap-2 text-sm text-gray-500 mb-8">
          <Link to="/" className="hover:text-white">Docs</Link>
          <span>/</span>
          <Link to="/api" className="hover:text-white">API Reference</Link>
          <span>/</span>
          <span className="text-gray-300">Orders</span>
        </div>

        <h1 className="text-4xl font-bold text-white mb-4">Orders</h1>
        <p className="text-xl text-gray-400 mb-8">
          The Orders API allows you to create, read, update, and delete orders for a merchant.
          Orders contain line items, customer information, and payment details.
        </p>

        {/* Quick Links */}
        <div className="mb-12 p-4 bg-white/5 border border-white/10 rounded-xl">
          <h3 className="text-sm font-medium text-gray-400 uppercase tracking-wider mb-3">
            On this page
          </h3>
          <ul className="space-y-2">
            {orderEndpoints.map((endpoint) => (
              <li key={endpoint.id}>
                <a
                  href={`#${endpoint.id}`}
                  onClick={() => setSelectedEndpoint(endpoint)}
                  className="flex items-center gap-2 text-gray-300 hover:text-om-orange transition-colors"
                >
                  <span className={`px-1.5 py-0.5 text-xs font-mono font-semibold rounded ${
                    endpoint.method === 'GET' ? 'bg-emerald-500/20 text-emerald-400' :
                    endpoint.method === 'POST' ? 'bg-blue-500/20 text-blue-400' :
                    endpoint.method === 'PUT' ? 'bg-amber-500/20 text-amber-400' :
                    'bg-red-500/20 text-red-400'
                  }`}>
                    {endpoint.method}
                  </span>
                  <span>{endpoint.title}</span>
                </a>
              </li>
            ))}
          </ul>
        </div>

        {/* The Order Object */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-4">The Order Object</h2>
          <div className="space-y-4">
            <div className="p-4 bg-white/5 border border-white/10 rounded-lg">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-400 border-b border-white/10">
                    <th className="pb-3 font-medium">Attribute</th>
                    <th className="pb-3 font-medium">Type</th>
                    <th className="pb-3 font-medium">Description</th>
                  </tr>
                </thead>
                <tbody className="text-gray-300">
                  <tr className="border-b border-white/5">
                    <td className="py-3 font-mono text-white">id</td>
                    <td className="py-3 text-gray-400">string</td>
                    <td className="py-3">Unique identifier for the order</td>
                  </tr>
                  <tr className="border-b border-white/5">
                    <td className="py-3 font-mono text-white">currency</td>
                    <td className="py-3 text-gray-400">string</td>
                    <td className="py-3">Three-letter ISO currency code (e.g., USD)</td>
                  </tr>
                  <tr className="border-b border-white/5">
                    <td className="py-3 font-mono text-white">total</td>
                    <td className="py-3 text-gray-400">integer</td>
                    <td className="py-3">Total amount in cents</td>
                  </tr>
                  <tr className="border-b border-white/5">
                    <td className="py-3 font-mono text-white">paymentState</td>
                    <td className="py-3 text-gray-400">string</td>
                    <td className="py-3">OPEN, PAID, PARTIALLY_PAID, REFUNDED, etc.</td>
                  </tr>
                  <tr className="border-b border-white/5">
                    <td className="py-3 font-mono text-white">title</td>
                    <td className="py-3 text-gray-400">string</td>
                    <td className="py-3">Order title/name</td>
                  </tr>
                  <tr className="border-b border-white/5">
                    <td className="py-3 font-mono text-white">note</td>
                    <td className="py-3 text-gray-400">string</td>
                    <td className="py-3">Additional notes for the order</td>
                  </tr>
                  <tr className="border-b border-white/5">
                    <td className="py-3 font-mono text-white">state</td>
                    <td className="py-3 text-gray-400">string</td>
                    <td className="py-3">Order state: open, locked</td>
                  </tr>
                  <tr className="border-b border-white/5">
                    <td className="py-3 font-mono text-white">createdTime</td>
                    <td className="py-3 text-gray-400">integer</td>
                    <td className="py-3">Unix timestamp in milliseconds</td>
                  </tr>
                  <tr>
                    <td className="py-3 font-mono text-white">modifiedTime</td>
                    <td className="py-3 text-gray-400">integer</td>
                    <td className="py-3">Unix timestamp in milliseconds</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

        {/* Endpoints */}
        <div className="space-y-16">
          {orderEndpoints.map((endpoint) => (
            <div 
              key={endpoint.id} 
              onClick={() => setSelectedEndpoint(endpoint)}
              className="cursor-pointer"
            >
              <EndpointDoc endpoint={endpoint} />
              <hr className="border-white/10 mt-16" />
            </div>
          ))}
        </div>
      </div>
    </ApiLayout>
  );
}
