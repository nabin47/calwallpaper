#!/usr/bin/env bash
# Runs a command, mirrors its output to a log file, and records the step name so
# failure-report.sh can tell which step failed and show the end of its log.
# Usage: run-step.sh "<step name>" <command> [args...]
set -euo pipefail

name="$1"
shift
dir="${CI_LOG_DIR:?CI_LOG_DIR must be set}"
mkdir -p "$dir"
printf '%s\n' "$name" > "$dir/current-step"
"$@" 2>&1 | tee "$dir/current.log"
# Success: clear the marker so a later non-Gradle failure is not blamed on this step.
rm -f "$dir/current-step"
