import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Header } from './Header';
import { ChevronRight } from 'lucide-react';
import { useState, useEffect, useRef } from 'react';

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
  const mainRef = useRef<HTMLElement>(null);
  const sidebarRef = useRef<HTMLElement>(null);
  const [expandedSections, setExpandedSections] = useState<Record<string, boolean>>({
    'api': true,
    'resources': true,
  });

  // Reset scroll position on route change
  useEffect(() => {
    if (mainRef.current) mainRef.current.scrollTop = 0;
    if (sidebarRef.current) sidebarRef.current.scrollTop = 0;
    window.scrollTo(0, 0);
  }, [location.pathname]);

  const toggleSection = (section: string) => {
    setExpandedSections(prev => ({ ...prev, [section]: !prev[section] }));
  };

  const isActive = (href: string) => location.pathname === href;

  return (
    <div className="min-h-screen flex flex-col bg-[#0f1117]">
      <Header />
      
      {/* Main content wrapper - below fixed header */}
      <div className="flex-1 flex pt-16">
        <div className="flex-1 flex max-w-[1800px] w-full mx-auto">
          
          {/* Left Sidebar - Navigation */}
          <aside className="hidden lg:flex flex-col w-64 flex-shrink-0 border-r border-white/5">
            <nav ref={sidebarRef} className="flex-1 overflow-y-auto py-6 px-4">
              {/* API Reference Section */}
              <div className="mb-6">
                <button
                  onClick={() => toggleSection('api')}
                  className="flex items-center justify-between w-full px-3 py-2 text-xs font-semibold uppercase tracking-wider text-gray-500 hover:text-gray-400"
                >
                  <span>API Reference</span>
                  <ChevronRight className={`w-4 h-4 transition-transform ${expandedSections['api'] ? 'rotate-90' : ''}`} />
                </button>
                
                {expandedSections['api'] && (
                  <div className="mt-1 space-y-1">
                    {apiNavItems.map((item) => (
                      <Link
                        key={item.href}
                        to={item.href}
                        className={`block px-3 py-2 text-sm rounded-lg transition-colors ${
                          isActive(item.href)
                            ? 'text-orange-500 bg-orange-500/10 font-medium'
                            : 'text-gray-400 hover:text-white hover:bg-white/5'
                        }`}
                      >
                        {item.title}
                      </Link>
                    ))}
                  </div>
                )}
              </div>

              {/* Resources Section */}
              <div className="pt-4 border-t border-white/5">
                <button
                  onClick={() => toggleSection('resources')}
                  className="flex items-center justify-between w-full px-3 py-2 text-xs font-semibold uppercase tracking-wider text-gray-500 hover:text-gray-400"
                >
                  <span>Resources</span>
                  <ChevronRight className={`w-4 h-4 transition-transform ${expandedSections['resources'] ? 'rotate-90' : ''}`} />
                </button>
                
                {expandedSections['resources'] && (
                  <div className="mt-1 space-y-1">
                    {resourceItems.map((item) => (
                      <Link
                        key={item.href}
                        to={item.href}
                        className={`block px-3 py-2 text-sm rounded-lg transition-colors ${
                          isActive(item.href)
                            ? 'text-orange-500 bg-orange-500/10 font-medium'
                            : 'text-gray-400 hover:text-white hover:bg-white/5'
                        }`}
                      >
                        {item.title}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            </nav>
          </aside>
          
          {/* Main Content - Documentation */}
          <main ref={mainRef} className="flex-1 min-w-0 overflow-y-auto">
            <div className="py-8 px-6 lg:px-10">
              {children}
            </div>
          </main>
          
          {/* Right Panel - Code/Sandbox */}
          {rightPanel && (
            <aside className="hidden xl:flex flex-col w-[400px] flex-shrink-0 border-l border-white/5 bg-[#1a1d27]/50">
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
