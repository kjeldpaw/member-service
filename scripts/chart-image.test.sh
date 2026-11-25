#!/usr/bin/env bash
# Tests the chart's image defaults (ADR 0001): with default values the image tag is the
# chart's appVersion and the pull policy is IfNotPresent. Needs helm.
# Runs locally and in CI: ./scripts/chart-image.test.sh
set -uo pipefail

cd "$(dirname "$0")/.."
chart=charts/member-service
failures=0

check() { # check <name> <condition>
  if eval "$2"; then echo "ok   $1"; else echo "FAIL $1"; failures=$((failures + 1)); fi
}

app_version="$(sed -n -E 's/^appVersion:[[:space:]]*"?([^"[:space:]]*)"?.*/\1/p' "$chart/Chart.yaml")"
# secrets.existingSecret is set only because the chart refuses to render without secrets; the
# image values under test keep their defaults.
render() { helm template member-service "$chart" --show-only templates/deployment.yaml --set secrets.existingSecret=member-service "$@"; }
rendered="$(render)" || exit 1
overridden="$(render --set image.tag=abc123)" || exit 1

check "appVersion names a published version, not latest" '[[ -n "$app_version" && "$app_version" != latest ]]'
check "the image tag defaults to appVersion ($app_version)" \
  'grep -qx "  *image: \"ghcr.io/kjeldpaw/member-service:$app_version\"" <<<"$rendered"'
check "the pull policy defaults to IfNotPresent" 'grep -qx "  *imagePullPolicy: IfNotPresent" <<<"$rendered"'
check "image.tag still overrides the default" \
  'grep -qx "  *image: \"ghcr.io/kjeldpaw/member-service:abc123\"" <<<"$overridden"'

if [[ $failures -gt 0 ]]; then echo "$failures failed"; exit 1; fi
echo "all passed"
