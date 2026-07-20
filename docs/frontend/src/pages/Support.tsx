import { Link } from 'react-router-dom';
import { DocsLayout } from '../components/Layout/DocsLayout';
import { LifeBuoy, Store, BookOpen } from 'lucide-react';

const channels = [
  {
    icon: LifeBuoy,
    color: '#a78bfa',
    title: 'GitHub Issues',
    description: 'Report a bug or request a feature in the OrderMate repository.',
    href: 'https://github.com/11thandOrange/OrderMate/issues',
  },
  {
    icon: Store,
    color: '#f97316',
    title: 'Clover App Market',
    description: "Installation issues, permissions, or billing questions specific to your Clover account.",
    href: 'https://www.clover.com/appmarket/apps/WWTF1AKT87VJ8',
  },
  {
    icon: BookOpen,
    color: '#34d399',
    title: 'Documentation',
    description: 'Most integration questions are answered in Getting Started, Guides, and the API Reference.',
    href: '/getting-started',
  },
];

export function Support() {
  return (
    <DocsLayout>
      <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
        <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
        <span>/</span>
        <span style={{ color: '#9ca3af' }}>Support</span>
      </nav>
      <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>Support</h1>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
        A few places to get help, depending on what you're stuck on.
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {channels.map((channel) => {
          const Icon = channel.icon;
          const isExternal = channel.href.startsWith('http');
          const content = (
            <>
              <div style={{ padding: '12px', borderRadius: '8px', background: 'rgba(255,255,255,0.05)' }}>
                <Icon style={{ width: '24px', height: '24px', color: channel.color }} />
              </div>
              <div style={{ flex: 1 }}>
                <h3 style={{ fontSize: '18px', fontWeight: 500, color: '#ffffff', marginBottom: '4px' }}>{channel.title}</h3>
                <p style={{ fontSize: '14px', color: '#9ca3af', margin: 0 }}>{channel.description}</p>
              </div>
            </>
          );
          const style: React.CSSProperties = { display: 'flex', alignItems: 'center', gap: '16px', padding: '16px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px', textDecoration: 'none' };
          return isExternal ? (
            <a key={channel.title} href={channel.href} target="_blank" rel="noopener noreferrer" style={style}>{content}</a>
          ) : (
            <Link key={channel.title} to={channel.href} style={style}>{content}</Link>
          );
        })}
      </div>
    </DocsLayout>
  );
}
