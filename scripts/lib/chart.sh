# Reads and renders the chart. Sourced by the release check and the chart tests, so they all read
# Chart.yaml and render the chart the same way. Needs bash and sed, and helm for render.

# chart_field <chart-dir> <key>: prints a top-level scalar from <chart-dir>/Chart.yaml, without
# surrounding quotes or a trailing comment; prints nothing if the key is missing.
chart_field() {
  sed -n -E "s/^$2:[[:space:]]*[\"']?([^\"'#[:space:]]*)[\"']?[[:space:]]*(#.*)?$/\1/p" "$1/Chart.yaml" | head -n 1
}

# render <chart-dir> [helm args...]: the chart's manifests for a release named member-service.
render() { render_as member-service "$@"; }

# render_as <release> <chart-dir> [helm args...]: the chart's manifests for the named release.
render_as() { helm template "$1" "$2" "${@:3}"; }

# has_line <text> <line>: the text contains the line, ignoring leading indentation.
has_line() { grep -qxF -- "$2" <<<"$(sed -E 's/^[[:space:]]+//' <<<"$1")"; }
