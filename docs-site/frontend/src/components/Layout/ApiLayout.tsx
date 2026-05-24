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
  const isActive = (href: string) => location.pathname === href;

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  return (
    <div className="min-h-screen flex flex-col" style={{ background: '#0f1117', color: '#ffffff' }}>
      {/* Fixed Header */}
      <header className="fixed top-0 left-0 right-0 z-50 h-16 border-b border-white/5" style={{ background: 'rgba(15, 17, 23, 0.95)', backdropFilter: 'blur(12px)' }}>
        <div className="flex items-center justify-between h-full px-6 max-w-[1800px] mx-auto">
          <div className="flex items-center gap-8">
            <Link to="/" className="flex items-center gap-1 text-xl font-bold">
              <span className="text-orange-500">Order</span>
              <span className="text-white">Mate</span>
              <span className="text-gray-400 font-normal ml-1">Docs</span>
            </Link>
            <nav className="hidden md:flex items-center gap-6 text-sm">
              <Link to="/getting-started" className="text-gray-400 hover:text-white transition">Getting Started</Link>
              <Link to="/features" className="text-gray-400 hover:text-white transition">Features</Link>
              <Link to="/api/orders" className="text-white font-medium">API Reference</Link>
              <Link to="/guides" className="text-gray-400 hover:text-white transition">Guides</Link>
            </nav>
          </div>
          <div className="flex items-center gap-4">
            <button className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm text-gray-400 border border-white/10" style={{ background: 'rgba(255,255,255,0.05)' }}>
              <Search className="w-4 h-4" />
              Search... <span className="text-xs ml-2">⌘K</span>
            </button>
            <a href="https://github.com/11thandOrange/OrderMate" target="_blank" rel="noopener noreferrer" className="text-gray-400 hover:text-white">
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
            </a>
          </div>
        </div>
      </header>

      {/* Main Layout - below fixed header */}
      <div className="flex-1 flex pt-16">
        <div className="flex-1 flex max-w-[1800px] w-full mx-auto">
          
          {/* Left Sidebar */}
          <aside className="hidden lg:flex flex-col w-64 flex-shrink-0 border-r border-white/5" style={{ background: '#0f1117' }}>
            <nav className="flex-1 py-6 px-4">
              {/* API Reference Section */}
              <div className="mb-6">
                <div className="flex items-center justify-between px-3 py-2 text-xs font-semibold uppercase tracking-wider text-gray-500">
                  <span>API Reference</span>
                  <ChevronRight className="w-4 h-4 rotate-90" />
                </div>
                <div className="mt-1 space-y-1">
                  {apiNavItems.map((item) => (
                    <Link
                      key={item.href}
                      to={item.href}
                      className={`block px-3 py-2 text-sm rounded-lg transition-colors ${
                        isActive(item.href)
                          ? 'text-orange-500 font-medium'
                          : 'text-gray-400 hover:text-white hover:bg-white/5'
                      }`}
                      style={isActive(item.href) ? { background: 'rgba(249, 115, 22, 0.1)' } : {}}
                    >
                      {item.title}
                    </Link>
                  ))}
                </div>
              </div>

              {/* Resources Section */}
              <div className="pt-4 border-t border-white/5">
                <div className="flex items-center justify-between px-3 py-2 text-xs font-semibold uppercase tracking-wider text-gray-500">
                  <span>Resources</span>
                  <ChevronRight className="w-4 h-4 rotate-90" />
                </div>
                <div className="mt-1 space-y-1">
                  {resourceItems.map((item) => (
                    <Link
                      key={item.href}
                      to={item.href}
                      className={`block px-3 py-2 text-sm rounded-lg transition-colors ${
                        isActive(item.href)
                          ? 'text-orange-500 font-medium'
                          : 'text-gray-400 hover:text-white hover:bg-white/5'
                      }`}
                      style={isActive(item.href) ? { background: 'rgba(249, 115, 22, 0.1)' } : {}}
                    >
                      {item.title}
                    </Link>
                  ))}
                </div>
              </div>
            </nav>
          </aside>
          
          {/* Main Content */}
          <main className="flex-1 min-w-0 overflow-y-auto">
            <div className="py-8 px-6 lg:px-10">
              <div className="max-w-3xl">
                {children}
              </div>
            </div>
          </main>
          
          {/* Right Panel - Try it out */}
          {rightPanel && (
            <aside className="hidden xl:flex flex-col w-[400px] flex-shrink-0 border-l border-white/5" style={{ background: 'rgba(26, 29, 39, 0.5)' }}>
              <div className="sticky top-16 overflow-y-auto max-h-[calc(100vh-4rem)] py-8 px-6">
                {rightPanel}
              </div>
            </aside>
          )}
        </div>
      </div>
    </div>
  );
}
