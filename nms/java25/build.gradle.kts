plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

// Java 25 NMS handler for Paper/Paper-fork 26.x.
// This module may reference APIs that require the 26.x dev bundle and Java 25 bytecode.
dependencies {
    compileOnly(project(":core"))
    paperweight.paperDevBundle("26.1.2.build.5-alpha")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    // Modern Paper dev bundles are Mojang-mapped at runtime; no reobf artifact is needed.
    matching { it.name == "reobfJar" }.configureEach {
        enabled = false
    }
}

configurations.named("reobf") {
    outgoing.artifacts.clear()
    outgoing.artifact(tasks.named("jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
