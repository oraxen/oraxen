// Each NMS module compiles the shared handler sources against its own Paper dev bundle.
// The plugin jar bundles every NMS module, so each module's copy gets its own package.
val sharedPackage = "io.th0rgal.oraxen.nms.handler.shared"
val handlerPackage = "io.th0rgal.oraxen.nms.handler.${project.name}"

val sharedHandlerSources = tasks.register<Sync>("sharedHandlerSources") {
    from(rootProject.file("nms/shared/src/main/java"))
    into(layout.buildDirectory.dir("generated/sources/sharedHandler/java/main"))
    includeEmptyDirs = false
    filteringCharset = Charsets.UTF_8.name()
    filter { line: String -> line.replace(sharedPackage, handlerPackage) }
    eachFile {
        path = path.replace(sharedPackage.replace('.', '/'), handlerPackage.replace('.', '/'))
    }
}

the<SourceSetContainer>().named("main") {
    java.srcDir(sharedHandlerSources)
}
