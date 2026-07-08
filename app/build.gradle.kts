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

// versionCode tracks git commit count, not a hand-maintained number - it can
// only go up, and it's always obvious which commit produced which build.
// gitShortSha is exposed separately via BuildConfig for exact traceability
// (shown in the Settings screen) since versionName only changes on
// deliberate bumps, not every commit.
fun runGit(vararg args: String): String = try {
    val process = ProcessBuilder("git", *args)
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    process.inputStream.bufferedReader().readText().trim().also { process.waitFor() }
} catch (e: Exception) {
    ""
}

val gitCommitCount = runGit("rev-list", "--count", "HEAD").toIntOrNull() ?: 1
val gitShortSha = runGit("rev-parse", "--short", "HEAD").ifBlank { "unknown" }

// Release signing key comes from keystore.properties (gitignored, not committed) -
// see keystore.properties.example for the format. Without it, assembleRelease
// falls back to an unsigned build (fine for CI/local checks, not for distribution).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "dev.zelenzoom.rowingbridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.zelenzoom.rowingbridge"
        minSdk = 29
        targetSdk = 35
        versionCode = gitCommitCount
        versionName = "0.3.0"

        buildConfigField("String", "STRAVA_CLIENT_ID", "\"${localProperties.getProperty("strava.clientId", "")}\"")
        buildConfigField("String", "STRAVA_CLIENT_SECRET", "\"${localProperties.getProperty("strava.clientSecret", "")}\"")
        buildConfigField("String", "GIT_SHA", "\"$gitShortSha\"")
    }

    signingConfigs {
        if (keystoreProperties.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystoreProperties.getProperty("storeFile") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
