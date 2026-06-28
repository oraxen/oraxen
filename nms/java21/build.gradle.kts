plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

// Java 21 NMS handler for Paper/Paper-fork 1.20.x through 1.21.11.
// Keep this module free of Java 25 bytecode so it can be loaded on Java 21 servers.
dependencies {
    compileOnly(rootProject)
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    // Paper 1.21.11 runs Mojang-mapped plugins; publish the plain jar as the NMS artifact.
    matching { it.name == "reobfJar" }.configureEach {
        enabled = false
    }
}

configurations.named("reobf") {
    outgoing.artifacts.clear()
    outgoing.artifact(tasks.named("jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
