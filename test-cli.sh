#!/bin/bash

set -e

APK=`\ls spoon-sample/app/target/*.apk`
TEST_APK=`\ls spoon-sample/tests/target/*.apk`

java -jar spoon/target/spoon-*-jar-with-dependencies.jar --apk "$APK" --test-apk "$TEST_APK" --output target

open target/index.html
