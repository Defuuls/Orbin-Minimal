# Orbin Minimal

Orbin Minimal is a focused Android imageboard client maintained independently from the full Orbin application.

## Product scope

Minimal intentionally supports only the core browsing loop:

- followed boards combined into a single activity-sorted feed
- board management
- thread reading
- image/media viewing
- Vichan- and Lynxchan-compatible providers
- local caching and lightweight persistence
- a fixed, opinionated experience instead of the full Orbin settings surface

The project does **not** depend on the full Orbin feature, domain, data, UI, or build-logic modules. Proven algorithms and protocol behavior may be selectively ported from `Defuuls/Orbin`, but every dependency added here must be justified by Minimal's own requirements.

## Architecture

The clean rebuild starts as a single Android application module with package boundaries for `model`, `network`, `provider`, `data`, `media`, and UI features. Modules will only be split out when there is a measurable maintenance or build benefit.

See `docs/ARCHITECTURE.md` for the dependency rules and migration policy.
