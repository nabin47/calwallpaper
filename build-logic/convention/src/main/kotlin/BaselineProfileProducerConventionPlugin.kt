import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * The Baseline Profile generator (`:baselineprofile`): a `com.android.test` module that drives the release app
 * on an emulator and records which code runs. Kotlin comes from AGP's built-in support. Nothing from this module
 * ships in the app.
 */
class BaselineProfileProducerConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.test")
            pluginManager.apply("androidx.baselineprofile")
            configureAndroidTest()
            configureQuality()
            dependencies {
                add("implementation", libs.library("androidx-benchmark-macro-junit4"))
                add("implementation", libs.library("androidx-test-uiautomator"))
                add("implementation", libs.library("androidx-test-ext-junit"))
            }
        }
    }
}
