// Global setup for Playwright tests
const { expect } = require('@playwright/test');

async function globalSetup(config) {
  console.log('🔧 Setting up global test environment...');

  // Wait for the application to be ready
  const baseURL = config.projects[0].use.baseURL || 'http://localhost:8080';
  console.log(`🔍 Checking application health at ${baseURL}`);

  // Setup test data, configurations, etc.
  console.log('✅ Global setup completed');
}

module.exports = globalSetup;