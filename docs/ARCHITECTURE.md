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
- `feature.feed` — merged followed-board feed
- `feature.boards` — followed-board management
- `feature.thread` — thread reader
- `feature.media` — fullscreen media viewer

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
