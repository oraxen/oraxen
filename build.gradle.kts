import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.jetbrains.kotlin.cli.jvm.compiler.writeOutput
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

plugins {
    id("java")
    //id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.2.4"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0" // Generates plugin.yml
    id("io.papermc.paperweight.userdev") version "1.7.0" apply false
    id("io.github.goooler.shadow") version "8.1.7"
    alias(idofrontLibs.plugins.mia.publication)
}

class NMSVersion(val nmsVersion: String, val serverVersion: String, val javaVersion: Int)

val SUPPORTED_VERSIONS: List<NMSVersion> = listOf(
    NMSVersion("v1_20_R1", "1.20.1-R0.1-SNAPSHOT", 17),
    NMSVersion("v1_20_R2", "1.20.2-R0.1-SNAPSHOT", 17),
    NMSVersion("v1_20_R3", "1.20.4-R0.1-SNAPSHOT", 17),
    NMSVersion("v1_20_R4", "1.20.6-R0.1-SNAPSHOT", 21),
)

val latest = "1.20.6"
val runJavaVersion = 21

SUPPORTED_VERSIONS.forEach {
    project(":${it.nmsVersion}") {
        apply(plugin = "java")
        apply(plugin = "io.papermc.paperweight.userdev")

        repositories {
            maven("https://papermc.io/repo/repository/maven-public/") // Paper
            maven("https://repo.mineinabyss.com/releases")
        }

        configurations.create("mojmap")

        dependencies {
            //compileOnly("io.papermc.paper:paper-api:" + it.serverVersion)
            compileOnly(project(":core"))
            paperDevBundle(it.serverVersion)
        }

        tasks {
            compileJava {
                options.encoding = Charsets.UTF_8.name()
            }
        }

        java {
            toolchain.languageVersion.set(JavaLanguageVersion.of(it.javaVersion))
        }
    }
}

val compiled = (project.findProperty("oraxen_compiled")?.toString() ?: "true").toBoolean()
val pluginPath = project.findProperty("oraxen2_plugin_path")?.toString()
val devPluginPath = project.findProperty("oraxen_dev_plugin_path")?.toString()
val foliaPluginPath = project.findProperty("oraxen_folia_plugin_path")?.toString()
val spigotPluginPath = project.findProperty("oraxen_spigot_plugin_path")?.toString()
val pluginVersion: String by project
val commandApiVersion = "9.4.0"
val adventureVersion = "4.15.0"
val platformVersion = "4.3.2"
val googleGsonVersion = "2.10.1"
val apacheLang3Version = "3.14.0"
val creativeVersion = "1.7.2"
group = "io.th0rgal"
version = pluginVersion

allprojects {
    apply(plugin = "java")
    repositories {
        mavenCentral()

        maven("https://papermc.io/repo/repository/maven-public/") // Paper
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
        maven("https://oss.sonatype.org/content/repositories/snapshots") // Because Spigot depends on Bungeecord ChatComponent-API
        maven("https://repo.dmulloy2.net/repository/public/") // ProtocolLib
        maven("https://libraries.minecraft.net/") // Minecraft repo (commodore)
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceHolderAPI
        maven("https://maven.elmakers.com/repository/") // EffectLib
        maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/") // CustomBlockData
        maven("https://repo.triumphteam.dev/snapshots") // actions-code, actions-spigot
        maven("https://mvn.lumine.io/repository/maven-public/") { metadataSources { artifact() } }// MythicMobs
        maven("https://repo.mineinabyss.com/releases") // PlayerAnimator
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots") // commandAPI snapshots
        maven("https://repo.auxilor.io/repository/maven-public/") // EcoItems
        maven("https://maven.enginehub.org/repo/")
        maven("https://repo.oraxen.com/releases")
        maven("https://repo.oraxen.com/snapshots")
        maven("https://jitpack.io") // JitPack
        maven("https://repo.unnamed.team/repository/unnamed-public/") // Creative
        maven("https://nexus.phoenixdevt.fr/repository/maven-public/") // MMOItems
        maven("https://repo.codemc.org/repository/maven-public/") // BlockLocker

        mavenLocal()
    }

    dependencies {
        val actionsVersion = "1.0.0-SNAPSHOT"
        compileOnly("gs.mclo:java:2.2.1")

        compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")
        compileOnly("net.kyori:adventure-text-serializer-plain:$adventureVersion")
        compileOnly("net.kyori:adventure-text-serializer-ansi:$adventureVersion")
        compileOnly("net.kyori:adventure-platform-bukkit:$platformVersion")
        compileOnly("com.comphenix.protocol:ProtocolLib:5.2.0-SNAPSHOT")
        compileOnly("me.clip:placeholderapi:2.11.4")
        compileOnly("me.gabytm.util:actions-core:$actionsVersion")
        compileOnly("org.springframework:spring-expression:6.0.6")
        compileOnly("io.lumine:Mythic-Dist:5.3.5")
        compileOnly("io.lumine:MythicCrucible:1.6.0-SNAPSHOT")
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9")
        compileOnly("commons-io:commons-io:2.11.0")
        compileOnly("com.google.code.gson:gson:$googleGsonVersion")
        compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.4")
        compileOnly("com.ticxo.modelengine:api:R3.1.8")
        compileOnly(files("../libs/compile/BSP.jar"))
        compileOnly("io.lumine:MythicLib:1.1.6") // Remove and add deps needed for Polymath
        compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT")
        compileOnly("net.Indyuce:MMOItems-API:6.9.5-SNAPSHOT")
        compileOnly("com.willfp:EcoItems:5.23.0")
        compileOnly("com.willfp:eco:6.65.5")
        compileOnly("com.willfp:libreforge:4.36.0")
        compileOnly("nl.rutgerkok:blocklocker:1.10.4-SNAPSHOT")
        compileOnly("org.apache.commons:commons-lang3:$apacheLang3Version")
        compileOnly("team.unnamed:creative-api:$creativeVersion")
        compileOnly("team.unnamed:creative-serializer-minecraft:$creativeVersion")
        compileOnly("team.unnamed:creative-server:$creativeVersion")
        compileOnly(files("../libs/compile/AxiomPaper-1.5.9.jar"))

        implementation("dev.jorel:commandapi-bukkit-shade:$commandApiVersion")
        implementation("org.bstats:bstats-bukkit:3.0.0")
        implementation("io.th0rgal:protectionlib:1.5.1")
        implementation("com.github.stefvanschie.inventoryframework:IF:0.10.12")
        implementation("com.jeff-media:custom-block-data:2.2.2")
        implementation("com.jeff_media:MorePersistentDataTypes:2.4.0")
        implementation("com.jeff-media:persistent-data-serializer:1.0")
        implementation("org.jetbrains:annotations:24.1.0") { isTransitive = false }
        implementation("dev.triumphteam:triumph-gui:3.1.7") { exclude("net.kyori") }

        implementation("me.gabytm.util:actions-spigot:$actionsVersion") { exclude(group = "com.google.guava") }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$latest-R0.1-SNAPSHOT")
    implementation(project(path = ":core"))
    SUPPORTED_VERSIONS.forEach {
        implementation(project(path = ":${it.nmsVersion}", configuration = "reobf"))
    }
}

project.java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(runJavaVersion))
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    runServer {
        pluginJars(fileTree("plugins"))
        minecraftVersion(latest)
    }

    jar {
        dependsOn(clean)
        finalizedBy(shadowJar)
    }
    shadowJar {
        SUPPORTED_VERSIONS.forEach {
            dependsOn(":${it.nmsVersion}:${if (it.javaVersion <= 17) "reobfJar" else "jar"}")
        }

        //archiveClassifier = null
        relocate("org.bstats", "io.th0rgal.oraxen.shaded.bstats")
        relocate("dev.triumphteam.gui", "io.th0rgal.oraxen.shaded.triumphteam.gui")
        relocate("com.jeff_media", "io.th0rgal.oraxen.shaded.jeff_media")
        relocate("com.github.stefvanschie.inventoryframework", "io.th0rgal.oraxen.shaded.inventoryframework")
        relocate("me.gabytm.util.actions", "io.th0rgal.oraxen.shaded.actions")
        relocate("org.intellij.lang.annotations", "io.th0rgal.oraxen.shaded.intellij.annotations")
        relocate("org.jetbrains.annotations", "io.th0rgal.oraxen.shaded.jetbrains.annotations")
        relocate("com.udojava.evalex", "io.th0rgal.oraxen.shaded.evalex")
        relocate("com.ticxo.playeranimator", "io.th0rgal.oraxen.shaded.playeranimator")
        relocate("dev.jorel", "io.th0rgal.oraxen.shaded")

        manifest {
            attributes(
                mapOf(
                    "Built-By" to System.getProperty("user.name"),
                    "Version" to pluginVersion,
                    "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSSZ").format(Date.from(Instant.now())),
                    "Created-By" to "Gradle ${gradle.gradleVersion}",
                    "Build-Jdk" to "${System.getProperty("java.version")} ${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")}",
                    "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}",
                    "Compiled" to (project.findProperty("oraxen_compiled")?.toString() ?: "true").toBoolean(),
                    "authUsr" to (project.findProperty("oraxenUsername")?.toString() ?: ""),
                    "authPw" to (project.findProperty("oraxenPassword")?.toString() ?: "")
                )
            )
        }
        archiveFileName.set("oraxen-${pluginVersion}.jar")
        archiveClassifier.set("")
    }

    build.get().dependsOn(publishToMavenLocal)
}

bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "io.th0rgal.oraxen.OraxenPlugin"
    version = pluginVersion
    name = "Oraxen"
    apiVersion = "1.20"
    authors = listOf("th0rgal", "boy0000")
    softDepend = listOf(
        "ProtocolLib",
        "LightAPI", "PlaceholderAPI", "MythicMobs", "MMOItems", "MythicCrucible", "MythicMobs",
        "WorldEdit", "WorldGuard", "Towny", "Factions", "Lands", "PlotSquared",
        "ModelEngine", "CrashClaim", "HuskClaims", "BentoBox", "AxiomPaper"
    )
    loadBefore = listOf("Realistic_World")
    permissions.create("oraxen.command") {
        description = "Allows the player to use the /oraxen command"
        default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.TRUE
    }
    libraries = listOf(
        "org.springframework:spring-expression:6.0.6",
        "org.apache.httpcomponents:httpmime:4.5.13",
        //"dev.jorel:commandapi-bukkit-shade-mojang-mapped:$commandApiVersion",
        "net.kyori:adventure-text-minimessage:$adventureVersion",
        "net.kyori:adventure-text-serializer-plain:$adventureVersion",
        "net.kyori:adventure-text-serializer-ansi:$adventureVersion",
        "net.kyori:adventure-platform-bukkit:$platformVersion",
        "com.google.code.gson:gson:$googleGsonVersion",
        "org.apache.commons:commons-lang3:$apacheLang3Version",
        "gs.mclo:java:2.2.1",
        "team.unnamed:creative-api:$creativeVersion",
        "team.unnamed:creative-serializer-minecraft:$creativeVersion",
        "team.unnamed:creative-server:$creativeVersion",
    )
}

if (pluginPath != null) {
    tasks {
        val defaultPath = findByName("shadowJar") ?: findByName("jar")
        // Define the main copy task
        val copyJar = register<Copy>("copyJar") {
            this.doNotTrackState("Overwrites the plugin jar to allow for easier reloading")
            dependsOn(shadowJar, jar)
            from(defaultPath)
            into(pluginPath)
            doLast { println("Copied to plugin directory $pluginPath") }
        }
        build.get().dependsOn(copyJar)
    }
}
