#!/bin/bash

cd "$(dirname $0)"

# remove old data from gradle.properties
cp gradle.properties gradle.properties.back
cat gradle.properties | grep -vE "BUILD_HOST|BUILD_DATE|JAVA_VERSION|GIT_REVISION" > gradle.properties.new
mv gradle.properties.new gradle.properties

# set variables in gradle.properties
echo "" >> gradle.properties
echo "BUILD_HOST = '$(hostname)'" >> gradle.properties
echo "BUILD_DATE = '$(date)'" >> gradle.properties
echo "JAVA_VERSION = '$(java -version 2>&1 | awk -F\" '/version/ {print $2}')'" >> gradle.properties
echo "GIT_REVISION = '$(git log --pretty=format:'%H' -n 1)'" >> gradle.properties

# build the game
./gradlew setupDecompWorkspace
./gradlew build

mv gradle.properties.back gradle.properties
