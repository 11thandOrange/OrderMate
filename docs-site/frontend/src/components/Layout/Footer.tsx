import { Container } from './Container';

const footerLinks = [
  { label: 'Website', href: 'https://getordermate.com' },
  { label: 'GitHub', href: 'https://github.com/11thandOrange/OrderMate' },
  { label: 'Support', href: 'mailto:support@11thandorange.com' },
];

export function Footer() {
  return (
    <footer className="py-10 border-t border-surface-border">
      <Container>
        <div className="flex flex-col md:flex-row justify-between items-center gap-6">
          {/* Logo & Brand */}
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-gradient-brand rounded-xl flex items-center justify-center shadow-lg shadow-brand/20">
              <span className="text-content-inverse font-bold text-sm">OM</span>
            </div>
            <div>
              <span className="text-content font-medium">OrderMate</span>
              <span className="text-content-muted ml-2">Documentation</span>
            </div>
          </div>

          {/* Links */}
          <nav className="flex items-center gap-8 text-sm">
            {footerLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                target="_blank"
                rel="noopener noreferrer"
                className="text-content-secondary hover:text-content transition-colors"
              >
                {link.label}
              </a>
            ))}
          </nav>
        </div>

        {/* Copyright */}
        <div className="mt-8 pt-6 border-t border-surface-border text-center text-sm text-content-muted">
          © {new Date().getFullYear()} 11th & Orange. All rights reserved.
        </div>
      </Container>
    </footer>
  );
}
