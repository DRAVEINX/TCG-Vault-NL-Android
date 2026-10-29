#!/usr/bin/env sh
# Minimal fallback until `gradle wrapper --gradle-version 8.11.1` generates the standard wrapper JAR.
if [ -f "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -classpath "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
fi
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "Gradle-wrapper.jar ontbreekt. Installeer Gradle 8.11.1 en voer 'gradle wrapper --gradle-version 8.11.1' uit." >&2
exit 1
