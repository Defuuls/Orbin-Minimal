# Pixel 10 Pro XL — manual verification checklist

Target device: **Pixel 10 Pro XL** (large phone / fold-adjacent width, **120 Hz** display).
Build: debug or release APK from `main` (or the PR under test). CI runs `:app:testDebugUnitTest` on every PR to `main` — items marked **CI** are already covered by those unit tests; still smoke them on device when touching related UI.

Legend: **Manual** = device/UI only · **CI** = unit-tested · **N/A** = out of Minimal scope

---

## Feed — fling and 120 Hz feel

| # | Check | Coverage |
|---|--------|----------|
| F1 | Follow ≥3 boards; open Feed; fling the list hard on 120 Hz — scroll stays smooth, no multi-frame hitching from thumb decode | Manual |
| F2 | Thumbs appear without crossfade flicker on fling; recycled rows do not flash wrong images | Manual |
| F3 | Near-viewport thumbs prefetch ahead of scroll (next few rows warm before fully on-screen) | Manual |
| F4 | Soft cap: huge multi-board catalogs stop composing past `MAX_RENDERED_FEED_ENTRIES` (400); list remains scrollable for the capped set | CI (`FeedEntriesTest`) + Manual smoke |
| F5 | Site chips (4chan / BBW Chan) preserve selection across rotation; Refresh reloads that site’s followed boards | Manual |
| F6 | Sort chips: **Board** (default) groups with headers; **Latest activity** is a flat list; switching does not refetch | CI (`FeedSortTest`, `FeedEntriesTest`) + Manual |

## Thumb decode

| # | Check | Coverage |
|---|--------|----------|
| T1 | Phone list thumbs use ~96 dp cell → density-scaled decode (not a fixed oversized bitmap) | Manual (Profiler / Layout Inspector optional) |
| T2 | Wide grid cards (~140 dp) decode larger than list thumbs but stay under the soft px cap | Manual |
| T3 | Fullscreen viewer decode capped (`VIEWER_SIZE_PX` / gallery max dp); pinch-zoom still usable | Manual |

## Wide layout (≥600 dp) — grid / two-pane

| # | Check | Coverage |
|---|--------|----------|
| W1 | Rotate to landscape or use a ≥600 dp width: Feed switches from `LazyColumn` to adaptive `LazyVerticalGrid` + side pane (“Select a thread…”) | Manual |
| W2 | Board headers span full grid width; thread cards fill adaptive columns (~220 dp min) | Manual |
| W3 | Narrow portrait returns to single-column list without crash or lost site/sort | Manual |

## Media gallery — HorizontalPager

| # | Check | Coverage |
|---|--------|----------|
| M1 | Open a multi-image thread; tap a thumb → `InternalMediaViewer` dialog; swipe pages with `HorizontalPager` | Manual |
| M2 | Settled page ±1 media prefetches into Coil; swipe to next image is warm | Manual |
| M3 | Video posts play in-page; backgrounding the app pauses sensibly; close returns to thread | Manual |
| M4 | Pinch/double-tap zoom on stills; pager swipe disabled or gated while zoomed (no accidental page change) | Manual |

## External links

| # | Check | Coverage |
|---|--------|----------|
| E1 | Post with `https://…` link → confirm dialog (“Open external link?”) before browser | Manual |
| E2 | Cancel dismisses without opening; Open only proceeds for HTTPS | Manual |
| E3 | Cleartext `http://`, non-web schemes ignored / not offered | CI (`ExternalLinksTest`) |

## Allowlists / security ingest

| # | Check | Coverage |
|---|--------|----------|
| A1 | Media only loads from allowlisted hosts (`i.4cdn.org`, `a.4cdn.org`, `bbw-chan.link`, …) over HTTPS | CI (`AllowlistsTest`) |
| A2 | Board slugs reject traversal / illegal characters on follow and parse | CI (`AllowlistsTest`, `FollowedBoardStoreTest`) |
| A3 | LynxChan relative media paths reject `//`, `..`, and off-allowlist absolute URLs | CI (`AllowlistsTest`) |

## Cache / refresh

| # | Check | Coverage |
|---|--------|----------|
| C1 | Within ~60 s, Refresh / revisit can satisfy catalog/thread JSON from OkHttp disk cache (`max-age=60` rewrite) | CI (`JsonGetCacheInterceptorTest`) + Manual (airplane-mode after warm load optional) |
| C2 | Coil disk/memory serves thumbs offline after a warm session | Manual |
| C3 | Followed boards persist across process death (SharedPreferences + in-memory snapshot until write) | CI (`FollowedBoardStoreTest`) + Manual |
| C4 | Partial catalog failure shows site warning without wiping other boards’ rows | CI (`LoadResultTest`) + Manual |

## Proof-of-work / challenge

| # | Check | Coverage |
|---|--------|----------|
| P1 | PoW / anti-bot challenge flows | **N/A** for Minimal (read-only browse; no posting / challenge solver) |

## Chrome / polish (smoke)

| # | Check | Coverage |
|---|--------|----------|
| X1 | Site selector is a single chip row (no extra filled Surface layer causing overdraw) | Manual |
| X2 | Top app bar + Boards / Refresh / Sort row remain usable with one-handed reach on XL | Manual |
| X3 | No duplicate dialogs when rapidly tapping external links or media thumbs | Manual |

---

## Suggested pass order

1. Cold start → follow boards → Feed fling (F1–F3) on 120 Hz  
2. Rotate / wide (W1–W3)  
3. Open thread → gallery pager (M1–M4)  
4. External link confirm (E1–E2)  
5. Airplane-mode cache smoke (C1–C2)  
6. Confirm allowlist / sort / soft-cap behavior already green in CI before shipping

## Out of scope on this device pass

Full Orbin settings, search, downloads manager UI, bookmarks, notifications, app lock, onboarding, theme hub, and PoW (see Architecture “Explicitly out of scope”).
