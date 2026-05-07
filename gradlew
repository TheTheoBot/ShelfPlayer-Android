#!/usr/bin/env sh
set -eu

APP_HOME="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
CLASSPATH="$APP_HOME/gradle/wrapper/*"

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
