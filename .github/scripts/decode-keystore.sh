#!/usr/bin/env bash
# Decodes KEYSTORE_BASE64 into $RUNNER_TEMP/release.jks, checks that KEYSTORE_PASSWORD opens it and that
# KEY_ALIAS exists, then exports SIGNING_KEYSTORE_PATH for Gradle. Never prints secret values.
# Only release.yml calls this; PR CI never receives signing secrets.
set -euo pipefail

keystore="${RUNNER_TEMP}/release.jks"

problem() {
  rm -f "$keystore"
  echo "::error::$1"
  exit 1
}

missing=()
for name in KEYSTORE_BASE64 KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD; do
  [[ -n "${!name:-}" ]] || missing+=("$name")
done
if (( ${#missing[@]} )); then
  problem "Missing or empty repository secrets: ${missing[*]}. Add them under Settings → Secrets and variables → Actions → Repository secrets (exact names, upper case)."
fi

# --ignore-garbage tolerates line wrapping, CRLF and stray spaces from copy-pasting.
if ! base64 --decode --ignore-garbage <<< "$KEYSTORE_BASE64" > "$keystore" 2> /dev/null || [[ ! -s "$keystore" ]]; then
  problem "KEYSTORE_BASE64 is not valid base64. Re-create it from the .jks file (see docs/release.md)."
fi

if ! keytool -list -keystore "$keystore" -storepass "$KEYSTORE_PASSWORD" > /dev/null 2>&1; then
  problem "KEYSTORE_PASSWORD does not open the keystore in KEYSTORE_BASE64 (or the file is not a keystore)."
fi

if ! keytool -list -keystore "$keystore" -storepass "$KEYSTORE_PASSWORD" -alias "$KEY_ALIAS" > /dev/null 2>&1; then
  problem "KEY_ALIAS was not found in the keystore. List aliases with: keytool -list -keystore <file>."
fi

echo "SIGNING_KEYSTORE_PATH=${keystore}" >> "$GITHUB_ENV"
echo "Keystore decoded and verified."
