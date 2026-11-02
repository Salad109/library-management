import { svelte } from '@sveltejs/vite-plugin-svelte'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [svelte()],
  build: {
    outDir: '../target/classes/static',
    emptyOutDir: true,
    rolldownOptions: { input: ['login.html'] },
  },
  server: { proxy: { '/api': 'http://localhost:8080' } },
})
