#!/usr/bin/env bash
#
# Oraxen Headless Resource Pack Generator
#
# Downloads a Minecraft server, runs it with Oraxen to generate the resource pack,
# and extracts the pack.zip without requiring a full server setup.
#
# Usage:
#   ./scripts/headless-pack-gen.sh [options]
#
# Options:
#   --version, -v       Minecraft version (e.g., 1.21.4, 1.21.11) [required]
#   --server, -s        Server type: paper, spigot, purpur (default: paper)
#   --output, -o        Output directory for pack.zip (default: ./build/pack)
#   --oraxen-jar, -j    Path to Oraxen jar (default: auto-detect from build/libs)
#   --config-dir, -c    Config directory to copy into server (optional)
#   --timeout, -t       Timeout in seconds (default: 300)
#   --keep-server, -k   Keep server directory after completion
#   --verbose           Enable verbose output
#   --help, -h          Show this help message
#
# Examples:
#   ./scripts/headless-pack-gen.sh -v 1.21.4
#   ./scripts/headless-pack-gen.sh -v 1.21.11 -o ./my-pack -c ./my-configs
#

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
MC_VERSION=""
SERVER_TYPE="paper"
OUTPUT_DIR="./build/pack"
ORAXEN_JAR=""
CONFIG_DIR=""
TIMEOUT=300
KEEP_SERVER=false
VERBOSE=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
WORK_DIR=""

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_verbose() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}[VERBOSE]${NC} $1"
    fi
}

# Show usage
show_help() {
    head -35 "$0" | tail -32 | sed 's/^#//' | sed 's/^ //'
    exit 0
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --version|-v)
                MC_VERSION="$2"
                shift 2
                ;;
            --server|-s)
                SERVER_TYPE="$2"
                shift 2
                ;;
            --output|-o)
                OUTPUT_DIR="$2"
                shift 2
                ;;
            --oraxen-jar|-j)
                ORAXEN_JAR="$2"
                shift 2
                ;;
            --config-dir|-c)
                CONFIG_DIR="$2"
                shift 2
                ;;
            --timeout|-t)
                TIMEOUT="$2"
                shift 2
                ;;
            --keep-server|-k)
                KEEP_SERVER=true
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --help|-h)
                show_help
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                ;;
        esac
    done

    # Validate required arguments
    if [[ -z "$MC_VERSION" ]]; then
        log_error "Minecraft version is required. Use --version or -v"
        exit 1
    fi
}

# Find Oraxen JAR
find_oraxen_jar() {
    if [[ -n "$ORAXEN_JAR" && -f "$ORAXEN_JAR" ]]; then
        log_verbose "Using specified Oraxen JAR: $ORAXEN_JAR"
        return 0
    fi

    # Auto-detect from build/libs
    local jar_pattern="$PROJECT_ROOT/build/libs/oraxen-*.jar"
    local jars
    jars=$(ls $jar_pattern 2>/dev/null | head -1 || true)

    if [[ -n "$jars" ]]; then
        ORAXEN_JAR="$jars"
        log_info "Auto-detected Oraxen JAR: $ORAXEN_JAR"
        return 0
    fi

    log_error "Could not find Oraxen JAR. Build the project first with './gradlew build' or specify with --oraxen-jar"
    exit 1
}

# Download server JAR from mcserverjars.com
download_server() {
    local server_jar="$WORK_DIR/server.jar"

    log_info "Fetching server download URL from mcserverjars.com..."

    # Get the latest build info for the specified version
    local api_url="https://mcserverjars.com/api/v1/projects/${SERVER_TYPE}/versions/${MC_VERSION}/latest"
    log_verbose "API URL: $api_url"

    local response
    response=$(curl -sfL "$api_url" 2>/dev/null) || {
        log_error "Failed to fetch server info. Is version $MC_VERSION available for $SERVER_TYPE?"
        exit 1
    }

    # Extract download URL from response
    local download_url
    download_url=$(echo "$response" | grep -oE '"download_url"\s*:\s*"[^"]+"' | head -1 | sed 's/"download_url"\s*:\s*"//' | sed 's/"$//')

    if [[ -z "$download_url" ]]; then
        log_error "Could not extract download URL from API response"
        log_verbose "Response: $response"
        exit 1
    fi

    log_info "Downloading $SERVER_TYPE $MC_VERSION..."
    log_verbose "Download URL: $download_url"

    curl -#fL -o "$server_jar" "$download_url" || {
        log_error "Failed to download server JAR"
        exit 1
    }

    log_success "Downloaded server.jar ($(du -h "$server_jar" | cut -f1))"
}

# Setup server directory
setup_server() {
    log_info "Setting up server directory..."

    # Create plugins directory
    mkdir -p "$WORK_DIR/plugins/Oraxen"

    # Copy Oraxen JAR
    cp "$ORAXEN_JAR" "$WORK_DIR/plugins/Oraxen.jar"
    log_verbose "Copied Oraxen JAR to plugins/"

    # Accept EULA
    echo "eula=true" > "$WORK_DIR/eula.txt"
    log_verbose "Created eula.txt"

    # Find an available port (random in range 30000-40000)
    local server_port
    while true; do
        server_port=$((30000 + RANDOM % 10000))
        if ! lsof -i ":$server_port" &>/dev/null; then
            break
        fi
    done
    log_verbose "Using server port: $server_port"

    # Create minimal server.properties
    cat > "$WORK_DIR/server.properties" << EOF
# Minimal server.properties for headless pack generation
server-port=${server_port}
online-mode=false
spawn-protection=0
max-players=0
enable-command-block=false
spawn-npcs=false
spawn-animals=false
spawn-monsters=false
generate-structures=false
level-type=flat
sync-chunk-writes=false
view-distance=2
simulation-distance=2
EOF
    log_verbose "Created server.properties"

    # Create Oraxen settings.yml with upload disabled
    cat > "$WORK_DIR/plugins/Oraxen/settings.yml" << 'EOF'
debug: false
Plugin:
  keep_this_up_to_date: false
  language: "english"
  auto_update_paper_config: false
  generation:
    default_assets: true
    default_configs: true
  formatting:
    inventory_titles: false
    titles: false
    subtitles: false
    action_bar: false
    anvil: false
    signs: false
    chat: false
    books: false

Glyphs:
  glyph_handler: VANILLA
  emoji_list_permission_only: false
  unicode_completions: false

TextEffects:
  enabled: false

Chat:
  chat_handler: MODERN

CustomArmor:
  type: COMPONENT
  disable_leather_repair: true

CustomBlocks:
  block_correction: NMS
  use_legacy_noteblocks: true

ItemUpdater:
  update_items: false
  update_items_on_reload: false

FurnitureUpdater:
  update_furniture: false
  update_on_reload: false
  update_on_load: false

Pack:
  generation:
    generate: true
    excluded_file_extensions: []
    appearance:
      item_properties: true
      model_data_ids: false
      model_data_float: false
      generate_predicates: false
    verify_pack_files: true
    fix_force_unicode_glyphs: true
    texture_slicer: true
    atlas:
      exclude_malformed_from_atlas: true
      generate: true
      type: "SPRITE"
    auto_generated_models_follow_texture_path: false
    compression: BEST_COMPRESSION
    protection: false
    comment: "Generated by Oraxen headless mode"

  import:
    merge_duplicate_fonts: true
    merge_duplicates: true
    retain_custom_model_data: true
    merge_item_base_models: false

  upload:
    enabled: false
    type: polymath
    polymath:
      server: atlas.oraxen.com
      secret: "oraxen"

  dispatch:
    send_pack: false
    send_on_reload: false
    delay: -1
    mandatory: false

  receive:
    enabled: false

Misc:
  reset_recipes: false
  add_recipes_to_book: false
  hide_scoreboard_numbers: false
  hide_scoreboard_background: false
  hide_tablist_background: false

oraxen_inventory:
  main_menu_title: ""
  menu_rows: 6
  menu_size: 45
  menu_layout: {}
EOF
    log_verbose "Created Oraxen settings.yml with upload disabled"

    # Copy custom config directory if specified
    if [[ -n "$CONFIG_DIR" && -d "$CONFIG_DIR" ]]; then
        log_info "Copying custom config from $CONFIG_DIR..."
        cp -r "$CONFIG_DIR"/* "$WORK_DIR/plugins/Oraxen/" 2>/dev/null || true
        # Ensure settings.yml still has upload disabled (only disable upload, not other features)
        if [[ -f "$CONFIG_DIR/settings.yml" ]]; then
            log_warn "Custom settings.yml detected - ensuring upload is disabled"
            # Use perl to target only the upload section's enabled setting
            # This is portable across BSD (macOS) and GNU (Linux) environments
            local settings_file="$WORK_DIR/plugins/Oraxen/settings.yml"
            perl -i -pe '
                BEGIN { $in_upload = 0; }
                if (/^  upload:$/) { $in_upload = 1; }
                elsif ($in_upload && /^  [a-zA-Z]/) { $in_upload = 0; }
                if ($in_upload && /^    enabled: true$/) { s/true/false/; }
            ' "$settings_file" 2>/dev/null || true
        fi
    fi

    log_success "Server directory setup complete"
}

# Start server and wait for pack generation
run_server() {
    log_info "Starting server (timeout: ${TIMEOUT}s)..."

    local log_file="$WORK_DIR/server.log"
    local pid_file="$WORK_DIR/server.pid"
    local pack_ready=false
    local start_time=$(date +%s)

    # Start server in background
    cd "$WORK_DIR"
    java -Xmx1G -Xms512M \
        -Dcom.mojang.eula.agree=true \
        -jar server.jar nogui \
        > "$log_file" 2>&1 &
    local server_pid=$!
    echo $server_pid > "$pid_file"

    log_info "Server started (PID: $server_pid)"
    log_info "Monitoring for pack generation completion..."

    # Monitor for completion
    while true; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))

        # Check timeout
        if [[ $elapsed -ge $TIMEOUT ]]; then
            log_error "Timeout reached (${TIMEOUT}s) - pack generation may not have completed"
            kill_server
            return 1
        fi

        # Check if server is still running
        if ! kill -0 $server_pid 2>/dev/null; then
            log_error "Server process died unexpectedly"
            if [[ -f "$log_file" ]]; then
                log_error "Last 20 lines of server log:"
                tail -20 "$log_file"
            fi
            return 1
        fi

        # Check for pack generation completion markers
        # Look for various completion indicators
        if grep -q "Resourcepack uploaded" "$log_file" 2>/dev/null || \
           grep -q "Oraxen.*pack.*generated\|pack/pack\.zip" "$log_file" 2>/dev/null || \
           [[ -f "$WORK_DIR/plugins/Oraxen/pack/pack.zip" ]]; then

            # Double check the pack.zip exists and has content
            if [[ -f "$WORK_DIR/plugins/Oraxen/pack/pack.zip" ]]; then
                local pack_size=$(stat -f%z "$WORK_DIR/plugins/Oraxen/pack/pack.zip" 2>/dev/null || stat -c%s "$WORK_DIR/plugins/Oraxen/pack/pack.zip" 2>/dev/null || echo "0")
                if [[ "$pack_size" -gt 1000 ]]; then
                    log_success "Pack generation detected! (${pack_size} bytes)"
                    pack_ready=true
                    break
                fi
            fi
        fi

        # Also check if the server finished startup (Done message)
        if grep -q "Done (.*s)! For help, type" "$log_file" 2>/dev/null; then
            log_verbose "Server startup complete, waiting for Oraxen pack generation..."
            # Give Oraxen a bit more time after server is done
            sleep 5

            # Check again
            if [[ -f "$WORK_DIR/plugins/Oraxen/pack/pack.zip" ]]; then
                local pack_size=$(stat -f%z "$WORK_DIR/plugins/Oraxen/pack/pack.zip" 2>/dev/null || stat -c%s "$WORK_DIR/plugins/Oraxen/pack/pack.zip" 2>/dev/null || echo "0")
                if [[ "$pack_size" -gt 1000 ]]; then
                    log_success "Pack generation complete! (${pack_size} bytes)"
                    pack_ready=true
                    break
                fi
            fi
        fi

        # Show progress every 10 seconds
        if [[ $((elapsed % 10)) -eq 0 && $elapsed -gt 0 ]]; then
            log_info "Still waiting... (${elapsed}s elapsed)"
            if [[ "$VERBOSE" == "true" ]] && [[ -f "$log_file" ]]; then
                echo "  Last log line: $(tail -1 "$log_file" 2>/dev/null || echo 'N/A')"
            fi
        fi

        sleep 2
    done

    # Stop the server gracefully
    kill_server

    if [[ "$pack_ready" == "true" ]]; then
        return 0
    else
        return 1
    fi
}

# Kill the server process
kill_server() {
    local pid_file="$WORK_DIR/server.pid"

    if [[ -f "$pid_file" ]]; then
        local pid=$(cat "$pid_file")
        if kill -0 $pid 2>/dev/null; then
            log_info "Stopping server (PID: $pid)..."

            # Try graceful shutdown first
            kill -TERM $pid 2>/dev/null || true
            sleep 3

            # Force kill if still running
            if kill -0 $pid 2>/dev/null; then
                log_warn "Server didn't stop gracefully, forcing..."
                kill -9 $pid 2>/dev/null || true
            fi

            log_success "Server stopped"
        fi
        rm -f "$pid_file"
    fi
}

# Extract the generated pack
extract_pack() {
    local pack_source="$WORK_DIR/plugins/Oraxen/pack/pack.zip"

    if [[ ! -f "$pack_source" ]]; then
        log_error "Pack file not found at $pack_source"
        return 1
    fi

    # Create output directory
    mkdir -p "$OUTPUT_DIR"

    # Copy pack.zip
    local output_file="$OUTPUT_DIR/pack.zip"
    cp "$pack_source" "$output_file"

    local pack_size=$(du -h "$output_file" | cut -f1)
    log_success "Pack extracted to: $output_file ($pack_size)"

    # Show pack contents summary
    if command -v unzip &> /dev/null; then
        local file_count=$(unzip -l "$output_file" 2>/dev/null | tail -1 | awk '{print $2}')
        log_info "Pack contains $file_count files"
    fi

    return 0
}

# Cleanup
cleanup() {
    if [[ -n "$WORK_DIR" && -d "$WORK_DIR" ]]; then
        # Always try to kill the server
        kill_server

        if [[ "$KEEP_SERVER" == "false" ]]; then
            log_info "Cleaning up server directory..."
            rm -rf "$WORK_DIR"
        else
            log_info "Server directory kept at: $WORK_DIR"
        fi
    fi
}

# Main function
main() {
    parse_args "$@"

    log_info "=== Oraxen Headless Pack Generator ==="
    log_info "Server: $SERVER_TYPE $MC_VERSION"
    log_info "Output: $OUTPUT_DIR"

    # Create temporary work directory
    WORK_DIR=$(mktemp -d -t oraxen-headless-XXXXXX)
    log_verbose "Work directory: $WORK_DIR"

    # Set trap for cleanup
    trap cleanup EXIT

    # Find Oraxen JAR
    find_oraxen_jar

    # Download server
    download_server

    # Setup server
    setup_server

    # Run server and wait for pack
    if run_server; then
        # Extract pack
        if extract_pack; then
            log_success "=== Pack generation complete! ==="
            exit 0
        else
            log_error "Failed to extract pack"
            exit 1
        fi
    else
        log_error "Pack generation failed"
        if [[ -f "$WORK_DIR/server.log" ]]; then
            log_error "Server log tail:"
            tail -50 "$WORK_DIR/server.log"
        fi
        exit 1
    fi
}

# Run main
main "$@"
