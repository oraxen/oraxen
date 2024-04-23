plugins {
    id("java")
    //id("io.papermc.paperweight.userdev") version "1.5.11"
    id("maven-publish")
    alias(idofrontLibs.plugins.shadowjar)
    id("org.ajoberstar.grgit.service") version "5.2.0"
    id(idofrontLibs.plugins.mia.testing.get().pluginId)
}

val pluginVersion = project.property("pluginVersion") as String
tasks {
    //publish.get().dependsOn(shadowJar)
    shadowJar.get().archiveFileName.set("oraxen-${pluginVersion}.jar")
    build.get().dependsOn(shadowJar)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    //paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")

    val pluginVersion: String by project
    val commandApiVersion = "9.3.0"
    val adventureVersion = "4.15.0"
    val platformVersion = "4.3.2"
    val googleGsonVersion = "2.10.1"
    val creativeVersion = "1.7.2"
    testCompileOnly("junit:junit:4.13.1")
    testImplementation(idofrontLibs.minecraft.mockbukkit)
    testImplementation("dev.jorel:commandapi-bukkit-shade:$commandApiVersion")
    testImplementation("com.comphenix.protocol:ProtocolLib:5.1.0")
    testImplementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    testImplementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
    testImplementation("net.kyori:adventure-text-serializer-ansi:$adventureVersion")
    testImplementation("net.kyori:adventure-platform-bukkit:$platformVersion")
    implementation("org.apache.commons:commons-lang3:3.14.0")
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
            //artifact(tasks.shadowJar.get().apply { archiveClassifier.set("") })
        }
    }

    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    username = System.getenv("MAVEN_USERNAME") ?: project.findProperty("oraxenUsername") as? String ?: ""
                    password = System.getenv("MAVEN_PASSWORD") ?: project.findProperty("oraxenPassword") as? String ?: ""
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
        val branch = getCheckedOutBranch()
        println("Branch: $branch")
        return when {
            branch.contentEquals("master") -> Type.RELEASE
            branch.contentEquals("develop") -> Type.SNAPSHOT
            branch.contentEquals("2.0/master") -> Type.SNAPSHOT
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
