plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
}

// Spigot 1.21.8 NMS module - uses Spigot mappings (obfuscated)
// This module is for Spigot servers, while v1_21_R5 is for Paper servers (Mojang-mapped)
// Requires: Run Spigot BuildTools for 1.21.8 to install artifacts to local Maven

repositories {
    mavenLocal() // For Spigot BuildTools artifacts
}

dependencies {
    compileOnly(project(":core"))
    // Use Spigot-mapped jar from BuildTools (via mavenLocal)
    compileOnly("org.spigotmc:spigot:1.21.8-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    jar {
        archiveClassifier.set("")
    }

    // Create a reobfJar task that just builds the jar (no remapping needed for Spigot)
    // This is needed for compatibility with the root project's shadowJar task
    register("reobfJar") {
        dependsOn(jar)
    }
}

// Expose jar as "reobf" configuration for root project compatibility
val reobf by configurations.creating
artifacts {
    add("reobf", tasks.jar)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
