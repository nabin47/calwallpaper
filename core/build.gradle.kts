plugins {
    id("calwp.kmp.jvm")
    id("calwp.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.core)
        }
    }
}
