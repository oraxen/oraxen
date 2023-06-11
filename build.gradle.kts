import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

plugins {
    id("java")
    id("idea")
    id("eclipse")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.0.1"
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

repositories {
    mavenCentral()

    maven("https://papermc.io/repo/repository/maven-public/") // Paper
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven("https://oss.sonatype.org/content/repositories/snapshots") // Because Spigot depends on Bungeecord ChatComponent-API
    maven("https://jitpack.io") // JitPack
    maven("https://repo.dmulloy2.net/repository/public/") // ProtocolLib
    maven("https://libraries.minecraft.net/") // Minecraft repo (commodore)
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceHolderAPI
    maven("https://maven.elmakers.com/repository/") // EffectLib
    maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/") // CustomBlockData
    maven("https://repo.triumphteam.dev/snapshots") // actions-code, actions-spigot
    maven("https://mvn.lumine.io/repository/maven-public/") { metadataSources { artifact() } }// MythicMobs
    //maven("https://mvn.lumine.io/repository/maven/") // PlayerAnimator
    maven("https://repo.mineinabyss.com/releases") // PlayerAnimator
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots") // commandAPI snapshots
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    val actionsVersion = "1.0.0-SNAPSHOT"

    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT") { exclude("net.kyori") }
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
    compileOnly("me.clip:placeholderapi:2.11.3")
    compileOnly("com.github.BeYkeRYkt:LightAPI:5.3.0-Bukkit")
    compileOnly("me.gabytm.util:actions-core:$actionsVersion")
    compileOnly("org.springframework:spring-expression:6.0.6")
    compileOnly("io.lumine:Mythic-Dist:5.2.0-SNAPSHOT")
    compileOnly("io.lumine:MythicCrucible:1.6.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.0")
    compileOnly("commons-io:commons-io:2.11.0")
    compileOnly("com.ticxo.modelengine:api:R3.1.5")
    compileOnly(files("libs/compile/BSP.jar"))
    compileOnly("dev.jorel:commandapi-bukkit-shade:9.0.2")
    compileOnly("io.lumine:MythicLib:1.1.6")
    compileOnly("net.Indyuce:MMOItems:6.7.3")
    compileOnly("org.joml:joml:1.10.5") // Because pre 1.19.4 api does not have this in the server-jar

    implementation("dev.triumphteam:triumph-gui:3.1.5")
    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("com.github.oraxen:protectionlib:1.2.7")
    implementation("net.kyori:adventure-text-minimessage:4.13.1")
    implementation("net.kyori:adventure-text-serializer-plain:4.13.1")
    implementation("net.kyori:adventure-platform-bukkit:4.3.0")
    implementation("com.github.stefvanschie.inventoryframework:IF:0.10.9")
    implementation("com.jeff_media:CustomBlockData:2.2.0")
    implementation("com.jeff_media:MorePersistentDataTypes:2.4.0")
    implementation("gs.mclo:java:2.2.1")
    implementation("com.ticxo:PlayerAnimator:R1.2.7")
    implementation("org.jetbrains:annotations:24.0.1") { isTransitive = false }

    implementation("me.gabytm.util:actions-spigot:$actionsVersion") { exclude(group = "com.google.guava") }
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

    runServer {
        minecraftVersion("1.19.3")
    }

    shadowJar {
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
        //relocate("org.joml", "io.th0rgal.oraxen.shaded.joml")

        //mapOf("dir" to "libs/compile", "include" to listOf("*.jar"))
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
    libraries = listOf("org.springframework:spring-expression:6.0.6", "org.apache.httpcomponents:httpmime:4.5.13", "dev.jorel:commandapi-bukkit-shade:9.0.2", "org.joml:joml:1.10.5")
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
