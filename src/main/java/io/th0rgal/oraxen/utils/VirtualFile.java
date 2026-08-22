package io.th0rgal.oraxen.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class VirtualFile implements Comparable<VirtualFile> {

    private String parentFolder;
    private String name;
    private InputStream inputStream;
    private Supplier<InputStream> inputStreamSupplier;

    public VirtualFile(String parentFolder, String name, InputStream inputStream) {
        parentFolder = SystemUtils.IS_OS_WINDOWS
                ? parentFolder.replace("\\", "/")
                : parentFolder;
        this.parentFolder = parentFolder.endsWith("/")
                ? parentFolder.substring(0, parentFolder.length() - 1)
                : parentFolder;
        this.name = name;
        this.inputStream = inputStream;
    }

    public VirtualFile(String parentFolder, String name, Supplier<InputStream> inputStreamSupplier) {
        this(parentFolder, name, (InputStream) null);
        this.inputStreamSupplier = inputStreamSupplier;
    }

    public InputStream getInputStream() {
        try {
            return inputStreamSupplier != null ? inputStreamSupplier.get() : inputStream;
        } catch (UncheckedIOException exception) {
            Logs.logWarning("Failed to open " + getPath() + ": " + exception.getMessage());
            return null;
        }
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        inputStreamSupplier = null;
    }

    public String getPath() {
        return parentFolder.isEmpty()
                ? name
                : parentFolder + "/" + name;
    }

    public void setPath(String newPath) {
        String newParent = newPath.substring(0, newPath.lastIndexOf("/"));
        this.parentFolder = SystemUtils.IS_OS_WINDOWS
                ? newParent.replace("\\", "/")
                : newParent;
        this.name = newPath.substring(newPath.lastIndexOf("/") + 1);
    }

    @Override
    public int compareTo(@NotNull VirtualFile other) {
        return other.getPath().compareTo(getPath());
    }

    @Nullable
    public JsonElement toJsonElement() {
        InputStream fontInput = inputStream;
        String fontContent;
        try {
            fontContent = IOUtils.toString(fontInput, StandardCharsets.UTF_8);
            inputStream.close();
            inputStream = new ByteArrayInputStream(fontContent.getBytes(StandardCharsets.UTF_8));
            return JsonParser.parseString(fontContent);
        } catch (Exception e) {
            Logs.logError(FilenameUtils.getName(getPath()) + " was empty");
            return null;
        }
    }

    @Nullable
    public JsonObject toJsonObject() {
        JsonElement element = toJsonElement();
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    public boolean isJsonObject() {
        JsonElement element = toJsonElement();
        return element != null && element.isJsonObject();
    }

}
