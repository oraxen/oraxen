plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.4.1"
}

// Paper 26.1.2 dev bundle for v1_26_R1 NMS module
// Note: modern Paper dev bundles don't provide reobf mappings, so we ship Mojang-mapped

dependencies {
    compileOnly(project(":core"))
    paperweight.paperDevBundle("26.1.2.build.5-alpha")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    // Disable reobfJar - modern dev bundles don't provide reobf mappings
    // and Paper runs with Mojang mappings anyway
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
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
