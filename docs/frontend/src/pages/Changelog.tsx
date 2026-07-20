import { Link } from 'react-router-dom';
import { DocsLayout } from '../components/Layout/DocsLayout';
import { changelog } from '../data/changelog';

const sectionStyle: Record<'added' | 'changed' | 'fixed', { label: string; color: string }> = {
  added: { label: 'Added', color: '#34d399' },
  changed: { label: 'Changed', color: '#9ca3af' },
  fixed: { label: 'Fixed', color: '#f87171' },
};

export function Changelog() {
  return (
    <DocsLayout>
      <nav style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', color: '#6b7280', marginBottom: '24px' }}>
        <Link to="/" style={{ color: '#6b7280', textDecoration: 'none' }}>Docs</Link>
        <span>/</span>
        <span style={{ color: '#9ca3af' }}>Changelog</span>
      </nav>

      <h1 style={{ fontSize: '36px', fontWeight: 700, color: '#ffffff', marginBottom: '16px' }}>Changelog</h1>
      <p style={{ fontSize: '18px', color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
        Generated from git history via <code style={{ color: '#fb923c' }}>scripts/generate-changelog.mjs</code>, grouped
        by day and bucketed by conventional-commit type (feat → Added, fix → Fixed, everything else → Changed).
        Re-run the script to pick up new commits.
      </p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '40px' }}>
        {changelog.map((entry) => (
          <section key={entry.date} style={{ borderLeft: '1px solid rgba(255,255,255,0.1)', paddingLeft: '20px' }}>
            <h2 style={{ fontSize: '18px', fontWeight: 600, color: '#ffffff', fontFamily: 'monospace', marginBottom: '12px' }}>
              {entry.date}
            </h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {(['added', 'changed', 'fixed'] as const).map((key) => {
                const items = entry[key];
                if (items.length === 0) return null;
                const { label, color } = sectionStyle[key];
                return (
                  <div key={key}>
                    <span style={{ fontSize: '11px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color }}>
                      {label}
                    </span>
                    <ul style={{ marginTop: '8px', display: 'flex', flexDirection: 'column', gap: '6px', listStyle: 'none', padding: 0 }}>
                      {items.map((item) => (
                        <li key={item} style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', color: '#d1d5db', fontSize: '14px' }}>
                          <span style={{ width: '5px', height: '5px', borderRadius: '50%', background: color, marginTop: '7px', flexShrink: 0 }} />
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                );
              })}
            </div>
          </section>
        ))}
      </div>
    </DocsLayout>
  );
}
