import { useState, useEffect } from 'react';
import logoSrc from '../../assets/ordermate-logo.svg';

export function Header() {
  const [scrolled, setScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 50);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <header
      className="fixed top-0 left-0 right-0 z-50 border-b border-glass-border transition-all duration-300"
      style={{
        background: scrolled ? 'rgba(0,0,0,0.4)' : 'rgba(0,0,0,0.2)',
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        padding: scrolled ? '12px 40px' : '16px 40px',
      }}
    >
      <div className="max-w-[1400px] mx-auto flex items-center justify-between">
        {/* Logo */}
        <a href="/" className="flex items-center gap-3 no-underline text-content">
          <div className="w-[55px] h-[55px] flex items-center justify-center overflow-hidden bg-transparent flex-shrink-0">
            <img src={logoSrc} alt="OrderMate" className="w-full h-full object-cover" />
          </div>
          <span className="text-2xl font-bold tracking-tight text-content">
            Order<span style={{ color: '#FF9F43' }}>Mate</span>
          </span>
        </a>

        {/* Desktop Navigation */}
        <nav className="hidden md:flex items-center gap-8">
          <a href="/#features" className="text-content-secondary hover:text-content text-[15px] font-medium transition-colors no-underline">
            Features
          </a>
          <a href="/#demos" className="text-content-secondary hover:text-content text-[15px] font-medium transition-colors no-underline">
            Demos
          </a>
          <a href="/#benefits" className="text-content-secondary hover:text-content text-[15px] font-medium transition-colors no-underline">
            Why OrderMate?
          </a>
          <a href="/faq" className="text-content-secondary hover:text-content text-[15px] font-medium transition-colors no-underline">
            FAQ
          </a>
          <a
            href="https://www.clover.com/appmarket/apps/WWTF1AKT87VJ8"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 px-7 py-3 rounded-full text-white font-semibold text-[15px] transition-all duration-300 no-underline hover:-translate-y-0.5"
            style={{
              background: '#FF9F43',
              boxShadow: '0 4px 16px rgba(255,159,67,0.4)',
            }}
            onMouseEnter={(e) => {
              (e.currentTarget as HTMLAnchorElement).style.background = '#e68a2e';
              (e.currentTarget as HTMLAnchorElement).style.boxShadow = '0 8px 24px rgba(255,159,67,0.5)';
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLAnchorElement).style.background = '#FF9F43';
              (e.currentTarget as HTMLAnchorElement).style.boxShadow = '0 4px 16px rgba(255,159,67,0.4)';
            }}
          >
            Install Now!
          </a>
        </nav>

        {/* Mobile Menu Toggle */}
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="md:hidden p-2 text-content-secondary hover:text-content"
          aria-label="Toggle menu"
        >
          {mobileMenuOpen ? (
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          ) : (
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          )}
        </button>
      </div>

      {/* Mobile Menu */}
      {mobileMenuOpen && (
        <div
          className="md:hidden border-t border-glass-border px-5 py-4 space-y-2"
          style={{ background: 'rgba(0,0,0,0.3)', backdropFilter: 'blur(20px)' }}
        >
          <a href="/#features" className="block px-4 py-2 text-content-secondary hover:text-content rounded-lg hover:bg-glass-bg transition-colors no-underline" onClick={() => setMobileMenuOpen(false)}>Features</a>
          <a href="/#demos" className="block px-4 py-2 text-content-secondary hover:text-content rounded-lg hover:bg-glass-bg transition-colors no-underline" onClick={() => setMobileMenuOpen(false)}>Demos</a>
          <a href="/#benefits" className="block px-4 py-2 text-content-secondary hover:text-content rounded-lg hover:bg-glass-bg transition-colors no-underline" onClick={() => setMobileMenuOpen(false)}>Why OrderMate?</a>
          <a href="/faq" className="block px-4 py-2 text-content-secondary hover:text-content rounded-lg hover:bg-glass-bg transition-colors no-underline" onClick={() => setMobileMenuOpen(false)}>FAQ</a>
          <a
            href="https://www.clover.com/appmarket/apps/WWTF1AKT87VJ8"
            target="_blank"
            rel="noopener noreferrer"
            className="block mt-2 px-4 py-2 text-center text-white font-semibold rounded-full no-underline"
            style={{ background: '#FF9F43' }}
          >
            Install Now!
          </a>
        </div>
      )}
    </header>
  );
}
