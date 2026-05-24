import { useState } from 'react';
import { Header } from '../components/Layout/Header';
import { Footer } from '../components/Layout/Footer';
import { SectionHeader } from '../components/composite/SectionHeader';
import { FeatureCard } from '../components/composite/FeatureCard';
import { DocsCTASection } from '../components/composite/CTASection';
import {
  StickyNote,
  ClipboardList,
  CalendarDays,
  Bell,
  Clock,
  Search,
  Layers,
  Palette,
  History,
  Bolt,
  Users,
  Smile,
  Store,
  Play,
} from 'lucide-react';
import logoSrc from '../assets/ordermate-logo.svg';

// ─── Hero: App Mockup ─────────────────────────────────────────────────────────

function OrderPill({ type, children }: { type: 'single-select' | 'multi-select' | 'calendar' | 'text-box'; children: React.ReactNode }) {
  const styles: Record<string, { bg: string; color: string }> = {
    'single-select': { bg: 'rgba(206,147,216,0.15)', color: '#CE93D8' },
    'multi-select':  { bg: 'rgba(129,199,132,0.15)', color: '#81C784' },
    'calendar':      { bg: 'rgba(100,181,246,0.15)', color: '#64B5F6' },
    'text-box':      { bg: 'rgba(161,136,127,0.15)', color: '#A1887F' },
  };
  const s = styles[type];
  return (
    <span
      className="inline-flex items-center gap-1 text-[10px] px-2 py-0.5 rounded-[10px] font-medium"
      style={{ background: s.bg, color: s.color }}
    >
      {children}
    </span>
  );
}

const CheckboxSVG = () => (
  <svg className="w-3 h-3 flex-shrink-0" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
    <path fill="currentColor" d="M19,3H5C3.9,3 3,3.9 3,5v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5C21,3.9 20.1,3 19,3zM19,19H5V5h14V19zM17.99,9l-1.41,-1.42 -6.59,6.59 -2.58,-2.57 -1.42,1.41 4,3.99z"/>
  </svg>
);
const CalSVG = () => (
  <svg className="w-3 h-3 flex-shrink-0" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
    <path fill="currentColor" d="M19,3h-1V1h-2v2H8V1H6v2H5C3.89,3 3.01,3.9 3.01,5L3,19c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM19,19H5V8h14v11zM7,10h5v5H7z"/>
  </svg>
);
const TextSVG = () => (
  <svg className="w-3 h-3 flex-shrink-0" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
    <path fill="currentColor" d="M5,17v2h14v-2H5zM9.5,12.8h5l0.9,2.2h2.1L12.75,4h-1.5L6.5,15h2.1L9.5,12.8zM12,5.98L13.87,11H10.13L12,5.98z"/>
  </svg>
);

function AppMockup() {
  return (
    <div
      className="rounded-[24px] p-6 relative overflow-hidden"
      style={{
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #1a1a2e 100%)',
        boxShadow: '0 16px 48px rgba(0,0,0,0.2)',
        border: '1px solid rgba(255,255,255,0.2)',
      }}
    >
      {/* Mockup header */}
      <div className="flex items-center gap-3 mb-5">
        <div className="w-10 h-10 rounded-[10px] overflow-hidden flex-shrink-0" style={{ background: '#FF9F43' }}>
          <img src={logoSrc} alt="OrderMate" className="w-full h-full object-cover" />
        </div>
        <span className="font-semibold text-content">OrderMate</span>
        <div className="flex gap-2 ml-auto">
          {[
            <svg key="list" className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"/></svg>,
            <svg key="cal" className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M19 3h-1V1h-2v2H8V1H6v2H5C3.9 3 3 3.9 3 5v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM7 10h5v5H7z"/></svg>,
            <svg key="cog" className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M19.14 12.94c.04-.3.06-.61.06-.94s-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96a7.02 7.02 0 0 0-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.48.48 0 0 0-.59.22L2.74 8.87a.47.47 0 0 0 .12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58a.47.47 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.47.47 0 0 0-.12-.61l-2.03-1.58zM12 15.6a3.6 3.6 0 1 1 0-7.2 3.6 3.6 0 0 1 0 7.2z"/></svg>,
          ].map((icon, i) => (
            <div
              key={i}
              className="w-9 h-9 rounded-[10px] flex items-center justify-center"
              style={{
                background: 'rgba(255,255,255,0.1)',
                color: i === 0 ? '#FF9F43' : 'rgba(255,255,255,0.7)',
              }}
            >
              {icon}
            </div>
          ))}
        </div>
      </div>

      {/* Order cards */}
      <div className="space-y-3">
        {/* Card 1 */}
        <div className="rounded-[16px] p-5" style={{ background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.1)' }}>
          <div className="flex justify-between items-center mb-3">
            <span className="font-semibold text-content text-[16px]">#1042</span>
            <div className="flex flex-wrap gap-2 items-center">
              <OrderPill type="multi-select"><CheckboxSVG /> Delivery</OrderPill>
              <OrderPill type="multi-select"><CheckboxSVG /> Gift Wrap</OrderPill>
              <OrderPill type="text-box"><TextSVG /> Call before delivery</OrderPill>
              <span className="text-[11px] px-2.5 py-1 rounded-full font-semibold uppercase" style={{ background: 'rgba(238,208,19,0.2)', color: '#eed013' }}>Open</span>
              <span className="text-[11px] px-2.5 py-1 rounded-full font-semibold uppercase" style={{ background: 'rgba(60,205,121,0.2)', color: '#3ccd79' }}>Paid</span>
            </div>
          </div>
          <div className="grid grid-cols-5 gap-3">
            {[
              { label: 'Order Date', value: 'May 3' },
              { label: 'Customer', value: 'Sarah M.' },
              { label: 'Employee', value: 'Alex T.' },
              { label: 'Payment', value: '💳 Card' },
              { label: 'Total', value: '$45.50' },
            ].map(({ label, value }) => (
              <div key={label} className="flex flex-col gap-1">
                <span className="text-[10px] uppercase tracking-wider" style={{ color: 'rgba(255,255,255,0.5)' }}>{label}</span>
                <span className="text-[13px] font-medium text-content">{value}</span>
              </div>
            ))}
          </div>
          <div className="mt-3 pt-3 border-t" style={{ borderColor: 'rgba(255,255,255,0.1)' }}>
            <div className="text-[10px] uppercase tracking-wider mb-2" style={{ color: 'rgba(255,255,255,0.5)' }}>Item Level Notes</div>
            <div className="flex flex-wrap gap-1.5">
              <OrderPill type="calendar"><CalSVG /> May 5</OrderPill>
              <OrderPill type="single-select"><CheckboxSVG /> Chocolate</OrderPill>
              <OrderPill type="single-select"><CheckboxSVG /> Large</OrderPill>
            </div>
          </div>
        </div>

        {/* Card 2 */}
        <div className="rounded-[16px] p-5" style={{ background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.1)' }}>
          <div className="flex justify-between items-center mb-3">
            <span className="font-semibold text-content text-[16px]">#1041</span>
            <div className="flex gap-2 items-center">
              <OrderPill type="calendar"><CalSVG /> May 10</OrderPill>
              <span className="text-[11px] px-2.5 py-1 rounded-full font-semibold uppercase" style={{ background: 'rgba(238,208,19,0.2)', color: '#eed013' }}>Open</span>
            </div>
          </div>
          <div className="grid grid-cols-5 gap-3">
            {[
              { label: 'Order Date', value: 'May 3' },
              { label: 'Customer', value: 'John D.' },
              { label: 'Employee', value: 'Sarah J.' },
              { label: 'Payment', value: '💵 Cash' },
              { label: 'Total', value: '$128.00' },
            ].map(({ label, value }) => (
              <div key={label} className="flex flex-col gap-1">
                <span className="text-[10px] uppercase tracking-wider" style={{ color: 'rgba(255,255,255,0.5)' }}>{label}</span>
                <span className="text-[13px] font-medium text-content">{value}</span>
              </div>
            ))}
          </div>
          <div className="mt-3 pt-3 border-t" style={{ borderColor: 'rgba(255,255,255,0.1)' }}>
            <div className="text-[10px] uppercase tracking-wider mb-2" style={{ color: 'rgba(255,255,255,0.5)' }}>Item Level Notes</div>
            <div className="flex flex-wrap gap-1.5">
              <OrderPill type="multi-select"><CheckboxSVG /> No Nuts</OrderPill>
              <OrderPill type="multi-select"><CheckboxSVG /> Gluten Free</OrderPill>
              <OrderPill type="multi-select"><CheckboxSVG /> Extra Frosting</OrderPill>
              <OrderPill type="single-select"><CheckboxSVG /> Vanilla</OrderPill>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Hero ─────────────────────────────────────────────────────────────────────

function HeroSection() {
  return (
    <section
      className="min-h-screen flex items-center px-10 pt-[120px] pb-20 relative overflow-hidden"
      id="home"
    >
      {/* Orange glow */}
      <div
        className="absolute pointer-events-none"
        style={{
          top: '-50%', right: '-20%', width: '80%', height: '150%',
          background: 'radial-gradient(circle, rgba(255,159,67,0.15) 0%, transparent 60%)',
        }}
      />

      <div className="max-w-[1400px] mx-auto grid lg:grid-cols-2 gap-20 items-center w-full">
        {/* Left: text */}
        <div className="z-10">
          {/* Badge */}
          <div
            className="inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium mb-6"
            style={{ background: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.2)', backdropFilter: 'blur(20px)' }}
          >
            <span style={{ color: '#3ccd79' }}>✦</span>
            Available on Clover App Market
          </div>

          <h1
            className="text-[56px] font-extrabold leading-[1.1] mb-6"
            style={{ letterSpacing: '-1px' }}
          >
            Supercharge Your{' '}
            <span
              style={{
                background: 'linear-gradient(135deg, #FF9F43, #ffcc80)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text',
              }}
            >
              Orders
            </span>
            {' '}with Custom Notes, Notifications, &amp; Scheduling
          </h1>

          <p className="text-xl text-content-secondary mb-10 max-w-[500px] leading-[1.7]">
            Add item-level and order-level notes, schedule pickups and deliveries on a visual calendar,
            and send customers email &amp; SMS notifications — all seamlessly integrated with your Clover POS.
          </p>

          <div className="flex flex-wrap gap-4">
            <a
              href="https://www.clover.com/appmarket/apps/WWTF1AKT87VJ8"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-10 py-[18px] rounded-full text-white font-semibold text-[17px] no-underline transition-all duration-300 hover:-translate-y-0.5"
              style={{ background: '#FF9F43', boxShadow: '0 4px 16px rgba(255,159,67,0.4)' }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLAnchorElement).style.background = '#e68a2e'; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLAnchorElement).style.background = '#FF9F43'; }}
            >
              Install on Clover
            </a>
            <a
              href="#demos"
              className="inline-flex items-center gap-2 px-10 py-[18px] rounded-full text-content font-semibold text-[17px] no-underline transition-all duration-300 hover:-translate-y-0.5"
              style={{ background: 'transparent', border: '2px solid rgba(255,255,255,0.2)' }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLAnchorElement).style.background = 'rgba(255,255,255,0.1)'; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLAnchorElement).style.background = 'transparent'; }}
            >
              <Play className="w-4 h-4" />
              Watch Demo
            </a>
          </div>

          {/* Stats */}
          <div
            className="flex gap-12 mt-14 pt-8"
            style={{ borderTop: '1px solid rgba(255,255,255,0.2)' }}
          >
            {[
              { value: '5★', label: 'Rating on Clover' },
              { value: '1,000+', label: 'Active Merchants' },
              { value: '10K+', label: 'Orders Managed' },
            ].map(({ value, label }) => (
              <div key={label}>
                <div className="text-[36px] font-bold text-content">{value}</div>
                <div className="text-sm text-content-secondary mt-1">{label}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Right: app mockup */}
        <div className="relative z-10">
          <AppMockup />

          {/* Floating card: notification */}
          <div
            className="absolute -top-5 -right-8 flex items-center gap-3 px-4 py-4 rounded-[16px] hidden lg:flex animate-[float_4s_ease-in-out_infinite]"
            style={{ background: 'rgba(255,255,255,0.95)', boxShadow: '0 8px 32px rgba(0,0,0,0.1)', color: '#1a1a2e' }}
          >
            <div className="w-10 h-10 rounded-[10px] flex items-center justify-center text-white" style={{ background: 'linear-gradient(135deg, #3ccd79, #2ecc71)' }}>
              <Bell className="w-5 h-5" />
            </div>
            <div>
              <div className="text-[13px] font-semibold">Notification Sent!</div>
              <div className="text-[11px] text-gray-500">Sarah M. was notified</div>
            </div>
          </div>

          {/* Floating card: calendar */}
          <div
            className="absolute -bottom-4 -left-10 px-4 py-4 rounded-[16px] hidden lg:block animate-[float_4s_ease-in-out_infinite_1s]"
            style={{ background: 'rgba(255,255,255,0.95)', boxShadow: '0 8px 32px rgba(0,0,0,0.1)', color: '#1a1a2e' }}
          >
            <div className="w-10 h-10 rounded-[10px] flex items-center justify-center text-white mb-2" style={{ background: 'linear-gradient(135deg, #7367F0, #9b59b6)' }}>
              <CalendarDays className="w-5 h-5" />
            </div>
            <div className="text-[13px] font-semibold">5 orders scheduled</div>
            <div className="text-[11px] text-gray-500">Next pickup at 2:30 PM</div>
          </div>
        </div>
      </div>

      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-10px); }
        }
      `}</style>
    </section>
  );
}

// ─── Features ─────────────────────────────────────────────────────────────────

const features = [
  { icon: StickyNote,    title: 'Item-Level Notes',            description: 'Attach custom notes to individual line items. Perfect for special requests, allergies, modifications, and per-item instructions that stay with each product.' },
  { icon: ClipboardList, title: 'Order-Level Notes',           description: 'Add notes to the entire order for delivery addresses, pickup times, gift messages, or any information that applies to the whole transaction.' },
  { icon: CalendarDays,  title: 'Visual Calendar',             description: 'View all scheduled orders in Day, Week, or Month view. Jump to today or next fulfillment with one tap. Never miss a pickup or delivery again.' },
  { icon: Bell,          title: 'Customer Notifications',      description: 'Send SMS or email notifications to customers when their order is ready. Create templates for messages you send most often!' },
  { icon: Clock,         title: 'Schedule Orders',             description: 'Merchants can schedule email reminders sent to them before an order is due. And schedule receipt printing so printers automatically print upcoming orders.' },
  { icon: Search,        title: 'Smart Search & Filters',      description: 'Customize filters to quickly search & find orders. Only search by the filters that matter most to you.' },
  { icon: Layers,        title: 'Register Overlay Integration',description: 'Access OrderMate directly from Clover Register with a floating button. Add notes and schedule orders without leaving the checkout flow.' },
  { icon: Palette,       title: 'Profile Customization',       description: 'Personalize with custom avatars and theme colors. Choose from multiple accent colors to match your brand or personal preference.' },
  { icon: History,       title: 'Order History Tracking',      description: 'View complete order history with payment details, refunds, and transaction timeline. Track partially paid, partially refunded, and closed orders.' },
];

function FeaturesSection() {
  return (
    <section
      className="py-[120px] px-10 relative"
      id="features"
      style={{ background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #1a1a2e 100%)' }}
    >
      <SectionHeader
        label="Features"
        title="Powerful Order Management Tools"
        description="OrderMate gives merchants unique calendar scheduling, customizable notes, and easy to use SMS & email notification."
      />
      <div className="max-w-[1400px] mx-auto grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
        {features.map((f) => (
          <FeatureCard key={f.title} icon={f.icon} title={f.title} description={f.description} />
        ))}
      </div>
    </section>
  );
}

// ─── Video Demos ──────────────────────────────────────────────────────────────

interface DemoCardProps {
  videoId: string;
  tag: string;
  title: string;
  description: string;
  wide?: boolean;
}

function DemoCard({ videoId, tag, title, description, wide }: DemoCardProps) {
  const [playing, setPlaying] = useState(false);
  const thumbUrl = `https://img.youtube.com/vi/${videoId}/maxresdefault.jpg`;

  return (
    <div
      className={`rounded-[24px] overflow-hidden transition-all duration-300 hover:-translate-y-1 ${wide ? 'col-span-1 lg:col-span-2' : ''}`}
      style={{
        background: 'rgba(255,255,255,0.1)',
        border: '1px solid rgba(255,255,255,0.2)',
        backdropFilter: 'blur(20px)',
      }}
      onMouseEnter={(e) => { (e.currentTarget as HTMLDivElement).style.boxShadow = '0 16px 48px rgba(0,0,0,0.2)'; }}
      onMouseLeave={(e) => { (e.currentTarget as HTMLDivElement).style.boxShadow = ''; }}
    >
      {/* Video container */}
      <div
        className="relative overflow-hidden"
        style={{ aspectRatio: wide ? '21/9' : '16/10', background: 'linear-gradient(135deg, #1a1a2e, #16213e)' }}
      >
        {playing ? (
          <iframe
            className="absolute inset-0 w-full h-full border-0"
            src={`https://www.youtube.com/embed/${videoId}?autoplay=1&controls=1&modestbranding=1&rel=0`}
            allow="autoplay; fullscreen"
            allowFullScreen
          />
        ) : (
          <div
            className="absolute inset-0 cursor-pointer"
            style={{ backgroundImage: `url(${thumbUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' }}
            onClick={() => setPlaying(true)}
          >
            <div className="absolute inset-0" style={{ background: 'rgba(0,0,0,0.3)' }} />
            <div
              className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[72px] h-[72px] rounded-full flex items-center justify-center transition-transform hover:scale-110"
              style={{ background: '#FF9F43', boxShadow: '0 4px 24px rgba(255,159,67,0.4)' }}
            >
              <Play className="w-7 h-7 text-white ml-1" />
            </div>
          </div>
        )}
      </div>

      {/* Info */}
      <div className="p-6">
        <span
          className="inline-block px-3 py-1 rounded-full text-[11px] font-semibold mb-3"
          style={{ background: 'rgba(115,103,240,0.2)', color: '#a5a0f5' }}
        >
          {tag}
        </span>
        <h3 className="text-lg font-semibold text-content mb-2">{title}</h3>
        <p className="text-sm text-content-secondary leading-relaxed">{description}</p>
      </div>
    </div>
  );
}

function DemosSection() {
  return (
    <section className="py-[120px] px-10" id="demos">
      <div className="max-w-[1400px] mx-auto">
        <SectionHeader
          label="See It In Action"
          title="Watch OrderMate in Action"
          description="See how easy it is to add notes, schedule orders, and keep customers informed."
        />
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-10">
          <DemoCard videoId="cwyF2NgQz54" tag="Item Management"    title="Add Item Level Notes"            description="Attach specific instructions to individual line items — customize each product with allergies, modifications, or special requests using single-select, multi-select, or text widgets." />
          <DemoCard videoId="nNO3-6LSZrY" tag="Order Management"   title="Add Order Level Notes"           description="Attach notes to the entire order for delivery instructions, pickup times, gift messages, or scheduling with the built-in calendar date picker." />
          <DemoCard videoId="EfkYBxFmgOM" tag="Customer Engagement" title="Send Customer Notifications"    description="Link customers to orders and send instant SMS or email notifications when orders are ready. Schedule automated reminders before due dates." />
          <DemoCard videoId="eAGtLMxInnw" tag="Personalization"    title="Easily Customize Your Settings"  description="Schedule reminders, receipt printing, and more with unique preferences that make your life easier." />
          <DemoCard videoId="j8ylpJlR21o" tag="Productivity"       title="Smart Search, Filters & Calendar View" description="Instantly find orders with smart search and powerful filters. View scheduled orders in Day, Week, or Month calendar view. Filter by payment status, custom fields, and date ranges." wide />
        </div>
      </div>
    </section>
  );
}

// ─── Benefits ─────────────────────────────────────────────────────────────────

const benefits = [
  { icon: Bolt,  title: 'Lightning Fast',                description: 'Find any order in seconds with smart search and instant filters' },
  { icon: Users, title: 'Keep Team in Sync',             description: 'Keep orders up to date and keep all team members and customers on the same page' },
  { icon: Smile, title: 'Happy Customers',               description: 'Keep customers informed with timely SMS and email notifications' },
  { icon: Store, title: 'Built by Merchants for Merchants', description: 'Order management made easy by peers who know what you need' },
];

function BenefitsSection() {
  return (
    <section
      className="py-[120px] px-10"
      id="benefits"
      style={{ background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #1a1a2e 100%)' }}
    >
      <SectionHeader
        label="Why OrderMate?"
        title="Built for Busy Clover Merchants"
        description="We understand the challenges of running a business. OrderMate saves you time and keeps customers happy."
      />
      <div className="max-w-[1200px] mx-auto grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        {benefits.map(({ icon: Icon, title, description }) => (
          <div key={title} className="text-center px-6 py-8">
            <div
              className="w-[72px] h-[72px] rounded-full flex items-center justify-center mx-auto mb-5 text-[28px]"
              style={{ background: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.2)', color: '#FF9F43' }}
            >
              <Icon className="w-7 h-7" />
            </div>
            <h4 className="text-lg font-semibold text-content mb-2">{title}</h4>
            <p className="text-sm text-content-secondary">{description}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function Home() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <HeroSection />
      <FeaturesSection />
      <DemosSection />
      <BenefitsSection />
      <DocsCTASection />
      <Footer />
    </div>
  );
}
