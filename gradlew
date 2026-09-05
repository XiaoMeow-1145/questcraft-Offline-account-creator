#!/bin/sh
echo "This is a Gradle wrapper script for Unix-based systems."
# This script would normally execute Gradle, but for our purposes
# we'll just make sure it exists as part of the standard Android project structure
exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"