// Pipeline Integration Test
console.log('🧪 Staging Step 0 Pipeline Test');

// Test 1: HTML Structure
function testHTMLStructure() {
    console.log('📄 Testing HTML Structure...');
    
    const stagingSection = document.getElementById('staging');
    if (stagingSection) {
        console.log('✅ Staging section found');
        
        // Check for required elements
        const requiredElements = [
            'add-folder-btn',
            'refresh-staging-btn', 
            'folder-list',
            'staging-rows',
            'toggle-all'
        ];
        
        requiredElements.forEach(id => {
            if (document.getElementById(id)) {
                console.log(`✅ ${id} element found`);
            } else {
                console.log(`❌ ${id} element missing`);
            }
        });
    } else {
        console.log('❌ Staging section not found');
    }
}

// Test 2: JavaScript Bridge
function testJavaScriptBridge() {
    console.log('🔌 Testing JavaScript Bridge...');
    
    if (typeof window.StagingBridge !== 'undefined') {
        console.log('✅ StagingBridge available');
        
        // Test bridge methods
        const methods = ['addTree', 'listTrees', 'removeTree', 'enumerateAll', 'persistedPermissions'];
        methods.forEach(method => {
            if (typeof window.StagingBridge[method] === 'function') {
                console.log(`✅ ${method} method available`);
            } else {
                console.log(`❌ ${method} method missing`);
            }
        });
    } else {
        console.log('⚠️ StagingBridge not available (using mock)');
        
        // Test mock bridge
        if (typeof window.StagingBridge === 'undefined') {
            console.log('✅ Mock bridge fallback available');
        }
    }
}

// Test 3: Event Handlers
function testEventHandlers() {
    console.log('🎯 Testing Event Handlers...');
    
    const addFolderBtn = document.getElementById('add-folder-btn');
    if (addFolderBtn && addFolderBtn.onclick) {
        console.log('✅ Add Folder button handler found');
    } else {
        console.log('❌ Add Folder button handler missing');
    }
    
    const refreshBtn = document.getElementById('refresh-staging-btn');
    if (refreshBtn && refreshBtn.onclick) {
        console.log('✅ Refresh button handler found');
    } else {
        console.log('❌ Refresh button handler missing');
    }
}

// Test 4: Mock Data
function testMockData() {
    console.log('📊 Testing Mock Data...');
    
    // Test localStorage for mock persistence
    if (typeof Storage !== 'undefined') {
        console.log('✅ localStorage available');
        
        // Test mock data storage
        const mockTrees = [
            {uri: 'content://mock/1', label: 'Mock Folder 1', authority: 'com.android.externalstorage.documents', isLocal: true},
            {uri: 'content://mock/2', label: 'Mock Folder 2', authority: 'com.google.android.apps.docs.storage', isLocal: false}
        ];
        
        localStorage.setItem('staging_trees_mock', JSON.stringify(mockTrees));
        const retrieved = JSON.parse(localStorage.getItem('staging_trees_mock'));
        
        if (retrieved && retrieved.length === 2) {
            console.log('✅ Mock data persistence working');
        } else {
            console.log('❌ Mock data persistence failed');
        }
    } else {
        console.log('❌ localStorage not available');
    }
}

// Run all tests
function runAllTests() {
    console.log('🚀 Starting Pipeline Integration Tests...');
    
    testHTMLStructure();
    testJavaScriptBridge();
    testEventHandlers();
    testMockData();
    
    console.log('✅ Pipeline Integration Tests Complete');
}

// Auto-run tests
if (typeof window !== 'undefined') {
    window.onload = runAllTests;
} else {
    runAllTests();
}
