# Release naming

Android keeps a monotonically increasing numeric `versionCode`; the human-facing `versionName`, Git tag, GitHub release title, APK filename, mapping filename, and checksum filenames use the codename assigned to that version.

## Current series: terminal conditions

From version **18** onward, releases are named after **terminal conditions in health** (clinical end-of-life / peri-arrest states) and tagged `v<number>-<codename>`.

| Version | Codename | Named for | Release tag | APK asset |
| ---: | --- | --- | --- | --- |
| 18 | Asystole | Asystole (no cardiac electrical activity) | `v18-asystole` | `orbin-minimal-asystole.apk` |
| 19 | Apnea | Apnea (cessation of breathing) | `v19-apnea` | `orbin-minimal-apnea.apk` |
| 20 | PEA | Pulseless electrical activity | `v20-pea` | `orbin-minimal-pea.apk` |
| 21 | VFib | Ventricular fibrillation | `v21-vfib` | `orbin-minimal-vfib.apk` |
| 22 | Arrest | Cardiac arrest | `v22-arrest` | `orbin-minimal-arrest.apk` |
| 23 | BrainDeath | Brain death | `v23-braindeath` | `orbin-minimal-braindeath.apk` |
| 24 | Coma | Coma | `v24-coma` | `orbin-minimal-coma.apk` |
| 25 | Agonal | Agonal respiration | `v25-agonal` | `orbin-minimal-agonal.apk` |
| 26 | Anuria | Anuria | `v26-anuria` | `orbin-minimal-anuria.apk` |
| 27 | Cachexia | Cachexia | `v27-cachexia` | `orbin-minimal-cachexia.apk` |
| 28 | Mottling | Terminal mottling | `v28-mottling` | `orbin-minimal-mottling.apk` |
| 29 | Shock | Refractory shock | `v29-shock` | `orbin-minimal-shock.apk` |
| 30 | MODS | Multiple organ dysfunction syndrome | `v30-mods` | `orbin-minimal-mods.apk` |
| 31 | ARDS | Acute respiratory distress syndrome | `v31-ards` | `orbin-minimal-ards.apk` |
| 32 | DIC | Disseminated intravascular coagulation | `v32-dic` | `orbin-minimal-dic.apk` |
| 33 | Sepsis | Septic shock | `v33-sepsis` | `orbin-minimal-sepsis.apk` |
| 34 | Uremia | Uremia | `v34-uremia` | `orbin-minimal-uremia.apk` |
| 35 | Hepatic | Acute hepatic failure | `v35-hepatic` | `orbin-minimal-hepatic.apk` |
| 36 | Ischemia | Critical ischemia | `v36-ischemia` | `orbin-minimal-ischemia.apk` |
| 37 | Necrosis | Tissue necrosis | `v37-necrosis` | `orbin-minimal-necrosis.apk` |
| 38 | Gangrene | Gangrene | `v38-gangrene` | `orbin-minimal-gangrene.apk` |
| 39 | Infarction | Infarction (terminal course) | `v39-infarction` | `orbin-minimal-infarction.apk` |
| 40 | Aspiration | Terminal aspiration | `v40-aspiration` | `orbin-minimal-aspiration.apk` |
| 41 | Anoxia | Anoxia | `v41-anoxia` | `orbin-minimal-anoxia.apk` |
| 42 | Exsanguination | Exsanguination | `v42-exsanguination` | `orbin-minimal-exsanguination.apk` |
| 43 | Tamponade | Cardiac tamponade | `v43-tamponade` | `orbin-minimal-tamponade.apk` |
| 44 | Herniation | Brain herniation | `v44-herniation` | `orbin-minimal-herniation.apk` |
| 45 | Vegetative | Persistent vegetative state | `v45-vegetative` | `orbin-minimal-vegetative.apk` |
| 46 | Acidosis | Refractory metabolic acidosis | `v46-acidosis` | `orbin-minimal-acidosis.apk` |
| 47 | Hypoxia | Refractory hypoxia | `v47-hypoxia` | `orbin-minimal-hypoxia.apk` |
| 48 | Asphyxia | Asphyxia | `v48-asphyxia` | `orbin-minimal-asphyxia.apk` |
| 49 | Pulseless | Pulseless state | `v49-pulseless` | `orbin-minimal-pulseless.apk` |
| 50 | Bradypnea | Terminal bradypnea | `v50-bradypnea` | `orbin-minimal-bradypnea.apk` |
| 51 | Oliguria | Terminal oliguria | `v51-oliguria` | `orbin-minimal-oliguria.apk` |
| 52 | Delirium | Terminal delirium | `v52-delirium` | `orbin-minimal-delirium.apk` |
| 53 | Obtundation | Obtundation | `v53-obtundation` | `orbin-minimal-obtundation.apk` |
| 54 | Stupor | Stupor | `v54-stupor` | `orbin-minimal-stupor.apk` |
| 55 | Unresponsive | Unresponsiveness | `v55-unresponsive` | `orbin-minimal-unresponsive.apk` |
| 56 | Circulatory | Circulatory collapse | `v56-circulatory` | `orbin-minimal-circulatory.apk` |
| 57 | Respiratory | Respiratory failure | `v57-respiratory` | `orbin-minimal-respiratory.apk` |
| 58 | Renal | End-stage renal failure | `v58-renal` | `orbin-minimal-renal.apk` |
| 59 | Multisystem | Multisystem failure | `v59-multisystem` | `orbin-minimal-multisystem.apk` |


## Retired series: famous minimalists

Versions 5–17 shipped under a famous-minimalists series and keep their published `v<number>-<codename>` tags. They hold their positions in the codename file because position determines `versionCode` — those entries must never be removed or reordered.

| Version | Codename | Named for | Release tag |
| ---: | --- | --- | --- |
| 5 | Judd | Donald Judd | `v5-judd` |
| 6 | Martin | Agnes Martin | `v6-martin` |
| 7 | Flavin | Dan Flavin | `v7-flavin` |
| 8 | LeWitt | Sol LeWitt | `v8-lewitt` |
| 9 | Andre | Carl Andre | `v9-andre` |
| 10 | Stella | Frank Stella | `v10-stella` |
| 11 | Morris | Robert Morris | `v11-morris` |
| 12 | Truitt | Anne Truitt | `v12-truitt` |
| 13 | Serra | Richard Serra | `v13-serra` |
| 14 | Kelly | Ellsworth Kelly | `v14-kelly` |
| 15 | Glass | Philip Glass | `v15-glass` |
| 16 | Reich | Steve Reich | `v16-reich` |
| 17 | Riley | Terry Riley | `v17-riley` |

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

Append only. Never reorder or remove a line that has already shipped: the tag and asset names of published releases depend on its position. Extend the table above and the file together before the list runs out.

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
