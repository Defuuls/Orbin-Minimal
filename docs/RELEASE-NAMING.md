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

The release workflow validates the tag against this table. A mismatched or reused organ fails before signing or publishing. Extend the table and the workflow mapping together before version 21.
