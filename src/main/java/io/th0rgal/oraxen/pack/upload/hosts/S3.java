package io.th0rgal.oraxen.pack.upload.hosts;

import io.th0rgal.oraxen.utils.HashUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/** S3-compatible storage using Signature V4 and content-addressed object keys. */
public class S3 implements HostingProvider {
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final String bucket;
    private final String region;
    private final String prefix;
    private final String publicUrl;
    private final boolean pathStyle;
    private final long presignedUrlExpiry;
    private String packUrl;
    private String sha1;
    private UUID packUUID;

    public S3(ConfigurationSection config) {
        endpoint = getString(config, "endpoint", "");
        accessKey = getString(config, "access-key", "");
        secretKey = getString(config, "secret-key", "");
        bucket = getString(config, "bucket", "");
        region = getString(config, "region", "us-east-1");
        prefix = getString(config, "prefix", "oraxen").replaceAll("^/+|/+$", "");
        publicUrl = getString(config, "public-url", "").replaceAll("/+$", "");
        pathStyle = config == null || config.getBoolean("path-style", true);
        presignedUrlExpiry = config == null ? 86400 : config.getLong("presigned-url-expiry", 86400);
    }

    private static String getString(ConfigurationSection config, String key, String fallback) {
        return config == null ? fallback : config.getString(key, fallback).trim();
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        return uploadPack(resourcePack, "");
    }

    @Override
    public boolean uploadPack(File resourcePack, String packVersion) {
        try {
            validateConfiguration();
        } catch (IllegalArgumentException ex) {
            Logs.logError("Invalid Pack.upload.s3 configuration: " + ex.getMessage());
            return false;
        }
        try {
            URI endpointUri = URI.create(endpoint);
            StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey));
            S3Configuration serviceConfig = S3Configuration.builder().pathStyleAccessEnabled(pathStyle)
                    .chunkedEncodingEnabled(false).build();
            String hash = calculateSHA1(resourcePack);
            String version = packVersion == null || packVersion.isBlank() ? ""
                    : "-" + packVersion.replaceAll("[^A-Za-z0-9._-]", "_");
            String key = (prefix.isEmpty() ? "" : prefix + "/") + "pack" + version + "-" + hash + ".zip";
            String url;
            // Compute the URL before uploading so invalid URL settings cannot leave a partial result.
            if (!publicUrl.isEmpty()) {
                url = publicUrl + "/" + encodeKey(key);
            } else {
                try (S3Presigner presigner = S3Presigner.builder().endpointOverride(endpointUri)
                        .region(Region.of(region)).credentialsProvider(credentials)
                        .serviceConfiguration(serviceConfig).build()) {
                    url = presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofSeconds(presignedUrlExpiry))
                            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                            .build()).url().toExternalForm();
                }
            }
            try (S3Client client = S3Client.builder().endpointOverride(endpointUri)
                    .region(Region.of(region)).credentialsProvider(credentials).serviceConfiguration(serviceConfig)
                    .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                    .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(5))
                            .socketTimeout(Duration.ofMinutes(5)))
                    .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofMinutes(5)))
                    .build()) {
                client.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                        .contentType("application/zip").build(), RequestBody.fromFile(resourcePack));
            }
            // Keep the last successful upload intact if a later upload fails.
            sha1 = hash;
            packUUID = UUID.nameUUIDFromBytes(HashUtils.hexToBytes(hash));
            packUrl = url;
            return true;
        } catch (S3Exception ex) {
            Logs.logError("S3 resource pack upload failed (HTTP " + ex.statusCode() + ").");
        } catch (IllegalArgumentException | SdkException | IOException | NoSuchAlgorithmException ex) {
            // SDK exception messages may contain signed URLs or credentials; do not log them.
            Logs.logError("S3 resource pack upload failed (" + ex.getClass().getSimpleName() + ").");
        }
        return false;
    }

    private void validateConfiguration() {
        if (accessKey.isBlank() || secretKey.isBlank() || bucket.isBlank() || region.isBlank())
            throw new IllegalArgumentException("access-key, secret-key, bucket and region must be set.");
        validateUrl(endpoint, "endpoint");
        if (!publicUrl.isEmpty()) validateUrl(publicUrl, "public-url");
        if (publicUrl.isEmpty() && (presignedUrlExpiry < 1 || presignedUrlExpiry > 604800))
            throw new IllegalArgumentException("presigned-url-expiry must be between 1 and 604800 seconds.");
    }

    private static void validateUrl(String value, String setting) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(setting + " must be an HTTP or HTTPS URL.");
        }
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException(setting + " must be an HTTP or HTTPS URL without credentials, query or fragment.");
    }

    private static String calculateSHA1(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream input = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        return HashUtils.bytesToHex(digest.digest());
    }

    private static String encodeKey(String key) {
        return Arrays.stream(key.split("/", -1))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }

    @Override public String getPackURL() { return packUrl; }
    @Override public byte[] getSHA1() { return sha1 == null ? null : HashUtils.hexToBytes(sha1); }
    @Override public String getOriginalSHA1() { return sha1; }
    @Override public UUID getPackUUID() { return packUUID; }
}
