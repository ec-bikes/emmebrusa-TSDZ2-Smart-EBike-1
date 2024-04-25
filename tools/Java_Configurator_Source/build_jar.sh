#!/bin/bash
set -e

# JAVA_DIR="tools/Java_Configurator_Source"
# CLASS_DIR="$JAVA_DIR/classes"
# SRC_DIR="$JAVA_DIR/src"

# rm -rf "$CLASS_DIR"
# javac --release 8 -d "$CLASS_DIR" "$SRC_DIR/TSDZ2_Configurator.java"
# jar -c -f JavaConfigurator.jar -e TSDZ2_Configurator -C "$CLASS_DIR" .
# rm -rf "$CLASS_DIR"

CLASS_DIR="classes"

rm -rf "$CLASS_DIR"
javac --release 8 -d "$CLASS_DIR" src/*.java src/util/*.java
jar -c -f ../../JavaConfigurator.jar -e TSDZ2_Configurator -C "$CLASS_DIR" .
# rm -rf "$CLASS_DIR"
