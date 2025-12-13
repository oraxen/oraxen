plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
    id("io.github.goooler.shadow") version "8.1.8"
}

// Paper 1.21.10 dev bundle provides better compatibility across 1.21.9-1.21.11
// The class names (e.g., ResourceLocation) remain consistent with runtime.
val enableReobf: Boolean = (project.findProperty("oraxen_enable_reobf")?.toString() ?: "false").toBoolean()

dependencies {
    compileOnly(project(":core"))
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    // paperweight creates this task; if mappings are missing it fails at execution time.
    matching { it.name == "reobfJar" }.configureEach {
        enabled = enableReobf
    }
}

// Root project depends on the `reobf` outgoing configuration when embedding NMS modules.
// If we disable reobfJar, we publish the plain jar to that configuration so resolution still works.
if (!enableReobf) {
    configurations.named("reobf") {
        // paperweight may have already registered an outgoing artifact for `reobfJar`.
        // Clear it so we don't accidentally embed a stale/previously-built reobf jar.
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.named("jar"))
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}


