
plugins {
    id("java")
    id("idea")
    id("eclipse")
    id("maven-publish")
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
