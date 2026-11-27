#!/bin/sh
# Compiles and runs all three test programs (no test framework needed). Needs a JDK 17+.
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir -p out out-test
javac -encoding UTF-8 -d out $(find src/main -name '*.java')
cp -r src/main/resources/* out/
javac -encoding UTF-8 -cp out -d out-test $(find src/test -name '*.java')
for T in ph.studytabai.ModelTests ph.studytabai.LogicTests ph.studytabai.ui.UiSmokeTest; do
  echo "=== $T"
  java -Djava.awt.headless=true -cp "out:out-test:lib/*" $T
done
