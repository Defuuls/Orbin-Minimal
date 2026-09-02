#!/usr/bin/env bash
# Shared release metadata for the Orbin Minimal workflows.
#
# Usage:
#   release-meta.sh codename <version>   Print "slug|Display|Named for" for a version.
#   release-meta.sh versions             Print "<number> <tag>" for every release tag,
#                                        ascending. Covers the retired organ-v<n>-<slug>
#                                        tags and the current v<n>-<slug> ones.
#   release-meta.sh latest               Print the highest released version number, or 0.
#   release-meta.sh previous <version>   Print the release tag directly below <version>.
set -euo pipefail

REPO_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
CODENAMES="$REPO_ROOT/.github/release-codenames.txt"

codename() {
  local version="$1" entry
  entry=$(grep -v '^[[:space:]]*#' "$CODENAMES" | grep -v '^[[:space:]]*$' | sed -n "${version}p")
  if [ -z "$entry" ]; then
    echo "No codename is assigned to version ${version}. Extend .github/release-codenames.txt and docs/RELEASE-NAMING.md first." >&2
    return 1
  fi
  printf '%s\n' "$entry"
}

versions() {
  git tag --list 'v*' 'organ-v*' |
    sed -nE 's/^(organ-)?v([0-9]+)-[a-z]+$/\2 &/p' |
    sort -k1,1n
}

case "${1:-}" in
  codename) codename "${2:?version required}" ;;
  versions) versions ;;
  latest)   versions | tail -n1 | cut -d' ' -f1 | grep . || echo 0 ;;
  previous) versions | awk -v cur="${2:?version required}" '$1 < cur { tag = $2 } END { print tag }' ;;
  *) echo "Usage: release-meta.sh {codename <version>|versions|latest|previous <version>}" >&2; exit 2 ;;
esac
