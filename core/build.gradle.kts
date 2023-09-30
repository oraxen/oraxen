
plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.5.6"
}

dependencies {
    val actionsVersion = "1.0.0-SNAPSHOT"
    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("dev.triumphteam:triumph-gui:3.1.5")
    implementation("com.github.oraxen:protectionlib:1.3.5")
    implementation("com.github.stefvanschie.inventoryframework:IF:0.10.9")
    implementation("com.jeff-media:custom-block-data:2.2.2")
    implementation("com.jeff_media:MorePersistentDataTypes:2.4.0")
    implementation("com.jeff-media:persistent-data-serializer:1.0")
    implementation("gs.mclo:java:2.2.1")
    implementation("com.ticxo:PlayerAnimator:R1.2.7")
    implementation("org.jetbrains:annotations:24.0.1") { isTransitive = false }

    implementation("me.gabytm.util:actions-spigot:$actionsVersion") { exclude(group = "com.google.guava") }
    paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
