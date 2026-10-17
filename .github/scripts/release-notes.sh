#!/usr/bin/env bash
# Prints Markdown release notes for <tag>, grouped by Conventional Commit type,
# covering commits since the previous v*.*.* tag (or all history for the first release).
# Usage: release-notes.sh <tag>
set -euo pipefail

tag="$1"
prev="$(git describe --tags --abbrev=0 --match 'v[0-9]*.[0-9]*.[0-9]*' "${tag}^" 2>/dev/null || true)"
range="${prev:+${prev}..}${tag}"
subjects="$(git log --no-merges --format='%s (%h)' "$range")"

section() {
  local title="$1" pattern="$2" lines
  lines="$(grep -E "$pattern" <<< "$subjects" || true)"
  if [[ -n "$lines" ]]; then
    printf '### %s\n\n' "$title"
    sed 's/^/- /' <<< "$lines"
    printf '\n'
  fi
}

breaking='^[a-z]+(\([^)]*\))?!:'
section "⚠️ Breaking changes" "$breaking"
section "Features" '^feat(\([^)]*\))?:'
section "Fixes" '^fix(\([^)]*\))?:'
section "Performance" '^perf(\([^)]*\))?:'
others="$(grep -vE "^(feat|fix|perf)(\([^)]*\))?:|${breaking}" <<< "$subjects" || true)"
if [[ -n "$others" ]]; then
  printf '### Other changes\n\n'
  sed 's/^/- /' <<< "$others"
  printf '\n'
fi

if [[ -n "$prev" ]]; then
  printf '**Full changelog:** %s...%s\n\n' "$prev" "$tag"
fi
printf 'Verify downloads with `sha256sum -c SHA256SUMS.txt`.\n'
