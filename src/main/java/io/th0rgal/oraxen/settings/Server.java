package io.th0rgal.oraxen.settings;

public class Server {


    private static final String OS_NAME = System.getProperty("os.name");

    public static boolean isIsUsingWindows() {
        return OS_NAME.startsWith("Windows");
    }


}
