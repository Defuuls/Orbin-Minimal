# Orbin Minimal extraction

Source: `Defuuls/Orbin`

The Orbin Minimal Android app is defined by `app-minimal` and `benchmark-minimal`, but it currently depends on shared runtime modules from the full Orbin project (`domain`, `data`, `network`, `media`, selected core modules, providers, features, `ui-next`, Gradle convention plugins, and the version catalog).

This branch begins the repository split. Shared modules will be copied here as required so `com.orbin.minimal` can build independently. Full-client-only entry points remain in `Defuuls/Orbin`.
