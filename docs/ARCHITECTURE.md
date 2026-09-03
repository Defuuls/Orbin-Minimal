# Architecture

## Goal

Orbin Minimal is a separate product, not a thin launcher over the full Orbin codebase.

## Dependency rule

New code may depend inward toward stable models and interfaces, but UI code must not depend on the full Orbin repository or its modules.

Initial package boundaries:

- `model` — immutable board, thread, post, and media models
- `provider` — provider contracts and Vichan/Lynxchan adapters
- `network` — HTTP client and transport DTOs
- `data` — repositories, cache coordination, followed-board persistence
- `media` — media URL/type handling and viewer support
- `feature.feed` — merged followed-board feed, ordered by `data.FeedSort`
- `feature.boards` — followed-board management
- `feature.thread` — thread reader
- `feature.media` — fullscreen media viewer

## Feed ordering

The merged feed is ordered by `FeedSort`, which defaults to `BOARD`:

- `BOARD` — groups threads by provider and board, newest thread first within
  each board, so a board's threads stay together and fresh ones surface. Ties
  on creation time fall back to thread id, so a refresh cannot reshuffle rows.
- `ACTIVITY` — one flat list, most recently bumped thread first, across boards.

"Newest" is thread creation time (`FeedThread.createdAtEpochMillis`), read from
the catalogs — Vichan's `time`, LynxChan's `creation` — and falling back to last
activity when a provider omits it. Sorting is a pure function over the loaded
list, so switching mode reorders what is on screen without refetching.

## Selective port policy

Code from `Defuuls/Orbin` is ported only when all of the following are true:

1. Minimal requires the behavior directly.
2. The code can be understood without importing a large dependency chain.
3. Full-app-specific settings, navigation, analytics, or UI abstractions can be removed.
4. Tests can be written around the Minimal-owned version.

Prefer rewriting small integration layers over importing generic full-app abstractions.

## Explicitly out of scope

- full settings hub
- search
- downloads manager UI
- bookmarks/history
- notifications/watch system
- app lock
- onboarding framework
- theme customization surface
- full-app navigation shell
- full-app feature modules

## Build strategy

Start with one `:app` module to keep the dependency graph small. Split packages into Gradle modules only when boundaries become stable enough to justify the build complexity.
