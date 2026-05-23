/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      // ===================
      // COLOR TOKENS
      // ===================
      colors: {
        // Brand colors
        brand: {
          DEFAULT: '#FF9F43',
          hover: '#e68a2e',
          dark: '#F46809',
          muted: 'rgba(255, 159, 67, 0.1)',
        },
        accent: {
          purple: '#7367F0',
          green: '#3ccd79',
          blue: '#3b82f6',
          amber: '#f59e0b',
          red: '#ef4444',
        },
        // Semantic background colors
        background: {
          DEFAULT: '#0f0f10',
          elevated: '#141416',
          surface: '#1a1a1e',
          overlay: 'rgba(0, 0, 0, 0.5)',
        },
        // Surface colors for cards, panels
        surface: {
          DEFAULT: 'rgba(255, 255, 255, 0.03)',
          hover: 'rgba(255, 255, 255, 0.06)',
          active: 'rgba(255, 255, 255, 0.09)',
          border: 'rgba(255, 255, 255, 0.08)',
          'border-hover': 'rgba(255, 255, 255, 0.15)',
        },
        // Text colors
        content: {
          DEFAULT: '#ffffff',
          secondary: '#a1a1aa',
          muted: '#71717a',
          inverse: '#0f0f10',
        },
        // Status colors
        status: {
          success: '#22c55e',
          warning: '#f59e0b',
          error: '#ef4444',
          info: '#3b82f6',
        },
        // Legacy support (can remove after full migration)
        'om-orange': '#FF9F43',
        'om-orange-hover': '#e68a2e',
        'om-orange-dark': '#F46809',
        'om-purple': '#7367F0',
        'om-green': '#3ccd79',
      },

      // ===================
      // SPACING TOKENS
      // ===================
      spacing: {
        // Container padding
        'container-x': '1.5rem',      // 24px - mobile
        'container-x-lg': '2.5rem',   // 40px - desktop
        // Section spacing
        'section-y': '5rem',          // 80px
        'section-y-sm': '3rem',       // 48px
        // Card padding
        'card': '1.5rem',             // 24px
        'card-sm': '1rem',            // 16px
        // Component gaps
        'gap-xs': '0.5rem',           // 8px
        'gap-sm': '0.75rem',          // 12px
        'gap-md': '1rem',             // 16px
        'gap-lg': '1.5rem',           // 24px
        'gap-xl': '2rem',             // 32px
      },

      // ===================
      // TYPOGRAPHY
      // ===================
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      fontSize: {
        // Display sizes
        'display-lg': ['3.5rem', { lineHeight: '1.1', fontWeight: '800' }],
        'display': ['3rem', { lineHeight: '1.1', fontWeight: '800' }],
        'display-sm': ['2.25rem', { lineHeight: '1.2', fontWeight: '700' }],
        // Heading sizes
        'heading-lg': ['1.875rem', { lineHeight: '1.3', fontWeight: '700' }],
        'heading': ['1.5rem', { lineHeight: '1.4', fontWeight: '600' }],
        'heading-sm': ['1.25rem', { lineHeight: '1.4', fontWeight: '600' }],
        // Body sizes  
        'body-lg': ['1.125rem', { lineHeight: '1.6' }],
        'body': ['1rem', { lineHeight: '1.6' }],
        'body-sm': ['0.875rem', { lineHeight: '1.5' }],
        // Caption
        'caption': ['0.75rem', { lineHeight: '1.4' }],
      },

      // ===================
      // BORDER RADIUS
      // ===================
      borderRadius: {
        'card': '1rem',        // 16px
        'card-lg': '1.5rem',   // 24px
        'button': '0.5rem',    // 8px
        'badge': '9999px',     // pill
        'input': '0.75rem',    // 12px
      },

      // ===================
      // SHADOWS
      // ===================
      boxShadow: {
        'card': '0 4px 24px rgba(0, 0, 0, 0.2)',
        'card-hover': '0 8px 32px rgba(0, 0, 0, 0.3)',
        'button': '0 4px 16px rgba(255, 159, 67, 0.25)',
        'button-hover': '0 8px 24px rgba(255, 159, 67, 0.35)',
        'glow': '0 0 20px rgba(255, 159, 67, 0.3)',
      },

      // ===================
      // TRANSITIONS
      // ===================
      transitionDuration: {
        'fast': '150ms',
        'normal': '200ms',
        'slow': '300ms',
      },

      // ===================
      // MAX WIDTHS
      // ===================
      maxWidth: {
        'container': '1200px',
        'container-lg': '1400px',
        'content': '720px',
        'prose': '65ch',
      },

      // ===================
      // GRADIENTS
      // ===================
      backgroundImage: {
        'gradient-brand': 'linear-gradient(135deg, #FF9F43 0%, #F46809 100%)',
        'gradient-surface': 'linear-gradient(135deg, rgba(255,255,255,0.05) 0%, rgba(255,255,255,0.02) 100%)',
        'gradient-glow': 'radial-gradient(circle at center, rgba(255,159,67,0.15) 0%, transparent 70%)',
      },
    },
  },
  plugins: [],
}
