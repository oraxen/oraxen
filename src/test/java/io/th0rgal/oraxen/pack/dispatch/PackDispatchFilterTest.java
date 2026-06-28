package io.th0rgal.oraxen.pack.dispatch;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackDispatchFilterTest {

    @Test
    void excludesClientsBelowConfiguredVersion() {
        assertFalse(PackDispatchFilter.canSendPack(47, "1.8.9", List.of("< 1.12")));
        assertTrue(PackDispatchFilter.canSendPack(335, "1.12", List.of("< 1.12")));
        assertTrue(PackDispatchFilter.canSendPack(340, "1.12.2", List.of("< 1.12")));
    }

    @Test
    void excludesExactClientVersion() {
        assertFalse(PackDispatchFilter.canSendPack(774, "1.21.11", List.of("= 1.21.11")));
        assertTrue(PackDispatchFilter.canSendPack(769, "1.21.4", List.of("= 1.21.11")));
    }

    @Test
    void supportsGreaterThanRules() {
        assertFalse(PackDispatchFilter.canSendPack(775, "26.1.2", List.of("> 1.21.11")));
        assertTrue(PackDispatchFilter.canSendPack(774, "1.21.11", List.of("> 1.21.11")));
    }

    @Test
    void supportsCanonicalAndRuntimeVersionAliases() {
        assertFalse(PackDispatchFilter.canSendPack(775, "26.1.2", List.of("= 1.26.1.2")));
        assertFalse(PackDispatchFilter.canSendPack(775, "1.26.1", List.of("= 26.1")));
        assertFalse(PackDispatchFilter.canSendPack(776, "26.2", List.of("= 26.2.0")));
        assertFalse(PackDispatchFilter.canSendPack(776, "1.26.2", List.of("= 1.26.2.0")));
    }

    @Test
    void anyMatchingRuleStopsDispatch() {
        List<String> rules = List.of("= 1.21.11", "< 1.12", "< 1.21.4");

        assertFalse(PackDispatchFilter.canSendPack(774, "1.21.11", rules));
        assertFalse(PackDispatchFilter.canSendPack(47, "1.8.9", rules));
        assertFalse(PackDispatchFilter.canSendPack(768, "1.21.2", rules));
        assertTrue(PackDispatchFilter.canSendPack(769, "1.21.4", rules));
    }

    @Test
    void exactRulesSupportSharedProtocolAliases() {
        assertFalse(PackDispatchFilter.canSendPack(772, "1.21.7", List.of("= 1.21.8")));
    }

    @Test
    void invalidRulesAreIgnored() {
        assertTrue(PackDispatchFilter.canSendPack(774, "1.21.11", List.of("<= 1.21.11", "banana")));
    }

    @Test
    void fallsBackToVersionStringWhenProtocolIsUnavailable() {
        assertFalse(PackDispatchFilter.canSendPack(null, "1.21.11", List.of("= 1.21.11")));
        assertTrue(PackDispatchFilter.canSendPack(null, "1.21.4", List.of("= 1.21.11")));
    }

    @Test
    void resolvesProtocolFromNestedConnectionHandle() {
        Integer protocolVersion = PackDispatchFilter.resolveProtocolVersion(new PaperConnection(new ListenerHandle(new NetworkConnection(768))));

        assertEquals(768, protocolVersion);
        assertFalse(PackDispatchFilter.canSendPack(protocolVersion, "1.21.2", List.of("= 1.21.2")));
    }

    private record PaperConnection(ListenerHandle handle) {}
    private record ListenerHandle(NetworkConnection connection) {}
    private record NetworkConnection(int protocolVersion) {}
}
