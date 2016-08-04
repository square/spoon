#!/bin/bash
#
# Simple script to test Java-based execution of Spoon. You must have assembled
# the jar prior to running this script (i.e., mvn clean verify).

set -e

APK=`\ls spoon-sample/app/target/*.apk`
TEST_APK=`\ls spoon-sample/tests/target/*.apk`

#java -jar spoon-runner/target/spoon-*-jar-with-dependencies.jar --apk "$APK" --test-apk "$TEST_APK" -skipDevices "emulator-5556" --output target
java -jar spoon-runner/target/spoon-*-jar-with-dependencies.jar --apk "$APK" --test-apk "$TEST_APK"  --output target

open target/index.html
