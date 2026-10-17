import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Root project: formats build scripts and build-logic, and verifies the module graph. */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.diffplug.spotless")
            val ktlintVersion = libs.version("ktlint")
            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("build-logic/**/*.kt")
                    targetExclude("**/build/**")
                    ktlint(ktlintVersion)
                }
                kotlinGradle {
                    target("*.gradle.kts", "build-logic/**/*.gradle.kts")
                    targetExclude("**/build/**")
                    ktlint(ktlintVersion)
                }
            }
            gradle.projectsEvaluated { ModuleGraph.verify(target) }
        }
    }
}
