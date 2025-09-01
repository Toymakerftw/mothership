#!/bin/bash

# Source the signing properties
source signing.properties

# Build the release APK
./gradlew assembleRelease

# Print the path to the generated APK
echo "Signed APK generated at: app/build/outputs/apk/release/app-release.apk"