import { Link } from 'react-router-dom';
import { Search, Menu, X } from 'lucide-react';
import { useState } from 'react';

export function Header() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  return (
    <header
      className="fixed top-0 left-0 right-0 z-50 h-16 border-b border-white/5"
      style={{ background: 'rgba(15, 17, 23, 0.95)', backdropFilter: 'blur(12px)' }}
    >
      <div className="flex items-center justify-between h-full px-6 max-w-[1800px] mx-auto">
        <div className="flex items-center gap-8">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-1 text-xl font-bold">
            <span className="text-orange-500">Order</span>
            <span className="text-white">Mate</span>
            <span className="text-gray-400 font-normal ml-1">Docs</span>
          </Link>

          {/* Desktop Nav */}
          <nav className="hidden md:flex items-center gap-6 text-sm">
            <Link to="/getting-started" className="text-gray-400 hover:text-white transition-colors">
              Getting Started
            </Link>
            <Link to="/features" className="text-gray-400 hover:text-white transition-colors">
              Features
            </Link>
            <Link to="/api" className="text-gray-400 hover:text-white transition-colors">
              API Reference
            </Link>
            <Link to="/guides" className="text-gray-400 hover:text-white transition-colors">
              Guides
            </Link>
          </nav>
        </div>

        {/* Right actions */}
        <div className="flex items-center gap-4">
          <button
            onClick={() => setSearchOpen(true)}
            className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm text-gray-400 border border-white/10 hover:bg-white/5 hover:text-white transition-colors"
            style={{ background: 'rgba(255,255,255,0.05)' }}
          >
            <Search className="w-4 h-4" />
            Search...
            <span className="text-xs ml-2">⌘K</span>
          </button>

          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 text-gray-400 hover:text-white"
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-white/10" style={{ background: 'rgba(15,17,23,0.98)' }}>
          <nav className="px-4 py-4 space-y-1">
            {[
              { label: 'Getting Started', to: '/getting-started' },
              { label: 'Features', to: '/features' },
              { label: 'API Reference', to: '/api' },
              { label: 'Guides', to: '/guides' },
            ].map(({ label, to }) => (
              <Link
                key={to}
                to={to}
                className="block px-4 py-2 text-gray-400 hover:text-white hover:bg-white/5 rounded-lg transition-colors"
                onClick={() => setMobileMenuOpen(false)}
              >
                {label}
              </Link>
            ))}
          </nav>
        </div>
      )}

      {/* Search Modal */}
      {searchOpen && (
        <div
          className="fixed inset-0 z-50 flex items-start justify-center pt-[20vh]"
          style={{ background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)' }}
          onClick={() => setSearchOpen(false)}
        >
          <div
            className="w-full max-w-xl mx-4 rounded-xl shadow-2xl border border-white/10"
            style={{ background: '#1a1d27' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center gap-3 px-4 py-3 border-b border-white/10">
              <Search className="w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search documentation..."
                className="flex-1 bg-transparent text-white placeholder-gray-500 focus:outline-none"
                autoFocus
              />
              <kbd className="px-2 py-1 text-xs text-gray-400 bg-white/10 rounded">ESC</kbd>
            </div>
            <div className="p-4 text-center text-gray-500 text-sm">
              Start typing to search...
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
