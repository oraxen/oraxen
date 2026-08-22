package io.th0rgal.oraxen.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Minimal {@code multipart/form-data} body builder for {@link java.net.http.HttpClient}.
 * File parts are streamed at send time instead of being buffered in memory.
 */
public final class MultipartBody implements AutoCloseable {

    private static final String CRLF = "\r\n";

    private final String boundary = "OraxenBoundary" + UUID.randomUUID().toString().replace("-", "");
    private final List<Supplier<InputStream>> parts = new ArrayList<>();
    private final List<Path> snapshots = new ArrayList<>();
    private long contentLength = 0;

    private MultipartBody() {
    }

    public static MultipartBody create() {
        return new MultipartBody();
    }

    public String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    public MultipartBody addPart(String name, String value) {
        byte[] part = ("--" + boundary + CRLF
                + "Content-Disposition: form-data; name=\"" + name + "\"" + CRLF
                + CRLF + value + CRLF).getBytes(StandardCharsets.UTF_8);
        parts.add(() -> new ByteArrayInputStream(part));
        contentLength += part.length;
        return this;
    }

    public MultipartBody addPart(String name, File file) {
        return addPart(name, file, file.getName(), "application/octet-stream");
    }

    public MultipartBody addPart(String name, File file, String fileName, String contentType) {
        byte[] header = ("--" + boundary + CRLF
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"" + CRLF
                + "Content-Type: " + contentType + CRLF + CRLF).getBytes(StandardCharsets.UTF_8);
        byte[] trailer = CRLF.getBytes(StandardCharsets.UTF_8);
        Path snapshot;
        try {
            snapshot = Files.createTempFile("oraxen-upload-", ".snapshot");
            Files.copy(file.toPath(), snapshot, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException exception) {
            throw new UncheckedIOException(exception);
        }
        snapshots.add(snapshot);
        parts.add(() -> new ByteArrayInputStream(header));
        parts.add(() -> {
            try {
                return new FileInputStream(snapshot.toFile());
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
        });
        parts.add(() -> new ByteArrayInputStream(trailer));
        try {
            contentLength += header.length + Files.size(snapshot) + trailer.length;
        } catch (java.io.IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return this;
    }

    @Override
    public void close() {
        for (Path snapshot : snapshots) {
            try {
                Files.deleteIfExists(snapshot);
            } catch (java.io.IOException ignored) {
                snapshot.toFile().deleteOnExit();
            }
        }
        snapshots.clear();
    }

    public HttpRequest.BodyPublisher bodyPublisher() {
        byte[] closingBoundary = ("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8);
        List<Supplier<InputStream>> allParts = new ArrayList<>(parts);
        allParts.add(() -> new ByteArrayInputStream(closingBoundary));
        // Wrap the streaming publisher with the pre-computed length so the request is sent
        // with a Content-Length header instead of chunked transfer encoding, matching the
        // wire behavior of the previous Apache HttpClient multipart entity.
        return HttpRequest.BodyPublishers.fromPublisher(
                HttpRequest.BodyPublishers.ofInputStream(() -> new SequenceInputStream(
                        Collections.enumeration(allParts.stream().map(Supplier::get).toList()))),
                contentLength + closingBoundary.length);
    }
}
