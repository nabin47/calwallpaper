import org.gradle.api.Plugin
import org.gradle.api.Project

/** Enables the kotlinx.serialization compiler plugin for `@Serializable` models. */
class SerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    }
}
