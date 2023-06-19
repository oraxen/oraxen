package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeakNotice {

    public static void print() {
        Logs.logError("This is a leaked version of Oraxen");
        Logs.logError("Piracy is not supported, shutting down plugin.");
        Logs.logError("Please consider purchasing Oraxen on SpigotMC to support the project and get access to the full version.");
        //Bukkit.getPluginManager().disablePlugin(OraxenPlugin.get());
    }

    public static boolean checkIsLeaked() {
        Logs.logSuccess("leaking");
        JarFile jarFile = OraxenPlugin.getJarFile();
        if (jarFile == null) return false;
        Enumeration<JarEntry> entries = jarFile.entries();
        Pattern pattern = Pattern.compile("(module-info|DirectLea.*).class");

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            Matcher matcher = pattern.matcher(entryName);
            if (matcher.matches()) return true;
        }
        return false;
    }
}
