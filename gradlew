#!/bin/bash

APP_HOME=$PWD
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"
GRADLE_OPTS="-Dorg.gradle.daemon=false"

if [ "x$JAVA_HOME" != "x" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec $JAVACMD $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -Dorg.gradle.appname=gradlew -classpath $CLASSPATH org.gradle.wrapper.GradleWrapperMain "$@"
