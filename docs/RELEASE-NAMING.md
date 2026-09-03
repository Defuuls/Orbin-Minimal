# Release naming

Android keeps a monotonically increasing numeric `versionCode`; the human-facing `versionName`, Git tag, GitHub release title, APK filename, mapping filename, and checksum filenames use the codename assigned to that version.

## Current series: minimalists

From version 5 onward, releases are named after famous minimalists and tagged `v<number>-<codename>`.

| Version | Codename | Named for | Release tag | APK asset |
| ---: | --- | --- | --- | --- |
| 5 | Judd | Donald Judd | `v5-judd` | `orbin-minimal-judd.apk` |
| 6 | Martin | Agnes Martin | `v6-martin` | `orbin-minimal-martin.apk` |
| 7 | Flavin | Dan Flavin | `v7-flavin` | `orbin-minimal-flavin.apk` |
| 8 | LeWitt | Sol LeWitt | `v8-lewitt` | `orbin-minimal-lewitt.apk` |
| 9 | Andre | Carl Andre | `v9-andre` | `orbin-minimal-andre.apk` |
| 10 | Stella | Frank Stella | `v10-stella` | `orbin-minimal-stella.apk` |
| 11 | Morris | Robert Morris | `v11-morris` | `orbin-minimal-morris.apk` |
| 12 | Truitt | Anne Truitt | `v12-truitt` | `orbin-minimal-truitt.apk` |
| 13 | Serra | Richard Serra | `v13-serra` | `orbin-minimal-serra.apk` |
| 14 | Kelly | Ellsworth Kelly | `v14-kelly` | `orbin-minimal-kelly.apk` |
| 15 | Glass | Philip Glass | `v15-glass` | `orbin-minimal-glass.apk` |
| 16 | Reich | Steve Reich | `v16-reich` | `orbin-minimal-reich.apk` |
| 17 | Riley | Terry Riley | `v17-riley` | `orbin-minimal-riley.apk` |
| 18 | Young | La Monte Young | `v18-young` | `orbin-minimal-young.apk` |
| 19 | Part | Arvo Pärt | `v19-part` | `orbin-minimal-part.apk` |
| 20 | Rams | Dieter Rams | `v20-rams` | `orbin-minimal-rams.apk` |
| 21 | Pawson | John Pawson | `v21-pawson` | `orbin-minimal-pawson.apk` |
| 22 | Ando | Tadao Ando | `v22-ando` | `orbin-minimal-ando.apk` |
| 23 | Mies | Ludwig Mies van der Rohe | `v23-mies` | `orbin-minimal-mies.apk` |
| 24 | Ryman | Robert Ryman | `v24-ryman` | `orbin-minimal-ryman.apk` |

## Retired series: body organs

Versions 1-4 shipped under an earlier body-organ series and keep their published `organ-v<number>-<codename>` tags. They are listed here, and hold their positions in the codename file, because position determines `versionCode` — the entries must never be removed or reordered.

| Version | Codename | Release tag | APK asset |
| ---: | --- | --- | --- |
| 1 | Heart | `organ-v1-heart` | `orbin-minimal-heart.apk` |
| 2 | Brain | `organ-v2-brain` | `orbin-minimal-brain.apk` |
| 3 | Lungs | `organ-v3-lungs` | `orbin-minimal-lungs.apk` |
| 4 | Liver | `organ-v4-liver` | `orbin-minimal-liver.apk` |

## The codename file

The machine-readable source of truth is [`.github/release-codenames.txt`](../.github/release-codenames.txt): the Nth non-comment line is `slug|Display|Named for` for `versionCode` N. Both release workflows read it through [`.github/scripts/release-meta.sh`](../.github/scripts/release-meta.sh), which also resolves the latest released version and the previous tag across both the retired and current tag prefixes.

A version counts as released only when a **published GitHub Release** exists for its tag. A tag on its own is not enough: the release workflow pushes the tag before it builds, so a cancelled or failed run leaves an orphaned tag behind. Those orphans are ignored, so a version number is never consumed by a release that never shipped, and re-running after a failure picks up the same version again.

Append only. Never reorder or remove a line that has already shipped: the tag and asset names of published releases depend on its position. Extend the table above and the file together before version 25.

## Automatic releases

`.github/workflows/auto-release.yml` releases without manual steps:

1. A push to `main` that touches `app/**`, the Gradle build files, or the release tooling starts the pipeline.
2. `decide` skips the run if the head commit is already tagged for a release or its message contains `[skip release]`, otherwise it takes the highest released version, adds one, and looks up that version's codename.
3. `verify` runs `:app:testDebugUnitTest`; a failure stops the release before anything is tagged or published.
4. `release` calls `.github/workflows/release.yml` as a reusable workflow, which creates the annotated tag, builds and signs the APK, computes checksums, generates notes and publishes the GitHub release.

Calling the release workflow directly (rather than pushing a tag and waiting for it to fire) avoids the GitHub rule that a tag pushed with `GITHUB_TOKEN` does not trigger further workflows.

Manual paths remain available: run **Release Orbin Minimal** with an explicit `tag` for an out-of-band release or `notes_only` to regenerate notes, or push a `v<number>-<codename>` tag yourself. The workflow validates the tag against the codename file in every case, and a mismatched or out-of-sequence codename fails before signing or publishing.

## Release artifacts

A published release carries the signed APK and its SHA-256 checksum, and nothing else.

The R8 mapping file is deliberately **not** published. It maps obfuscated names back to the originals, so attaching it to the release would hand anyone who downloads the APK the means to de-obfuscate it, undoing what `isMinifyEnabled` buys. The build still produces it and uploads it as a private GitHub Actions artifact (90-day retention), where repository members can fetch it to de-obfuscate a crash trace.

It is staged outside `release-staging/` rather than filtered out of it: every file in that directory is published by glob, so the mapping must never be written there in the first place.

## Independent signing identity

Orbin Minimal uses its own Android signing key, separate from the full Orbin application. The public SHA-256 certificate fingerprint for this signing identity is:

`3E:C1:AF:02:DF:66:7C:AF:B4:96:9D:8C:8F:F4:12:CB:AE:D8:8F:37:D3:E8:39:A3:D3:49:67:3A:7E:AF:F9:F8`

The private keystore and passwords must never be committed to this repository. Store them only as the repository Actions secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`, and keep an offline backup of the keystore.
