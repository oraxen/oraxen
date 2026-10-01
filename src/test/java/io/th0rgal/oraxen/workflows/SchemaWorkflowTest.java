package io.th0rgal.oraxen.workflows;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaWorkflowTest {

    @Test
    void watchesRootModuleSchemaSources() throws Exception {
        String workflow = Files.readString(Path.of(".github/workflows/schema.yml"));

        assertTrue(workflow.contains("'src/main/java/io/th0rgal/oraxen/utils/schema/**'"));
        assertTrue(workflow.contains("'src/main/java/io/th0rgal/oraxen/items/**'"));
        assertTrue(workflow.contains("'src/main/java/io/th0rgal/oraxen/mechanics/**'"));
        assertFalse(workflow.contains("'core/src/main/java/"));
        assertFalse(workflow.contains("'core/build.gradle.kts'"));
        assertFalse(workflow.contains("api.papermc.io/v2"));
        assertTrue(workflow.contains("sha256sum -c -"));
        assertTrue(workflow.contains("enabled: false"));
    }

    @Test
    void seedsThePublishedSchemaAsTheOrderingBase() throws Exception {
        String workflow = Files.readString(Path.of(".github/workflows/schema.yml"));

        // CI must start from the committed schema, otherwise the first run has no ordering
        // base and reorders every entry of the file.
        assertTrue(workflow.contains("cp schemas/oraxen-schema.json test-server/plugins/Oraxen/oraxen-schema.json"));
    }
}
