#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd -- "$script_dir/.." && pwd)"
properties_file="$project_dir/gradle.properties"
backup_file="$(mktemp)"

cp "$properties_file" "$backup_file"
trap 'cp "$backup_file" "$properties_file"; rm -f "$backup_file"' EXIT

saved_version="$(sed -n 's/^[[:space:]]*pluginVersion[[:space:]]*=[[:space:]]*//p' "$properties_file" | head -n 1)"
commit="$(git -C "$project_dir" rev-parse --short HEAD)"

if [[ -z "$saved_version" ]]; then
  echo "pluginVersion was not found in $properties_file" >&2
  exit 1
fi

sed -i '' -E \
  -e "s|^([[:space:]]*pluginVersion[[:space:]]*=[[:space:]]*).*$|\\1${saved_version}-${commit}|" \
  -e 's|^([[:space:]]*oraxen_compiled[[:space:]]*=[[:space:]]*).*$|\1false|' \
  "$properties_file"

if [[ -x "$project_dir/gradlew" ]]; then
  "$project_dir/gradlew" -p "$project_dir" build
else
  gradle -p "$project_dir" build
fi

built_jar="$(find "$project_dir/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print0 | xargs -0 ls -t | head -n 1)"

if [[ -z "$built_jar" ]]; then
  echo "Built jar was not found in $project_dir/build/libs" >&2
  exit 1
fi

if command -v pbcopy >/dev/null 2>&1; then
  osascript -e 'on run argv' -e 'set the clipboard to POSIX file (item 1 of argv)' -e 'end run' "$built_jar"
elif command -v wl-copy >/dev/null 2>&1; then
  wl-copy --type application/java-archive < "$built_jar"
elif command -v xclip >/dev/null 2>&1; then
  xclip -selection clipboard -t application/java-archive -i "$built_jar"
else
  echo "No supported clipboard utility was found" >&2
  exit 1
fi
