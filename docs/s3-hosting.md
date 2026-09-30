# S3 resource-pack hosting

Set `Pack.upload.type: s3` in `settings.yml` to upload generated resource-pack ZIPs to Amazon S3 or an S3-compatible service such as MinIO or Cloudflare R2. Both single-pack and multi-version uploads use this provider, on Paper and Folia. The existing upload managers perform network work asynchronously.

```yaml
Pack:
  upload:
    enabled: true
    type: s3
    s3:
      endpoint: "https://s3.us-east-1.amazonaws.com"
      access-key: "YOUR_ACCESS_KEY"
      secret-key: "YOUR_SECRET_KEY"
      bucket: "minecraft-packs"
      region: "us-east-1"
      prefix: "oraxen"
      path-style: true
      public-url: "https://packs.example.com"
      presigned-url-expiry: 86400
```

Create the bucket before uploading. Set the API endpoint and signing region required by your provider (for example, R2 uses `https://<account-id>.r2.cloudflarestorage.com` and region `auto`). Use HTTPS in production. `path-style: true` sends requests to `endpoint/bucket/key`; set it to `false` for providers requiring `bucket.endpoint/key`. The public download host may differ from the API endpoint.

Credentials need `s3:PutObject` on the chosen bucket/prefix, and `s3:GetObject` when using presigned downloads. Oraxen does not create buckets, change ACLs or bucket policies, or delete old objects. Keep credentials out of shared configuration files and source control.

`public-url` is the URL of the **bucket root**, without a query or fragment. Oraxen appends the prefix and object key, encoding spaces and special characters. For direct public S3 downloads, include the bucket in the URL, such as `https://minecraft-packs.s3.us-east-1.amazonaws.com`. Configure public read access or a CDN origin capable of reading the bucket yourself.

Leave `public-url` empty for private buckets. Oraxen generates a Signature V4 presigned GET URL with `presigned-url-expiry` seconds of validity (1–604800, at most seven days). URLs are generated during upload and retained for subsequent player joins; reload/regenerate the pack before they expire, or use a public/CDN URL for continuous availability. Presigned URLs are bearer links and appear in the normal pack-upload logs/events: treat them as private.

Keys have the form `oraxen/pack-<sha1>.zip`, or `oraxen/pack-<minecraft-version>-<sha1>.zip` for multi-version packs. Empty `prefix` places objects at the bucket root. Content hashes prevent CDN caches from serving a previous pack at the same URL and let multiple server instances reuse identical objects. Changed packs retain their old objects; configure a bucket lifecycle policy if needed. Hashes and pack UUIDs are computed from the local ZIP, rather than the S3 ETag, which is not a resource-pack SHA-1.

Failed uploads do not replace the provider's last successful URL/hash/UUID. Upload errors report HTTP status or exception type without printing credentials or SDK request details. The SDK streams files from disk and uses bounded connection and upload timeouts.
