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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
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

include(
    "core",
    "v1_20_R1",
    "v1_20_R2",
    "v1_20_R3",
    "v1_20_R4",
    "v1_21_R1",
    "v1_21_R2",
    "v1_21_R3",
    "v1_21_R4",
    "v1_21_R5",
    "v1_21_R6"
)
