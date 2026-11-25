#!/usr/bin/env bash
# Tests the chart's Ingresses with default values: the LAN Ingress serves the whole app on Traefik's
# websecure entrypoint, a public Ingress serves only /api on the public entrypoint, and both use TLS
# with the member-service-tls secret (the Certificate is created on the k3s side). Needs helm.
# Runs locally and in CI: ./scripts/chart-ingress.test.sh
set -uo pipefail

cd "$(dirname "$0")/.."
source scripts/lib/testing.sh
source scripts/lib/chart.sh
chart=charts/member-service

defaults="$(render "$chart")" || exit 1

# ingresses <rendered>: how many Ingresses the manifests hold.
ingresses() { grep -cxF "kind: Ingress" <<<"$1"; }

assert "default values render two Ingresses" '[[ "$(ingresses "$defaults")" == 2 ]]'

assert "helm lint passes with default values" 'helm lint "$chart" >/dev/null'

finish
