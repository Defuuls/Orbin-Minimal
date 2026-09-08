plugins {
    id("com.android.application") version "9.4.0" apply false
    id("com.android.test") version "9.3.2" apply false
    // Held at 2.4.10 until CodeQL supports 2.4.20+ (CodeQL 2.26.2 ceiling).
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    // Root-only unused/misdeclared dependency advice: `gradle buildHealth`
    id("com.autonomousapps.dependency-analysis") version "3.19.1"
    id("androidx.baselineprofile") version "1.5.0-rc02" apply false
}
