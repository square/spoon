#!/bin/bash

java -jar cli/target/*-with-dep*.jar \
    --device-config test-device-configs.yml \
    --run-config test-run-config.yml
