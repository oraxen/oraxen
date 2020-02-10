/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/11 24:47:13
 *
 * Oraxen/Oraxen/Sh.java
 *
 */

package io.th0rgal.oraxen.pack.upload.hosts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Sh implements HostingProvider {
    private byte[] sha1;
    private String result;

    public static Function<String, List<String>> path(String placeholder, List<String> args) {
        return path -> args.stream().map(val -> val.replace(placeholder, path)).collect(Collectors.toList());
    }

    private final Function<String, List<String>> commands;

    public Sh(Function<String, List<String>> commands) {
        this.commands = commands;
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        final List<String> cmd = commands.apply(resourcePack.getPath());
        ProcessBuilder builder = new ProcessBuilder(cmd);
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkExec(String.join(" ", cmd));
            } catch (SecurityException se) {
                return AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> run(builder));
            }
        }
        return run(builder);
    }

    private boolean run(ProcessBuilder builder) {
        final Process start;
        try {
            start = builder.start();
        } catch (IOException ignore) {
            return false;
        }
        final InputStream stream = start.getInputStream();
        try {
            byte[] result = read(stream);
            byte[] sha1 = read(stream);
            if (sha1.length == 0) sha1 = null;
            if (start.waitFor() == 0) {
                this.sha1 = sha1;
                this.result = new String(result);
                return true;
            }
        } catch (IOException | InterruptedException ignore) {
        }
        return false;
    }

    private static byte[] read(InputStream stream) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        boolean r = false;
        do {
            int code = stream.read();
            if (code == -1) break; // EOF
            if (code == '\r') {
                if (r) os.write('\r');
                r = true;
                continue;
            }
            if (code == '\n') {
                break;
            }
            if (r) os.write('\r');
            os.write(code);
        } while (true);
        return os.toByteArray();
    }

    @Override
    public String getPackURL() {
        return result;
    }

    @Override
    public byte[] getSHA1() {
        return sha1;
    }
}
