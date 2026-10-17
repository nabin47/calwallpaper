import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** The `app` module. Kotlin comes from AGP's built-in support. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            configureAndroidApplication()
            configureQuality()
            dependencies {
                add("testImplementation", libs.library("kotlin-test-junit"))
                add("testImplementation", libs.library("junit4"))
            }
        }
    }
}
