#!/bin/bash

# Appsage APK Build Script
# This script builds the debug version of the application.

# Exit on any error
set -e

echo "----------------------------------------"
echo "🚀 Starting Appsage APK Build..."
echo "----------------------------------------"

# Ensure gradlew is executable
if [ -f "./gradlew" ]; then
    chmod +x gradlew
else
    echo "❌ Error: gradlew not found in the current directory."
    exit 1
fi

# Clean and build the debug APK
echo "📦 Running ./gradlew assembleDebug..."
./gradlew assembleDebug

echo ""
echo "----------------------------------------"
echo "✅ Build Successful!"
echo "----------------------------------------"
echo "The debug APK is located at:"
echo "app/build/outputs/apk/debug/app-debug.apk"
echo "----------------------------------------"
