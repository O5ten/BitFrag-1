#!/bin/sh
#
# TODO Read up on how to make this into a proper Makefile (or similar)
#
# Creates a nifty jar from all these .java files
if [ "$0" != "./build.sh" ]; then
    echo "Please run this from the root of the BitFrag directory"
    exit 1
fi
mkdir -p target
javac -d ./target/ src/main/java/net/comploud/code/bitfrag/*.java
cd target
jar cvef net.comploud.code.bitfrag.BitFrag BitFrag.jar *
echo 'Tadaa! One BitFrag.jar created. Run it with "java -jar BitFrag.jar"'
