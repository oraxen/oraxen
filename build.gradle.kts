import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

plugins {
    id("java")
    //id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0" // Generates plugin.yml
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21" apply false
    id("com.gradleup.shadow") version "9.4.1"
    id("maven-publish")
    id("org.ajoberstar.grgit.service") version "5.2.0"
}


val compiled = (project.findProperty("oraxen_compiled")?.toString() ?: "true").toBoolean()
val pluginPath = project.findProperty("oraxen_plugin_path")?.toString()
val devPluginPath = project.findProperty("oraxen_dev_plugin_path")?.toString()
val foliaPluginPath = project.findProperty("oraxen_folia_plugin_path")?.toString()
val pluginVersion: String by project
val runServerVersion = findProperty("mcVersion") as String? ?: "26.1.2"
val runServerMajorVersion = Regex("""^\D*(?:1\.)?(\d+)""")
    .find(runServerVersion)
    ?.groupValues
    ?.get(1)
    ?.toIntOrNull()
val configuredRunJavaVersion = project.findProperty("runJavaVersion")?.toString()
val runServerJavaVersion = if (configuredRunJavaVersion != null) {
    configuredRunJavaVersion.toIntOrNull()
        ?: throw GradleException("Invalid runJavaVersion '$configuredRunJavaVersion'. Expected an integer Java language level (e.g., 21, 25).")
} else {
    if ((runServerMajorVersion ?: 0) >= 26) 25 else 21
}
group = "io.th0rgal"
version = pluginVersion

allprojects {
    apply(plugin = "java")

    repositories {
        maven("https://repo.papermc.io/repository/maven-public/") {
            content {
                includeGroup("io.papermc.paper") // Paper
                // extra stuff required by paper
                includeGroup("net.md-5")
                includeGroup("com.mojang")
            }
        }
        maven("https://libraries.minecraft.net/") {
            content { includeGroup("net.minecraft") } // Minecraft repo (commodore)
        }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
            content { includeGroup("me.clip") } // PlaceHolderAPI
        }
//        maven("https://maven.elmakers.com/repository/") // EffectLib
        maven("https://repo.triumphteam.dev/snapshots") {
            content { includeGroup("me.gabytm.util") } // actions-code, actions-spigot
        }
        maven("https://mvn.lumine.io/repository/maven-public/") {
            metadataSources { artifact() }
            content {
                includeModule("io.lumine", "MythicLib")
                includeModule("io.lumine", "Mythic-Dist")
                includeModule("io.lumine", "MythicCrucible-API")
                includeGroup("com.ticxo.modelengine") // ModelEngine
            }
        }
        maven("https://repo.oraxen.com/releases") {
            content {
                includeGroup("md.thomas.hopper") // hopper
            }
        }
        maven("https://repo.momirealms.net/releases/") {
            content { includeGroup("net.momirealms") } // AntiGriefLib
        }
        maven("https://repo.oraxen.com/snapshots") {
            content {
                includeGroup("io.th0rgal")
                includeGroup("md.thomas.hopper")
            }
        }
        maven("https://repo.auxilor.io/repository/maven-public/") {
            content { includeGroup("com.willfp") } // EcoItems, eco, libreforge
        }
        maven("https://maven.enginehub.org/repo/") {
            content {
                includeGroupAndSubgroups("com.sk89q.worldedit") // WorldEdit
                includeGroupAndSubgroups("com.sk89q.worldguard") // WorldGuard
                includeGroupAndSubgroups("org.enginehub") // WorldEdit transitive dependencies (lin-bus-bom, etc)
            }
        }
        maven("https://nexus.phoenixdevt.fr/repository/maven-public/") {
            content {
                includeModule("io.lumine", "MythicLib-dist")
                includeGroup("net.Indyuce") // MMOItems
            }
        }
        maven("https://repo.codemc.org/repository/maven-public/") {
            content { includeGroup("nl.rutgerkok") } // BlockLocker
        }
        maven("https://repo.codemc.io/repository/maven-releases/") {
            content { includeGroup("com.github.retrooper") }
        }
        maven("https://repo.skriptlang.org/releases") {
            content { includeGroup("com.github.SkriptLang") } // Skript
        }
        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }
        mavenCentral()
    }
}


dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // libraries in plugin.yml > libraries
    compileOnly(oraxenLibs.bundles.libraries.bukkit) {
        exclude("org.jetbrains", "annotations")
    }
    // things that are included in minecraft but not exposed
    compileOnly(oraxenLibs.bundles.libraries.included)
    // Plugin dependencies
    compileOnly(oraxenLibs.bundles.plugins) {
        exclude("org.jetbrains", "annotations")
        exclude("com.google.code.gson", "gson")
        exclude("net.kyori")
    }
    compileOnly(files("libs/compile/BSP.jar"))
    // shaded dependencies
    implementation(oraxenLibs.bundles.libraries.shade) {
        exclude("com.google.code.gson", "gson")
        exclude("net.kyori")
        exclude(group = "com.google.guava")
    }

    // Test dependencies
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation(oraxenLibs.spring.expression)
    testImplementation("net.kyori:adventure-api:4.18.0")
    testImplementation("net.kyori:adventure-text-minimessage:4.18.0")
    testImplementation("net.kyori:adventure-text-serializer-legacy:4.18.0")
    testImplementation("com.google.guava:guava:33.0.0-jre")
    testImplementation("com.google.code.gson:gson:2.10.1")
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(runServerJavaVersion)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    compileTestJava {
        if (!project.hasProperty("runVersionLoadingTest")) {
            exclude("io/th0rgal/oraxen/loading/versionLoadingTest.java")
        }
    }

    test {
        useJUnitPlatform {
            if (!project.hasProperty("runVersionLoadingTest")) {
                excludeTags("version-loading")
            }
        }

        if (project.hasProperty("runVersionLoadingTest")) {
            // Build the plugin before JUnit starts. A nested Gradle build from the test would
            // execute compileJava -> clean and delete the outer test's result files.
            dependsOn(shadowJar)
            systemProperty("junit.jupiter.execution.parallel.enabled", "true")
            systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
            systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "5")
            systemProperty("junit.jupiter.execution.parallel.config.fixed.max-pool-size", "5")
            testLogging {
                events("passed", "failed", "skipped", "standardOut", "standardError")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
                showStandardStreams = true
            }
        }
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filesNotMatching(
            listOf(
                "**/*.png",
                "**/*.ogg",
                "**/models/**",
                "**/textures/**",
                "**/font/**.json",
                "**/plugin.yml"
            )
        ) {
            expand(mapOf(project.version.toString() to pluginVersion))
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        downloadPlugins {
            hangar("ProtocolLib", "5.4.0")
        }
        minecraftVersion(runServerVersion)
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }

    shadowJar {
        val nmsJava21Jar = project(":nms:java21").tasks.named<Jar>("jar")
        val nmsJava25Jar = project(":nms:java25").tasks.named<Jar>("jar")
        dependsOn(nmsJava21Jar, nmsJava25Jar)
        from(nmsJava21Jar.flatMap { it.archiveFile }.map { zipTree(it) }) {
            exclude("META-INF/**")
        }
        from(nmsJava25Jar.flatMap { it.archiveFile }.map { zipTree(it) }) {
            exclude("META-INF/**")
        }

        archiveClassifier = null
        oraxenLibs.bundles.libraries.shade.get().forEach {
            val plugin = it;
            val group = it.group!!
                .replace("jeff-media", "jeff_media") // they use a different package than the group...
            val parts = group
                .split(".")
            var relocated = parts[parts.size - 1]
            if (parts.size > 2) {
                relocated = parts[parts.size - 2] + "." + relocated
            }
            logger.lifecycle("Relocating ${group} to io.th0rgal.oraxen.shaded." + relocated)
            relocate(group, "io.th0rgal.oraxen.shaded." + relocated) {
                exclude("io.th0rgal.oraxen.**")
            }
        }
        // exception for this one dunno who includes that...
        relocate("org.intellij.lang.annotations", "io.th0rgal.oraxen.shaded.intellij.annotations")
        relocate("com.udojava.evalex", "io.th0rgal.oraxen.shaded.udojava.evalex")
        relocate("javax.json", "io.th0rgal.oraxen.shaded.javax.json")

        manifest {
            attributes(
                mapOf(
                    "Built-By" to System.getProperty("user.name"),
                    "Version" to pluginVersion,
                    "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSSZ").format(Date.from(Instant.now())),
                    "Created-By" to "Gradle ${gradle.gradleVersion}",
                    "Build-Jdk" to "${System.getProperty("java.version")} ${System.getProperty("java.vendor")} ${
                        System.getProperty(
                            "java.vm.version"
                        )
                    }",
                    "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${
                        System.getProperty(
                            "os.version"
                        )
                    }",
                    "Compiled" to (project.findProperty("oraxen_compiled")?.toString() ?: "true").toBoolean(),
                    // Tell Paper not to remap this plugin - modern NMS modules use Mojang mappings
                    "paperweight-mappings-namespace" to "mojang"
                )
            )
        }
        archiveFileName.set("oraxen-${pluginVersion}.jar")
        archiveClassifier.set("")
    }

    compileJava.get().dependsOn(clean)
    build.get().dependsOn(shadowJar)
}

// Task to run PackMerger debug tool
tasks.register<JavaExec>("runPackMergerDebug") {
    group = "debug"
    description = "Runs the PackMerger debug tool to analyze resource pack zip files"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("io.th0rgal.oraxen.pack.generation.PackMergerDebugRunner")

    // Pass command line args: ./gradlew runPackMergerDebug --args="path/to/pack.zip"
    if (project.hasProperty("packFile")) {
        args(project.property("packFile").toString())
    }
}


bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "io.th0rgal.oraxen.OraxenPlugin"
    version = pluginVersion
    name = "Oraxen"
    apiVersion = "1.18"
    foliaSupported = true
    authors = listOf("th0rgal", "https://github.com/oraxen/oraxen/blob/master/CONTRIBUTORS.md")
    softDepend = listOf(
        "ProtocolLib",
        "packetevents",
        "LightAPI", "PlaceholderAPI", "MythicMobs", "MMOItems", "MythicCrucible", "MythicMobs", "BossShopPro",
        "CrateReloaded", "ItemBridge", "WorldEdit", "WorldGuard", "Towny", "Factions", "Lands", "PlotSquared",
        "NBTAPI", "ModelEngine", "ViaBackwards", "HuskClaims", "HuskTowns", "BentoBox", "Skript", "Iris",
        "ExecutableItems"
    )
    loadBefore = listOf("Realistic_World")
    permissions.create("oraxen.command") {
        description = "Allows the player to use the /oraxen command"
        default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.TRUE
    }
    permissions.create("oraxen.introduction") {
        description = "Allows the player to receive Oraxen's first-run introduction guide"
        default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
    }
    libraries = oraxenLibs.bundles.libraries.bukkit.get().map { it.toString() }
}


// Headless pack generation task
// Usage: ./gradlew generatePack -PmcVersion=1.21.4 [-PserverType=paper] [-PoutputDir=./build/pack] [-PconfigDir=./my-configs]
tasks.register<Exec>("generatePack") {
    group = "pack"
    description = "Generate resource pack using headless server mode. Use -PmcVersion=X.XX.X to specify version."
    dependsOn("shadowJar")

    val mcVersion = project.findProperty("mcVersion")?.toString() ?: ""
    val serverType = project.findProperty("serverType")?.toString() ?: "paper"
    val outputDir = project.findProperty("outputDir")?.toString() ?: "${project.layout.buildDirectory.get()}/pack"
    val configDir = project.findProperty("configDir")?.toString() ?: ""
    val timeout = project.findProperty("packTimeout")?.toString() ?: "300"
    val verbose = project.findProperty("verbose")?.toString()?.toBoolean() ?: false
    val keepServer = project.findProperty("keepServer")?.toString()?.toBoolean() ?: false

    val scriptPath = "${project.projectDir}/scripts/headless-pack-gen.sh"
    val jarPath = "${project.layout.buildDirectory.get()}/libs/oraxen-${pluginVersion}.jar"

    executable = "bash"
    val args = mutableListOf(
        scriptPath,
        "--version", mcVersion,
        "--server", serverType,
        "--output", outputDir,
        "--oraxen-jar", jarPath,
        "--timeout", timeout
    )

    if (configDir.isNotEmpty()) {
        args.addAll(listOf("--config-dir", configDir))
    }
    if (verbose) {
        args.add("--verbose")
    }
    if (keepServer) {
        args.add("--keep-server")
    }

    setArgs(args)

    doFirst {
        if (mcVersion.isEmpty()) {
            throw GradleException("Minecraft version is required. Use -PmcVersion=X.XX.X (e.g., -PmcVersion=1.21.4)")
        }
        println("Generating resource pack for $serverType $mcVersion...")
    }
}

publishing {
    val publishData = PublishData(project)
    publications {
        create<MavenPublication>("maven") {
            groupId = rootProject.group.toString()
            artifactId = rootProject.name
            version = publishData.getVersion()

            from(components["java"])
            //artifact(tasks.shadowJar.get().apply { archiveClassifier.set("") })
        }
    }

    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    username =
                        System.getenv("MAVEN_USERNAME") ?: project.findProperty("oraxenUsername") as? String ?: ""
                    password =
                        System.getenv("MAVEN_PASSWORD") ?: project.findProperty("oraxenPassword") as? String ?: ""
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }

            url = uri(publishData.getRepository())
            name = "oraxen"
        }
    }
}

class PublishData(private val project: Project) {
    private var type: Type = getReleaseType()
    private var hashLength: Int = 7

    private fun getReleaseType(): Type {
        val ref = System.getenv("GITHUB_REF") ?: ""
        val branch = getCheckedOutBranch()
        println("Branch: $branch")
        return when {
            ref.startsWith("refs/tags/") -> Type.RELEASE
            branch.contentEquals("master") -> Type.RELEASE
            branch.contentEquals("develop") -> Type.SNAPSHOT
            else -> Type.DEV
        }
    }

    private fun getCheckedOutGitCommitHash(): String =
        System.getenv("GITHUB_SHA")?.substring(0, hashLength) ?: "local"

    private fun getCheckedOutBranch(): String =
        System.getenv("GITHUB_REF")?.replace("refs/heads/", "")
            ?: grgitService.service.get().grgit.branch.current().name

    fun getVersion(): String = getVersion(false)

    fun getVersion(appendCommit: Boolean): String =
        type.append(getVersionString(), appendCommit, getCheckedOutGitCommitHash())

    fun getVersionString(): String =
        (rootProject.version as String).removeSuffix("-SNAPSHOT").removeSuffix("-DEV")

    fun getRepository(): String = type.repo

    enum class Type(private val append: String, val repo: String, private val addCommit: Boolean) {
        RELEASE("", "https://repo.oraxen.com/releases/", false),
        DEV("-DEV", "https://repo.oraxen.com/development/", true),
        SNAPSHOT("-SNAPSHOT", "https://repo.oraxen.com/snapshots/", true);

        fun append(name: String, appendCommit: Boolean, commitHash: String): String =
            name.plus(append).plus(if (appendCommit && addCommit) "-".plus(commitHash) else "")
    }
}
