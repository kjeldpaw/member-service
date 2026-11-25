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

# ingress <rendered> <name>: the manifest of the Ingress with that name.
ingress() {
  awk -v name="$2" '
    /^---/ { if (kind && named) printf "%s", doc; doc = ""; kind = named = 0; next }
    { doc = doc $0 "\n" }
    $0 == "kind: Ingress" { kind = 1 }
    $0 == "  name: " name { named = 1 }
    END { if (kind && named) printf "%s", doc }' <<<"$1"
}

# tls_secrets <ingress>: the TLS secret names, one per line.
tls_secrets() { sed -nE 's/^[[:space:]]*-? ?secretName: //p' <<<"$1"; }

# tls_host <ingress>: the host the TLS entry covers, the line after its hosts:.
tls_host() { grep -A1 -E '^[[:space:]]*-? ?hosts:$' <<<"$1" | sed -nE 's/^[[:space:]]*- "?([^":]*)"?$/\1/p'; }

# paths <ingress>: the path of each rule, one per line.
paths() { sed -nE 's/^[[:space:]]*- path: //p' <<<"$1"; }

# rejects <name> <message> [helm args...]: rendering with the args fails, saying <message>.
rejects() {
  local out
  if out="$(render "$chart" "${@:3}" 2>&1)"; then fail "$1" "rendered"
  elif ! grep -qF -- "$2" <<<"$out"; then fail "$1" "$out"
  else pass "$1"; fi
}

assert "default values render two Ingresses" '[[ "$(ingresses "$defaults")" == 2 ]]'

public="$(ingress "$defaults" member-service-public)"
assert "the public Ingress is on Traefik's public entrypoint" \
  'has_line "$public" "traefik.ingress.kubernetes.io/router.entrypoints: public"'
assert "the public Ingress serves only the /api prefix" \
  '[[ "$(paths "$public")" == /api ]] && has_line "$public" "pathType: Prefix"'

lan="$(ingress "$defaults" member-service)"
assert "the LAN Ingress is on Traefik's websecure entrypoint" \
  'has_line "$lan" "traefik.ingress.kubernetes.io/router.entrypoints: websecure"'
assert "the LAN Ingress serves the whole app under /" \
  '[[ "$(paths "$lan")" == / ]] && has_line "$lan" "pathType: Prefix"'

for name in member-service member-service-public; do
  manifest="$(ingress "$defaults" "$name")"
  assert "$name uses TLS with the member-service-tls secret" \
    '[[ "$(tls_secrets "$manifest")" == member-service-tls ]]'
  assert "$name's TLS covers its host" '[[ "$(tls_host "$manifest")" == member-service.k3s.figge.dk ]]'
done

# The entrypoint comes from ingress.<lan|public>.entrypoint, not from the extra annotations, so
# clearing those can't drop an Ingress onto Traefik's default entrypoints, which include the LAN ones.
cleared="$(render "$chart" --set ingress.public.annotations=null --set ingress.lan.annotations=null)" \
  || exit 1
assert "clearing the public Ingress's extra annotations keeps it on the public entrypoint" \
  'has_line "$(ingress "$cleared" member-service-public)" "traefik.ingress.kubernetes.io/router.entrypoints: public"'
assert "clearing the LAN Ingress's extra annotations keeps it on the websecure entrypoint" \
  'has_line "$(ingress "$cleared" member-service)" "traefik.ingress.kubernetes.io/router.entrypoints: websecure"'
assert "no Ingress renders empty annotations" \
  '! has_line "$cleared" "null" && ! has_line "$cleared" "annotations: {}"'

extra="$(render "$chart" --set ingress.public.annotations.example/extra=yes)" || exit 1
extra_public="$(ingress "$extra" member-service-public)"
assert "extra annotations render alongside the public entrypoint" \
  'has_line "$extra_public" "example/extra: \"yes\"" \
    && has_line "$extra_public" "traefik.ingress.kubernetes.io/router.entrypoints: public"'

overridden="$(render "$chart" \
  --set 'ingress.public.annotations.traefik\.ingress\.kubernetes\.io/router\.entrypoints=websecure')" || exit 1
assert "an entrypoints key in the extra annotations doesn't move the public Ingress" \
  '[[ "$(grep -c "router.entrypoints:" <<<"$(ingress "$overridden" member-service-public)")" == 1 ]] \
    && has_line "$(ingress "$overridden" member-service-public)" "traefik.ingress.kubernetes.io/router.entrypoints: public"'

for side in lan public; do
  rejects "an empty ingress.$side.entrypoint is refused" "ingress.$side.entrypoint is required" \
    --set "ingress.$side.entrypoint="
done

assert "helm lint passes with default values" 'helm lint "$chart" >/dev/null'

finish
