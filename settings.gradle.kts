rootProject.name = "oraxen"

pluginManagement {
    plugins {
        id("net.minecrell.plugin-yml.bukkit") version "0.6.0-SNAPSHOT" // Generates plugin.yml
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}
