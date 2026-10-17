#!/usr/bin/env bash
# Fails if the Room compiler produced schema files that are missing from, or differ from, the commit.
# Room needs these committed to verify migrations. The files are printed so they can be copied into
# the repository (the CI failure comment shows them).
set -euo pipefail

dirs=(data/schemas)
changes="$(git status --porcelain --untracked-files=all -- "${dirs[@]}")"
if [[ -z "$changes" ]]; then
  echo "Room schemas are committed and up to date."
  exit 0
fi

echo "Room schema files are not committed or are out of date:"
echo "$changes"
while read -r _ file; do
  echo "----- $file"
  cat "$file"
done <<< "$changes"
exit 1
