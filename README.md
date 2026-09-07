# Orbin Minimal

Orbin Minimal is a focused Android imageboard client maintained independently from the full Orbin application.

## Product scope

Minimal intentionally supports only the core browsing loop:

- followed boards combined into a single feed (default **Board** sort; optional **Latest activity**)
- board management
- thread reading
- image/media viewing
- Vichan- and Lynxchan-compatible providers
- lightweight local caching (see below)
- a fixed, opinionated experience instead of the full Orbin settings surface

The project does **not** depend on the full Orbin feature, domain, data, UI, or build-logic modules. Proven algorithms and protocol behavior may be selectively ported from `Defuuls/Orbin`, but every dependency added here must be justified by Minimal's own requirements.

## Local caching and persistence

Minimal does **not** ship a general offline catalog store. What it does cache:

- **JSON HTTP** — OkHttp disk cache (~5 MB) with `JsonGetCacheInterceptor` rewriting successful JSON GETs to `Cache-Control: public, max-age=60` so short-lived catalog/thread responses can be reused
- **Images** — Coil memory cache (~20% of available) plus a ~64 MB disk cache (`image_cache`); list thumbs decode to density-scaled sizes
- **Followed boards** — SharedPreferences JSON via `FollowedBoardStore` (in-memory snapshot until the next write)

Pull-to-refresh / Refresh still hits the network when the max-age window has expired.

## Architecture

The clean rebuild starts as a single Android application module with package boundaries for `model`, `network`, `provider`, `data`, `media`, and UI features. Modules will only be split out when there is a measurable maintenance or build benefit.

See `docs/ARCHITECTURE.md` for the dependency rules, feed windowing notes, and migration policy. For Pixel 10 Pro XL manual verification, see `docs/PIXEL_XL_TEST_PLAN.md`.
