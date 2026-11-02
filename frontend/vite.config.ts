import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [tailwindcss(), svelte()],
  build: {
    outDir: '../target/classes/static',
    emptyOutDir: true,
    rolldownOptions: { input: ['login.html', 'books.html'] },
  },
  server: { proxy: { '/api': 'http://localhost:8080' } },
})
