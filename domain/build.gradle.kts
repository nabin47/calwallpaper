plugins {
    id("calwp.kmp.jvm")
    id("calwp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
        }
    }
}
