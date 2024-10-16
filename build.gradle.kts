import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml
import xyz.jpenilla.resourcefactory.bukkit.Permission
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries

plugins {
    id("java")
    //id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.2.4"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.1.1" // Generates plugin.yml based on the Gradle config
    id("io.papermc.paperweight.userdev") version "1.7.2" apply false
    id("com.gradleup.shadow") version "8.3.0"
    alias(idofrontLibs.plugins.mia.publication)
}

class NMSVersion(val nmsVersion: String, val serverVersion: String)
infix fun String.toNms(that: String) = NMSVersion(this, that)
val SUPPORTED_VERSIONS: List<NMSVersion> = listOf(
    "v1_20_R3" toNms "1.20.4-R0.1-SNAPSHOT",
    "v1_20_R4" toNms "1.20.6-R0.1-SNAPSHOT",
    "v1_21_R1" toNms "1.21-R0.1-SNAPSHOT"
)

val compiled = (project.findProperty("oraxen_compiled")?.toString() ?: "true").toBoolean()
val pluginPath = project.findProperty("oraxen2_plugin_path")?.toString()
val devPluginPath = project.findProperty("oraxen_dev_plugin_path")?.toString()
val foliaPluginPath = project.findProperty("oraxen_folia_plugin_path")?.toString()
val spigotPluginPath = project.findProperty("oraxen_spigot_plugin_path")?.toString()
val pluginVersion: String by project
val commandApiVersion = "9.5.3"
val adventureVersion = "4.17.0"
val platformVersion = "4.3.4"
val googleGsonVersion = "2.11.0"
val apacheLang3Version = "3.17.0"
val apacheHttpClientVersion = "5.4"
val creativeVersion = "1.7.3"
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
        maven("https://repo.mineinabyss.com/releases")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots") // commandAPI snapshots
        maven("https://repo.oraxen.com/releases")
        maven("https://repo.oraxen.com/snapshots")
        maven("https://repo.auxilor.io/repository/maven-public/") // EcoItems
        maven("https://maven.enginehub.org/repo/")
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
        compileOnly("me.clip:placeholderapi:2.11.6")
        compileOnly("me.gabytm.util:actions-core:$actionsVersion")
        compileOnly("org.springframework:spring-expression:6.0.6")
        compileOnly("io.lumine:Mythic-Dist:5.7.0-SNAPSHOT")
        compileOnly("io.lumine:MythicCrucible:2.0.0-SNAPSHOT")
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9")
        compileOnly("commons-io:commons-io:2.11.0")
        compileOnly("com.google.code.gson:gson:$googleGsonVersion")
        compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.6")
        compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT")
        compileOnly("net.Indyuce:MMOItems-API:6.9.5-SNAPSHOT")
        compileOnly("com.willfp:EcoItems:5.43.1")
        compileOnly("com.willfp:eco:6.70.1")
        compileOnly("com.willfp:libreforge:4.58.1")
        compileOnly("nl.rutgerkok:blocklocker:1.12.2")
        compileOnly("org.apache.commons:commons-lang3:$apacheLang3Version")
        compileOnly("org.apache.httpcomponents.client5:httpclient5:$apacheHttpClientVersion")
        compileOnly(files("../libs/AxiomPaper-1.5.12.jar"))
        compileOnly("team.unnamed:creative-server:$creativeVersion")

        implementation("team.unnamed:creative-api:$creativeVersion") { exclude("net.kyori") }
        implementation("team.unnamed:creative-serializer-minecraft:$creativeVersion") { exclude("net.kyori") }
        implementation("dev.jorel:commandapi-bukkit-shade:$commandApiVersion")
        implementation("org.bstats:bstats-bukkit:3.1.0")
        implementation("io.th0rgal:protectionlib:1.6.1")
        implementation("com.github.stefvanschie.inventoryframework:IF:0.10.17")
        implementation("com.jeff-media:custom-block-data:2.2.2")
        implementation("com.jeff-media:MorePersistentDataTypes:2.4.0")
        implementation("com.jeff-media:persistent-data-serializer:1.0")
        implementation("org.jetbrains:annotations:26.0.1") { isTransitive = false }
        implementation("dev.triumphteam:triumph-gui:3.1.10") { exclude("net.kyori") }

        implementation("me.gabytm.util:actions-spigot:$actionsVersion") { exclude(group = "com.google.guava") }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation(project(path = ":core"))
    SUPPORTED_VERSIONS.forEach {
        implementation(project(path = ":${it.nmsVersion}", configuration = "reobf"))
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {

    compileJava {
        dependsOn(clean)
        options.encoding = Charsets.UTF_8.name()
    }

    runServer {
        minecraftVersion("1.21")
    }

    jar {
        finalizedBy(shadowJar)
    }
    shadowJar {
        SUPPORTED_VERSIONS.forEach {
            dependsOn(":${it.nmsVersion}:reobfJar")
        }

        fun shade(pattern: String) = relocate(pattern, "io.th0rgal.oraxen.shaded." + pattern.substringAfter("."))

        shade("org.bstats")
        //shade("dev.triumphteam.gui")
        //shade("com.jeff_media")
        //shade("com.github.stefvanschie.inventoryframework")
        //shade("me.gabytm.util.actions")
        //shade("org.intellij.lang.annotations")
        //shade("org.jetbrains.annotations")
        //shade("com.udojava.evalex")
        //shade("dev.jorel")
        //shade("team.unnamed")

        manifest {
            attributes(
                mapOf(
                    "paperweight-mappings-namespace" to "spigot",
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
        exclude("LICENSE", "pack/**")
        archiveFileName.set("oraxen-${pluginVersion}.jar")
        archiveClassifier.set("")
    }

    build.get().dependsOn(publishToMavenLocal, shadowJar)
}

bukkitPluginYaml {
    main = "io.th0rgal.oraxen.OraxenPlugin"
    load = BukkitPluginYaml.PluginLoadOrder.POSTWORLD
    authors.add("boy0000")
    name = "Oraxen"
    apiVersion = "1.20"

    permissions.create("oraxen.command") {
        description = "Allows the player to use the /oraxen command"
        default = Permission.Default.TRUE
    }
    softDepend = listOf(
        "LightAPI", "PlaceholderAPI", "MythicMobs", "MMOItems", "MythicCrucible", "MythicMobs",
        "WorldEdit", "WorldGuard", "Towny", "Factions", "Lands", "PlotSquared",
        "ModelEngine", "HuskTowns", "HuskClaims", "BentoBox", "AxiomPaper"
    )
    libraries = listOf(
        "org.springframework:spring-expression:6.0.6",
        "net.kyori:adventure-text-minimessage:$adventureVersion",
        "net.kyori:adventure-text-serializer-plain:$adventureVersion",
        "net.kyori:adventure-text-serializer-ansi:$adventureVersion",
        "net.kyori:adventure-platform-bukkit:$platformVersion",
        "com.google.code.gson:gson:$googleGsonVersion",
        "org.apache.commons:commons-lang3:$apacheLang3Version",
        "org.apache.httpcomponents.client5:httpclient5:$apacheHttpClientVersion",
        "gs.mclo:java:2.2.1",
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
            doLast {
                println("Copied to plugin directory $pluginPath")
                Path(pluginPath).listDirectoryEntries()
                    .filter { it.fileName.toString().matches("oraxen-.*.jar".toRegex()) }
                    .filterNot { it.fileName.toString().endsWith("$pluginVersion.jar") }
                    .forEach { delete(it) }
            }
        }
        build.get().dependsOn(copyJar)
    }
}
