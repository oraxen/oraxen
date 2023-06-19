plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.5.5"
}

dependencies {
    paperweight.paperDevBundle("1.18.1-R0.1-SNAPSHOT")
    implementation(project(":core"))
}

tasks {

    build {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}
