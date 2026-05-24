import { Container } from './Container';

const footerLinks = [
  { label: 'Website', href: 'https://getordermate.com' },
  { label: 'GitHub', href: 'https://github.com/11thandOrange/OrderMate' },
  { label: 'Support', href: 'mailto:support@11thandorange.com' },
];

export function Footer() {
  return (
    <footer className="py-10 border-t border-white/5" style={{ background: '#0f1117' }}>
      <Container>
        <div className="flex flex-col md:flex-row justify-between items-center gap-6">
          <div className="flex items-center gap-2 text-xl font-bold">
            <span className="text-orange-500">Order</span>
            <span className="text-white">Mate</span>
            <span className="text-gray-500 font-normal ml-1 text-base">Documentation</span>
          </div>

          <nav className="flex items-center gap-8 text-sm">
            {footerLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                target="_blank"
                rel="noopener noreferrer"
                className="text-gray-400 hover:text-white transition-colors"
              >
                {link.label}
              </a>
            ))}
          </nav>
        </div>

        <div className="mt-8 pt-6 border-t border-white/5 text-center text-sm text-gray-600">
          &copy; {new Date().getFullYear()} 11th &amp; Orange. All rights reserved.
        </div>
      </Container>
    </footer>
  );
}
