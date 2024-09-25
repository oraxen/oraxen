rootProject.name = "oraxen"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.mineinabyss.com/releases")
    }

    val idofrontVersion: String by settings
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("com.mineinabyss.conventions"))
                useVersion(idofrontVersion)
        }
    }
}

dependencyResolutionManagement {
    val idofrontVersion: String by settings

    repositories {
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.mineinabyss.com/snapshots")
        mavenLocal()
    }

    versionCatalogs {
        create("libs").from("com.mineinabyss:catalog:$idofrontVersion")
        create("oraxenLibs").from(files("gradle/oraxenLibs.versions.toml"))
    }
}

include(
    "core",
    //"v1_18_R1",
    //"v1_18_R2",
    //"v1_19_R1",
    //"v1_19_R2",
    //"v1_19_R3",
    "v1_20_R1",
    "v1_20_R2",
    "v1_20_R3",
    "v1_20_R4",
    "v1_21_R1",
)
