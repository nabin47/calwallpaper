import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
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
        lint { configureStrictLint(this@configureAndroidApplication) }
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
