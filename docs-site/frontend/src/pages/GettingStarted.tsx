import { DocsLayout } from '../components/Layout/DocsLayout';
import { CodeBlock } from '../components/ApiReference/CodeBlock';
import { ArrowRight, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';

export function GettingStarted() {
  return (
    <DocsLayout>
      <div className="prose prose-invert max-w-none">
        {/* Breadcrumb */}
        <div className="flex items-center gap-2 text-sm text-gray-500 mb-8">
          <Link to="/" className="hover:text-white">Docs</Link>
          <span>/</span>
          <span className="text-gray-300">Getting Started</span>
        </div>

        <h1 className="text-4xl font-bold text-white mb-4">Getting Started</h1>
        <p className="text-xl text-gray-400 mb-8">
          Get OrderMate up and running on your Clover device in just a few minutes.
        </p>

        {/* Prerequisites */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-4">Prerequisites</h2>
          <ul className="space-y-3">
            <li className="flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-emerald-400 mt-0.5 shrink-0" />
              <span className="text-gray-300">A Clover merchant account</span>
            </li>
            <li className="flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-emerald-400 mt-0.5 shrink-0" />
              <span className="text-gray-300">Clover device (Station, Mini, Flex, or Mobile)</span>
            </li>
            <li className="flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-emerald-400 mt-0.5 shrink-0" />
              <span className="text-gray-300">Active internet connection</span>
            </li>
          </ul>
        </section>

        {/* Installation */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-4">Installation</h2>
          
          <div className="space-y-6">
            <div className="p-6 bg-white/5 border border-white/10 rounded-xl">
              <h3 className="text-lg font-medium text-white mb-3">
                1. Install from Clover App Market
              </h3>
              <p className="text-gray-400 mb-4">
                The easiest way to install OrderMate is through the Clover App Market.
              </p>
              <a
                href="https://www.clover.com/appmarket/apps/WWTF1AKT87VJ8"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 px-4 py-2 bg-om-orange hover:bg-om-orange-hover text-white font-medium rounded-lg transition-colors"
              >
                Open Clover App Market
                <ArrowRight className="w-4 h-4" />
              </a>
            </div>

            <div className="p-6 bg-white/5 border border-white/10 rounded-xl">
              <h3 className="text-lg font-medium text-white mb-3">
                2. Grant Permissions
              </h3>
              <p className="text-gray-400 mb-4">
                OrderMate requires the following permissions to function:
              </p>
              <ul className="space-y-2 text-gray-300">
                <li>• <strong>Orders</strong> - Read and write access to orders</li>
                <li>• <strong>Customers</strong> - Read and write access to customer data</li>
                <li>• <strong>Inventory</strong> - Read access to inventory items</li>
                <li>• <strong>Payments</strong> - Read access to payment information</li>
              </ul>
            </div>

            <div className="p-6 bg-white/5 border border-white/10 rounded-xl">
              <h3 className="text-lg font-medium text-white mb-3">
                3. Launch OrderMate
              </h3>
              <p className="text-gray-400">
                After installation, find OrderMate in your Clover app drawer and tap to launch. 
                The app will sync with your existing orders automatically.
              </p>
            </div>
          </div>
        </section>

        {/* Configuration */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-4">Initial Configuration</h2>
          <p className="text-gray-400 mb-6">
            After launching OrderMate for the first time, you'll be guided through the setup process:
          </p>

          <div className="space-y-4">
            <div className="flex gap-4">
              <div className="w-8 h-8 rounded-full bg-om-orange/20 text-om-orange flex items-center justify-center font-semibold shrink-0">
                1
              </div>
              <div>
                <h4 className="text-white font-medium mb-1">Set your notification preferences</h4>
                <p className="text-gray-400 text-sm">
                  Configure SMS and email notifications for order updates.
                </p>
              </div>
            </div>

            <div className="flex gap-4">
              <div className="w-8 h-8 rounded-full bg-om-orange/20 text-om-orange flex items-center justify-center font-semibold shrink-0">
                2
              </div>
              <div>
                <h4 className="text-white font-medium mb-1">Customize your widgets</h4>
                <p className="text-gray-400 text-sm">
                  Set up custom fields and widgets to capture additional order information.
                </p>
              </div>
            </div>

            <div className="flex gap-4">
              <div className="w-8 h-8 rounded-full bg-om-orange/20 text-om-orange flex items-center justify-center font-semibold shrink-0">
                3
              </div>
              <div>
                <h4 className="text-white font-medium mb-1">Configure calendar settings</h4>
                <p className="text-gray-400 text-sm">
                  Set your business hours and calendar view preferences.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* API Access */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold text-white mb-4">API Access</h2>
          <p className="text-gray-400 mb-4">
            To use the Clover API with OrderMate, you'll need to obtain an API token from your Clover developer account.
          </p>

          <CodeBlock
            code={`# Set your API credentials
export CLOVER_API_KEY="your_api_key_here"
export CLOVER_MERCHANT_ID="your_merchant_id"

# Test your connection
curl -H "Authorization: Bearer $CLOVER_API_KEY" \\
  "https://api.clover.com/v3/merchants/$CLOVER_MERCHANT_ID"`}
            language="bash"
            title="Test API Connection"
          />
        </section>

        {/* Next Steps */}
        <section className="p-6 bg-gradient-to-r from-om-orange/10 to-om-purple/10 border border-om-orange/20 rounded-xl">
          <h2 className="text-xl font-semibold text-white mb-4">Next Steps</h2>
          <div className="grid sm:grid-cols-2 gap-4">
            <Link
              to="/features/orders"
              className="p-4 bg-white/5 hover:bg-white/10 rounded-lg transition-colors"
            >
              <h3 className="text-white font-medium mb-1">Order Management →</h3>
              <p className="text-sm text-gray-400">Learn how to manage orders effectively</p>
            </Link>
            <Link
              to="/api"
              className="p-4 bg-white/5 hover:bg-white/10 rounded-lg transition-colors"
            >
              <h3 className="text-white font-medium mb-1">API Reference →</h3>
              <p className="text-sm text-gray-400">Explore the API documentation</p>
            </Link>
          </div>
        </section>
      </div>
    </DocsLayout>
  );
}
