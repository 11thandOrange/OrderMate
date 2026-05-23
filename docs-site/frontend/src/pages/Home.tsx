import { Link } from 'react-router-dom';
import { Header } from '../components/Layout/Header';
import { 
  BookOpen, 
  Code2, 
  Zap, 
  ArrowRight, 
  ShoppingBag,
  Calendar,
  Bell,
  ChevronRight
} from 'lucide-react';

export function Home() {
  return (
    <div className="min-h-screen bg-[#0a0a0b]">
      <Header />
      
      {/* Hero Section */}
      <section className="relative pt-24 pb-16 px-6 overflow-hidden">
        {/* Background gradient */}
        <div className="absolute inset-0 bg-gradient-to-b from-om-orange/5 via-transparent to-transparent pointer-events-none" />
        <div className="absolute top-20 left-1/2 -translate-x-1/2 w-[600px] h-[300px] bg-om-orange/8 rounded-full blur-[100px] pointer-events-none" />
        
        <div className="relative max-w-4xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-om-orange/10 border border-om-orange/20 mb-6">
            <Zap className="w-4 h-4 text-om-orange" />
            <span className="text-sm text-om-orange font-medium">Clover POS Integration</span>
          </div>
          
          <h1 className="text-4xl sm:text-5xl md:text-6xl font-bold mb-6 leading-[1.1]">
            <span className="text-white">OrderMate</span>
            <br />
            <span className="bg-gradient-to-r from-om-orange via-orange-400 to-amber-500 bg-clip-text text-transparent">
              Documentation
            </span>
          </h1>
          
          <p className="text-lg text-gray-400 mb-10 max-w-2xl mx-auto leading-relaxed">
            Everything you need to integrate OrderMate with your Clover POS system. 
            Learn about features, explore the API, and build powerful integrations.
          </p>
          
          <div className="flex flex-wrap justify-center gap-4">
            <Link
              to="/getting-started"
              className="inline-flex items-center gap-2 px-6 py-3 bg-om-orange hover:bg-om-orange-hover text-white font-semibold rounded-lg transition-all shadow-lg shadow-om-orange/25 hover:shadow-om-orange/40"
            >
              Get Started
              <ArrowRight className="w-4 h-4" />
            </Link>
            <Link
              to="/api"
              className="inline-flex items-center gap-2 px-6 py-3 bg-white/5 hover:bg-white/10 text-white font-medium rounded-lg border border-white/10 hover:border-white/20 transition-all"
            >
              <Code2 className="w-4 h-4" />
              API Reference
            </Link>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-20 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-14">
            <h2 className="text-3xl font-bold text-white mb-4">
              Explore the Documentation
            </h2>
            <p className="text-gray-400 max-w-2xl mx-auto">
              Comprehensive guides and references to help you get the most out of OrderMate
            </p>
          </div>
          
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
            <Link
              to="/getting-started"
              className="group relative p-6 bg-gradient-to-br from-white/[0.07] to-white/[0.03] hover:from-white/[0.1] hover:to-white/[0.05] border border-white/10 hover:border-om-orange/30 rounded-2xl transition-all duration-300"
            >
              <div className="w-12 h-12 mb-5 rounded-xl bg-om-orange/10 flex items-center justify-center group-hover:bg-om-orange/20 transition-colors">
                <BookOpen className="w-6 h-6 text-om-orange" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-om-orange transition-colors">
                Getting Started
              </h3>
              <p className="text-sm text-gray-400 leading-relaxed mb-4">
                Learn how to install and configure OrderMate on your Clover device.
              </p>
              <span className="inline-flex items-center text-sm text-om-orange opacity-0 group-hover:opacity-100 transition-opacity">
                Read more <ChevronRight className="w-4 h-4 ml-1" />
              </span>
            </Link>

            <Link
              to="/features"
              className="group relative p-6 bg-gradient-to-br from-white/[0.07] to-white/[0.03] hover:from-white/[0.1] hover:to-white/[0.05] border border-white/10 hover:border-emerald-500/30 rounded-2xl transition-all duration-300"
            >
              <div className="w-12 h-12 mb-5 rounded-xl bg-emerald-500/10 flex items-center justify-center group-hover:bg-emerald-500/20 transition-colors">
                <ShoppingBag className="w-6 h-6 text-emerald-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-emerald-400 transition-colors">
                Order Management
              </h3>
              <p className="text-sm text-gray-400 leading-relaxed mb-4">
                Manage orders, track status, and handle customer requests efficiently.
              </p>
              <span className="inline-flex items-center text-sm text-emerald-400 opacity-0 group-hover:opacity-100 transition-opacity">
                Read more <ChevronRight className="w-4 h-4 ml-1" />
              </span>
            </Link>

            <Link
              to="/features"
              className="group relative p-6 bg-gradient-to-br from-white/[0.07] to-white/[0.03] hover:from-white/[0.1] hover:to-white/[0.05] border border-white/10 hover:border-blue-500/30 rounded-2xl transition-all duration-300"
            >
              <div className="w-12 h-12 mb-5 rounded-xl bg-blue-500/10 flex items-center justify-center group-hover:bg-blue-500/20 transition-colors">
                <Calendar className="w-6 h-6 text-blue-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-blue-400 transition-colors">
                Calendar View
              </h3>
              <p className="text-sm text-gray-400 leading-relaxed mb-4">
                View scheduled orders in day, week, or month format with smart filters.
              </p>
              <span className="inline-flex items-center text-sm text-blue-400 opacity-0 group-hover:opacity-100 transition-opacity">
                Read more <ChevronRight className="w-4 h-4 ml-1" />
              </span>
            </Link>

            <Link
              to="/features"
              className="group relative p-6 bg-gradient-to-br from-white/[0.07] to-white/[0.03] hover:from-white/[0.1] hover:to-white/[0.05] border border-white/10 hover:border-amber-500/30 rounded-2xl transition-all duration-300"
            >
              <div className="w-12 h-12 mb-5 rounded-xl bg-amber-500/10 flex items-center justify-center group-hover:bg-amber-500/20 transition-colors">
                <Bell className="w-6 h-6 text-amber-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-amber-400 transition-colors">
                Notifications
              </h3>
              <p className="text-sm text-gray-400 leading-relaxed mb-4">
                Send SMS and email notifications to keep customers informed.
              </p>
              <span className="inline-flex items-center text-sm text-amber-400 opacity-0 group-hover:opacity-100 transition-opacity">
                Read more <ChevronRight className="w-4 h-4 ml-1" />
              </span>
            </Link>
          </div>
        </div>
      </section>

      {/* API Section */}
      <section className="py-20 px-6 bg-gradient-to-b from-transparent via-white/[0.02] to-transparent">
        <div className="max-w-6xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-start">
            <div className="lg:sticky lg:top-24">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 mb-6">
                <span className="w-2 h-2 bg-emerald-400 rounded-full animate-pulse" />
                <span className="text-sm text-emerald-400 font-medium">Live Examples</span>
              </div>
              
              <h2 className="text-3xl font-bold text-white mb-4">
                Powerful API Reference
              </h2>
              <p className="text-gray-400 mb-8 leading-relaxed">
                Interactive documentation with live examples. Test API endpoints 
                directly in your browser with our sandbox environment.
              </p>
              
              <ul className="space-y-4 mb-10">
                <li className="flex items-start gap-3 text-gray-300">
                  <span className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center mt-0.5 shrink-0">
                    <span className="w-2 h-2 bg-emerald-400 rounded-full"></span>
                  </span>
                  <span>Orders, Line Items, Customers, Payments</span>
                </li>
                <li className="flex items-start gap-3 text-gray-300">
                  <span className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center mt-0.5 shrink-0">
                    <span className="w-2 h-2 bg-emerald-400 rounded-full"></span>
                  </span>
                  <span>Webhooks for real-time notifications</span>
                </li>
                <li className="flex items-start gap-3 text-gray-300">
                  <span className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center mt-0.5 shrink-0">
                    <span className="w-2 h-2 bg-emerald-400 rounded-full"></span>
                  </span>
                  <span>Code examples in cURL, Python, Kotlin</span>
                </li>
              </ul>
              
              <Link
                to="/api"
                className="inline-flex items-center gap-2 px-5 py-2.5 bg-white/5 hover:bg-white/10 text-white font-medium rounded-lg border border-white/10 hover:border-white/20 transition-all"
              >
                Explore API Reference
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
            
            <div className="bg-[#12121a] rounded-2xl border border-white/10 overflow-hidden shadow-2xl">
              <div className="flex items-center gap-3 px-4 py-3 bg-white/5 border-b border-white/10">
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-full bg-red-500/80" />
                  <span className="w-3 h-3 rounded-full bg-yellow-500/80" />
                  <span className="w-3 h-3 rounded-full bg-green-500/80" />
                </div>
                <div className="flex-1 flex items-center gap-2">
                  <span className="px-2 py-0.5 text-xs font-mono font-semibold rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                    GET
                  </span>
                  <code className="text-sm text-gray-400">/v3/merchants/{'{mId}'}/orders</code>
                </div>
              </div>
              <div className="p-5">
                <div className="text-xs text-gray-500 uppercase tracking-wider mb-3">Response</div>
                <pre className="text-sm text-gray-300 leading-relaxed font-mono">
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
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-6">
        <div className="max-w-4xl mx-auto text-center">
          <div className="p-10 rounded-3xl bg-gradient-to-br from-om-orange/10 via-transparent to-purple-500/10 border border-white/10">
            <h2 className="text-2xl sm:text-3xl font-bold text-white mb-4">
              Ready to get started?
            </h2>
            <p className="text-gray-400 mb-8 max-w-lg mx-auto">
              Start building with OrderMate today. Our documentation will guide you through every step.
            </p>
            <div className="flex flex-wrap justify-center gap-4">
              <Link
                to="/getting-started"
                className="inline-flex items-center gap-2 px-6 py-3 bg-om-orange hover:bg-om-orange-hover text-white font-semibold rounded-lg transition-all"
              >
                Get Started
                <ArrowRight className="w-4 h-4" />
              </Link>
              <a
                href="https://github.com/11thandOrange/OrderMate"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 px-6 py-3 bg-white/5 hover:bg-white/10 text-white font-medium rounded-lg border border-white/10 transition-all"
              >
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
                </svg>
                View on GitHub
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-10 px-6 border-t border-white/5">
        <div className="max-w-6xl mx-auto">
          <div className="flex flex-col md:flex-row justify-between items-center gap-6">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 bg-gradient-to-br from-om-orange to-om-orange-dark rounded-xl flex items-center justify-center shadow-lg shadow-om-orange/20">
                <span className="text-white font-bold text-sm">OM</span>
              </div>
              <div>
                <span className="text-white font-medium">OrderMate</span>
                <span className="text-gray-500 ml-2">Documentation</span>
              </div>
            </div>
            <div className="flex items-center gap-8 text-sm">
              <a href="https://getordermate.com" target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-white transition-colors">
                Website
              </a>
              <a href="https://github.com/11thandOrange/OrderMate" target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-white transition-colors">
                GitHub
              </a>
              <a href="mailto:support@11thandorange.com" className="text-gray-400 hover:text-white transition-colors">
                Support
              </a>
            </div>
          </div>
          <div className="mt-8 pt-6 border-t border-white/5 text-center text-sm text-gray-500">
            © {new Date().getFullYear()} 11th & Orange. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}
