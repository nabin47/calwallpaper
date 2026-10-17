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
    implementation(libs.kotlin.serialization.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.room3.gradlePlugin)
    implementation(libs.roborazzi.gradlePlugin)
    implementation(libs.baselineprofile.gradlePlugin)
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
        register("serialization") {
            id = "calwp.serialization"
            implementationClass = "SerializationConventionPlugin"
        }
        register("room") {
            id = "calwp.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("screenshots") {
            id = "calwp.screenshots"
            implementationClass = "ScreenshotsConventionPlugin"
        }
        register("baselineProfileConsumer") {
            id = "calwp.baselineprofile.consumer"
            implementationClass = "BaselineProfileConsumerConventionPlugin"
        }
        register("baselineProfileProducer") {
            id = "calwp.baselineprofile.producer"
            implementationClass = "BaselineProfileProducerConventionPlugin"
        }
        register("androidCompose") {
            id = "calwp.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
    }
}
