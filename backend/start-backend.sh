#!/bin/bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
cd "$(dirname "$0")"
mvn spring-boot:run
