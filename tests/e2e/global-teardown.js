// Global teardown for Playwright tests
const { exec } = require('child_process');
const util = require('util');
const execAsync = util.promisify(exec);

async function globalTeardown(config) {
  console.log('🧹 Cleaning up global test environment...');

  // Skip process cleanup - let existing server continue running for reuse
  console.log('🔄 Skipping process cleanup to allow server reuse');

  console.log('✅ Global teardown completed');
}

module.exports = globalTeardown;