#!/usr/bin/env node

/**
 * Test validation script to check if all test files are properly structured
 * Run with: node cypress/validate-tests.js
 */

const fs = require('fs');
const path = require('path');

const testFiles = [
    'cypress/e2e/auth.cy.js',
    'cypress/e2e/dcf-calculation.cy.js',
    'cypress/e2e/watchlist.cy.js',
    'cypress/e2e/error-scenarios.cy.js',
    'cypress/e2e/all-tests.cy.js'
];

const supportFiles = [
    'cypress/support/commands.js',
    'cypress/support/e2e.js'
];

const configFiles = [
    'cypress.config.js'
];

console.log('🔍 Validating Cypress test structure...\n');

let allValid = true;

// Check test files
console.log('📋 Test Files:');
testFiles.forEach(file => {
    const filePath = path.join(__dirname, '..', file);
    if (fs.existsSync(filePath)) {
        const content = fs.readFileSync(filePath, 'utf8');

        // Basic validation checks
        const hasDescribe = content.includes('describe(');
        const hasIt = content.includes('it(');
        const hasCyCommands = content.includes('cy.');

        if (hasDescribe && hasIt && hasCyCommands) {
            console.log(`  ✅ ${file} - Valid test structure`);
        } else {
            console.log(`  ❌ ${file} - Invalid test structure`);
            allValid = false;
        }
    } else {
        console.log(`  ❌ ${file} - File not found`);
        allValid = false;
    }
});

// Check support files
console.log('\n🛠  Support Files:');
supportFiles.forEach(file => {
    const filePath = path.join(__dirname, '..', file);
    if (fs.existsSync(filePath)) {
        console.log(`  ✅ ${file} - Found`);
    } else {
        console.log(`  ❌ ${file} - File not found`);
        allValid = false;
    }
});

// Check config files
console.log('\n⚙️  Configuration Files:');
configFiles.forEach(file => {
    const filePath = path.join(__dirname, '..', file);
    if (fs.existsSync(filePath)) {
        console.log(`  ✅ ${file} - Found`);
    } else {
        console.log(`  ❌ ${file} - File not found`);
        allValid = false;
    }
});

// Check package.json scripts
console.log('\n📦 Package.json Scripts:');
const packageJsonPath = path.join(__dirname, '..', 'package.json');
if (fs.existsSync(packageJsonPath)) {
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    const scripts = packageJson.scripts || {};

    const requiredScripts = ['cypress:open', 'cypress:run', 'test:e2e', 'test:e2e:open'];
    requiredScripts.forEach(script => {
        if (scripts[script]) {
            console.log(`  ✅ ${script} - Found`);
        } else {
            console.log(`  ❌ ${script} - Missing`);
            allValid = false;
        }
    });
} else {
    console.log('  ❌ package.json - File not found');
    allValid = false;
}

// Summary
console.log('\n' + '='.repeat(50));
if (allValid) {
    console.log('🎉 All tests and configuration files are valid!');
    console.log('\nTo run the tests:');
    console.log('1. Start the backend server: cd backend && ./mvnw spring-boot:run');
    console.log('2. Start the frontend server: cd frontend && npm start');
    console.log('3. Run tests: npm run test:e2e');
    process.exit(0);
} else {
    console.log('❌ Some issues found. Please fix them before running tests.');
    process.exit(1);
}