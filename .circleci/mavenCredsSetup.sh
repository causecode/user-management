#!/bin/sh

GRADLE_PROPERTIES="./gradle.properties"
export GRADLE_PROPERTIES
echo "Gradle Properties should exist at $GRADLE_PROPERTIES"

if [ ! -f "$GRADLE_PROPERTIES" ]; then
    echo "Gradle Properties does not exist"

    echo "Creating Gradle Properties file..."
    touch $GRADLE_PROPERTIES
fi

echo "Writing MAVEN_CREDENTIALS to gradle.properties..."
echo "ossrhUsername=$OSSRH_USERNAME" >> $GRADLE_PROPERTIES
echo "ossrhPassword=$OSSRH_PASSWORD" >> $GRADLE_PROPERTIES
