## Oraxen Plugin
Oraxen is a Paper/Folia plugin for versions 1.20.1 through 26.3 that allows administrators to add custom content to their servers through YAML configuration and simple file setup.

### Rules
- All changes must be compatible with Folia.
- Prefer the modern Paper API over older Bukkit APIs.
- Variables should be lowercase or camelCase.
- When logging an exception, log the descriptive message on its own line with the normal prefix. Log the exception on the next line without a prefix.
- NMS handler code lives once in `nms/shared` and each NMS module copies it into its own package at build time. Put calls that differ between the 1.21.11 and 26.x dev bundles in the `NmsAccessors` class of both `nms/java21` and `nms/java25`.
- Run the version loading tests with `./gradlew :test --tests io.th0rgal.oraxen.loading.VersionLoadingTest -PrunVersionLoadingTest=true`. Without `:test`, the filter also applies to `nms:java21` and fails there with "No tests found".
