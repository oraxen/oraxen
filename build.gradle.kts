import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

plugins {
    id("java")
    id("idea")
    id("eclipse")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3" // Generates plugin.yml
}

val compiled = (project.findProperty("oraxen_compiled")?.toString() ?: "true").toBoolean()
val pluginPath = project.findProperty("oraxen_plugin_path")
val pluginVersion: String by project
group = "io.th0rgal"
version = pluginVersion

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filesNotMatching(listOf("**/*.png", "**/*.ogg", "**/models/**", "**/textures/**", "**/font/**.json", "**/plugin.yml")) {
            expand(mapOf(project.version.toString() to pluginVersion))
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        dependsOn(":v1_18_R2:reobfJar")
        dependsOn(":v1_19_R1:reobfJar")
        dependsOn(":v1_19_R2:reobfJar")
        dependsOn(":v1_19_R3:reobfJar")
        dependsOn(":v1_20_R1:reobfJar")

        //archiveClassifier = null
        relocate("org.bstats", "io.th0rgal.oraxen.shaded.bstats")
        relocate("net.kyori", "io.th0rgal.oraxen.shaded.kyori")
        relocate("dev.triumphteam.gui", "io.th0rgal.oraxen.shaded.triumphteam.gui")
        relocate("com.jeff_media.customblockdata", "io.th0rgal.oraxen.shaded.customblockdata")
        relocate("com.jeff_media.morepersistentdatatypes", "io.th0rgal.oraxen.shaded.morepersistentdatatypes")
        relocate("com.github.stefvanschie.inventoryframework", "io.th0rgal.oraxen.shaded.if")
        relocate("me.gabytm.util.actions", "io.th0rgal.oraxen.shaded.actions")
        relocate("org.intellij.lang.annotations", "io.th0rgal.oraxen.shaded.intellij.annotations")
        relocate("org.jetbrains.annotations", "io.th0rgal.oraxen.shaded.jetbrains.annotations")
        relocate("com.udojava.evalex", "io.th0rgal.oraxen.shaded.evalex")
        relocate("com.ticxo.playeranimator", "io.th0rgal.oraxen.shaded.playeranimator")

        manifest {
            attributes(
                mapOf(
                    "Built-By" to System.getProperty("user.name"),
                    "Version" to pluginVersion,
                    "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSSZ").format(Date.from(Instant.now())),
                    "Created-By" to "Gradle ${gradle.gradleVersion}",
                    "Build-Jdk" to "${System.getProperty("java.version")} ${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")}",
                    "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}",
                    "Premium" to !compiled
                )
            )
        }
        if (!compiled) exclude("io/th0rgal/oraxen/CompileNotice\$PrintNotice.class")
        archiveFileName.set("oraxen-${pluginVersion}.jar")
        minimize()
    }

    compileJava.get().dependsOn(clean)
    build.get().dependsOn(shadowJar)
}

bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "io.th0rgal.oraxen.OraxenPlugin"
    version = pluginVersion
    name = "Oraxen"
    apiVersion = "1.18"
    authors = listOf("th0rgal", "boy0000")
    softDepend = listOf("LightAPI", "PlaceholderAPI", "MythicMobs", "MMOItems", "MythicCrucible", "BossShopPro", "CrateReloaded", "ItemBridge", "WorldEdit", "WorldGuard", "Towny", "Factions", "Lands", "PlotSquared", "NBTAPI", "ModelEngine", "CrashClaim", "ViaBackwards")
    depend = listOf("ProtocolLib")
    loadBefore = listOf("Realistic_World")
    libraries = listOf("org.springframework:spring-expression:6.0.6", "org.apache.httpcomponents:httpmime:4.5.13", "dev.jorel:commandapi-bukkit-shade:$commandApiVersion", "org.joml:joml:1.10.5")
    permissions.create("oraxen.command") {
        description = "Allows the player to use the /oraxen command"
        default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.TRUE
    }
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components.getByName("java"))
        }
    }
}

if (pluginPath != null) {
    tasks {
        register<Copy>("copyJar") {
            this.doNotTrackState("Overwrites the plugin jar to allow for easier reloading")
            dependsOn(shadowJar, jar)
            from(findByName("reobfJar") ?: findByName("shadowJar") ?: findByName("jar"))
            into(pluginPath)
            doLast {
                println("Copied to plugin directory $pluginPath")
            }
        }
        named<DefaultTask>("build").get().dependsOn("copyJar")
    }
}
