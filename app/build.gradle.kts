plugins {
    id("calwp.android.application")
    id("calwp.android.compose")
}

// Release signing comes from CI secrets (see docs/release.md). When any value is missing (fork or
// Dependabot PRs, local builds) no release signing config exists and debug builds use the debug key.
val signingEnv =
    listOf("SIGNING_KEYSTORE_PATH", "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
        .associateWith { providers.environmentVariable(it).orNull.orEmpty() }
val hasSigningKey = signingEnv.values.all { it.isNotEmpty() }

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
            // Same key on every CI run, so a newer debug APK installs over the previous one.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
    implementation(libs.androidx.compose.material3)
}
