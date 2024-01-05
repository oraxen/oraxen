plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.5.6"
    id("maven-publish")
    alias(libs.plugins.shadowjar)
    id("org.ajoberstar.grgit.service") version "5.2.0"
}

val pluginVersion = project.property("pluginVersion") as String
tasks {
    shadowJar.get().archiveFileName.set("oraxen-${pluginVersion}.jar")
    build.get().dependsOn(shadowJar)
}

dependencies {
    val actionsVersion = "1.0.0-SNAPSHOT"
    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("dev.triumphteam:triumph-gui:3.1.5") { exclude("net.kyori") }
    implementation("io.th0rgal:protectionlib:1.3.6")
    implementation("com.github.stefvanschie.inventoryframework:IF:0.10.9")
    implementation("com.jeff-media:custom-block-data:2.2.2")
    implementation("com.jeff_media:MorePersistentDataTypes:2.4.0")
    implementation("com.jeff-media:persistent-data-serializer:1.0")
    implementation("gs.mclo:java:2.2.1")
    implementation("com.ticxo:PlayerAnimator:R1.2.8") { isChanging = true }
    implementation("org.jetbrains:annotations:24.0.1") { isTransitive = false }

    implementation("me.gabytm.util:actions-spigot:$actionsVersion") { exclude(group = "com.google.guava") }
    paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT")
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}