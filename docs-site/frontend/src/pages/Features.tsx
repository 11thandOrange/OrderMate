import { Link, useParams } from 'react-router-dom';
import { DocsLayout } from '../components/Layout/DocsLayout';
import { 
  ShoppingBag, 
  Calendar, 
  Bell, 
  Settings2, 
  Palette
} from 'lucide-react';

const featuresContent: Record<string, {
  title: string;
  description: string;
  icon: React.ElementType;
  color: string;
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
    color: 'text-emerald-400',
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
    color: 'text-blue-400',
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
    color: 'text-amber-400',
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
    color: 'text-purple-400',
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
    color: 'text-pink-400',
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
        <div className="max-w-3xl">
          <div className="flex items-center gap-2 text-sm text-gray-500 mb-8">
            <Link to="/" className="hover:text-white">Docs</Link>
            <span>/</span>
            <span className="text-gray-300">Features</span>
          </div>

          <h1 className="text-4xl font-bold text-white mb-4">Features</h1>
          <p className="text-xl text-gray-400 mb-8">
            Explore the powerful features that make OrderMate the best order management solution for Clover merchants.
          </p>

          <div className="grid gap-4">
            {Object.entries(featuresContent).map(([key, value]) => (
              <Link
                key={key}
                to={`/features/${key}`}
                className="group flex items-center gap-4 p-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all"
              >
                <div className={`p-3 rounded-lg bg-white/5`}>
                  <value.icon className={`w-6 h-6 ${value.color}`} />
                </div>
                <div className="flex-1">
                  <h3 className="text-lg font-medium text-white group-hover:text-om-orange transition-colors">
                    {value.title}
                  </h3>
                  <p className="text-sm text-gray-400">{value.description}</p>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </DocsLayout>
    );
  }

  const content = featuresContent[feature];
  
  if (!content) {
    return (
      <DocsLayout>
        <div className="text-center py-12">
          <h1 className="text-2xl font-bold text-white mb-4">Feature not found</h1>
          <Link to="/features" className="text-om-orange hover:underline">
            Back to Features
          </Link>
        </div>
      </DocsLayout>
    );
  }

  const Icon = content.icon;

  return (
    <DocsLayout>
      <div className="max-w-3xl">
        {/* Breadcrumb */}
        <div className="flex items-center gap-2 text-sm text-gray-500 mb-8">
          <Link to="/" className="hover:text-white">Docs</Link>
          <span>/</span>
          <Link to="/features" className="hover:text-white">Features</Link>
          <span>/</span>
          <span className="text-gray-300">{content.title}</span>
        </div>

        {/* Header */}
        <div className="flex items-center gap-4 mb-6">
          <div className={`p-3 rounded-xl bg-white/5`}>
            <Icon className={`w-8 h-8 ${content.color}`} />
          </div>
          <div>
            <h1 className="text-4xl font-bold text-white">{content.title}</h1>
          </div>
        </div>
        <p className="text-xl text-gray-400 mb-12">{content.description}</p>

        {/* Sections */}
        <div className="space-y-12">
          {content.sections.map((section, index) => (
            <section key={index}>
              <h2 className="text-2xl font-semibold text-white mb-4">{section.title}</h2>
              <p className="text-gray-400 mb-4">{section.content}</p>
              {section.features && (
                <ul className="space-y-2">
                  {section.features.map((feature, i) => (
                    <li key={i} className="flex items-start gap-3 text-gray-300">
                      <span className="w-1.5 h-1.5 bg-om-orange rounded-full mt-2 shrink-0"></span>
                      <span>{feature}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          ))}
        </div>
      </div>
    </DocsLayout>
  );
}
