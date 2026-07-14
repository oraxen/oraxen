package io.th0rgal.oraxen.loading;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/*
* Downloads a server jar, cleans up the server, builds and deploys Oraxen to the server, sets the Java Version and verifies Oraxen loads fine on the server.
* Supports Paper 1.20.1-26.2 and Folia 1.21.11-26.1.2.
*
* Run all versions via './gradlew test --tests io.th0rgal.oraxen.loading.VersionLoadingTest -PrunVersionLoadingTest=true'.
* Run a specific version via './gradlew test --tests "io.th0rgal.oraxen.loading.VersionLoadingTest.<Version>" -PrunVersionLoadingTest=true'.
* <Version> can be e.g. 'Paper_1_21_11'.
* */

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VersionLoadingTest {

    private static final Path serverDir = Path.of(System.getProperty("user.home"), "Oraxen", "Servers");
    private static final Path pluginsDir = serverDir.resolve("plugins");
    private static final Path paperDir = serverDir.resolve("Paper");
    private static final Path foliaDir = serverDir.resolve("Folia");
    private static final Path logsDir = serverDir.resolve("logs");
    
    private static final Duration serverTimeout = Duration.ofMinutes(3);
    private static final Duration serverStopDelay = Duration.ofSeconds(4);
    private static volatile Throwable previousVersionFailure;
    private static Path builtPluginJar;

    private static final List<String> paperVersions = List.of(
            "1.20.1", "1.20.4", "1.20.6",
            "1.21.3", "1.21.4", "1.21.5", "1.21.8", "1.21.10", "1.21.11",
            "26.1.2", "26.2"
    );
    private static final List<String> foliaVersions = List.of("1.21.11", "26.1.2");

    private static final Map<String, Integer> javaVersions = Map.of(
            "1.20.1-1.21.11", 21,
            "26.1.2", 25,
            "26.2", 25
    );

    private static final Map<String, String> paperURLs = Map.ofEntries(
            Map.entry("26.2", "https://fill-data.papermc.io/v1/objects/49c86f1276e5dded67b6121bf7c2e91401f6eb99769cbb093aa19be07fa612f2/paper-26.2-38.jar"),
            Map.entry("26.1.2", "https://fill-data.papermc.io/v1/objects/d30fae0c74092b10855f0412ca6b265c60301a013d34bc28a2a41bf5682dd80b/paper-26.1.2-69.jar"),
            Map.entry("1.21.11", "https://fill-data.papermc.io/v1/objects/5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba/paper-1.21.11-132.jar"),
            Map.entry("1.21.10", "https://fill-data.papermc.io/v1/objects/158703f75a26f842ea656b3dc6d75bf3d1ec176b97a2c36384d0b80b3871af53/paper-1.21.10-130.jar"),
            Map.entry("1.21.8", "https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar"),
            Map.entry("1.21.5", "https://fill-data.papermc.io/v1/objects/2ae6ae22adf417699746e0f89fc2ef6cb6ee050a5f6608cee58f0535d60b509e/paper-1.21.5-114.jar"),
            Map.entry("1.21.4", "https://fill-data.papermc.io/v1/objects/5ee4f542f628a14c644410b08c94ea42e772ef4d29fe92973636b6813d4eaffc/paper-1.21.4-232.jar"),
            Map.entry("1.21.3", "https://fill-data.papermc.io/v1/objects/5ee4f542f628a14c644410b08c94ea42e772ef4d29fe92973636b6813d4eaffc/paper-1.21.4-232.jar"),
            Map.entry("1.20.6", "https://fill-data.papermc.io/v1/objects/4b011f5adb5f6c72007686a223174fce82f31aeb4b34faf4652abc840b47e640/paper-1.20.6-151.jar"),
            Map.entry("1.20.4", "https://fill-data.papermc.io/v1/objects/cabed3ae77cf55deba7c7d8722bc9cfd5e991201c211665f9265616d9fe5c77b/paper-1.20.4-499.jar"),
            Map.entry("1.20.1", "https://fill-data.papermc.io/v1/objects/234a9b32098100c6fc116664d64e36ccdb58b5b649af0f80bcccb08b0255eaea/paper-1.20.1-196.jar")
    );

    private static final Map<String, String> foliaURLs = Map.ofEntries(
            Map.entry("26.1.2", "https://fill-data.papermc.io/v1/objects/607afd1c3320008e1ffd2eaee6780ace4419d5f8c527b75e79f259be79ebf57b/folia-26.1.2-8.jar"),
            Map.entry("1.21.11", "https://fill-data.papermc.io/v1/objects/f52c408490a0225611e67907a3ca19f7e6da2c6bc899e715d5f46844e7103c39/folia-1.21.11-14.jar")
    );

    @Test @Tag("version-loading") @Order(1) void Paper_1_20_1() throws Exception { testPaper("1.20.1"); }
    @Test @Tag("version-loading") @Order(2) void Paper_1_20_4() throws Exception { testPaper("1.20.4"); }
    @Test @Tag("version-loading") @Order(3) void Paper_1_20_6() throws Exception { testPaper("1.20.6"); }
    @Test @Tag("version-loading") @Order(4) void Paper_1_21_3() throws Exception { testPaper("1.21.3"); }
    @Test @Tag("version-loading") @Order(5) void Paper_1_21_4() throws Exception { testPaper("1.21.4"); }
    @Test @Tag("version-loading") @Order(6) void Paper_1_21_5() throws Exception { testPaper("1.21.5"); }
    @Test @Tag("version-loading") @Order(7) void Paper_1_21_8() throws Exception { testPaper("1.21.8"); }
    @Test @Tag("version-loading") @Order(8) void Paper_1_21_10() throws Exception { testPaper("1.21.10"); }
    @Test @Tag("version-loading") @Order(9) void Paper_1_21_11() throws Exception { testPaper("1.21.11"); }
    @Test @Tag("version-loading") @Order(10) void Paper_26_1_2() throws Exception { testPaper("26.1.2"); }
    @Test @Tag("version-loading") @Order(11) void Paper_26_2() throws Exception { testPaper("26.2"); }
    @Test @Tag("version-loading") @Order(12) void Folia_1_21_11() throws Exception { testFolia("1.21.11"); }
    @Test @Tag("version-loading") @Order(13) void Folia_26_1_2() throws Exception { testFolia("26.1.2"); }

    private static void testPaper(String version) throws Exception {
        assumeTrue(previousVersionFailure == null, "Skipping because a previous version failed: " + previousVersionFailure);
        try {
            assertTrue(paperVersions.contains(version), "Unexpected Paper version " + version);
            Path projectDir = findProjectDir();
            buildPluginIfNeeded(projectDir);
            prepareServerDir();
            copyBuiltJar(builtPluginJar);
            runServer("paper", "Paper", version, paperDir, paperURLs);
        } catch (Exception | Error failure) {
            previousVersionFailure = failure;
            throw failure;
        }
    }

    private static void testFolia(String version) throws Exception {
        assumeTrue(previousVersionFailure == null, "Skipping because a previous version failed: " + previousVersionFailure);
        try {
            assertTrue(foliaVersions.contains(version), "Unexpected Folia version " + version);
            Path projectDir = findProjectDir();
            buildPluginIfNeeded(projectDir);
            prepareServerDir();
            copyBuiltJar(builtPluginJar);
            runServer("folia", "Folia", version, foliaDir, foliaURLs);
        } catch (Exception | Error failure) {
            previousVersionFailure = failure;
            throw failure;
        }
    }

    private static void buildPluginIfNeeded(Path projectDir) throws Exception {
        if (builtPluginJar != null && Files.exists(builtPluginJar)) return;

        ProcessBuilder builder = gradleWrapper(projectDir, "shadowJar", "-x", "test");
        builder.directory(projectDir.toFile()).redirectErrorStream(true);
        Process process = builder.start();
        String output = readAll(process);
        assertTrue(process.waitFor(10, TimeUnit.MINUTES), "Timed out while building Oraxen");
        assertTrue(process.exitValue() == 0, "Gradle build failed:\n" + output);
        builtPluginJar = findBuiltJar(projectDir);
    }

    private static void prepareServerDir() throws IOException {
        Files.createDirectories(serverDir);
        try (Stream<Path> paths = Files.walk(serverDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(serverDir))
                    .filter(path -> !path.equals(paperDir) && !path.startsWith(paperDir))
                    .filter(path -> !path.equals(foliaDir) && !path.startsWith(foliaDir))
                    .filter(path -> !path.equals(logsDir) && !path.startsWith(logsDir))
                    .forEach(VersionLoadingTest::delete);
        }
        Files.createDirectories(pluginsDir);
        Files.createDirectories(paperDir);
        Files.createDirectories(foliaDir);
        Files.writeString(serverDir.resolve("eula.txt"), "eula=true\n", StandardCharsets.UTF_8);
    }

    private static void runServer(String project, String name, String version, Path jarDir, Map<String, String> urls) throws Exception {
        ensureServerJar(project, version, jarDir, urls);
        Files.deleteIfExists(logsDir.resolve("latest.log"));
        Path javaHome = javaHome(version);
        ProcessBuilder builder = new ProcessBuilder(javaExecutable(javaHome), "-Xmx1G", "-jar", name + "/" + version + ".jar", "--nogui", "--port", "25590");
        builder.directory(serverDir.toFile()).redirectErrorStream(true);
        builder.environment().put("JAVA_HOME", javaHome.toString());
        builder.environment().put("PATH", javaHome.resolve("bin") + System.getProperty("path.separator") + builder.environment().getOrDefault("PATH", ""));

        Process process = builder.start();
        StringBuilder output = new StringBuilder();
        try {
            detectPassOrFail(name, version, process, output);
        } finally {
            Thread.sleep(serverStopDelay.toMillis());
            stopServer(process);
        }
    }

    private static void detectPassOrFail(String name, String version, Process process, StringBuilder output) throws Exception {
        boolean enabled = false;
        Instant deadline = Instant.now().plus(serverTimeout);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            while (Instant.now().isBefore(deadline)) {
                if (!reader.ready()) {
                    if (!process.isAlive()) failWithServerOutput(name, version, name + " exited with code " + process.exitValue() + " before Oraxen loaded", output);
                    Thread.sleep(100);
                    continue;
                }
                String line = reader.readLine();
                if (line == null) break;
                output.append(line).append(System.lineSeparator());
                String lower = line.toLowerCase();
                if (lower.contains("enabling oraxen") || lower.contains("oraxen") && lower.contains("enabled")) enabled = true;
                if (lower.contains("disabling oraxen")) failWithServerOutput(name, version, "Oraxen was disabled during startup", output);
                if (lower.contains("error occurred while enabling oraxen")) failWithServerOutput(name, version, name + " reported an error while enabling Oraxen", output);
                if (enabled && line.contains("Done (")) return;
            }
        }
        failWithServerOutput(name, version, "Timed out after " + serverTimeout + " waiting for Oraxen to load", output);
    }

    private static Path ensureServerJar(String project, String version, Path jarDir, Map<String, String> urls) throws IOException {
        Path jar = jarDir.resolve(version + ".jar");
        if (Files.exists(jar) && !urls.containsKey(version)) return jar;
        String url = Optional.ofNullable(urls.get(version)).orElseGet(() -> latestUrl(project, version));
        Files.createDirectories(jarDir);
        Path temp = jarDir.resolve(version + ".jar.download");
        try (var input = URI.create(url).toURL().openStream()) {
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temp, jar, StandardCopyOption.REPLACE_EXISTING);
        return jar;
    }

    private static String latestUrl(String project, String version) {
        try (var input = URI.create("https://api.papermc.io/v2/projects/" + project + "/versions/" + version + "/builds").toURL().openStream()) {
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\\"build\\\"\\s*:\\s*(\\d+)").matcher(json);
            int build = -1;
            while (matcher.find()) build = Integer.parseInt(matcher.group(1));
            if (build >= 0) return "https://api.papermc.io/v2/projects/" + project + "/versions/" + version + "/builds/" + build + "/downloads/" + project + "-" + version + "-" + build + ".jar";
        } catch (IOException ignored) {
        }
        throw new IllegalArgumentException("No " + project + " URL for " + version);
    }

    private static Path findProjectDir() {
        for (Path current = Path.of("").toAbsolutePath(); current != null; current = current.getParent()) {
            if (Files.exists(current.resolve("gradlew")) || Files.exists(current.resolve("gradlew.bat"))) return current;
        }
        throw new IllegalStateException("Could not find project directory containing gradlew");
    }

    private static Path findBuiltJar(Path projectDir) throws IOException {
        List<Path> searchRoots = List.of(projectDir.resolve("build/libs"), projectDir.resolve("core/build/libs"));
        for (Path searchRoot : searchRoots) {
            if (!Files.isDirectory(searchRoot)) continue;
            try (Stream<Path> jars = Files.list(searchRoot)) {
                Optional<Path> builtJar = jars.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().matches("oraxen-.*\\.jar"))
                        .filter(VersionLoadingTest::hasPluginDescriptor)
                        .max(Comparator.comparingLong(path -> path.toFile().lastModified()));
                if (builtJar.isPresent()) return builtJar.get();
            }
        }
        throw new IllegalStateException("Could not find built Oraxen jar in " + searchRoots);
    }

    private static ProcessBuilder gradleWrapper(Path projectDir, String... arguments) {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path wrapper = projectDir.resolve(windows ? "gradlew.bat" : "gradlew");
        List<String> command = new ArrayList<>();
        if (windows) {
            command.add("cmd");
            command.add("/c");
            command.add(wrapper.toString());
        } else if (Files.isExecutable(wrapper)) {
            command.add(wrapper.toString());
        } else {
            command.add("sh");
            command.add(wrapper.toString());
        }
        command.addAll(List.of(arguments));
        return new ProcessBuilder(command);
    }

    private static boolean hasPluginDescriptor(Path jar) {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            return jarFile.getEntry("plugin.yml") != null || jarFile.getEntry("paper-plugin.yml") != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void copyBuiltJar(Path builtJar) throws IOException {
        Files.copy(builtJar, pluginsDir.resolve(builtJar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String javaExecutable(Path javaHome) {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        return javaHome.resolve("bin").resolve(windows ? "java.exe" : "java").toString();
    }

    private static Path javaHome(String version) {
        int javaVersion = requiredJavaVersion(version);
        return findJavaHome(javaVersion).orElseThrow(() -> new IllegalArgumentException(
                "No Java " + javaVersion + " home found for " + version + ". Set JAVA_" + javaVersion + "_HOME or JDK_" + javaVersion + "_HOME."
        ));
    }

    private static int requiredJavaVersion(String version) {
        Integer exact = javaVersions.get(version);
        if (exact != null) return exact;
        return javaVersions.entrySet().stream()
                .filter(entry -> entry.getKey().contains("-") && inRange(version, entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No Java version configured for " + version));
    }

    private static Optional<Path> findJavaHome(int javaVersion) {
        List<String> envVars = List.of("JAVA_" + javaVersion + "_HOME", "JDK_" + javaVersion + "_HOME", "JAVA" + javaVersion + "_HOME", "JDK" + javaVersion + "_HOME");
        for (String envVar : envVars) {
            Optional<Path> candidate = validJavaHome(System.getenv(envVar), javaVersion);
            if (candidate.isPresent()) return candidate;
        }

        Optional<Path> javaHome = validJavaHome(System.getenv("JAVA_HOME"), javaVersion);
        if (javaHome.isPresent()) return javaHome;

        if (isMac()) {
            Optional<Path> macJavaHome = macJavaHome(javaVersion);
            if (macJavaHome.isPresent()) return macJavaHome;
        }

        for (Path candidate : commonJavaHomes(javaVersion)) {
            Optional<Path> valid = validJavaHome(candidate.toString(), javaVersion);
            if (valid.isPresent()) return valid;
        }

        for (Path candidate : installedJavaHomes()) {
            Optional<Path> valid = validJavaHome(candidate.toString(), javaVersion);
            if (valid.isPresent()) return valid;
        }

        return Runtime.version().feature() == javaVersion ? Optional.of(Path.of(System.getProperty("java.home"))) : Optional.empty();
    }

    private static Optional<Path> macJavaHome(int javaVersion) {
        try {
            Process process = new ProcessBuilder("/usr/libexec/java_home", "-v", Integer.toString(javaVersion)).redirectErrorStream(true).start();
            String output = readAll(process).trim();
            if (process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0) return validJavaHome(output, javaVersion);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    private static List<Path> commonJavaHomes(int javaVersion) {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        if (windows) return List.of(
                Path.of("C:\\Program Files\\Java\\jdk-" + javaVersion),
                Path.of("C:\\Program Files\\Eclipse Adoptium\\jdk-" + javaVersion),
                Path.of("C:\\Program Files\\Microsoft\\jdk-" + javaVersion)
        );
        return List.of(
                Path.of("/usr/lib/jvm/java-" + javaVersion + "-openjdk"),
                Path.of("/usr/lib/jvm/java-" + javaVersion + "-openjdk-amd64"),
                Path.of("/usr/lib/jvm/jdk-" + javaVersion),
                Path.of("/Library/Java/JavaVirtualMachines/temurin-" + javaVersion + ".jdk/Contents/Home"),
                Path.of("/Library/Java/JavaVirtualMachines/microsoft-" + javaVersion + ".jdk/Contents/Home")
        );
    }

    private static List<Path> installedJavaHomes() {
        List<Path> homes = new ArrayList<>();
        List<Path> roots = List.of(
                Path.of("/usr/lib/jvm"),
                Path.of("/Library/Java/JavaVirtualMachines"),
                Path.of("C:\\Program Files\\Java"),
                Path.of("C:\\Program Files\\Eclipse Adoptium"),
                Path.of("C:\\Program Files\\Microsoft")
        );
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> paths = Files.list(root)) {
                paths.forEach(path -> homes.add(path.toString().endsWith(".jdk") ? path.resolve("Contents/Home") : path));
            } catch (IOException ignored) {
            }
        }
        return homes;
    }

    private static Optional<Path> validJavaHome(String javaHome, int javaVersion) {
        if (javaHome == null || javaHome.isBlank()) return Optional.empty();
        Path home = Path.of(javaHome);
        if (!Files.isRegularFile(Path.of(javaExecutable(home)))) return Optional.empty();
        return javaFeatureVersion(home) == javaVersion ? Optional.of(home) : Optional.empty();
    }

    private static int javaFeatureVersion(Path javaHome) {
        try {
            Process process = new ProcessBuilder(javaExecutable(javaHome), "-version").redirectErrorStream(true).start();
            String output = readAll(process);
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) return -1;
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("version \\\"(\\d+)").matcher(output);
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            return -1;
        }
    }

    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    private static boolean inRange(String version, String range) {
        String[] bounds = range.split("-", 2);
        return compare(version, bounds[0]) >= 0 && compare(version, bounds[1]) <= 0;
    }

    private static int compare(String left, String right) {
        int[] leftParts = parts(left), rightParts = parts(right);
        for (int i = 0; i < Math.max(leftParts.length, rightParts.length); i++) {
            int result = Integer.compare(i < leftParts.length ? leftParts[i] : 0, i < rightParts.length ? rightParts[i] : 0);
            if (result != 0) return result;
        }
        return 0;
    }

    private static int[] parts(String version) {
        String[] parts = version.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) numbers[i] = Integer.parseInt(parts[i].replaceAll("\\D.*", ""));
        return numbers;
    }

    private static String readAll(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) output.append(line).append(System.lineSeparator());
            return output.toString();
        }
    }

    private static void stopServer(Process process) throws IOException, InterruptedException {
        if (!process.isAlive()) return;
        process.getOutputStream().write("stop\n".getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().flush();
        if (!process.waitFor(30, TimeUnit.SECONDS)) process.destroyForcibly();
    }

    private static void failWithServerOutput(String name, String version, String reason, StringBuilder output) {
        fail(name + " " + version + " failed: " + reason
                + "\nSee server log at " + serverDir.resolve("logs/latest.log").toAbsolutePath()
                + (output.isEmpty() ? "" : "\nServer output:\n" + output));
    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to delete " + path, exception);
        }
    }
}
