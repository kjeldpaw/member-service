# Reads the chart's Chart.yaml. Sourced by the release check and its tests, so both read
# Chart.yaml the same way. Needs nothing but bash and sed.

# chart_field <chart-dir> <key>: prints a top-level scalar from <chart-dir>/Chart.yaml, without
# surrounding quotes or a trailing comment; prints nothing if the key is missing.
chart_field() {
  sed -n -E "s/^$2:[[:space:]]*[\"']?([^\"'#[:space:]]*)[\"']?[[:space:]]*(#.*)?$/\1/p" "$1/Chart.yaml" | head -n 1
}
