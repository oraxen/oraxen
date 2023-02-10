import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

plugins {
    id("java")
    id("idea")
    id("eclipse")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("xyz.jpenilla.run-paper") version "2.0.1"
}

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
    maven("https://repo.mineinabyss.com/releases") // ModelEngine
    maven("https://repo.dmulloy2.net/repository/public/") // ProtocolLib
    maven("https://libraries.minecraft.net/") // Minecraft repo (commodore)
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceHolderAPI
    maven("https://maven.elmakers.com/repository/") // EffectLib
    maven("https://repo.codemc.org/repository/maven-public") // CodeMc (bstats)
    maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/") // CustomBlockData
    maven("https://repo.triumphteam.dev/snapshots") // actions-code, actions-spigot
    maven("https://mvn.lumine.io/repository/maven-public/") // MythicMobs
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots") // commandAPI snapshots
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    val actionsVersion = "1.0.0-SNAPSHOT"

    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT") { exclude(group = "net.kyori") }
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0-SNAPSHOT")
    compileOnly("com.github.Hazebyte:CrateReloadedAPI:d7ae2a14c6")
    compileOnly("com.github.jojodmo:ItemBridge:-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("com.github.BeYkeRYkt:LightAPI:5.3.0-Bukkit")
    compileOnly("me.gabytm.util:actions-core:$actionsVersion")
    compileOnly("org.springframework:spring-expression:6.0.3")
    compileOnly("io.lumine:Mythic-Dist:5.2.0-SNAPSHOT")
    compileOnly("io.lumine:MythicCrucible:1.6.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.0")
    compileOnly("commons-io:commons-io:2.11.0")
    compileOnly("com.ticxo:modelengine:R3.0.1")
    compileOnly(fileTree(mapOf("dir" to "libs/compile", "include" to listOf("*.jar"))))

    implementation("dev.triumphteam:triumph-gui:3.1.2")
    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("com.oraxen:protectionlib:1.1.4")
    implementation("net.kyori:adventure-text-minimessage:4.13.0-SNAPSHOT")
    implementation("net.kyori:adventure-text-serializer-plain:4.13.0-SNAPSHOT")
    implementation("net.kyori:adventure-text-serializer-legacy:4.13.0-SNAPSHOT")
    implementation("net.kyori:adventure-text-serializer-gson:4.13.0-SNAPSHOT")
    implementation("net.kyori:adventure-platform-bukkit:4.2.0")
    implementation("com.github.stefvanschie.inventoryframework:IF:0.10.8")
    implementation("dev.jorel:commandapi-shade:8.7.3")
    implementation("com.jeff_media:CustomBlockData:2.2.0")
    implementation("com.jeff_media:MorePersistentDataTypes:2.3.1")
    implementation("gs.mclo:mclogs:2.1.1")

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
        relocate("dev.jorel.commandapi", "io.th0rgal.oraxen.shaded.commandapi")
        relocate("me.gabytm.util.actions", "io.th0rgal.oraxen.shaded.actions")
        relocate("org.intellij.lang.annotations", "io.th0rgal.oraxen.shaded.intellij.annotations")
        relocate("org.jetbrains.annotations", "io.th0rgal.oraxen.shaded.jetbrains.annotations")
        relocate("com.udojava.evalex", "io.th0rgal.oraxen.shaded.evalex")
        //mapOf("dir" to "libs/compile", "include" to listOf("*.jar"))
        manifest {
            attributes(
                mapOf(
                    "Built-By" to System.getProperty("user.name"),
                    "Version" to pluginVersion,
                    "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSSZ").format(Date.from(Instant.now())),
                    "Created-By" to "Gradle ${gradle.gradleVersion}",
                    "Build-Jdk" to "${System.getProperty("java.version")} ${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")}",
                    "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}"
                )
            )
        }
        archiveFileName.set("oraxen-${pluginVersion}.jar")
    }

    compileJava.get().dependsOn(clean)
    build.get().dependsOn(shadowJar)
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components.getByName("java"))
        }
    }
}

val pluginPath = project.findProperty("oraxen_plugin_path")
if (pluginPath != null) {
    tasks {
        register<Copy>("copyJar") {
            from(findByName("reobfJar") ?: findByName("shadowJar") ?: findByName("jar"))
            into(pluginPath)
            doLast {
                println("Copied to plugin directory $pluginPath")
            }
        }
        named<DefaultTask>("build") {
            dependsOn("copyJar")
        }
    }
}
