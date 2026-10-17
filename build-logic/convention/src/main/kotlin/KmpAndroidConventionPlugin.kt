import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Kotlin Multiplatform module with an Android target (`data`), using AGP's
 * `com.android.kotlin.multiplatform.library` plugin. The module script configures `kotlin { android { } }`.
 */
class KmpAndroidConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            configureQuality()
        }
    }
}
