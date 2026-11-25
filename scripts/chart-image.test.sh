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
deployment() { render "$chart" --show-only templates/deployment.yaml "$@"; }
rendered="$(deployment)" || exit 1
overridden="$(deployment --set image.tag=abc123)" || exit 1

assert "appVersion names a published version, not latest" '[[ -n "$app_version" && "$app_version" != latest ]]'
assert "values.yaml names an image repository" '[[ -n "$repository" ]]'
assert "the image tag defaults to appVersion ($app_version)" \
  'has_line "$rendered" "image: \"$repository:$app_version\""'
assert "the pull policy defaults to IfNotPresent" 'has_line "$rendered" "imagePullPolicy: IfNotPresent"'
assert "image.tag still overrides the default" 'has_line "$overridden" "image: \"$repository:abc123\""'

finish
