#!/usr/bin/env bash
# Runs Android Lint for every module and, on failure, prints each module's full text report, so the CI
# failure comment lists every issue instead of only the first one Gradle shows.
set -uo pipefail

if ./gradlew lint --continue; then
  exit 0
fi
echo
echo "===== Full lint reports ====="
find . -path '*lint_intermediate_text_report*' -name '*.txt' -print0 | sort -z | xargs -0 -r -I{} sh -c 'echo "--- {}"; cat "{}"'
exit 1
