#!/bin/sh
# Build and run without Maven. Needs a JDK 17+. Put pdfbox + its dependencies in ./lib for PDF support.
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main -name '*.java')
cp -r src/main/resources/* out/
exec java -cp "out:lib/*" ph.studytabai.Main
