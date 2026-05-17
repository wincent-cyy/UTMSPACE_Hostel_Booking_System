pluginManagement {
    repositories {
        // 1. Cleaned up Google repository to prevent regex group blocking
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    // 2. Changed to PREFER_SETTINGS so your project cleanly prioritizes custom repositories
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // 3. Properly configured JitPack repository using Kotlin DSL syntax
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "UTMSPACE_HostelBookingSystem"
include(":app")