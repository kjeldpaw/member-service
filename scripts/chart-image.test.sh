#!/usr/bin/env bash
# Tests the chart's image defaults (ADR 0001): with default values the image tag is the
# chart's appVersion and the pull policy is IfNotPresent. Needs helm.
# Runs locally and in CI: ./scripts/chart-image.test.sh
set -uo pipefail

cd "$(dirname "$0")/.."
source scripts/lib/testing.sh
source scripts/lib/chart.sh
chart=charts/member-service

app_version="$(chart_field "$chart" appVersion)"
# The chart's own image.repository, so the expected image follows values.yaml.
repository="$(helm show values "$chart" | awk '/^image:/{f=1;next} /^[^[:space:]#]/{f=0} f && $1=="repository:"{print $2}')"
# secrets.existingSecret is set only because the chart refuses to render without secrets; the
# image values under test keep their defaults.
render() { helm template member-service "$chart" --show-only templates/deployment.yaml --set secrets.existingSecret=member-service "$@"; }
rendered="$(render)" || exit 1
overridden="$(render --set image.tag=abc123)" || exit 1

assert "appVersion names a published version, not latest" '[[ -n "$app_version" && "$app_version" != latest ]]'
assert "values.yaml names an image repository" '[[ -n "$repository" ]]'
assert "the image tag defaults to appVersion ($app_version)" \
  'grep -qx "  *image: \"$repository:$app_version\"" <<<"$rendered"'
assert "the pull policy defaults to IfNotPresent" 'grep -qx "  *imagePullPolicy: IfNotPresent" <<<"$rendered"'
assert "image.tag still overrides the default" 'grep -qx "  *image: \"$repository:abc123\"" <<<"$overridden"'

finish
