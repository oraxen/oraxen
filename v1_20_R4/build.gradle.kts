plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.7.2"
    id("io.github.goooler.shadow") version "8.1.8"
}

repositories {
    maven("https://papermc.io/repo/repository/maven-public/") // Paper
}

dependencies {
    compileOnly(project(":core"))
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}