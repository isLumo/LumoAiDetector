#!/bin/bash
set -e
if [ -f "./gradlew" ]; then
    ./gradlew clean shadowJar
    exit $?
fi
if ! command -v gradle &> /dev/null; then
    echo "Gradle not found in PATH. Install Gradle 8.5+ or use IntelliJ IDEA."
    exit 1
fi
gradle clean shadowJar
