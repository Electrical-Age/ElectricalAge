#!/bin/bash -l

echo "Starting build..."
cd /app
./gradlew build

echo "Build finished !"
