package io.th0rgal.oraxen;

import io.th0rgal.oraxen.utils.logs.Logs;

public final class CompileNotice {

    public static void print() {
        try {
            PrintNotice.print();
        } catch (Throwable ignore) {

        }
    }

    private static class PrintNotice {
        public static void print() {
            Logs.logError("This is a compiled version of Oraxen.");
            Logs.logWarning("Compiled versions come without Default assets and support is not provided.");
            Logs.logWarning("Please consider purchasing Oraxen on SpigotMC to support the project and get access to the full version.");
        }
    }
}
