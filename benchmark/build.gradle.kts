plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.plugin.compose")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.orbin.minimal.benchmark"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // Macrobenchmark tooling floor, independent of the app's minSdk.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

baselineProfile {
    // Generation needs a rooted emulator or unlocked device (see baseline-profile.yml).
    useConnectedDevices = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0-rc02")
    implementation("androidx.test.uiautomator:uiautomator:2.4.0")
}
