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
    description: 'Keep your customers informed with automated SMS and email notifications.',
    icon: Bell,
    sections: [
      {
        title: 'Overview',
        content: 'OrderMate helps you communicate with customers through automated notifications. Send order confirmations, status updates, and reminders without manual effort.',
      },
      {
        title: 'SMS Notifications',
        content: 'Send text messages to customers for important updates:',
        features: [
          'Order confirmation when order is placed',
          'Ready for pickup notification',
          'Out for delivery updates',
          'Custom messages for special situations',
        ],
      },
      {
        title: 'Email Notifications',
        content: 'Professional email templates for detailed communications:',
        features: [
          'Order receipts with itemized details',
          'Status change notifications',
          'Delivery tracking information',
          'Customizable templates with your branding',
        ],
      },
    ],
  },
  widgets: {
    title: 'Widgets & Custom Fields',
    description: 'Capture additional information with customizable widgets and fields.',
    icon: Settings2,
    sections: [
      {
        title: 'Overview',
        content: 'Extend OrderMate to capture the information that matters to your business. Create custom fields and widgets that appear during order creation and editing.',
      },
      {
        title: 'Widget Types',
        content: 'Choose from various widget types to collect different kinds of data:',
        features: [
          'Text Input - Free-form text entry',
          'Dropdown - Single selection from options',
          'Multi-select - Multiple option selection',
          'Date/Time Picker - Schedule dates and times',
          'Toggle - Yes/No boolean fields',
        ],
      },
      {
        title: 'Configuration',
        content: 'Customize each widget to fit your workflow:',
        features: [
          'Set custom labels and placeholder text',
          'Mark fields as required or optional',
          'Define default values',
          'Order widgets for optimal flow',
        ],
      },
    ],
  },
  'custom-fields': {
    title: 'Custom Fields',
    description: 'Define custom data fields to track order-specific information.',
    icon: Palette,
    sections: [
      {
        title: 'Overview',
        content: 'Custom fields allow you to store additional information on orders that isn\'t captured by default. This data syncs with Clover and can be used for filtering and reporting.',
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
