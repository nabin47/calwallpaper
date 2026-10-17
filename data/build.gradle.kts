plugins {
    id("calwp.kmp.android")
    id("calwp.room")
    id("calwp.serialization")
}

kotlin {
    android {
        namespace = "io.github.nabin47.calwallpaper.data"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        withHostTest { }
    }

    compilerOptions {
        // Room's KMP database constructor is an `expect object` (KSP generates the `actual`); expect/actual
        // classes are a Beta language feature that must be opted into explicitly. See ADR 0002.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.ktor.client.core)
        }
        getByName("androidMain").dependencies {
            implementation(libs.androidx.sqlite.framework)
            implementation(libs.androidx.datastore)
            implementation(libs.ktor.client.okhttp)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test.junit)
            implementation(libs.ktor.client.mock)
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
}
