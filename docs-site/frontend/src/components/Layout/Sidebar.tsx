import { Link, useLocation } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';
import type { NavItem } from '../../types/api';
import { useState } from 'react';

interface SidebarProps {
  navigation: NavItem[];
  className?: string;
  style?: React.CSSProperties;
}

export function Sidebar({ navigation, className = '', style }: SidebarProps) {
  const location = useLocation();
  const [expanded, setExpanded] = useState<Record<string, boolean>>(() => {
    const initial: Record<string, boolean> = {};
    navigation.forEach((item) => {
      if (item.children?.some((child) => location.pathname.startsWith(child.href))) {
        initial[item.href] = true;
      }
    });
    return initial;
  });

  const toggleExpanded = (href: string) => {
    setExpanded((prev) => ({ ...prev, [href]: !prev[href] }));
  };

  const isActive = (href: string) => {
    return location.pathname === href || location.pathname.startsWith(href + '/');
  };

  return (
    <aside className={className} style={style}>
      {/* Scrollable navigation container */}
      <div className="flex-1 overflow-y-auto pr-4">
        <nav className="sticky top-20 space-y-1">
          {navigation.map((item) => (
            <div key={item.href}>
              {item.children ? (
                <>
                  <button
                    onClick={() => toggleExpanded(item.href)}
                    className={`w-full flex items-center justify-between px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
                      isActive(item.href)
                        ? 'text-white bg-white/10'
                        : 'text-gray-400 hover:text-white hover:bg-white/5'
                    }`}
                  >
                    <span>{item.title}</span>
                    <ChevronRight
                      className={`w-4 h-4 transition-transform ${
                        expanded[item.href] ? 'rotate-90' : ''
                      }`}
                    />
                  </button>
                  {expanded[item.href] && (
                    <div className="ml-3 mt-1 space-y-1 border-l border-white/10 pl-3">
                      {item.children.map((child) => (
                        <Link
                          key={child.href}
                          to={child.href}
                          className={`block px-3 py-1.5 text-sm rounded-lg transition-colors ${
                            location.pathname === child.href
                              ? 'text-brand bg-brand/10'
                              : 'text-gray-400 hover:text-white hover:bg-white/5'
                          }`}
                        >
                          {child.title}
                        </Link>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <Link
                  to={item.href}
                  className={`block px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
                    isActive(item.href)
                      ? 'text-white bg-white/10'
                      : 'text-gray-400 hover:text-white hover:bg-white/5'
                  }`}
                >
                  {item.title}
                </Link>
              )}
            </div>
          ))}
        </nav>
      </div>
    </aside>
  );
}
