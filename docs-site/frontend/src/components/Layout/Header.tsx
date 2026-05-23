import { Link } from 'react-router-dom';
import { Search, Menu, X } from 'lucide-react';
import { useState } from 'react';

export function Header() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-[#0a0a0b]/90 backdrop-blur-xl border-b border-white/5">
      <div className="max-w-[1800px] mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-3">
            <div className="w-8 h-8 bg-gradient-to-br from-om-orange to-om-orange-dark rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">OM</span>
            </div>
            <span className="text-xl font-semibold text-white">
              Order<span className="text-om-orange">Mate</span>
              <span className="text-gray-500 font-normal ml-2">Docs</span>
            </span>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-8">
            <Link
              to="/getting-started"
              className="text-sm text-gray-400 hover:text-white transition-colors"
            >
              Getting Started
            </Link>
            <Link
              to="/features"
              className="text-sm text-gray-400 hover:text-white transition-colors"
            >
              Features
            </Link>
            <Link
              to="/api"
              className="text-sm text-gray-400 hover:text-white transition-colors"
            >
              API Reference
            </Link>
            <Link
              to="/guides"
              className="text-sm text-gray-400 hover:text-white transition-colors"
            >
              Guides
            </Link>
          </nav>

          {/* Right Actions */}
          <div className="flex items-center gap-4">
            {/* Search */}
            <button
              onClick={() => setSearchOpen(true)}
              className="hidden sm:flex items-center gap-2 px-3 py-1.5 bg-white/5 border border-white/10 rounded-lg text-sm text-gray-400 hover:bg-white/10 hover:text-white transition-all"
            >
              <Search className="w-4 h-4" />
              <span>Search...</span>
              <kbd className="hidden lg:inline-flex items-center gap-1 px-1.5 py-0.5 text-xs bg-white/10 rounded">
                ⌘K
              </kbd>
            </button>

            {/* GitHub */}
            <a
              href="https://github.com/11thandOrange/OrderMate"
              target="_blank"
              rel="noopener noreferrer"
              className="p-2 text-gray-400 hover:text-white transition-colors"
            >
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
              </svg>
            </a>

            {/* Mobile Menu Toggle */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="md:hidden p-2 text-gray-400 hover:text-white"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-[#0f0f10] border-t border-white/10">
          <nav className="px-4 py-4 space-y-2">
            <Link
              to="/getting-started"
              className="block px-4 py-2 text-gray-300 hover:bg-white/5 rounded-lg"
              onClick={() => setMobileMenuOpen(false)}
            >
              Getting Started
            </Link>
            <Link
              to="/features"
              className="block px-4 py-2 text-gray-300 hover:bg-white/5 rounded-lg"
              onClick={() => setMobileMenuOpen(false)}
            >
              Features
            </Link>
            <Link
              to="/api"
              className="block px-4 py-2 text-gray-300 hover:bg-white/5 rounded-lg"
              onClick={() => setMobileMenuOpen(false)}
            >
              API Reference
            </Link>
            <Link
              to="/guides"
              className="block px-4 py-2 text-gray-300 hover:bg-white/5 rounded-lg"
              onClick={() => setMobileMenuOpen(false)}
            >
              Guides
            </Link>
          </nav>
        </div>
      )}

      {/* Search Modal */}
      {searchOpen && (
        <div
          className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-start justify-center pt-[20vh]"
          onClick={() => setSearchOpen(false)}
        >
          <div
            className="w-full max-w-xl bg-[#1a1a2e] border border-white/10 rounded-xl shadow-2xl"
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
