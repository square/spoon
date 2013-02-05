#!/bin/bash

echo ""
echo "Make sure test devices are plugged in and recognized by ADB."
echo ""
echo "Press [enter] when ready..."
read

set -ex

rm -rf sample
cd ..
mvn clean verify -e
cp -R spoon-sample/tests/target/spoon-output website/sample
cd -
git add sample
git add -u sample

echo "Sample output updated. Make sure to commit the changes."
