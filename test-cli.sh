#!/bin/bash

set -e

APK=`\ls sample/app/target/*.apk`
TEST_APK=`\ls sample/tests/target/*.apk`

java -jar spoon/target/spoon-*-jar-with-dependencies.jar --apk "$APK" --test-apk "$TEST_APK" --output target

open target/index.html
