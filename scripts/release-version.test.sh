#!/usr/bin/env bash
# Tests for scripts/release-version.sh, the release workflow's tag/Chart.yaml check.
# Runs locally and in CI: ./scripts/release-version.test.sh
set -uo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
check="$here/release-version.sh"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
failures=0

chart() { # chart <dir> <version> <appVersion>
  mkdir -p "$1"
  printf 'apiVersion: v2\nname: member-service\ntype: application\nversion: %s\nappVersion: %s\n' "$2" "$3" > "$1/Chart.yaml"
}

expect_ok() { # expect_ok <name> <expected stdout> <args...>
  local name="$1" want="$2"; shift 2
  local got; got="$("$check" "$@" 2>/dev/null)"; local status=$?
  if [[ $status -eq 0 && "$got" == "$want" ]]; then echo "ok   $name"
  else echo "FAIL $name: status $status, output '$got', want '$want'"; failures=$((failures + 1)); fi
}

expect_fail() { # expect_fail <name> <args...>
  local name="$1"; shift
  if "$check" "$@" >/dev/null 2>&1; then echo "FAIL $name: check passed, want failure"; failures=$((failures + 1))
  else echo "ok   $name"; fi
}

chart "$work/match" 1.2.3 '"1.2.3"'
expect_ok "a tag matching version and quoted appVersion prints X.Y.Z" 1.2.3 v1.2.3 "$work/match"

chart "$work/unquoted" 1.2.3 1.2.3
expect_ok "an unquoted appVersion matches too" 1.2.3 v1.2.3 "$work/unquoted"

expect_fail "a tag ahead of Chart.yaml fails" v1.2.4 "$work/match"

chart "$work/app-behind" 1.2.3 '"1.2.2"'
expect_fail "an appVersion not matching the tag fails" v1.2.3 "$work/app-behind"

chart "$work/chart-behind" 1.2.2 '"1.2.3"'
expect_fail "a chart version not matching the tag fails" v1.2.3 "$work/chart-behind"

expect_fail "a tag without the v prefix fails" 1.2.3 "$work/match"
expect_fail "a tag that isn't X.Y.Z fails" v1.2 "$work/match"
expect_fail "a pre-release suffix fails" v1.2.3-rc1 "$work/match"
expect_fail "a GitHub ref prefix is not stripped" refs/tags/v1.2.3 "$work/match"
expect_fail "a missing Chart.yaml fails" v1.2.3 "$work/nowhere"

if [[ $failures -gt 0 ]]; then echo "$failures failed"; exit 1; fi
echo "all passed"
