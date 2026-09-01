plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val minimalVersionCode = providers.gradleProperty("orbin.minimalVersionCode").get().toInt()
val minimalVersionName = providers.gradleProperty("orbin.minimalVersionName").get()
val releaseKeystoreFile = System.getenv("ORBIN_KEYSTORE_FILE")
val releaseKeystorePassword = System.getenv("ORBIN_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ORBIN_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ORBIN_KEY_PASSWORD")

android {
    namespace = "com.orbin.minimal"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.orbin.minimal"
        minSdk = 31
        targetSdk = 36
        versionCode = minimalVersionCode
        versionName = minimalVersionName
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (!releaseKeystoreFile.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystoreFile)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("io.coil-kt.coil3:coil-compose:3.6.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.0")
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
