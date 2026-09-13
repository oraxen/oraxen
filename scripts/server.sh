#!/usr/bin/env bash
set -euo pipefail

# Starts a local Paper/Folia server for a version used by VersionLoadingTest.
#
# Usage:
#   ./scripts/server.sh --version Paper/26.2
#   ./scripts/server.sh --version Folia/1.21.11 --no-reset

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
SERVERS_DIR="${HOME}/Oraxen/Servers"
VERSION_SPEC=""
NO_RESET=false

usage() {
    cat <<'EOF'
Usage: ./scripts/server.sh --version <Paper|Folia>/<version> [--no-reset]

Options:
  --version, -v  Server and Minecraft version, for example Paper/26.2
  --no-reset     Keep the existing plugins and world files
  --help, -h     Show this help
EOF
}

error() {
    echo "Error: $*" >&2
    exit 1
}

require_argument() {
    [[ -n "${2:-}" ]] || error "$1 requires a value"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version|-v)
            require_argument "$1" "${2:-}"
            VERSION_SPEC="$2"
            shift 2
            ;;
        --version=*)
            VERSION_SPEC="${1#*=}"
            [[ -n "$VERSION_SPEC" ]] || error "--version requires a value"
            shift
            ;;
        --no-reset)
            NO_RESET=true
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            error "unknown option '$1' (use --help for usage)"
            ;;
    esac
done

[[ -n "$VERSION_SPEC" ]] || error "--version is required"

if [[ "$VERSION_SPEC" =~ ^(Paper|Folia)/([0-9]+(\.[0-9]+)*)$ ]]; then
    SERVER_NAME="${BASH_REMATCH[1]}"
    MC_VERSION="${BASH_REMATCH[2]}"
else
    error "version must look like Paper/26.2 or Folia/1.21.11"
fi

PROJECT_NAME="$(printf '%s' "$SERVER_NAME" | tr '[:upper:]' '[:lower:]')"
SERVER_DIR="$SERVERS_DIR/$SERVER_NAME/$MC_VERSION"
SERVER_JAR="$SERVER_DIR/server.jar"

required_java_version() {
    if [[ "$MC_VERSION" == 26.* ]]; then
        echo 25
    else
        echo 21
    fi
}

java_feature_version() {
    local java_command="$1"
    "$java_command" -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n 1
}

valid_java() {
    local java_command="$1"
    local expected_version="$2"
    [[ -x "$java_command" ]] && [[ "$(java_feature_version "$java_command")" == "$expected_version" ]]
}

find_java() {
    local expected_version="$1"
    local java_home
    local java_command
    local environment_name

    for environment_name in \
        "JAVA_${expected_version}_HOME" "JDK_${expected_version}_HOME" \
        "JAVA${expected_version}_HOME" "JDK${expected_version}_HOME"; do
        java_home="${!environment_name:-}"
        if [[ -n "$java_home" ]] && valid_java "$java_home/bin/java" "$expected_version"; then
            echo "$java_home/bin/java"
            return 0
        fi
    done

    java_home="${JAVA_HOME:-}"
    if [[ -n "$java_home" ]] && valid_java "$java_home/bin/java" "$expected_version"; then
        echo "$java_home/bin/java"
        return 0
    fi

    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        java_home="$(/usr/libexec/java_home -v "$expected_version" 2>/dev/null || true)"
        if [[ -n "$java_home" ]] && valid_java "$java_home/bin/java" "$expected_version"; then
            echo "$java_home/bin/java"
            return 0
        fi
    fi

    local candidate
    for candidate in \
        "/usr/lib/jvm/java-${expected_version}-openjdk" \
        "/usr/lib/jvm/java-${expected_version}-openjdk-amd64" \
        "/usr/lib/jvm/jdk-${expected_version}" \
        "/Library/Java/JavaVirtualMachines/temurin-${expected_version}.jdk/Contents/Home" \
        "/Library/Java/JavaVirtualMachines/microsoft-${expected_version}.jdk/Contents/Home"; do
        if valid_java "$candidate/bin/java" "$expected_version"; then
            echo "$candidate/bin/java"
            return 0
        fi
    done

    if command -v java >/dev/null 2>&1 && valid_java "$(command -v java)" "$expected_version"; then
        command -v java
        return 0
    fi

    return 1
}

find_oraxen_jar() {
    local jar
    local jars=()
    shopt -s nullglob
    for jar in "$PROJECT_DIR"/build/libs/oraxen-*.jar; do
        [[ "$jar" == *-sources.jar || "$jar" == *-javadoc.jar ]] || jars+=("$jar")
    done
    shopt -u nullglob

    if ((${#jars[@]} == 0)); then
        echo "No Oraxen jar found; building shadowJar..." >&2
        if [[ -x "$PROJECT_DIR/gradlew" ]]; then
            "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" shadowJar >&2
        else
            (cd "$PROJECT_DIR" && sh ./gradlew shadowJar >&2)
        fi
        shopt -s nullglob
        for jar in "$PROJECT_DIR"/build/libs/oraxen-*.jar; do
            [[ "$jar" == *-sources.jar || "$jar" == *-javadoc.jar ]] || jars+=("$jar")
        done
        shopt -u nullglob
    fi

    ((${#jars[@]} > 0)) || error "could not find the built Oraxen jar in $PROJECT_DIR/build/libs"

    # The normal build produces one jar. If more are present, use the newest one.
    local newest_jar="${jars[0]}"
    for jar in "${jars[@]:1}"; do
        [[ "$jar" -nt "$newest_jar" ]] && newest_jar="$jar"
    done
    echo "$newest_jar"
}

known_download_url() {
    case "$PROJECT_NAME/$MC_VERSION" in
        paper/26.2) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/36fee4f3a7020eb2e2d6f8d70d849beaf0f024d86f09302b9ccf2d96f266127e/paper-26.2-71.jar' ;;
        paper/26.1.2) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/d30fae0c74092b10855f0412ca6b265c60301a013d34bc28a2a41bf5682dd80b/paper-26.1.2-69.jar' ;;
        paper/1.21.11) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba/paper-1.21.11-132.jar' ;;
        paper/1.21.10) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/158703f75a26f842ea656b3dc6d75bf3d1ec176b97a2c36384d0b80b3871af53/paper-1.21.10-130.jar' ;;
        paper/1.21.8) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar' ;;
        paper/1.21.5) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/2ae6ae22adf417699746e0f89fc2ef6cb6ee050a5f6608cee58f0535d60b509e/paper-1.21.5-114.jar' ;;
        paper/1.21.4) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/5ee4f542f628a14c644410b08c94ea42e772ef4d29fe92973636b6813d4eaffc/paper-1.21.4-232.jar' ;;
        paper/1.21.3) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/87e973e1d338e869e7fdbc4b8fadc1579d7bb0246a0e0cf6e5700ace6c8bc17e/paper-1.21.3-83.jar' ;;
        paper/1.20.6) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/4b011f5adb5f6c72007686a223174fce82f31aeb4b34faf4652abc840b47e640/paper-1.20.6-151.jar' ;;
        paper/1.20.4) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/cabed3ae77cf55deba7c7d8722bc9cfd5e991201c211665f9265616d9fe5c77b/paper-1.20.4-499.jar' ;;
        paper/1.20.1) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/234a9b32098100c6fc116664d64e36ccdb58b5b649af0f80bcccb08b0255eaea/paper-1.20.1-196.jar' ;;
        folia/26.1.2) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/607afd1c3320008e1ffd2eaee6780ace4419d5f8c527b75e79f259be79ebf57b/folia-26.1.2-8.jar' ;;
        folia/1.21.11) printf '%s\n' 'https://fill-data.papermc.io/v1/objects/f52c408490a0225611e67907a3ca19f7e6da2c6bc899e715d5f46844e7103c39/folia-1.21.11-14.jar' ;;
        *) return 1 ;;
    esac
}

download_server_jar() {
    [[ -f "$SERVER_JAR" ]] && return 0

    local download_url
    download_url="$(known_download_url || true)"
    if [[ -z "$download_url" ]]; then
        local builds_url="https://api.papermc.io/v2/projects/${PROJECT_NAME}/versions/${MC_VERSION}/builds"
        local builds_json
        local build
        builds_json="$(curl --fail --silent --show-error --location "$builds_url")" \
            || error "could not fetch ${SERVER_NAME} ${MC_VERSION} build information"
        build="$(printf '%s' "$builds_json" | grep -oE '"build"[[:space:]]*:[[:space:]]*[0-9]+' | sed 's/[^0-9]//g' | tail -n 1)"
        [[ -n "$build" ]] || error "could not find a ${SERVER_NAME} build for Minecraft ${MC_VERSION}"
        download_url="https://api.papermc.io/v2/projects/${PROJECT_NAME}/versions/${MC_VERSION}/builds/${build}/downloads/${PROJECT_NAME}-${MC_VERSION}-${build}.jar"
    fi

    local temporary_jar="$SERVER_DIR/server.jar.download"
    mkdir -p "$SERVER_DIR"
    echo "Downloading ${SERVER_NAME} ${MC_VERSION}..."
    curl --fail --location --output "$temporary_jar" "$download_url" \
        || error "could not download ${SERVER_NAME} ${MC_VERSION}"
    mv "$temporary_jar" "$SERVER_JAR"
}

reset_server_files() {
    mkdir -p "$SERVER_DIR"
    if [[ "$NO_RESET" == false ]]; then
        # Keep the downloaded server jar so changing between runs does not redownload it.
        shopt -s dotglob nullglob
        local path
        for path in "$SERVER_DIR"/*; do
            [[ "$path" == "$SERVER_JAR" ]] || rm -rf "$path"
        done
        shopt -u dotglob nullglob
    fi
    mkdir -p "$SERVER_DIR/plugins"
}

regenerate_server_files() {
    # These files are intentionally recreated even with --no-reset.
    printf 'eula=true\n' > "$SERVER_DIR/eula.txt"
}

java_command="$(find_java "$(required_java_version)")" \
    || error "Java $(required_java_version) is required for ${MC_VERSION}; set JAVA_$(required_java_version)_HOME or JAVA_HOME"

oraxen_jar="$(find_oraxen_jar)"
download_server_jar
reset_server_files
regenerate_server_files
cp "$oraxen_jar" "$SERVER_DIR/plugins/Oraxen.jar"

cat <<EOF
Starting ${SERVER_NAME} ${MC_VERSION}
Server directory: ${SERVER_DIR}
Oraxen jar: ${oraxen_jar}
EOF

cd "$SERVER_DIR"
exec "$java_command" -Xmx1G -jar server.jar --nogui --port 0
