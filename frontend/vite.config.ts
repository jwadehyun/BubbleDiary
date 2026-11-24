import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Spring Boot (port 8080) owns the API and the OAuth login flow. The dev server forwards those paths
// without rewriting the Host header, so OAuth redirects come back to localhost:5173.
const backend = 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backend,
      '/oauth2': backend,
      '/login/oauth2': backend,
      '/logout': backend,
    },
  },
  build: {
    // Spring Boot serves the built app at http://localhost:8080.
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
});
