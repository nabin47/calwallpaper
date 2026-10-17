import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * JVM screenshot tests with Roborazzi on Robolectric (no emulator, CLAUDE.md §8). Apply after
 * `calwp.android.library`. Golden images live next to the tests in `src/test/screenshots/`.
 *
 * - `./gradlew verifyRoborazziDebug` fails on any pixel difference (CI).
 * - `./gradlew recordRoborazziDebug` rewrites the goldens (the "Record screenshots" workflow).
 */
class ScreenshotsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")
            extensions.configure<LibraryExtension> {
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            // Robolectric's FileDescriptorInterceptor (used by native graphics) reflects into JDK internals,
            // which JDK 17+ blocks unless the packages are opened to the test classpath.
            tasks.withType<Test>().configureEach {
                jvmArgs(
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
            }
            dependencies {
                add("testImplementation", libs.library("robolectric"))
                add("testImplementation", libs.library("roborazzi"))
            }
        }
    }
}
