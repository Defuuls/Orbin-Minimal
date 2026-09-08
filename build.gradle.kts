plugins {
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    // Root-only unused/misdeclared dependency advice: `gradle buildHealth`
    id("com.autonomousapps.dependency-analysis") version "3.19.1"
}
