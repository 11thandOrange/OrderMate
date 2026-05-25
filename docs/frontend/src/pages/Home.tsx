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
      {/* Breadcrumb */}
      <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
        <span style={{ color: '#9ca3af' }}>Documentation</span>
      </nav>

      {/* Page Title */}
      <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>OrderMate Docs</h1>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
        Everything you need to integrate OrderMate with your Clover POS system. Learn about
        features, explore the API, and build powerful integrations.
      </p>

      {/* Quick Links Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '16px', marginBottom: '48px' }}>
        {quickLinks.map((item) => {
          const Icon = item.icon;
          return (
            <Link
              key={item.to}
              to={item.to}
              style={{ padding: '20px', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.03)', textDecoration: 'none' }}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '16px' }}>
                <div style={{ width: '36px', height: '36px', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, background: 'rgba(249, 115, 22, 0.1)' }}>
                  <Icon style={{ width: '20px', height: '20px', color: '#f97316' }} />
                </div>
                <div>
                  <h3 style={{ fontSize: '14px', fontWeight: 600, color: '#ffffff', marginBottom: '4px' }}>{item.title}</h3>
                  <p style={{ fontSize: '14px', color: '#9ca3af', lineHeight: 1.5, margin: 0 }}>{item.description}</p>
                </div>
              </div>
            </Link>
          );
        })}
      </div>

      {/* Quick Start Section */}
      <section style={{ padding: '24px', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.03)' }}>
        <h2 style={{ fontSize: '18px', fontWeight: 600, color: '#ffffff', marginBottom: '8px' }}>Quick Start</h2>
        <p style={{ fontSize: '14px', color: '#9ca3af', marginBottom: '16px' }}>
          Get up and running with OrderMate in minutes. Follow the installation guide to set up
          the app on your Clover device and start managing orders.
        </p>
        <Link
          to="/getting-started"
          style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '8px 16px', fontSize: '14px', fontWeight: 500, color: '#ffffff', borderRadius: '8px', background: '#f97316', textDecoration: 'none' }}
        >
          Get Started
        </Link>
      </section>
    </ApiLayout>
  );
}
