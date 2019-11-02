package io.th0rgal.oraxen.settings;

public class Server {

    public static final String OS_NAME = System.getProperty("os.name");

    public static boolean isUsingWindows() {
        return OS_NAME.startsWith("Windows");
    }

}
