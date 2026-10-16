import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** The `libs` version catalog (`gradle/libs.versions.toml`), for use inside convention plugins. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Reads a required version from the catalog, e.g. `android-compileSdk`. */
internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

/** Reads a required integer version from the catalog (SDK levels). */
internal fun VersionCatalog.intVersion(alias: String): Int = version(alias).toInt()

/** Reads a required library from the catalog. */
internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias).get()
