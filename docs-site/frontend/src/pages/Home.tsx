import { Link } from 'react-router-dom';
import { Header } from '../components/Layout/Header';
import { 
  BookOpen, 
  Code2, 
  Zap, 
  ArrowRight, 
  ShoppingBag,
  Calendar,
  Bell
} from 'lucide-react';

export function Home() {
  return (
    <div className="min-h-screen bg-[#0f0f10]">
      <Header />
      
      {/* Hero Section */}
      <section className="pt-32 pb-20 px-6">
        <div className="max-w-4xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-om-orange/10 border border-om-orange/20 mb-6">
            <Zap className="w-4 h-4 text-om-orange" />
            <span className="text-sm text-om-orange">Clover POS Integration</span>
          </div>
          
          <h1 className="text-5xl md:text-6xl font-bold text-white mb-6">
            OrderMate
            <span className="text-om-orange"> Documentation</span>
          </h1>
          
          <p className="text-xl text-gray-400 mb-10 max-w-2xl mx-auto">
            Everything you need to integrate OrderMate with your Clover POS system. 
            Learn about features, explore the API, and build powerful integrations.
          </p>
          
          <div className="flex flex-wrap justify-center gap-4">
            <Link
              to="/getting-started"
              className="inline-flex items-center gap-2 px-6 py-3 bg-om-orange hover:bg-om-orange-hover text-white font-medium rounded-lg transition-colors"
            >
              Get Started
              <ArrowRight className="w-4 h-4" />
            </Link>
            <Link
              to="/api"
              className="inline-flex items-center gap-2 px-6 py-3 bg-white/10 hover:bg-white/20 text-white font-medium rounded-lg border border-white/20 transition-colors"
            >
              <Code2 className="w-4 h-4" />
              API Reference
            </Link>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-20 px-6 border-t border-white/5">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-3xl font-bold text-white text-center mb-12">
            Explore the Documentation
          </h2>
          
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            <Link
              to="/getting-started"
              className="group p-6 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all"
            >
              <BookOpen className="w-10 h-10 text-om-orange mb-4" />
              <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-om-orange transition-colors">
                Getting Started
              </h3>
              <p className="text-sm text-gray-400">
                Learn how to install and configure OrderMate on your Clover device.
              </p>
            </Link>

            <Link
              to="/features/orders"
              className="group p-6 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all"
            >
              <ShoppingBag className="w-10 h-10 text-emerald-400 mb-4" />
              <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-emerald-400 transition-colors">
                Order Management
              </h3>
              <p className="text-sm text-gray-400">
                Manage orders, track status, and handle customer requests efficiently.
              </p>
            </Link>

            <Link
              to="/features/calendar"
              className="group p-6 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all"
            >
              <Calendar className="w-10 h-10 text-blue-400 mb-4" />
              <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-blue-400 transition-colors">
                Calendar View
              </h3>
              <p className="text-sm text-gray-400">
                View scheduled orders in day, week, or month format with smart filters.
              </p>
            </Link>

            <Link
              to="/features/notifications"
              className="group p-6 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all"
            >
              <Bell className="w-10 h-10 text-amber-400 mb-4" />
              <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-amber-400 transition-colors">
                Notifications
              </h3>
              <p className="text-sm text-gray-400">
                Send SMS and email notifications to keep customers informed.
              </p>
            </Link>
          </div>
        </div>
      </section>

      {/* API Section */}
      <section className="py-20 px-6 bg-gradient-to-b from-transparent to-white/5">
        <div className="max-w-6xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div>
              <h2 className="text-3xl font-bold text-white mb-4">
                Powerful API Reference
              </h2>
              <p className="text-gray-400 mb-6">
                Interactive documentation with live examples. Test API endpoints 
                directly in your browser with our sandbox environment.
              </p>
              <ul className="space-y-3 mb-8">
                <li className="flex items-center gap-3 text-gray-300">
                  <span className="w-2 h-2 bg-emerald-400 rounded-full"></span>
                  Orders, Line Items, Customers, Payments
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <span className="w-2 h-2 bg-emerald-400 rounded-full"></span>
                  Webhooks for real-time notifications
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <span className="w-2 h-2 bg-emerald-400 rounded-full"></span>
                  Code examples in cURL, Python, Kotlin
                </li>
              </ul>
              <Link
                to="/api"
                className="inline-flex items-center gap-2 text-om-orange hover:text-om-orange-hover font-medium"
              >
                Explore API Reference
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
            
            <div className="bg-[#1a1a2e] rounded-xl border border-white/10 overflow-hidden">
              <div className="flex items-center gap-2 px-4 py-3 bg-white/5 border-b border-white/10">
                <span className="px-2 py-0.5 text-xs font-mono font-semibold rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  GET
                </span>
                <code className="text-sm text-gray-400">/v3/merchants/{'{mId}'}/orders</code>
              </div>
              <pre className="p-4 text-sm text-gray-300 overflow-x-auto">
{`{
  "elements": [
    {
      "id": "ABC123",
      "total": 2499,
      "paymentState": "PAID",
      "title": "Order #1001",
      "createdTime": 1699900000000
    }
  ]
}`}
              </pre>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 px-6 border-t border-white/10">
        <div className="max-w-6xl mx-auto flex flex-col md:flex-row justify-between items-center gap-4">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-gradient-to-br from-om-orange to-om-orange-dark rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">OM</span>
            </div>
            <span className="text-gray-400">OrderMate Documentation</span>
          </div>
          <div className="flex items-center gap-6 text-sm text-gray-500">
            <a href="https://getordermate.com" target="_blank" rel="noopener noreferrer" className="hover:text-white transition-colors">
              Website
            </a>
            <a href="https://github.com/11thandOrange/OrderMate" target="_blank" rel="noopener noreferrer" className="hover:text-white transition-colors">
              GitHub
            </a>
            <a href="mailto:support@11thandorange.com" className="hover:text-white transition-colors">
              Support
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
