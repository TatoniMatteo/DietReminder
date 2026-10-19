import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/global-setup.ts',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report/html', open: 'never' }],
    ['junit', { outputFile: 'playwright-report/junit.xml' }],
    ['json', { outputFile: 'playwright-report/results.json' }],
  ],
  projects: [
    { name: 'chromium-desktop', use: { browserName: 'chromium' } },
    { name: 'firefox-desktop', use: { browserName: 'firefox' } },
    {
      name: 'chromium-mobile',
      use: {
        browserName: 'chromium',
        viewport: { width: 393, height: 852 },
        isMobile: true,
        hasTouch: true,
      },
    },
  ],
  workers: 1,
  timeout: 45000,
  use: {
    baseURL: process.env.WEB_TEST_URL ?? 'http://127.0.0.1:8088',
    locale: 'it-IT',
    browserName: 'chromium',
    viewport: { width: 1440, height: 1000 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
})
