plugins {
    id("calwp.kmp.jvm")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Scene and RenderRequest expose core and domain types, so consumers need them too.
            api(project(":core"))
            api(project(":domain"))
        }
    }
}
