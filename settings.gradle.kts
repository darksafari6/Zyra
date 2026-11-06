pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "ZYRA"
include(":app", ":core-ui", ":core-data", ":core-network", ":feature-chat", ":feature-voice", ":feature-onboarding", ":feature-settings", ":assistant-service", ":device-control")
