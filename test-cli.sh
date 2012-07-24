#!/bin/bash

java -jar spoon/target/spoon-*-with-dep*.jar \
    --device-config test-device-configs.yml \
    --run-config test-run-config.yml \
    --debug
