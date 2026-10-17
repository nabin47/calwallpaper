plugins {
    id("calwp.kmp.jvm")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
        }
    }
}
