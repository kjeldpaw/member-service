# Pass/fail bookkeeping for the scripts/*.test.sh tests. Source it, report each case with pass,
# fail or assert, and end with finish, which prints a summary and exits non-zero on any failure.

failures=0

pass() { # pass <name>
  echo "ok   $1"
}

fail() { # fail <name> [reason]
  echo "FAIL $1${2:+: $2}"
  failures=$((failures + 1))
}

assert() { # assert <name> <condition>: passes when the condition, run with eval, succeeds
  if eval "$2"; then pass "$1"; else fail "$1"; fi
}

finish() {
  if [[ $failures -gt 0 ]]; then echo "$failures failed"; exit 1; fi
  echo "all passed"
}
