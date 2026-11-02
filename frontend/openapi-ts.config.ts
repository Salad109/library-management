import { defineConfig } from '@hey-api/openapi-ts'

export default defineConfig({
  input: 'openapi.json',
  output: 'src/generated',
  plugins: [
    { name: '@hey-api/client-fetch', throwOnError: true },
    '@hey-api/sdk',
    { name: '@hey-api/typescript', enums: 'javascript' },
  ],
})
