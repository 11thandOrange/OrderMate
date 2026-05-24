import { Link } from 'react-router-dom';
import { ApiLayout } from '../components/Layout/ApiLayout';
import { BookOpen, Code2, Zap, ShoppingBag, Calendar, Bell } from 'lucide-react';

const quickLinks = [
  { title: 'Getting Started', description: 'Install and configure OrderMate on your Clover device.', icon: BookOpen, to: '/getting-started' },
  { title: 'Order Management', description: 'Manage orders, track status, and handle customer requests.', icon: ShoppingBag, to: '/features/orders' },
  { title: 'Calendar View', description: 'View scheduled orders in day, week, or month format.', icon: Calendar, to: '/features/calendar' },
  { title: 'Notifications', description: 'Send SMS and email notifications to customers.', icon: Bell, to: '/features/notifications' },
  { title: 'API Reference', description: 'Full REST API documentation with live examples.', icon: Code2, to: '/api/orders' },
  { title: "What's New", description: 'Latest features and improvements to OrderMate.', icon: Zap, to: '/changelog' },
];

export function Home() {
  return (
    <ApiLayout>
      <nav className="flex items-center gap-2 text-sm text-gray-500 mb-6">
        <span className="text-gray-400">Documentation</span>
      </nav>

      <h1 className="text-4xl font-bold text-white mb-4">OrderMate Docs</h1>
      <p className="text-lg text-gray-400 mb-10 leading-relaxed">
        Everything you need to integrate OrderMate with your Clover POS system. Learn about
        features, explore the API, and build powerful integrations.
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-12">
        {quickLinks.map((item) => {
          const Icon = item.icon;
          return (
            <Link
              key={item.to}
              to={item.to}
              className="group p-5 rounded-xl border border-white/10 hover:border-orange-500/30 transition-colors"
              style={{ background: 'rgba(255,255,255,0.03)' }}
            >
              <div className="flex items-start gap-4">
                <div className="w-9 h-9 rounded-lg flex items-center justify-center shrink-0" style={{ background: 'rgba(249, 115, 22, 0.1)' }}>
                  <Icon className="w-5 h-5 text-orange-500" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-white mb-1 group-hover:text-orange-400 transition-colors">{item.title}</h3>
                  <p className="text-sm text-gray-400 leading-relaxed">{item.description}</p>
                </div>
              </div>
            </Link>
          );
        })}
      </div>

      <section className="p-6 rounded-xl border border-white/10" style={{ background: 'rgba(255,255,255,0.03)' }}>
        <h2 className="text-lg font-semibold text-white mb-2">Quick Start</h2>
        <p className="text-sm text-gray-400 mb-4">
          Get up and running with OrderMate in minutes. Follow the installation guide to set up
          the app on your Clover device and start managing orders.
        </p>
        <Link
          to="/getting-started"
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white rounded-lg bg-orange-500 hover:bg-orange-600 transition-colors"
        >
          Get Started
        </Link>
      </section>
    </ApiLayout>
  );
}
