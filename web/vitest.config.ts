import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
export default defineConfig({
  plugins: [react()],
  test: {
    include: ['src/**/*.test.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/main.tsx', 'src/lib/types.ts'],
      thresholds: {
        'src/lib/password.ts': { lines: 100, branches: 100 },
        'src/lib/shopping.ts': { lines: 100, branches: 70 },
      },
      reporter: ['text', 'html', 'json-summary', 'lcov'],
    },
  },
})
