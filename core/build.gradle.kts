
plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.5.5"
}

dependencies {
    val actionsVersion = "1.0.0-SNAPSHOT"

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
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}