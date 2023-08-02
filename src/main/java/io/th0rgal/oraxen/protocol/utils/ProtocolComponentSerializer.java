package io.th0rgal.oraxen.protocol.utils;

public interface ProtocolComponentSerializer {

    Object deserialize(String input);

    String serialize(Object component);
}
