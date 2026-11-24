# Releases are tagged versions, and Argo CD follows the tag

A release is a git tag `vX.Y.Z`. The tag makes CI build and push the image
`:X.Y.Z`, with the version in Gradle and in `Chart.yaml` (`version` and
`appVersion`) set to match. In the k3s repo, the Argo CD Application uses the
chart at `targetRevision: vX.Y.Z` and leaves `image.tag` unset, so the image
tag comes from `appVersion`. Bumping that one line deploys the chart and the
image together, and Renovate can propose the bump as a PR.

## Considered Options

- **`:latest` with `pullPolicy: Always`**: the running version depends on
  when the pod last restarted, and rolling back means another push.
- **A commit SHA in the k3s values**: reproducible, but it doesn't say what
  is running, and the chart and image versions are bumped separately.
- **Argo CD Image Updater**: deploys automatically, but adds a component and
  makes the cluster write back to git.

## Consequences

- Pushes to main publish only `:<sha>` images, for testing. Nothing
  publishes `:latest`.
- A release whose `Chart.yaml` doesn't match its tag deploys the wrong image.
  The release workflow should check that they match.
