import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import { ChevronRight, Search } from 'lucide-react';

interface ApiLayoutProps {
  children: ReactNode;
  rightPanel?: ReactNode;
}

const apiNavItems = [
  { title: 'Orders', href: '/api/orders' },
  { title: 'Line Items', href: '/api/line-items' },
  { title: 'Customers', href: '/api/customers' },
  { title: 'Payments', href: '/api/payments' },
  { title: 'Webhooks', href: '/api/webhooks' },
];

const resourceItems = [
  { title: 'Authentication', href: '/api/auth' },
  { title: 'Error Handling', href: '/api/errors' },
  { title: 'Rate Limits', href: '/api/rate-limits' },
];

export function ApiLayout({ children, rightPanel }: ApiLayoutProps) {
  const location = useLocation();
  const isActive = (href: string) => location.pathname === href || (href === '/api/orders' && location.pathname === '/api');

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: '#0f1117', color: '#ffffff' }}>
      {/* Fixed Header - EXACT MATCH TO MOCKUP */}
      <header style={{ 
        position: 'fixed', 
        top: 0, 
        left: 0, 
        right: 0, 
        zIndex: 50, 
        height: '64px', 
        borderBottom: '1px solid rgba(255,255,255,0.05)',
        background: 'rgba(15, 17, 23, 0.95)', 
        backdropFilter: 'blur(12px)' 
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: '100%', padding: '0 24px', maxWidth: '1800px', margin: '0 auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '32px' }}>
            <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '20px', fontWeight: 'bold', textDecoration: 'none' }}>
              <span style={{ color: '#f97316' }}>Order</span>
              <span style={{ color: '#ffffff' }}>Mate</span>
              <span style={{ color: '#9ca3af', fontWeight: 'normal', marginLeft: '4px' }}>Docs</span>
            </Link>
            <nav style={{ display: 'flex', alignItems: 'center', gap: '24px', fontSize: '14px' }}>
              <Link to="/getting-started" style={{ color: '#9ca3af', textDecoration: 'none' }}>Getting Started</Link>
              <Link to="/features" style={{ color: '#9ca3af', textDecoration: 'none' }}>Features</Link>
              <Link to="/api/orders" style={{ color: '#ffffff', fontWeight: 500, textDecoration: 'none' }}>API Reference</Link>
              <Link to="/guides" style={{ color: '#9ca3af', textDecoration: 'none' }}>Guides</Link>
            </nav>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <button style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 12px', borderRadius: '8px', fontSize: '14px', color: '#9ca3af', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.05)', cursor: 'pointer' }}>
              <Search style={{ width: '16px', height: '16px' }} />
              Search... <span style={{ fontSize: '12px', marginLeft: '8px' }}>⌘K</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Layout - EXACT MATCH TO MOCKUP */}
      <div style={{ flex: 1, display: 'flex', paddingTop: '64px' }}>
        <div style={{ flex: 1, display: 'flex', maxWidth: '1800px', width: '100%', margin: '0 auto' }}>
          
          {/* Left Sidebar - NO overflow-y-auto */}
          <aside style={{ display: 'flex', flexDirection: 'column', width: '256px', flexShrink: 0, borderRight: '1px solid rgba(255,255,255,0.05)', background: '#0f1117' }}>
            <nav style={{ flex: 1, padding: '24px 16px' }}>
              {/* API Reference Section */}
              <div style={{ marginBottom: '24px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px', fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#6b7280' }}>
                  <span>API Reference</span>
                  <ChevronRight style={{ width: '16px', height: '16px', transform: 'rotate(90deg)' }} />
                </div>
                <div style={{ marginTop: '4px' }}>
                  {apiNavItems.map((item) => (
                    <Link
                      key={item.href}
                      to={item.href}
                      style={{
                        display: 'block',
                        padding: '8px 12px',
                        fontSize: '14px',
                        borderRadius: '8px',
                        textDecoration: 'none',
                        marginBottom: '4px',
                        color: isActive(item.href) ? '#f97316' : '#9ca3af',
                        fontWeight: isActive(item.href) ? 500 : 400,
                        background: isActive(item.href) ? 'rgba(249, 115, 22, 0.1)' : 'transparent',
                      }}
                    >
                      {item.title}
                    </Link>
                  ))}
                </div>
              </div>

              {/* Resources Section */}
              <div style={{ paddingTop: '16px', borderTop: '1px solid rgba(255,255,255,0.05)' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px', fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', color: '#6b7280' }}>
                  <span>Resources</span>
                  <ChevronRight style={{ width: '16px', height: '16px', transform: 'rotate(90deg)' }} />
                </div>
                <div style={{ marginTop: '4px' }}>
                  {resourceItems.map((item) => (
                    <Link
                      key={item.href}
                      to={item.href}
                      style={{
                        display: 'block',
                        padding: '8px 12px',
                        fontSize: '14px',
                        borderRadius: '8px',
                        textDecoration: 'none',
                        marginBottom: '4px',
                        color: isActive(item.href) ? '#f97316' : '#9ca3af',
                        fontWeight: isActive(item.href) ? 500 : 400,
                        background: isActive(item.href) ? 'rgba(249, 115, 22, 0.1)' : 'transparent',
                      }}
                    >
                      {item.title}
                    </Link>
                  ))}
                </div>
              </div>
            </nav>
          </aside>
          
          {/* Main Content - overflow-y-auto ONLY here */}
          <main style={{ flex: 1, minWidth: 0, overflowY: 'auto' }}>
            <div style={{ padding: '32px 24px', paddingLeft: '40px', paddingRight: '40px' }}>
              <div style={{ maxWidth: '768px' }}>
                {children}
              </div>
            </div>
          </main>
          
          {/* Right Panel - Try it out */}
          {rightPanel && (
            <aside style={{ display: 'flex', flexDirection: 'column', width: '400px', flexShrink: 0, borderLeft: '1px solid rgba(255,255,255,0.05)', background: 'rgba(26, 29, 39, 0.5)' }}>
              <div style={{ position: 'sticky', top: '64px', overflowY: 'auto', maxHeight: 'calc(100vh - 64px)', padding: '32px 24px' }}>
                {rightPanel}
              </div>
            </aside>
          )}
        </div>
      </div>
    </div>
  );
}
