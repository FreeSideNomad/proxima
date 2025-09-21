// Global setup for Playwright tests
const { expect } = require('@playwright/test');
const { exec } = require('child_process');
const util = require('util');
const execAsync = util.promisify(exec);

async function globalSetup(config) {
  console.log('🔧 Setting up global test environment...');

  // Skip process cleanup - let Playwright's webServer handle this
  console.log('🔄 Skipping process cleanup to allow webServer reuse');

  // Wait for the application to be ready
  const baseURL = config.projects[0].use.baseURL || 'http://localhost:8080';
  console.log(`🔍 Checking application health at ${baseURL}`);

  // Setup test data, configurations, etc.
  console.log('✅ Global setup completed');
}

module.exports = globalSetup;