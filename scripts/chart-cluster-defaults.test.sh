#!/usr/bin/env bash
# Tests that the chart's default values work on the k3s cluster without overrides: they point at
# the shared Postgres, Keycloak at its public hostname and the cluster OpenTelemetry collector, take
# secrets from an existing Secret, and allow CORS from the frontend's origin. Needs helm.
# Runs locally and in CI: ./scripts/chart-cluster-defaults.test.sh
set -uo pipefail

cd "$(dirname "$0")/.."
source scripts/lib/testing.sh
source scripts/lib/chart.sh
chart=charts/member-service

# secret_ref <rendered>: the Secret the Deployment takes its environment from.
secret_ref() { grep -A1 -- "- secretRef:" <<<"$1" | sed -nE 's/^[[:space:]]*name: //p'; }

defaults="$(render "$chart")" || exit 1

# Hostnames from the k3s repo: Bitnami release "postgres" in namespace postgres, the
# OpenTelemetryCollector "otel" in namespace opentelemetry (Service "otel-collector"), and Keycloak's
# public hostname, which is also the token issuer.
postgres=postgres-postgresql.postgres.svc.cluster.local:5432/member_service
assert "the JDBC URL points at the shared Postgres" \
  'has_line "$defaults" "QUARKUS_DATASOURCE_JDBC_URL: \"jdbc:postgresql://$postgres\""'
assert "the reactive URL points at the shared Postgres" \
  'has_line "$defaults" "QUARKUS_DATASOURCE_REACTIVE_URL: \"postgresql://$postgres\""'
assert "OIDC points at Keycloak's public hostname, realm wandywharang" \
  'has_line "$defaults" "QUARKUS_OIDC_AUTH_SERVER_URL: \"https://keycloak.k3s.figge.dk/realms/wandywharang\""'
assert "the Keycloak admin client points at Keycloak's public hostname" \
  'has_line "$defaults" "QUARKUS_KEYCLOAK_ADMIN_CLIENT_SERVER_URL: \"https://keycloak.k3s.figge.dk\""'
assert "the Keycloak admin client uses realm wandywharang" \
  'has_line "$defaults" "QUARKUS_KEYCLOAK_ADMIN_CLIENT_REALM: \"wandywharang\""'
assert "OTLP goes to the cluster collector" \
  'has_line "$defaults" "QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT: \"http://otel-collector.opentelemetry.svc.cluster.local:4317\""'
assert "no default points into the member-service namespace" \
  '! grep -q "\.member-service\.svc" <<<"$defaults"'

assert "CORS allows the frontend's origin by default" \
  'has_line "$defaults" "QUARKUS_HTTP_CORS_ORIGINS: \"https://wandywharang.k3s.figge.dk\""'
other_origin="$(render "$chart" --set cors.allowedOrigin=https://frontend.example)" || exit 1
assert "cors.allowedOrigin changes the allowed origin" \
  'has_line "$other_origin" "QUARKUS_HTTP_CORS_ORIGINS: \"https://frontend.example\""'
# rejects <name> <message> [helm args...]: rendering with the args fails, saying <message>.
rejects() {
  local out
  if out="$(render "$chart" "${@:3}" 2>&1)"; then fail "$1" "rendered"
  elif ! grep -qF -- "$2" <<<"$out"; then fail "$1" "$out"
  else pass "$1"; fi
}
rejects "QUARKUS_HTTP_CORS_ORIGINS under config is refused in favour of cors.allowedOrigin" \
  "set cors.allowedOrigin instead of config.QUARKUS_HTTP_CORS_ORIGINS" \
  --set config.QUARKUS_HTTP_CORS_ORIGINS=https://frontend.example
for origin in "https://a.example,https://b.example" "https://a.example/" "a.example" "https://*.example" \
  "/https://.*/" "https://a.example/app"; do
  rejects "cors.allowedOrigin $origin is refused: it must be one scheme://host[:port] origin" \
    "cors.allowedOrigin must be a single origin" --set-json "cors.allowedOrigin=\"$origin\""
done
with_port="$(render "$chart" --set cors.allowedOrigin=http://localhost:5173)" || exit 1
assert "cors.allowedOrigin may name a port" \
  'has_line "$with_port" "QUARKUS_HTTP_CORS_ORIGINS: \"http://localhost:5173\""'

assert "by default no Secret is rendered" '! has_line "$defaults" "kind: Secret"'
existing="$(render "$chart" --set secrets.existingSecret=from-bitwarden)" || exit 1
assert "with existingSecret set, no Secret is rendered" '! has_line "$existing" "kind: Secret"'
assert "with existingSecret set, the Deployment reads that Secret" \
  '[[ "$(secret_ref "$existing")" == from-bitwarden ]]'
# Another release name, so the generated Secret (local-member-service-secrets) can't be mistaken
# for the member-service-secrets default.
inline="$(render_as local "$chart" -f "$chart/values.secret.example.yaml")" || exit 1
assert "inline secret values still render a Secret for local installs" 'has_line "$inline" "kind: Secret"'
assert "inline secret values are what the Deployment reads" \
  '[[ "$(secret_ref "$inline")" == local-member-service-secrets ]]'

assert "helm lint passes with default values" 'helm lint "$chart" >/dev/null'

finish
