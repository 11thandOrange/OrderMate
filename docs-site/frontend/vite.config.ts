import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: process.env.NODE_ENV === 'production' ? '/OrderMate/' : '/',
  server: {
    host: '0.0.0.0',
    port: 12001,
    allowedHosts: [
      'localhost',
      '127.0.0.1',
      '.all-hands.dev',
      'work-1-mqsvvknpjhahubjf.prod-runtime.all-hands.dev',
      'work-2-mqsvvknpjhahubjf.prod-runtime.all-hands.dev',
    ],
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
})
