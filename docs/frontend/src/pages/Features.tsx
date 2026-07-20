import { Link, useParams } from 'react-router-dom';
import { DocsLayout } from '../components/Layout/DocsLayout';
import { 
  ShoppingBag, 
  Calendar, 
  Bell, 
  Settings2, 
  Palette
} from 'lucide-react';

const iconColors: Record<string, string> = {
  orders: '#34d399',
  calendar: '#60a5fa',
  notifications: '#fbbf24',
  widgets: '#a78bfa',
  'custom-fields': '#f472b6',
};

const featuresContent: Record<string, {
  title: string;
  description: string;
  icon: React.ElementType;
  sections: {
    title: string;
    content: string;
    features?: string[];
  }[];
}> = {
  orders: {
    title: 'Order Management',
    description: 'Efficiently manage all your orders in one place with powerful filtering and search capabilities.',
    icon: ShoppingBag,
    sections: [
      {
        title: 'Overview',
        content: 'OrderMate provides a comprehensive order management system that integrates seamlessly with your Clover POS. View all orders at a glance, filter by status, and quickly find what you need.',
        features: [
          'Real-time order synchronization with Clover',
          'Smart search across order details, customer names, and notes',
          'Filter by payment status: Open, Paid, Partially Paid, Refunded, Closed',
          'Custom fields for additional order information',
        ],
      },
      {
        title: 'Order Cards',
        content: 'Each order is displayed as an intuitive card showing key information at a glance:',
        features: [
          'Order number and title',
          'Customer name and contact information',
          'Payment status with color-coded badges',
          'Scheduled pickup/delivery time',
          'Quick action buttons for common tasks',
        ],
      },
      {
        title: 'Order Details',
        content: 'Click on any order to see the full details including:',
        features: [
          'Complete line items with modifications',
          'Customer history and previous orders',
          'Payment information and transactions',
          'Order notes and custom field values',
          'Activity timeline showing all changes',
        ],
      },
    ],
  },
  calendar: {
    title: 'Calendar View',
    description: 'Visualize your scheduled orders in day, week, or month view with smart color coding.',
    icon: Calendar,
    sections: [
      {
        title: 'Overview',
        content: 'The calendar view helps you manage scheduled orders like pickups, deliveries, and appointments. See your day at a glance or plan ahead for the week.',
      },
      {
        title: 'View Options',
        content: 'Switch between different calendar views based on your needs:',
        features: [
          'Day View - Detailed hourly breakdown of scheduled orders',
          'Week View - 7-day overview with drag-and-drop support',
          'Month View - High-level planning and capacity overview',
        ],
      },
      {
        title: 'Color Coding',
        content: 'Orders are automatically color-coded based on type and status:',
        features: [
          'Blue - Pickup orders',
          'Green - Delivery orders',
          'Orange - In-store orders',
          'Red - Overdue or urgent orders',
        ],
      },
    ],
  },
  notifications: {
    title: 'Notifications',
    description: 'Merchant-triggered messages to customers, plus an automatic reminder email to the merchant before an order is due.',
    icon: Bell,
    sections: [
      {
        title: 'Overview',
        content: 'OrderMate has two separate notification paths: a manual, template-driven SMS/email tool a staff member opens from an order, and a fully automatic reminder email sent to the merchant themselves before an order\'s due date. They share the same delivery mechanism (the Bird messaging API) but nothing else - conflating them undersells the automatic one.',
      },
      {
        title: 'Manual: Send to Customer',
        content: 'From an order, a staff member opens the send-notification dialog, picks an SMS or Email tab, and optionally selects a saved template. Selecting a template auto-fills the message body (and subject, for email) by substituting placeholders with real order data before sending:',
        features: [
          'Tabs: SMS or Email, each with its own recipient field (phone vs. email) and validation',
          'Templates support {{merchant_name}}, {{order_id}}, {{customer_name}}, {{order_total}}, {{due_date}}, {{due_time}}, {{item_count}}, and {{order_notes}} placeholders',
          'The recipient field pre-fills from the customer\'s saved email/phone on the order, but can be edited or set to multiple comma-separated recipients',
          'Sent via the Bird API, tagged with the order ID so it shows up in that order\'s notification history',
        ],
      },
      {
        title: 'Automatic: Scheduled Reminder to Merchant',
        content: 'Separately, when an order gets a due date, OrderMate schedules a one-time alarm (via Android\'s AlarmManager) a configurable number of days and minutes before that due date. When it fires, it emails the merchant\'s own Clover-registered support address - not the customer - a summary of the order so nothing gets missed:',
        features: [
          'Timing is configurable per-merchant (days + minutes before due date)',
          'Email includes customer name, due date/time, total, and a line-item table pulled live from Clover at send time',
          'Runs even if the app isn\'t open, since it\'s a scheduled system alarm, not a foreground timer',
          'Automatically re-scheduled if the order\'s due date changes',
        ],
      },
    ],
  },
  widgets: {
    title: 'Widgets',
    description: 'Structured extra fields on items and orders, rendered from a shared V2 schema.',
    icon: Settings2,
    sections: [
      {
        title: 'Overview',
        content: 'A widget is a configured field - like "Category" or "Due Date" - that appears in the item or order editor. Every widget is an instance of the same V2 schema, so the editor, the color-coding, and the filter UI all work the same way regardless of which widget a merchant adds.',
      },
      {
        title: 'The four widget types',
        content: 'There are exactly four widget types, each with its own accent color used consistently across pills, filters, and chips:',
        features: [
          'Calendar - a date picker (blue)',
          'Single Select - one choice from a merchant-defined option list (purple)',
          'Multi Select - any number of choices from a merchant-defined option list (green)',
          'Text Box - free-form text (brown)',
        ],
      },
      {
        title: 'Item-level vs. order-level',
        content: 'Every widget belongs to one of two levels, which determines where it appears and its default state for new merchants:',
        features: [
          'Item-level widgets attach to a line item\'s notes and are enabled by default (Due Date, Category, Tags, Description)',
          'Order-level widgets attach to the order itself and are disabled by default until a merchant turns them on (Deadline, Group, Order Tags, Details)',
        ],
      },
      {
        title: 'Popup behavior',
        content: 'A separate settings object controls how and where widgets surface, independent of any individual widget\'s configuration:',
        features: [
          'Whether item-level notes are enabled at all',
          'Whether order-level notes are enabled at all',
          'Whether the OrderMate button shows inside Clover\'s Register app',
          'Whether the widget popup triggers automatically when an item is added to the cart',
        ],
      },
    ],
  },
  'custom-fields': {
    title: 'Custom Fields',
    description: 'How merchants configure their own widgets - the same V2 schema described on the Widgets page, from the configuration side.',
    icon: Palette,
    sections: [
      {
        title: 'Overview',
        content: '"Custom field" and "widget" describe the same underlying object from two angles: Widgets covers the schema and rendering; this page covers what a merchant actually sets when creating one. Custom fields sync with Clover and can be used for filtering and reporting once created.',
      },
      {
        title: 'What you configure per field',
        content: 'Creating a custom field means choosing a widget type and level, then setting:',
        features: [
          'Label - the field name shown to staff',
          'Enabled - whether the field is currently active',
          'Required - whether staff must fill it in before saving',
          'Show in filter - whether the field appears as a filter option in the order list (on by default for every type except Text Box, which defaults off)',
          'Sort order - where the field appears relative to others at the same level',
          'Options with per-option colors - for Single Select and Multi Select fields, each choice can override the widget type\'s default accent color',
        ],
      },
      {
        title: 'Use Cases',
        content: 'Common uses for custom fields include:',
        features: [
          'Delivery instructions and addresses',
          'Customer preferences and allergies',
          'Special occasion details (birthday, anniversary)',
          'Internal tracking codes or references',
          'Service type (dine-in, takeout, catering)',
        ],
      },
    ],
  },
};

export function Features() {
  const { feature } = useParams<{ feature?: string }>();
  
  // Default to showing the features overview if no specific feature is selected
  if (!feature) {
    return (
      <DocsLayout>
        {/* Breadcrumb */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
          <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
          <span>/</span>
          <span style={{ color: '#9ca3af' }}>Features</span>
        </nav>

        {/* Page Title */}
        <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>Features</h1>
        <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
          Explore the powerful features that make OrderMate the best order management solution for Clover merchants.
        </p>

        {/* Features Grid */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {Object.entries(featuresContent).map(([key, value]) => (
            <Link
              key={key}
              to={`/features/${key}`}
              style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '16px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px', textDecoration: 'none' }}
            >
              <div style={{ padding: '12px', borderRadius: '8px', background: 'rgba(255,255,255,0.05)' }}>
                <value.icon style={{ width: '24px', height: '24px', color: iconColors[key] }} />
              </div>
              <div style={{ flex: 1 }}>
                <h3 style={{ fontSize: '18px', fontWeight: 500, color: '#ffffff', marginBottom: '4px' }}>
                  {value.title}
                </h3>
                <p style={{ fontSize: '14px', color: '#9ca3af', margin: 0 }}>{value.description}</p>
              </div>
            </Link>
          ))}
        </div>
      </DocsLayout>
    );
  }

  const content = featuresContent[feature];
  
  if (!content) {
    return (
      <DocsLayout>
        <div style={{ textAlign: 'center', padding: '48px 0' }}>
          <h1 style={{ fontSize: '24px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>Feature not found</h1>
          <Link to="/features" style={{ color: '#f97316', textDecoration: 'underline' }}>
            Back to Features
          </Link>
        </div>
      </DocsLayout>
    );
  }

  const Icon = content.icon;

  return (
    <DocsLayout>
      {/* Breadcrumb */}
      <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
        <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
        <span>/</span>
        <Link to="/features" style={{ color: '#6b7280', textDecoration: 'none' }}>Features</Link>
        <span>/</span>
        <span style={{ color: '#9ca3af' }}>{content.title}</span>
      </nav>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '24px' }}>
        <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(255,255,255,0.05)' }}>
          <Icon style={{ width: '32px', height: '32px', color: iconColors[feature] }} />
        </div>
        <div>
          <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', margin: 0 }}>{content.title}</h1>
        </div>
      </div>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '48px', lineHeight: 1.6 }}>{content.description}</p>

      {/* Sections */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '48px' }}>
        {content.sections.map((section, index) => (
          <section key={index}>
            <h2 style={{ fontSize: '24px', fontWeight: 600, color: '#ffffff', marginBottom: '16px' }}>{section.title}</h2>
            <p style={{ color: '#9ca3af', marginBottom: '16px' }}>{section.content}</p>
            {section.features && (
              <ul style={{ display: 'flex', flexDirection: 'column', gap: '8px', listStyle: 'none', padding: 0, margin: 0 }}>
                {section.features.map((feat, i) => (
                  <li key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', color: '#d1d5db' }}>
                    <span style={{ width: '6px', height: '6px', background: '#f97316', borderRadius: '50%', marginTop: '8px', flexShrink: 0 }}></span>
                    <span>{feat}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        ))}
      </div>
    </DocsLayout>
  );
}
