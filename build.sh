#!/bin/bash

echo "=========================================="
echo "Building Recommendation FL Android App"
echo "=========================================="

# Check if we're in the right directory
if [ ! -d "client" ] || [ ! -d "server" ]; then
    echo "Error: Please run this script from the recommendation_fl directory"
    exit 1
fi

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
    echo "Warning: ANDROID_HOME not set. Make sure Android SDK is installed."
fi

# Navigate to client directory
cd client

echo "Cleaning previous builds..."
./gradlew clean

echo "Building debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "=========================================="
    echo "Build successful! 🎉"
    echo "=========================================="
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "To install on device/emulator:"
    echo "adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "To start the FL server:"
    echo "cd ../server && python recommendation_server.py"
    echo ""
    echo "Make sure to:"
    echo "1. Enable USB debugging on your Android device"
    echo "2. Connect device or start emulator"
    echo "3. Install the APK"
    echo "4. Start the FL server"
    echo "5. Configure the app with server IP and port"
else
    echo "=========================================="
    echo "Build failed! ❌"
    echo "=========================================="
    echo "Check the error messages above for details."
    exit 1
fi
