rootProject.name = "oraxen"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.mineinabyss.com/releases")
    }
}

plugins {
    // allows for better class redefinitions with run-paper
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
//    repositories {
//        maven("https://repo.mineinabyss.com/releases")
//        maven("https://repo.mineinabyss.com/snapshots")
//        mavenLocal()
//    }

    versionCatalogs {
        create("oraxenLibs") {
            from(files("gradle/oraxenLibs.versions.toml"))
        }
    }
}

// Core plus split Paper/Paper-fork NMS modules. Java 21 handlers are kept
// loadable on older servers, while 26.x-only code is isolated in Java 25 bytecode.
include(
    "core",
    "nms:java21",
    "nms:java25"
)
