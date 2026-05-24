import logoSrc from '../../assets/ordermate-logo.svg';

export function Footer() {
  return (
    <footer
      className="py-[60px] px-10 border-t border-glass-border"
      style={{ background: 'rgba(0,0,0,0.3)' }}
    >
      <div className="max-w-[1400px] mx-auto flex flex-wrap justify-between items-center gap-6">
        {/* Logo */}
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 flex items-center justify-center overflow-hidden bg-transparent">
            <img src={logoSrc} alt="OrderMate" className="w-full h-full object-cover" />
          </div>
          <span className="text-xl font-bold text-content tracking-tight">
            Order<span style={{ color: '#FF9F43' }}>Mate</span>
          </span>
        </div>

        {/* Links */}
        <nav className="flex flex-wrap gap-8">
          {[
            { label: 'Features', href: '/#features' },
            { label: 'Demos', href: '/#demos' },
            { label: 'Why OrderMate?', href: '/#benefits' },
            { label: 'FAQ', href: '/faq' },
            { label: 'Support', href: 'mailto:support@11thandorange.com' },
          ].map((link) => (
            <a
              key={link.label}
              href={link.href}
              className="text-content-secondary hover:text-content text-sm transition-colors no-underline"
            >
              {link.label}
            </a>
          ))}
        </nav>

        {/* Copyright */}
        <p className="text-content-secondary text-sm">
          &copy; 2026 OrderMate. Built for Clover Merchants.
        </p>
      </div>
    </footer>
  );
}
