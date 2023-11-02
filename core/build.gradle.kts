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
    implementation("com.github.oraxen:protectionlib:1.3.5")
    implementation("com.github.stefvanschie.inventoryframework:IF:0.10.9")
    implementation("com.jeff-media:custom-block-data:2.2.2")
    implementation("com.jeff_media:MorePersistentDataTypes:2.4.0")
    implementation("com.jeff-media:persistent-data-serializer:1.0")
    implementation("gs.mclo:java:2.2.1")
    implementation("com.ticxo:PlayerAnimator:R1.2.7")
    implementation("org.jetbrains:annotations:24.0.1") { isTransitive = false }
    implementation("team.unnamed:creative-api:1.2.2-SNAPSHOT")
    implementation("team.unnamed:creative-serializer-minecraft:1.2.2-SNAPSHOT")
    implementation("team.unnamed:creative-server:1.2.2-SNAPSHOT")

    implementation("me.gabytm.util:actions-spigot:$actionsVersion") { exclude(group = "com.google.guava") }
    paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        relocate("team.unnamed", "io.th0rgal.oraxen.shaded.unnamed")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

publishing {
    val publishData = PublishData(project)
    publications {
        create<MavenPublication>("maven") {
            groupId = rootProject.group.toString()
            artifactId = rootProject.name
            version = publishData.getVersion()

            from(components["java"])
        }
    }

    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    username = System.getenv("MAVEN_USERNAME") ?: project.findProperty("oraxenUsername") as String
                    password = System.getenv("MAVEN_PASSWORD") ?: project.findProperty("oraxenPassword") as String
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }

            url = uri(publishData.getRepository())
            name = "oraxen"
            this.isAllowInsecureProtocol = true
        }
    }
}

class PublishData(private val project: Project) {
    var type: Type = getReleaseType()
    var hashLength: Int = 7

    private fun getReleaseType(): Type {
        val branch = getCheckedOutBranch()
        println("Branch: $branch")
        return when {
            branch.contentEquals("master") -> Type.RELEASE
            branch.contentEquals("develop") -> Type.SNAPSHOT
            else -> Type.DEV
        }
    }

    private fun getCheckedOutGitCommitHash(): String =
        System.getenv("GITHUB_SHA")?.substring(0, hashLength) ?: "local"

    private fun getCheckedOutBranch(): String =
        System.getenv("GITHUB_REF")?.replace("refs/heads/", "") ?: grgitService.service.get().grgit.branch.current().name

    fun getVersion(): String = getVersion(false)

    fun getVersion(appendCommit: Boolean): String =
        type.append(getVersionString(), appendCommit, getCheckedOutGitCommitHash())

    private fun getVersionString(): String =
        (rootProject.version as String).replace("-SNAPSHOT", "").replace("-DEV", "")

    fun getRepository(): String = type.repo

    enum class Type(private val append: String, val repo: String, private val addCommit: Boolean) {
        RELEASE("", "http://repo.oraxen.com:8080/releases/", false),
        DEV("-DEV", "http://repo.oraxen.com:8080/development/", true),
        SNAPSHOT("-SNAPSHOT", "http://repo.oraxen.com:8080/snapshots/", true);

        fun append(name: String, appendCommit: Boolean, commitHash: String): String =
            name.plus(append).plus(if (appendCommit && addCommit) "-".plus(commitHash) else "")
    }
}
