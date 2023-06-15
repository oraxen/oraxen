
plugins {
    id("java")
    id("idea")
    id("eclipse")
    id("maven-publish")
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
    compileOnly(files("../libs/compile/BSP.jar"))
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

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
