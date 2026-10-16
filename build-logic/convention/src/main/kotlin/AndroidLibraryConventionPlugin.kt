import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Android-only library (`android-wallpaper`, `android-widgets`). Kotlin comes from AGP's built-in support. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            configureAndroidLibrary()
            configureQuality()
            dependencies {
                add("testImplementation", libs.library("kotlin-test-junit"))
                add("testImplementation", libs.library("junit4"))
            }
        }
    }
}
