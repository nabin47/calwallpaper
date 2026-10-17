#!/usr/bin/env bash
# Builds the debug APK, signed with the fixed debug key when the DEBUG_KEYSTORE_BASE64 secret is available
# (ADR 0006). The decoded keystore lives in the runner's temp directory, never in the workspace.
set -euo pipefail

if [[ -n "${DEBUG_KEYSTORE_BASE64:-}" ]]; then
  keystore="${RUNNER_TEMP:-/tmp}/calwp-debug.keystore"
  printf '%s' "$DEBUG_KEYSTORE_BASE64" | base64 --decode > "$keystore"
  export DEBUG_KEYSTORE_PATH="$keystore"
  # The certificate fingerprint is public; printing it lets anyone check which key signed the APK.
  echo "Signing with the fixed debug key:"
  keytool -list -v -keystore "$keystore" -storepass android -alias androiddebugkey | grep 'SHA256:' || true
else
  echo "::warning::DEBUG_KEYSTORE_BASE64 is not set (fork PR, or the secret is missing). This APK uses a throwaway debug key, so installing it over another CI build needs an uninstall first."
fi

./gradlew :app:assembleDebug
