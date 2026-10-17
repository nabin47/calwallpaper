plugins {
    id("calwp.android.library")
    id("calwp.serialization")
    id("calwp.screenshots")
}

android {
    namespace = "io.github.nabin47.calwallpaper.wallpaper"
}

// No Compose and no DI framework here: this code runs in the `:wallpaper` process (CLAUDE.md §3, ADR 0004).
dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":render"))
    implementation(project(":data"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.annotation)
    // LocalTasksDatabase (from :data) is a Room type; its class must be on this module's compile classpath.
    implementation(libs.androidx.room3.runtime)
}
