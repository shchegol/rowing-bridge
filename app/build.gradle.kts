import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Strava credentials come from local.properties (gitignored), not committed.
// Register your own app at https://www.strava.com/settings/api - see the
// project plan for the exact Authorization Callback Domain to use.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "dev.zelenzoom.rowingbridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.zelenzoom.rowingbridge"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "STRAVA_CLIENT_ID", "\"${localProperties.getProperty("strava.clientId", "")}\"")
        buildConfigField("String", "STRAVA_CLIENT_SECRET", "\"${localProperties.getProperty("strava.clientSecret", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.browser)
    implementation(libs.okhttp)
    implementation(libs.garmin.fit)

    debugImplementation(libs.androidx.ui.tooling)
}
