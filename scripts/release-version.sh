#!/usr/bin/env bash
# Derives the release version from a tag and checks it against the chart (ADR 0001).
#
#   scripts/release-version.sh <tag> [chart-dir]
#
# <tag> must be vX.Y.Z. Prints X.Y.Z and exits 0 when Chart.yaml's `version` and
# `appVersion` are both exactly X.Y.Z; otherwise explains on stderr and exits 1.
# chart-dir defaults to charts/member-service. Needs nothing but bash, so it runs
# the same in the release workflow and locally as a dry run.
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "usage: $0 <tag> [chart-dir]" >&2
  exit 2
fi

source "$(dirname "$0")/lib/chart.sh"

tag="$1"
chart_dir="${2:-$(dirname "$0")/../charts/member-service}"
chart_yaml="$chart_dir/Chart.yaml"

if [[ ! "$tag" =~ ^v([0-9]+\.[0-9]+\.[0-9]+)$ ]]; then
  echo "error: tag '$tag' is not vX.Y.Z" >&2
  exit 1
fi
version="${BASH_REMATCH[1]}"

if [[ ! -f "$chart_yaml" ]]; then
  echo "error: $chart_yaml not found" >&2
  exit 1
fi

status=0
for key in version appVersion; do
  value="$(chart_field "$chart_dir" "$key")"
  if [[ "$value" != "$version" ]]; then
    echo "error: tag $tag needs $key: $version in $chart_yaml, found '$value'" >&2
    status=1
  fi
done
[[ $status -eq 0 ]] || exit 1

echo "$version"
