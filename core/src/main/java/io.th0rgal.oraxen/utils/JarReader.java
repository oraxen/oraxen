package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static io.th0rgal.oraxen.utils.JarReader.StringPatternMatching.calculateStringSimilarity;

public class JarReader {

    public static boolean checkIsLeaked() {
        JarFile jarFile = OraxenPlugin.getJarFile();
        if (jarFile == null) return false;
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (!entryName.endsWith(".class")) continue;
            if (entryName.contains("/")) continue;

            entryName = entry.getName().substring(0, 10);

            if (calculateStringSimilarity(entryName,"DirectLeaks") > 0.8) return true;
            if (calculateStringSimilarity(entryName,"module-info") > 0.8) return true;
        }
        return false;
    }

    public static String getManifestContent() {
        JarFile jarFile = OraxenPlugin.getJarFile();
        if (jarFile == null) return "";
        Enumeration<JarEntry> entries = jarFile.entries();
        String manifest = "";

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (!entryName.contains("MANIFEST")) continue;
            InputStream manifestStream;
            try {
                manifestStream = jarFile.getInputStream(entry);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }

            try {
                manifest = IOUtils.toString(manifestStream, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        }
        return manifest;
    }

    public static class StringPatternMatching {
        public static double calculateStringSimilarity(String str1, String str2) {
            int str1Length = str1.length();
            int str2Length = str2.length();

            if (str1Length == 0 && str2Length == 0) {
                return 1.0;
            }

            int matchingDistance = Math.max(str1Length, str2Length) / 2 - 1;
            boolean[] str1Matches = new boolean[str1Length];
            boolean[] str2Matches = new boolean[str2Length];

            int matchingCount = 0;
            for (int i = 0; i < str1Length; i++) {
                int start = Math.max(0, i - matchingDistance);
                int end = Math.min(i + matchingDistance + 1, str2Length);

                for (int j = start; j < end; j++) {
                    if (!str2Matches[j] && str1.charAt(i) == str2.charAt(j)) {
                        str1Matches[i] = true;
                        str2Matches[j] = true;
                        matchingCount++;
                        break;
                    }
                }
            }

            if (matchingCount == 0) {
                return 0.0;
            }

            int transpositionCount = 0;
            int k = 0;
            for (int i = 0; i < str1Length; i++) {
                if (str1Matches[i]) {
                    int j;
                    for (j = k; j < str2Length; j++) {
                        if (str2Matches[j]) {
                            k = j + 1;
                            break;
                        }
                    }

                    if (str1.charAt(i) != str2.charAt(j)) {
                        transpositionCount++;
                    }
                }
            }

            transpositionCount /= 2;

            double jaroSimilarity = (double) matchingCount / str1Length;
            double jaroWinklerSimilarity = jaroSimilarity + (0.1 * transpositionCount * (1 - jaroSimilarity));

            return jaroWinklerSimilarity;
        }
    }


}
