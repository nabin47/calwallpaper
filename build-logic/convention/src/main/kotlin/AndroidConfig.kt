import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import com.android.build.api.dsl.TestExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Java level for Android modules. Kotlin's jvmTarget follows it under AGP built-in Kotlin. */
internal val ANDROID_JAVA_VERSION = JavaVersion.VERSION_17

/** Shared settings for the `app` module (SDK levels, Java level, strict lint). */
internal fun Project.configureAndroidApplication() {
    extensions.configure<ApplicationExtension> {
        compileSdk = libs.intVersion("android-compileSdk")
        defaultConfig {
            minSdk = libs.intVersion("android-minSdk")
            targetSdk = libs.intVersion("android-targetSdk")
        }
        compileOptions {
            sourceCompatibility = ANDROID_JAVA_VERSION
            targetCompatibility = ANDROID_JAVA_VERSION
        }
        lint {
            configureStrictLint(this@configureAndroidApplication)
            // `data` (KMP Android library) has no `lint` task of its own; app lint covers it as a dependency.
            checkDependencies = true
        }
    }
}

/** Shared settings for plain Android library modules. */
internal fun Project.configureAndroidLibrary() {
    extensions.configure<LibraryExtension> {
        compileSdk = libs.intVersion("android-compileSdk")
        defaultConfig {
            minSdk = libs.intVersion("android-minSdk")
        }
        compileOptions {
            sourceCompatibility = ANDROID_JAVA_VERSION
            targetCompatibility = ANDROID_JAVA_VERSION
        }
        lint { configureStrictLint(this@configureAndroidLibrary) }
    }
}

/**
 * Shared settings for the Baseline Profile generator (a `com.android.test` module). Its minSdk is 28, the lowest
 * level on which Macrobenchmark can collect profiles; the app itself still supports 26.
 */
internal fun Project.configureAndroidTest() {
    extensions.configure<TestExtension> {
        compileSdk = libs.intVersion("android-compileSdk")
        defaultConfig {
            minSdk = PROFILE_GENERATOR_MIN_SDK
            targetSdk = libs.intVersion("android-targetSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = ANDROID_JAVA_VERSION
            targetCompatibility = ANDROID_JAVA_VERSION
        }
        lint { configureStrictLint(this@configureAndroidTest) }
    }
}

private const val PROFILE_GENERATOR_MIN_SDK = 28

/** Lint warnings fail the build; the shared `lint.xml` turns off checks that only nag about newer versions. */
private fun Lint.configureStrictLint(project: Project) {
    abortOnError = true
    warningsAsErrors = true
    checkReleaseBuilds = true
    lintConfig =
        project.rootProject.layout.projectDirectory
            .file("lint.xml")
            .asFile
}
