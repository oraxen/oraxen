package io.th0rgal.oraxen.utils.reflection.version;

import com.syntaxphoenix.syntaxapi.utils.java.Strings;
import com.syntaxphoenix.syntaxapi.version.DefaultVersion;
import com.syntaxphoenix.syntaxapi.version.Version;
import com.syntaxphoenix.syntaxapi.version.VersionAnalyzer;
import com.syntaxphoenix.syntaxapi.version.VersionFormatter;

public class ServerVersion extends DefaultVersion {

    public static final ServerAnalyzer ANALYZER = new ServerAnalyzer();

    protected int refaction;

    public ServerVersion() {
        this(0);
    }

    public ServerVersion(int major) {
        this(major, 0);
    }

    public ServerVersion(int major, int minor) {
        this(major, minor, 0);
    }

    public ServerVersion(int major, int minor, int patch) {
        this(major, minor, patch, 0);
    }

    public ServerVersion(int major, int minor, int patch, int refaction) {
        super(major, minor, patch);
        this.refaction = refaction;
    }

    public int getRefaction() {
        return refaction;
    }

    @Override
    protected ServerVersion setMajor(int major) {
        super.setMajor(major);
        return this;
    }

    @Override
    protected ServerVersion setMinor(int minor) {
        super.setMinor(minor);
        return this;
    }

    @Override
    protected ServerVersion setPatch(int patch) {
        super.setPatch(patch);
        return this;
    }

    protected ServerVersion setRefaction(int refaction) {
        this.refaction = refaction;
        return this;
    }

    @Override
    public boolean isHigher(Version version) {
        if (super.isHigher(version))
            return true;
        if (version instanceof ServerVersion) {
            ServerVersion other = (ServerVersion) version;
            return refaction > other.refaction;
        } else return refaction > 0;
    }

    @Override
    public boolean isSimilar(Version version) {
        return super.isSimilar(version) && ((version instanceof ServerVersion) && refaction == ((ServerVersion) version).refaction);
    }

    @Override
    public boolean isLower(Version version) {
        if (super.isHigher(version))
            return true;
        if (version instanceof ServerVersion) {
            ServerVersion other = (ServerVersion) version;
            return refaction < other.refaction;
        }
        return false;
    }

    @Override
    public ServerVersion clone() {
        return new ServerVersion(getMajor(), getMinor(), getPatch(), refaction);
    }

    @Override
    public ServerVersion update(int major, int minor, int patch) {
        return update(major, minor, patch, 0);
    }

    public ServerVersion update(int major, int minor, int patch, int refaction) {
        return ((ServerVersion) super.update(major, minor, patch)).setRefaction(this.refaction + refaction);
    }

    @Override
    protected ServerVersion init(int major, int minor, int patch) {
        return (ServerVersion) super.init(major, minor, patch);
    }

    @Override
    public ServerAnalyzer getAnalyzer() {
        return ANALYZER;
    }

    @Override
    public VersionFormatter getFormatter() {
        return version -> {
            StringBuilder builder = new StringBuilder();
            builder.append('v');
            builder.append(version.getMajor());
            builder.append('_');
            builder.append(version.getMinor());
            builder.append("_R");
            builder.append(version.getPatch());

            if (version instanceof ServerVersion) {
                ServerVersion server = (ServerVersion) version;
                if (server.getRefaction() != 0) {
                    builder.append('.');
                    builder.append(server.getRefaction());
                }
            }

            return builder.toString();
        };
    }

    public static class ServerAnalyzer implements VersionAnalyzer {
        @Override
        public ServerVersion analyze(String formatted) {
            ServerVersion version = new ServerVersion();
            String[] parts = (formatted = formatted.replaceFirst("v", "")).contains("_") ? formatted.split("_")
                    : (formatted.contains(".") ? formatted.split("\\.") : new String[]{formatted});
            try {
                if (parts.length == 1) {
                    version.setMajor(Strings.isNumeric(parts[0]) ? Integer.parseInt(parts[0]) : 0);
                } else if (parts.length == 2) {
                    version.setMajor(Strings.isNumeric(parts[0]) ? Integer.parseInt(parts[0]) : 0);
                    version.setMinor(Strings.isNumeric(parts[1]) ? Integer.parseInt(parts[1]) : 0);
                } else if (parts.length >= 3) {
                    version.setMajor(Strings.isNumeric(parts[0]) ? Integer.parseInt(parts[0]) : 0);
                    version.setMinor(Strings.isNumeric(parts[1]) ? Integer.parseInt(parts[1]) : 0);
                    if ((parts[2] = parts[2].replaceFirst("R", "")).contains(".")) {
                        String[] parts0 = parts[2].split("\\.");
                        version.setPatch(Strings.isNumeric(parts0[0]) ? Integer.parseInt(parts0[0]) : 0);
                        version.setRefaction(Strings.isNumeric(parts0[1]) ? Integer.parseInt(parts0[1]) : 0);
                    } else {
                        version.setPatch(Strings.isNumeric(parts[2]) ? Integer.parseInt(parts[2]) : 0);
                    }
                }
            } catch (NumberFormatException ex) {

            }
            return version;
        }
    }

}
