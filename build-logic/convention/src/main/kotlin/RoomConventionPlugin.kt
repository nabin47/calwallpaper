import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies KSP and the Room 3 Gradle plugin. Apply after `calwp.kmp.android`. The module script sets
 * `room3 { schemaDirectory(...) }` and adds the compiler to `kspAndroid`, because that configuration only
 * exists once the Android target is declared there.
 */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("com.google.devtools.ksp")
            apply("androidx.room3")
        }
    }
}
