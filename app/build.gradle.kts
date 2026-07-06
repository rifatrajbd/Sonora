plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.sonora.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sonora.music"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default self-hosted squid.wtf backend that fronts Qobuz / Tidal / Amazon (FLAC).
        // Override in local secrets.properties -> SONORA_SQUID_BASE_URL for your own instance.
        val squidBase = providers.gradleProperty("SONORA_SQUID_BASE_URL").getOrElse("https://squid.wtf/")
        buildConfigField("String", "SQUID_BASE_URL", "\"$squidBase\"")

        val jiosaavnBase = providers.gradleProperty("SONORA_JIOSAAVN_BASE_URL").getOrElse("https://saavn-api.nandanvarma.com/")
        buildConfigField("String", "JIOSAAVN_BASE_URL", "\"$jiosaavnBase\"")
    }

    buildTypes {
        release {
            // Preview releases are signed with the debug key so the APK is installable straight
            // from GitHub Releases (Sonora is side-loaded, not on Play Store). Swap in a real
            // upload keystore before a proper 1.0. Minify is off for now to avoid R8 stripping
            // NewPipeExtractor's reflection until the shrink rules are fully verified.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.palette)
    debugImplementation(libs.androidx.ui.tooling)

    // Media3 / ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.datasource.okhttp)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (settings)
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Image loading
    implementation(libs.coil.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // YouTube Music extraction (InnerTube client-spoof handled by the library)
    implementation(libs.newpipe.extractor)
}
