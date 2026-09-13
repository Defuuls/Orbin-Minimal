plugins {
    id("com.android.application") version "9.4.0" apply false
    id("com.android.test") version "9.4.0" apply false
    // Kotlin 2.4.20 fixes CVE-2026-53914. CodeQL temporarily rewrites only its
    // disposable, cache-disabled analysis workspace until the shipped extractor catches up.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    // Root-only unused/misdeclared dependency advice: `gradle buildHealth`
    id("com.autonomousapps.dependency-analysis") version "3.19.1"
    id("androidx.baselineprofile") version "1.5.0" apply false
}
