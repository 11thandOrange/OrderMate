/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'om-orange': '#FF9F43',
        'om-orange-hover': '#e68a2e',
        'om-orange-dark': '#F46809',
        'om-purple': '#7367F0',
        'om-green': '#3ccd79',
        'om-dark': '#1a1a2e',
        'om-dark-secondary': '#16213e',
        'om-glass': 'rgba(255, 255, 255, 0.1)',
        'om-glass-border': 'rgba(255, 255, 255, 0.2)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      backgroundImage: {
        'gradient-primary': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'gradient-dark': 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #1a1a2e 100%)',
      },
    },
  },
  plugins: [],
}
