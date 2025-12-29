plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
    id("io.github.goooler.shadow") version "8.1.8"
}

// Paper 1.21.10 dev bundle for v1_21_R6_old NMS module
// This module supports 1.21.9 and 1.21.10 (which use ResourceLocation, not Identifier)
// Paper 1.20.5+ uses Mojang mappings at runtime, so we ship Mojang-mapped code

dependencies {
    compileOnly(project(":core"))
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    // Disable reobfJar - Paper 1.20.5+ runs with Mojang mappings
    matching { it.name == "reobfJar" }.configureEach {
        enabled = false
    }
}

// Root project depends on the `reobf` outgoing configuration when embedding NMS modules.
// Since reobfJar is disabled, publish the plain Mojang-mapped jar.
configurations.named("reobf") {
    outgoing.artifacts.clear()
    outgoing.artifact(tasks.named("jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

