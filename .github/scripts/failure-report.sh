#!/usr/bin/env bash
# Builds a Markdown failure report (failing step + last 150 log lines) into $CI_LOG_DIR/report.md
# and appends it to the job summary. Called from ci.yml with `if: failure()`.
set -euo pipefail

dir="${CI_LOG_DIR:?CI_LOG_DIR must be set}"
mkdir -p "$dir"
run_url="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
sha="${PR_HEAD_SHA:-$GITHUB_SHA}"

{
  echo "<!-- calwp-ci-report -->"
  if [[ -f "$dir/current-step" ]]; then
    step="$(cat "$dir/current-step")"
    echo "### ❌ CI failed at step: \`${step}\`"
    echo
    echo "Commit \`${sha:0:7}\` · [full run log](${run_url})"
    echo
    echo "<details open><summary>Last 150 lines of the <code>${step}</code> log</summary>"
    echo
    echo '```text'
    # Strip ANSI colors, cap line width, and neutralize code fences inside the log.
    tail -n 150 "$dir/current.log" | sed -e 's/\x1b\[[0-9;]*m//g' -e 's/```/`` `/g' | cut -c1-400
    echo '```'
    echo
    echo "</details>"
  else
    echo "### ❌ CI failed outside a Gradle step"
    echo
    echo "Commit \`${sha:0:7}\`. A setup or upload step failed; open the [run log](${run_url}) to see it."
  fi
} > "$dir/report.md"

cat "$dir/report.md" >> "$GITHUB_STEP_SUMMARY"
