package io.th0rgal.oraxen;

import com.google.gson.Gson;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Paper plugin loader that resolves Oraxen's runtime libraries before the plugin is
 * classloaded. The library coordinates and repositories are generated at build time by
 * plugin-yml into {@code paper-libraries.json}, replacing the legacy Bukkit
 * {@code plugin.yml > libraries} mechanism.
 */
public class OraxenPluginLoader implements PluginLoader {

    /**
     * Direct use of Maven Central is disallowed by newer Paper versions; use the mirror
     * Paper recommends (overridable with the same system property Paper honors).
     */
    private static final String MAVEN_CENTRAL_MIRROR =
            System.getProperty("paper.preferredCentralRepository", "https://maven-central.storage-download.googleapis.com/maven2");

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        PluginLibraries libraries = load();
        libraries.dependencies().forEach(dependency ->
                resolver.addDependency(new Dependency(new DefaultArtifact(dependency), null)));
        remapCentral(libraries.repositories()).forEach((name, url) ->
                resolver.addRepository(new RemoteRepository.Builder(name, "default", url).build()));
        classpathBuilder.addLibrary(resolver);
    }

    private PluginLibraries load() {
        try (InputStream in = getClass().getResourceAsStream("/paper-libraries.json")) {
            if (in == null)
                throw new IllegalStateException("paper-libraries.json is missing from the Oraxen jar");
            PluginLibraries libraries = new Gson().fromJson(
                    new InputStreamReader(in, StandardCharsets.UTF_8), PluginLibraries.class);
            if (libraries == null)
                throw new IllegalStateException("paper-libraries.json has an empty or null root");
            return libraries;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, String> remapCentral(Map<String, String> repositories) {
        Map<String, String> remapped = new LinkedHashMap<>();
        repositories.forEach((name, url) -> {
            if (url.contains("repo.maven.apache.org") || url.contains("repo1.maven.org")) {
                remapped.put(name, MAVEN_CENTRAL_MIRROR);
            } else {
                remapped.put(name, url);
            }
        });
        return remapped;
    }

    private record PluginLibraries(Map<String, String> repositories, List<String> dependencies) {
        /**
         * Gson maps absent JSON keys to {@code null}; normalize them to empty collections so a
         * malformed or truncated {@code paper-libraries.json} cannot crash the plugin load phase.
         */
        private PluginLibraries {
            repositories = repositories != null ? repositories : Map.of();
            dependencies = dependencies != null ? dependencies : List.of();
        }
    }
}
