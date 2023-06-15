plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.5.5"
}

dependencies {
    paperweight.paperDevBundle("1.19.2-R0.1-SNAPSHOT")
    implementation(project(":core"))
}
