#!/bin/bash

java -cp cli/target/*-with-dep*.jar com.squareup.spoon.cli.CLI \
    --device-config test-device-configs.yml \
    --run-config test-run-config.yml
