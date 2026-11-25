#!/usr/bin/env bash
# Tests that the Gradle project version comes from the build, with a development default.
# Runs locally and in CI: ./scripts/gradle-version.test.sh
set -uo pipefail

cd "$(dirname "$0")/.."
source scripts/lib/testing.sh

expect_version() { # expect_version <name> <want> [gradle args...]
  local name="$1" want="$2"; shift 2
  local got; got="$(./gradlew -q properties "$@" | sed -n 's/^version: //p')"
  if [[ "$got" == "$want" ]]; then pass "$name"; else fail "$name" "version '$got', want '$want'"; fi
}

expect_version "a build without a version gets the development default" 0.0.0-SNAPSHOT
expect_version "a release build gets the version it is given" 1.2.3 -Pversion=1.2.3

finish
