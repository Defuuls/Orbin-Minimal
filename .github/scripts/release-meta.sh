#!/usr/bin/env bash
# Shared release metadata for the Orbin Minimal workflows.
#
# Usage:
#   release-meta.sh codename <version>   Print "slug|Display|Named for" for a version.
#   release-meta.sh versions             Print "<number> <tag>" for every released
#                                        version, ascending. Covers the retired
#                                        organ-v<n>-<slug> tags and the current
#                                        v<n>-<slug> ones.
#
# A version counts as released only when a GitHub Release exists for its tag.
# A tag alone is not enough: a cancelled or failed run leaves the tag behind
# (it is pushed before the build), and that orphan must not consume a version
# number. When GH_TOKEN and GITHUB_REPOSITORY are set the published releases are
# fetched and used as the filter; without them (running locally) every matching
# tag is listed, which is the safe read-only default.
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

published_tags() {
  [ -n "${GH_TOKEN:-}" ] && [ -n "${GITHUB_REPOSITORY:-}" ] && command -v gh >/dev/null 2>&1 || return 0
  gh api "repos/${GITHUB_REPOSITORY}/releases" --paginate --jq '.[] | select(.draft | not) | .tag_name'
}

versions() {
  local published
  published=$(published_tags)
  git tag --list 'v*' 'organ-v*' |
    sed -nE 's/^(organ-)?v([0-9]+)-[a-z]+$/\2 &/p' |
    awk -v published="$published" '
      BEGIN {
        count = split(published, lines, "\n")
        for (i = 1; i <= count; i++) if (lines[i] != "") { seen[lines[i]] = 1; filter = 1 }
      }
      !filter || ($2 in seen)
    ' |
    sort -k1,1n
}

case "${1:-}" in
  codename) codename "${2:?version required}" ;;
  versions) versions ;;
  latest)   versions | tail -n1 | cut -d' ' -f1 | grep . || echo 0 ;;
  previous) versions | awk -v cur="${2:?version required}" '$1 < cur { tag = $2 } END { print tag }' ;;
  *) echo "Usage: release-meta.sh {codename <version>|versions|latest|previous <version>}" >&2; exit 2 ;;
esac
