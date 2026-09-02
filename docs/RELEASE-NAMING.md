# Release naming: body organs

Orbin Minimal release codenames use a fixed body-organ sequence. Android keeps a monotonically increasing numeric `versionCode`; the human-facing `versionName`, Git tag, GitHub release title, APK filename, mapping filename, and checksum filenames use the assigned organ.

| Version | Organ | Release tag | APK asset |
| ---: | --- | --- | --- |
| 1 | Heart | `organ-v1-heart` | `orbin-minimal-heart.apk` |
| 2 | Brain | `organ-v2-brain` | `orbin-minimal-brain.apk` |
| 3 | Lungs | `organ-v3-lungs` | `orbin-minimal-lungs.apk` |
| 4 | Liver | `organ-v4-liver` | `orbin-minimal-liver.apk` |
| 5 | Kidneys | `organ-v5-kidneys` | `orbin-minimal-kidneys.apk` |
| 6 | Stomach | `organ-v6-stomach` | `orbin-minimal-stomach.apk` |
| 7 | Pancreas | `organ-v7-pancreas` | `orbin-minimal-pancreas.apk` |
| 8 | Spleen | `organ-v8-spleen` | `orbin-minimal-spleen.apk` |
| 9 | Thyroid | `organ-v9-thyroid` | `orbin-minimal-thyroid.apk` |
| 10 | Bladder | `organ-v10-bladder` | `orbin-minimal-bladder.apk` |
| 11 | Gallbladder | `organ-v11-gallbladder` | `orbin-minimal-gallbladder.apk` |
| 12 | Appendix | `organ-v12-appendix` | `orbin-minimal-appendix.apk` |
| 13 | Esophagus | `organ-v13-esophagus` | `orbin-minimal-esophagus.apk` |
| 14 | Trachea | `organ-v14-trachea` | `orbin-minimal-trachea.apk` |
| 15 | Thymus | `organ-v15-thymus` | `orbin-minimal-thymus.apk` |
| 16 | Pituitary | `organ-v16-pituitary` | `orbin-minimal-pituitary.apk` |
| 17 | Hypothalamus | `organ-v17-hypothalamus` | `orbin-minimal-hypothalamus.apk` |
| 18 | Intestines | `organ-v18-intestines` | `orbin-minimal-intestines.apk` |
| 19 | Colon | `organ-v19-colon` | `orbin-minimal-colon.apk` |
| 20 | Skin | `organ-v20-skin` | `orbin-minimal-skin.apk` |

The machine-readable source of truth for this sequence is [`.github/release-organs.txt`](../.github/release-organs.txt): the Nth non-comment line is the organ for `versionCode` N. Both release workflows read that file, so the table above and the file must be extended together before version 21.

## Automatic releases

`.github/workflows/auto-release.yml` releases without manual steps:

1. A push to `main` that touches `app/**`, the Gradle build files, or the release workflows starts the pipeline.
2. `decide` skips the run if the head commit is already tagged `organ-v*` or its message contains `[skip release]`, otherwise it picks the next unused version and its organ from `.github/release-organs.txt`.
3. `verify` runs `:app:testDebugUnitTest`; a failure stops the release before anything is tagged or published.
4. `release` calls `.github/workflows/release.yml` as a reusable workflow, which creates the annotated tag, builds and signs the APK, computes checksums, generates notes and publishes the GitHub release.

Calling the release workflow directly (rather than pushing a tag and waiting for it to fire) avoids the GitHub rule that a tag pushed with `GITHUB_TOKEN` does not trigger further workflows.

Manual paths remain available: run **Release Orbin Minimal** with an explicit `tag` for an out-of-band release or `notes_only` to regenerate notes, or push an `organ-v*` tag yourself. The workflow validates the tag against the table in every case, and a mismatched or reused organ fails before signing or publishing.

## Independent signing identity

Orbin Minimal uses its own Android signing key, separate from the full Orbin application. The public SHA-256 certificate fingerprint for this signing identity is:

`3E:C1:AF:02:DF:66:7C:AF:B4:96:9D:8C:8F:F4:12:CB:AE:D8:8F:37:D3:E8:39:A3:D3:49:67:3A:7E:AF:F9:F8`

The private keystore and passwords must never be committed to this repository. Store them only as the repository Actions secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`, and keep an offline backup of the keystore.
