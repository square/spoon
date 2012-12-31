#!/bin/bash

echo ""
echo "Make sure test devices are plugged in and recognized by ADB."
echo ""
echo "Press [enter] when ready..."
read

set -ex

git rm -rf sample
cd ..
mvn clean verify
cp -R sample/tests/target/spoon-output website/sample
cd -
git add sample

echo "Sample output updated. Make sure to commit the changes."