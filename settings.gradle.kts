pluginManagement {
  repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories { google(); mavenCentral() }
}
rootProject.name = "AutoCutPad"
include(":app")
include(":feature:clip")
include(":feature:retrieval")
// Whisper functionality consolidated into app module
// include(":feature:whisper")
