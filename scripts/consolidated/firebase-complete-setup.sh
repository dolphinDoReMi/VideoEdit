#!/bin/bash

# Mira v0.1.0 - Complete Firebase Setup Automation Script
# This script automates everything possible for Firebase setup

echo "🔥 Mira Firebase Complete Setup Script"
echo "======================================"
echo ""

# Function to check if google-services.json exists
check_google_services() {
    if [ -f "app/google-services.json" ]; then
        echo "✅ google-services.json found in app/ directory"
        return 0
    else
        echo "❌ google-services.json not found"
        return 1
    fi
}

# Function to extract App ID from google-services.json
extract_app_id() {
    if [ -f "app/google-services.json" ]; then
        # Extract the project number and app ID from google-services.json
        PROJECT_NUMBER=$(grep -o '"project_number":"[^"]*"' app/google-services.json | cut -d'"' -f4)
        APP_ID=$(grep -o '"mobilesdk_app_id":"[^"]*"' app/google-services.json | cut -d'"' -f4)
        
        if [ -n "$PROJECT_NUMBER" ] && [ -n "$APP_ID" ]; then
            FIREBASE_APP_ID="1:${PROJECT_NUMBER}:android:${APP_ID}"
            echo "✅ Extracted Firebase App ID: $FIREBASE_APP_ID"
            return 0
        else
            echo "❌ Could not extract App ID from google-services.json"
            return 1
        fi
    else
        echo "❌ google-services.json not found"
        return 1
    fi
}

# Function to update app/build.gradle.kts with the correct App ID
update_gradle_app_id() {
    local app_id="$1"
    
    if [ -f "app/build.gradle.kts" ]; then
        # Use sed to replace the placeholder App ID with the actual one
        sed -i.bak "s/appId = \"1:YOUR_PROJECT_NUMBER:android:YOUR_APP_ID\"/appId = \"$app_id\"/" app/build.gradle.kts
        
        if grep -q "appId = \"$app_id\"" app/build.gradle.kts; then
            echo "✅ Updated app/build.gradle.kts with App ID: $app_id"
            return 0
        else
            echo "❌ Failed to update app/build.gradle.kts"
            return 1
        fi
    else
        echo "❌ app/build.gradle.kts not found"
        return 1
    fi
}

# Function to check Firebase CLI login
check_firebase_cli() {
    if command -v firebase &> /dev/null; then
        if firebase projects:list &> /dev/null; then
            echo "✅ Firebase CLI is installed and logged in"
            return 0
        else
            echo "❌ Firebase CLI is installed but not logged in"
            echo "Please run: firebase login"
            return 1
        fi
    else
        echo "❌ Firebase CLI is not installed"
        echo "Please install: npm install -g firebase-tools"
        return 1
    fi
}

# Function to build and upload APK
build_and_upload() {
    echo ""
    echo "🚀 Building internal APK..."
    
    # Clean and build
    ./gradlew clean
    ./gradlew assembleInternal
    
    if [ $? -eq 0 ]; then
        echo "✅ Internal APK built successfully"
    else
        echo "❌ Build failed"
        return 1
    fi
    
    echo ""
    echo "📤 Uploading to Firebase App Distribution..."
    
    # Upload to Firebase App Distribution
    ./gradlew appDistributionUploadInternal
    
    if [ $? -eq 0 ]; then
        echo "✅ APK uploaded to Firebase App Distribution successfully!"
        return 0
    else
        echo "❌ Upload failed"
        return 1
    fi
}

# Function to create tester invitation template
create_tester_template() {
    cat > tester_invitation_template.txt << 'EOF'
Subject: Mira v0.1.0 Internal Testing - Firebase App Distribution

Hi [Tester Name],

You've been invited to test Mira v0.1.0 through Firebase App Distribution!

🎯 What is Mira?
Mira is an AI-powered video editor that automatically selects the most engaging segments from your videos.

📱 How to Install:
1. Click the Firebase invitation link in your email
2. Download the APK (44MB)
3. Install on your Android device (7.0+)
4. Start testing!

🧪 Testing Focus:
- Core functionality (video selection, auto-cut)
- Performance on different devices
- UI/UX feedback
- Bug reporting

📞 Support: [your-email@domain.com]

Thank you for helping us make Mira better!

Best regards,
The Mira Development Team
EOF
    
    echo "✅ Created tester_invitation_template.txt"
}

# Main execution
main() {
    echo "🔍 Checking prerequisites..."
    
    # Check if google-services.json exists
    if ! check_google_services; then
        echo ""
        echo "📋 Manual steps required:"
        echo ""
        echo "1. Go to your Firebase Console: https://console.firebase.google.com/"
        echo "2. Click 'Add app' → Android icon"
        echo "3. Package name: com.mira.videoeditor.internal"
        echo "4. App nickname: Mira Internal Testing"
        echo "5. Download google-services.json"
        echo "6. Place it in app/ directory"
        echo "7. Run this script again"
        echo ""
        exit 1
    fi
    
    # Extract App ID from google-services.json
    if ! extract_app_id; then
        echo "❌ Cannot proceed without valid App ID"
        exit 1
    fi
    
    # Update Gradle file with correct App ID
    if ! update_gradle_app_id "$FIREBASE_APP_ID"; then
        echo "❌ Cannot proceed without updating Gradle configuration"
        exit 1
    fi
    
    # Check Firebase CLI
    if ! check_firebase_cli; then
        echo ""
        echo "📋 Firebase CLI setup required:"
        echo "1. Install: npm install -g firebase-tools"
        echo "2. Login: firebase login"
        echo "3. Run this script again"
        echo ""
        exit 1
    fi
    
    # Build and upload APK
    if build_and_upload; then
        echo ""
        echo "🎉 Firebase setup complete!"
        echo ""
        echo "📋 Next steps:"
        echo "1. Go to Firebase Console → App Distribution"
        echo "2. Click 'Add testers'"
        echo "3. Add email addresses of your testers"
        echo "4. Click 'Send invitations'"
        echo ""
        echo "📊 Monitor your app:"
        echo "- Firebase Console → App Distribution"
        echo "- Check crash reports and analytics"
        echo "- Review tester feedback"
        echo ""
        
        # Create tester invitation template
        create_tester_template
        
        echo "✅ Tester invitation template created: tester_invitation_template.txt"
        echo ""
        echo "🚀 Your Mira v0.1.0 Firebase App Distribution is ready!"
    else
        echo "❌ Setup incomplete - please check errors above"
        exit 1
    fi
}

# Run main function
main
