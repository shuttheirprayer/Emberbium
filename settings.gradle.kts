pluginManagement {
    repositories {
        // Vendored copy of the ModDevGradle PR #118 build. Upstream resolves it from
        // prmaven.neoforged.net, an ephemeral per-PR maven that will eventually be garbage
        // collected, which would leave a fresh clone unbuildable. Listed first so the local
        // copy wins; the PR maven below stays as a fallback while it still exists.
        maven(settingsDir.resolve("gradle/vendor-maven")) {
            name = "Vendored ModDevGradle PR #118"
            content {
                includeModule("net.neoforged", "moddev-gradle")
                includeModule("net.neoforged.moddev.legacy", "net.neoforged.moddev.legacy.gradle.plugin")
            }
        }
        maven("https://maven.minecraftforge.net")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://maven.parchmentmc.org")
        mavenCentral()
        gradlePluginPortal()
        maven("https://prmaven.neoforged.net/ModDevGradle/pr118") {
            name = "Maven for PR #118"
            content {
                includeModule("net.neoforged", "moddev-gradle")
                includeModule("net.neoforged.moddev", "net.neoforged.moddev.gradle.plugin")
                includeModule("net.neoforged.moddev.repositories", "net.neoforged.moddev.repositories.gradle.plugin")
                includeModule("net.neoforged.moddev.legacy", "net.neoforged.moddev.legacy.gradle.plugin")
            }
        }
    }
}

val minecraft_version: String by settings
rootProject.name = "emberbium-${minecraft_version}"
