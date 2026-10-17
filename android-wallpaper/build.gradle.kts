plugins {
    id("calwp.android.library")
    id("calwp.screenshots")
}

android {
    namespace = "io.github.nabin47.calwallpaper.wallpaper"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":render"))
}
