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
        // added for photo view
        maven("https://www.jitpack.io" )
        mavenCentral()
        jcenter()
    }
}

rootProject.name = "Insta-Fire"
include(":app")
