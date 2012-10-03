#!/bin/bash

java -jar spoon/target/spoon-*-with-dep*.jar \
    --apk sample/app/target/spoon-sample-app-1.0.0-SNAPSHOT.apk \
    --test-apk sample/tests/target/spoon-sample-tests-1.0.0-SNAPSHOT.apk \
    --title "Spoon Sample" \
    --debug
