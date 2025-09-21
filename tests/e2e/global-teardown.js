// Global teardown for Playwright tests

async function globalTeardown(config) {
  console.log('🧹 Cleaning up global test environment...');

  // Cleanup test data, close connections, etc.
  console.log('✅ Global teardown completed');
}

module.exports = globalTeardown;