import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/**
 * Enforces CLAUDE.md §4/§8: dependencies point inward and nothing depends on `:app`.
 * Runs once after all projects are configured; a violation fails the build with the offending edges.
 */
internal object ModuleGraph {
    private val sharedAndroid = setOf(":core", ":domain", ":render", ":data")

    private val allowed: Map<String, Set<String>> =
        mapOf(
            ":core" to emptySet(),
            ":domain" to setOf(":core"),
            ":render" to setOf(":core", ":domain"),
            ":data" to setOf(":core", ":domain"),
            ":android-wallpaper" to sharedAndroid,
            ":android-widgets" to sharedAndroid,
            ":app" to sharedAndroid + setOf(":android-wallpaper", ":android-widgets"),
        )

    /** Throws [GradleException] listing every forbidden project-to-project dependency. */
    fun verify(root: Project) {
        val violations =
            root.subprojects.flatMap { project ->
                val permitted = allowed[project.path]
                if (permitted == null) {
                    listOf("${project.path} is not registered in build-logic ModuleGraph")
                } else {
                    projectDependencies(project)
                        .filter { it != project.path && it !in permitted }
                        .map { "${project.path} must not depend on $it" }
                }
            }
        if (violations.isNotEmpty()) {
            throw GradleException("Module graph violations:\n" + violations.joinToString("\n") { "  - $it" })
        }
    }

    private fun projectDependencies(project: Project): Set<String> =
        project.configurations
            .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
            .map { it.path }
            .toSet()
}
