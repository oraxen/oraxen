rootProject.name = "oraxen"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.mineinabyss.com/releases")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.mineinabyss.com/snapshots")
        mavenLocal()
    }

    versionCatalogs {
        create("oraxenLibs").from(files("gradle/oraxenLibs.versions.toml"))
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
    "v1_21_R5"
)
