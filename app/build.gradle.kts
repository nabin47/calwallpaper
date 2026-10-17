plugins {
    id("calwp.android.application")
    id("calwp.android.compose")
    id("calwp.baselineprofile.consumer")
    // Navigation 3 back-stack keys are @Serializable, so the stack survives process death (ADR 0009).
    id("calwp.serialization")
}

// Release signing comes from secrets that only release.yml provides (see docs/release.md). Everywhere
// else (PR CI, local builds) no release signing config exists; debug builds use the standard debug key.
val signingEnv =
    listOf("SIGNING_KEYSTORE_PATH", "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
        .associateWith { providers.environmentVariable(it).orNull.orEmpty() }
val hasSigningKey = signingEnv.values.all { it.isNotEmpty() }

// Debug signing (ADR 0006): CI signs debug APKs with one fixed debug-only key, decoded from the
// DEBUG_KEYSTORE_BASE64 secret, so each new build installs over the last. It is never the release key; its
// passwords are the standard, non-secret debug ones. Without it (forks, local builds) the usual debug key is used.
val fixedDebugKeystore = providers.environmentVariable("DEBUG_KEYSTORE_PATH").orNull?.takeIf { it.isNotEmpty() }

android {
    namespace = "io.github.nabin47.calwallpaper"

    defaultConfig {
        // Permanent once published on Google Play. Do not change (CLAUDE.md §10).
        applicationId = "io.github.nabin47.calwallpaper"
        versionCode = providers.gradleProperty("calwp.versionCode").getOrElse("1").toInt()
        versionName = providers.gradleProperty("calwp.versionName").getOrElse("0.0.0-dev")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (fixedDebugKeystore != null) {
            create("fixedDebug") {
                storeFile = file(fixedDebugKeystore)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (hasSigningKey) {
            create("release") {
                storeFile = file(signingEnv.getValue("SIGNING_KEYSTORE_PATH"))
                storePassword = signingEnv.getValue("KEYSTORE_PASSWORD")
                keyAlias = signingEnv.getValue("KEY_ALIAS")
                keyPassword = signingEnv.getValue("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            signingConfigs.findByName("fixedDebug")?.let { signingConfig = it }
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":render"))
    implementation(project(":data"))
    implementation(project(":android-wallpaper"))
    implementation(project(":android-widgets"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)

    // Generates the Baseline Profile (test-only; nothing from it ships). See baselineprofile/README.md.
    baselineProfile(project(":baselineprofile"))
}
