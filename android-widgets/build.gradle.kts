plugins {
    id("calwp.android.library")
    // Glance is built on the Compose runtime. Widgets run in the app's main process, never in `:wallpaper`.
    id("calwp.android.compose")
}

android {
    namespace = "io.github.nabin47.calwallpaper.widgets"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":render"))

    // The app module declares the widget receivers, so Glance is part of this module's API.
    api(libs.androidx.glance.appwidget)
}
