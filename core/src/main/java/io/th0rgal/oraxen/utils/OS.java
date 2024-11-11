package io.th0rgal.oraxen.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class OS {

    private OsInfo osInfo;

    private OS(final String name, final String version, final String arch) {
        if (name != null) {
            if (name.startsWith("Windows"))
                this.osInfo = new OsInfo(name, version, arch, name);
            else if (name.startsWith("Mac"))
                initMacOsInfo(name, version, arch);
            else if (name.startsWith("Darwin"))
                initDarwinOsInfo(name, version, arch);
            else
                for (String linuxName : linux)
                    if (name.startsWith(linuxName))
                        initLinuxOsInfo(name, version, arch);
        }
        if (this.osInfo == null)
            this.osInfo = new OsInfo(name, version, arch, name);
    }

    private static class SingletonHolder {
        static String name = System.getProperty("os.name");
        static String version = System.getProperty("os.version");
        static String arch = System.getProperty("os.arch");
        private static final OS INSTANCE = new OS(name, version, arch);

    }

    public static OS getOs() {
        return SingletonHolder.INSTANCE;
    }

    public String getName() {
        return osInfo.name();
    }

    public String getArch() {
        return osInfo.arch();
    }

    public String getVersion() {
        return osInfo.version();
    }

    public String getPlatformName() {
        return osInfo.platformName();
    }

    private static final Map<String, String> macOs = new HashMap<>();
    private static final Map<Integer, String> darwin = new HashMap<>();
    private static final List<String> linux = new ArrayList<>();

    static {
        macOs.put("10.0", "Puma");
        macOs.put("10.1", "Cheetah");
        macOs.put("10.2", "Jaguar");
        macOs.put("10.3", "Panther");
        macOs.put("10.4", "Tiger");
        macOs.put("10.5", "Leopard");
        macOs.put("10.6", "Snow Leopard");
        macOs.put("10.7", "Snow Lion");
        macOs.put("10.8", "Mountain Lion");
        macOs.put("10.9", "Mavericks");
        macOs.put("10.10", "Yosemite");
        macOs.put("10.11", "El Capitan");
        macOs.put("10.12", "Sierra");
        macOs.put("10.13", "High Sierra");
        macOs.put("10.14", "Mojave");
        macOs.put("10.15", "Catalina");
        macOs.put("11.1", "Big Sur");
        macOs.put("12", "Monterey");
        macOs.put("13", "Ventura");
        macOs.put("14", "Sonoma");
        macOs.put("15", "Sequoia");

        darwin.put(5, "Puma");
        darwin.put(6, "Jaguar");
        darwin.put(7, "Panther");
        darwin.put(8, "Tiger");
        darwin.put(9, "Leopard");
        darwin.put(10, "Snow Leopard");
        darwin.put(11, "Lion");
        darwin.put(12, "Mountain Lion");
        darwin.put(13, "Mavericks");
        darwin.put(14, "Yosemite");
        darwin.put(15, "El Capitan");
        darwin.put(16, "Sierra");

        linux.addAll(Arrays.asList("Linux", "SunOS"));
    }

    private void initMacOsInfo(final String name, final String version, final String arch) {
        String[] versions = version.split("\\.");
        double numericVersion = Double.parseDouble(versions[0] + "." + versions[1]);
        if (numericVersion < 10)
            this.osInfo = new OsInfo(name, version, arch, "Mac OS " + version);
        else {
            this.osInfo = new OsInfo(name, version, arch,
                    "macOS " + ((Integer.parseInt(versions[0]) >= 12) ? macOs.get(versions[0]) : macOs.get(version))
                            + " (" + version + ")");
        }
    }

    private void initDarwinOsInfo(final String name, final String version, final String arch) {
        String[] versions = version.split("\\.");
        int numericVersion = Integer.parseInt(versions[0]);
        this.osInfo = new OsInfo(name, version, arch, "OS X " + darwin.get(numericVersion) + " (" + version + ")");
    }

    private void initLinuxOsInfo(final String name, final String version, final String arch) {
        OsInfo osInfo;
        // The most likely is to have a LSB compliant distro
        osInfo = getPlatformNameFromLsbRelease(name, version, arch);

        // Generic Linux platform name
        if (osInfo == null)
            osInfo = getPlatformNameFromFile(name, version, arch, "/etc/system-release");

        File dir = new File("/etc/");
        if (osInfo == null && dir.exists()) {
            // if generic 'system-release' file is not present, then try to find another one
            osInfo = getPlatformNameFromFile(name, version, arch, getFileEndingWith(dir, "-release"));

            // if generic 'system-release' file is not present, then try to find '_version'
            if (osInfo == null)
                osInfo = getPlatformNameFromFile(name, version, arch, getFileEndingWith(dir, "_version"));

            // try with /etc/issue file
            if (osInfo == null)
                osInfo = getPlatformNameFromFile(name, version, arch, "/etc/issue");

        }

        // if nothing found yet, looks for the version info
        File fileVersion = new File("/proc/version");
        if (osInfo == null && fileVersion.exists())
            osInfo = getPlatformNameFromFile(name, version, arch, fileVersion.getAbsolutePath());

        // if nothing found, well...
        if (osInfo == null)
            osInfo = new OsInfo(name, version, arch, name);

        this.osInfo = osInfo;
    }

    private String getFileEndingWith(final File dir, final String fileEndingWith) {
        File[] fileList = dir.listFiles((dir1, filename) -> filename.endsWith(fileEndingWith));
        if (fileList != null && fileList.length > 0)
            return fileList[0].getAbsolutePath();
        else
            return null;
    }

    private OsInfo getPlatformNameFromFile(final String name, final String version, final String arch,
            final String filename) {
        if (filename == null)
            return null;
        File f = new File(filename);
        if (f.exists())
            try {
                BufferedReader br = new BufferedReader(new FileReader(filename));
                return readPlatformName(name, version, arch, br);
            } catch (IOException e) {
                return null;
            }
        return null;
    }

    OsInfo readPlatformName(final String name, final String version, final String arch, final BufferedReader br)
            throws IOException {
        String line;
        String lineToReturn = null;
        int lineNb = 0;
        while ((line = br.readLine()) != null) {
            if (lineNb++ == 0)
                lineToReturn = line;
            if (line.startsWith("PRETTY_NAME"))
                return new OsInfo(name, version, arch, line.substring(13, line.length() - 1));
        }
        return new OsInfo(name, version, arch, lineToReturn);
    }

    private OsInfo getPlatformNameFromLsbRelease(final String name, final String version, final String arch) {
        String fileName = "/etc/lsb-release";
        File f = new File(fileName);
        if (f.exists())
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                return readPlatformNameFromLsb(name, version, arch, br);
            } catch (IOException e) {
                return null;
            }
        return null;
    }

    OsInfo readPlatformNameFromLsb(final String name, final String version, final String arch, final BufferedReader br)
            throws IOException {
        String distribDescription = null;
        String distribCodename = null;

        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("DISTRIB_DESCRIPTION"))
                distribDescription = line.replace("DISTRIB_DESCRIPTION=", "").replace("\"", "");
            if (line.startsWith("DISTRIB_CODENAME"))
                distribCodename = line.replace("DISTRIB_CODENAME=", "");
        }
        if (distribDescription != null && distribCodename != null)
            return new OsInfo(name, version, arch, distribDescription + " (" + distribCodename + ")");
        return null;
    }

    record OsInfo(String name, String version, String arch, String platformName) {
    }

}
