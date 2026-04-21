import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['src/**/*.spec.ts'],
    environment: 'node',
    globals: true,
    clearMocks: true,
    setupFiles: ['src/test-setup.ts'],
  },
});


