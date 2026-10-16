import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Applies the shared quality gates to a module: Spotless (ktlint), detekt, and Kotlin
 * warnings-as-errors. CLAUDE.md §3 forbids suppressing warnings, so every Kotlin compilation fails on them.
 */
internal fun Project.configureQuality() {
    pluginManager.apply("com.diffplug.spotless")
    pluginManager.apply("dev.detekt")

    val ktlintVersion = libs.version("ktlint")
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(ktlintVersion)
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(ktlintVersion)
        }
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        parallel.set(true)
        config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
        source.setFrom(layout.projectDirectory.dir("src"))
    }

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(true)
        }
    }
}
