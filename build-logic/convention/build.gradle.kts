plugins {
    `kotlin-dsl`
}

group = "io.github.nabin47.calwallpaper.buildlogic"

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("root") {
            id = "calwp.root"
            implementationClass = "RootConventionPlugin"
        }
        register("kmpJvm") {
            id = "calwp.kmp.jvm"
            implementationClass = "KmpJvmConventionPlugin"
        }
        register("kmpAndroid") {
            id = "calwp.kmp.android"
            implementationClass = "KmpAndroidConventionPlugin"
        }
        register("androidLibrary") {
            id = "calwp.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "calwp.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "calwp.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
    }
}
