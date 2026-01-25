pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Connectias-Plugins"

include(":connectias-plugin-sdk")
include(":ping-plugin")
include(":test-plugin")
include(":network-info-plugin")
include(":test2-plugin")
