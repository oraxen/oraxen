package io.th0rgal.oraxen.nms;

import io.th0rgal.oraxen.utils.MinecraftVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NMSHandlersTest {

    @Test
    void reusesJava25HandlerForAll26Releases() {
        assertHandler("java21", "1.21.11");
        assertHandler("java25", "26.1.2");
        assertHandler("java25", "26.2");
        assertHandler("java25", "1.26.2");
        assertHandler("java25", "26.3");
        assertHandler("java25", "26.3.1");
        assertHandler("java25", "1.26.3");
        assertHandler("java25", "1.26.4");
    }

    private void assertHandler(String module, String version) {
        assertEquals("io.th0rgal.oraxen.nms.handler." + module + ".NMSHandler",
                NMSHandlers.handlerClassForVersion(new MinecraftVersion(version)));
    }
}
