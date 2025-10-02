#!/bin/bash

# Mira v0.1.0 - Firebase Setup Automation Script
# This script automates everything possible for Firebase setup

echo "🔥 Mira Firebase Complete Automation Script"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if google-services.json exists
check_google_services() {
    if [ -f "app/google-services.json" ]; then
        print_status $GREEN "✅ google-services.json found in app/ directory"
        return 0
    else
        print_status $RED "❌ google-services.json not found"
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
            print_status $GREEN "✅ Extracted Firebase App ID: $FIREBASE_APP_ID"
            return 0
        else
            print_status $RED "❌ Could not extract App ID from google-services.json"
            return 1
        fi
    else
        print_status $RED "❌ google-services.json not found"
        return 1
    fi
}

# Function to update app/build.gradle.kts with the correct App ID
update_gradle_app_id() {
    local app_id="$1"
    
    if [ -f "app/build.gradle.kts" ]; then
        # Create backup
        cp app/build.gradle.kts app/build.gradle.kts.backup
        
        # Use sed to replace the placeholder App ID with the actual one
        sed -i.tmp "s/appId = \"1:YOUR_PROJECT_NUMBER:android:YOUR_APP_ID\"/appId = \"$app_id\"/" app/build.gradle.kts
        
        # Clean up temp file
        rm -f app/build.gradle.kts.tmp
        
        if grep -q "appId = \"$app_id\"" app/build.gradle.kts; then
            print_status $GREEN "✅ Updated app/build.gradle.kts with App ID: $app_id"
            return 0
        else
            print_status $RED "❌ Failed to update app/build.gradle.kts"
            return 1
        fi
    else
        print_status $RED "❌ app/build.gradle.kts not found"
        return 1
    fi
}

# Function to check Firebase CLI login
check_firebase_cli() {
    if command -v firebase &> /dev/null; then
        if firebase projects:list &> /dev/null; then
            print_status $GREEN "✅ Firebase CLI is installed and logged in"
            return 0
        else
            print_status $YELLOW "⚠️  Firebase CLI is installed but not logged in"
            print_status $BLUE "Please run: firebase login"
            return 1
        fi
    else
        print_status $RED "❌ Firebase CLI is not installed"
        print_status $BLUE "Installing Firebase CLI..."
        npm install -g firebase-tools
        if [ $? -eq 0 ]; then
            print_status $GREEN "✅ Firebase CLI installed successfully"
            print_status $YELLOW "⚠️  Please run: firebase login"
            return 1
        else
            print_status $RED "❌ Failed to install Firebase CLI"
            return 1
        fi
    fi
}

# Function to build and upload APK
build_and_upload() {
    echo ""
    print_status $BLUE "🚀 Building internal APK..."
    
    # Clean and build
    ./gradlew clean
    ./gradlew assembleInternal
    
    if [ $? -eq 0 ]; then
        print_status $GREEN "✅ Internal APK built successfully"
    else
        print_status $RED "❌ Build failed"
        return 1
    fi
    
    echo ""
    print_status $BLUE "📤 Uploading to Firebase App Distribution..."
    
    # Upload to Firebase App Distribution
    ./gradlew appDistributionUploadInternal
    
    if [ $? -eq 0 ]; then
        print_status $GREEN "✅ APK uploaded to Firebase App Distribution successfully!"
        return 0
    else
        print_status $RED "❌ Upload failed"
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
    
    print_status $GREEN "✅ Created tester_invitation_template.txt"
}

# Function to create tester groups template
create_tester_groups_template() {
    cat > tester_groups_template.txt << 'EOF'
# Mira v0.1.0 - Tester Groups Template

## Internal Testers (5-8 people)
- Android developers
- QA testers
- Product managers
- Design team members

## Beta Testers (10-15 people)
- Power users
- Content creators
- Friends and family
- Community members

## QA Team (3-5 people)
- Device testing specialists
- Performance testing experts
- Compatibility testing team

## Email List Template
Copy and paste these into Firebase Console → App Distribution → Add testers:

### Internal Testers
[email1@domain.com]
[email2@domain.com]
[email3@domain.com]

### Beta Testers
[email4@domain.com]
[email5@domain.com]
[email6@domain.com]

### QA Team
[email7@domain.com]
[email8@domain.com]
[email9@domain.com]
EOF
    
    print_status $GREEN "✅ Created tester_groups_template.txt"
}

# Main execution
main() {
    print_status $BLUE "🔍 Checking prerequisites..."
    
    # Check if google-services.json exists
    if ! check_google_services; then
        echo ""
        print_status $YELLOW "📋 Manual steps required:"
        echo ""
        print_status $BLUE "1. Go to your Firebase Console: https://console.firebase.google.com/"
        print_status $BLUE "2. Click 'Add app' → Android icon"
        print_status $BLUE "3. Package name: com.mira.videoeditor.internal"
        print_status $BLUE "4. App nickname: Mira Internal Testing"
        print_status $BLUE "5. Download google-services.json"
        print_status $BLUE "6. Place it in app/ directory"
        print_status $BLUE "7. Run this script again"
        echo ""
        exit 1
    fi
    
    # Extract App ID from google-services.json
    if ! extract_app_id; then
        print_status $RED "❌ Cannot proceed without valid App ID"
        exit 1
    fi
    
    # Update Gradle file with correct App ID
    if ! update_gradle_app_id "$FIREBASE_APP_ID"; then
        print_status $RED "❌ Cannot proceed without updating Gradle configuration"
        exit 1
    fi
    
    # Check Firebase CLI
    if ! check_firebase_cli; then
        echo ""
        print_status $YELLOW "📋 Firebase CLI setup required:"
        print_status $BLUE "1. Run: firebase login"
        print_status $BLUE "2. Run this script again"
        echo ""
        exit 1
    fi
    
    # Build and upload APK
    if build_and_upload; then
        echo ""
        print_status $GREEN "🎉 Firebase setup complete!"
        echo ""
        print_status $YELLOW "📋 Next steps:"
        print_status $BLUE "1. Go to Firebase Console → App Distribution"
        print_status $BLUE "2. Click 'Add testers'"
        print_status $BLUE "3. Add email addresses of your testers"
        print_status $BLUE "4. Click 'Send invitations'"
        echo ""
        print_status $YELLOW "📊 Monitor your app:"
        print_status $BLUE "- Firebase Console → App Distribution"
        print_status $BLUE "- Check crash reports and analytics"
        print_status $BLUE "- Review tester feedback"
        echo ""
        
        # Create templates
        create_tester_template
        create_tester_groups_template
        
        print_status $GREEN "✅ Templates created:"
        print_status $BLUE "- tester_invitation_template.txt"
        print_status $BLUE "- tester_groups_template.txt"
        echo ""
        print_status $GREEN "🚀 Your Mira v0.1.0 Firebase App Distribution is ready!"
    else
        print_status $RED "❌ Setup incomplete - please check errors above"
        exit 1
    fi
}

# Run main function
main
