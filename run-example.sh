#!/usr/bin/env sh
set -eu

classes_dir="${TMPDIR:-/tmp}/course-release-balance-classes"
mkdir -p "$classes_dir"
javac -d "$classes_dir" $(find src/main/java -name '*.java')
java -cp "$classes_dir" education.devtools.DeveloperToolsRechargeExample \
  "release-42" "build-108" "algebra-foundations"
