import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Pure Kotlin Multiplatform module (`core`, `domain`, `render`). Only the JVM target for now: code in
 * `commonMain` must stay free of `java.*` and Android APIs so iOS targets can be added later.
 */
class KmpJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            extensions.configure<KotlinMultiplatformExtension> {
                jvm {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }
                sourceSets.getByName("commonTest").dependencies {
                    implementation(kotlin("test"))
                }
            }
            pluginManager.withPlugin("java-base") {
                extensions.configure<JavaPluginExtension> {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
            configureQuality()
        }
    }
}
