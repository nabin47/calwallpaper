plugins {
    id("calwp.android.library")
}

android {
    namespace = "io.github.nabin47.calwallpaper.wallpaper"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":render"))
}
