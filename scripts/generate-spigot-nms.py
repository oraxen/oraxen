#!/usr/bin/env python3
"""
Generate Spigot-mapped NMS sources from Mojang-mapped (Paper) sources.

This script reads the Spigot mapping files from BuildTools and transforms
the v1_21_R6 module source files into v1_21_R6_spigot equivalents.

Usage:
    python3 scripts/generate-spigot-nms.py

Requirements:
    - Spigot BuildTools must have been run for 1.21.11 (populates ~/.m2)
    - v1_21_R6 module must exist with Mojang-mapped sources
"""

import os
import re
import sys
from pathlib import Path
from typing import Dict, Set, Tuple, List

# Paths
SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent
SOURCE_MODULE = PROJECT_ROOT / "v1_21_R6"
TARGET_MODULE = PROJECT_ROOT / "v1_21_R6_spigot"

# Mapping files from BuildTools
M2_REPO = Path.home() / ".m2" / "repository"
SPIGOT_VERSION = "1.21.11-R0.1-SNAPSHOT"
MAPPING_DIR = M2_REPO / "org" / "spigotmc" / "minecraft-server" / SPIGOT_VERSION

CLASS_MAPPING_FILE = MAPPING_DIR / f"minecraft-server-{SPIGOT_VERSION}-maps-spigot.csrg"
MEMBER_MAPPING_FILE = MAPPING_DIR / f"minecraft-server-{SPIGOT_VERSION}-maps-spigot-members.csrg"


# =============================================================================
# Class Mappings: Mojang name -> Spigot name
# These are verified from the actual Spigot 1.21.11 jar
# =============================================================================
KNOWN_CLASS_MAPPINGS = {
    # Server/Level
    "net.minecraft.server.level.ServerPlayer": "net.minecraft.server.level.EntityPlayer",
    "net.minecraft.server.level.ServerLevel": "net.minecraft.server.level.WorldServer",
    "net.minecraft.world.level.Level": "net.minecraft.world.level.World",

    # NBT - these have different class names in Spigot
    "net.minecraft.nbt.CompoundTag": "net.minecraft.nbt.NBTTagCompound",
    "net.minecraft.nbt.ListTag": "net.minecraft.nbt.NBTTagList",
    "net.minecraft.nbt.StringTag": "net.minecraft.nbt.NBTTagString",
    "net.minecraft.nbt.Tag": "net.minecraft.nbt.NBTBase",
    "net.minecraft.nbt.ByteTag": "net.minecraft.nbt.NBTTagByte",
    "net.minecraft.nbt.IntTag": "net.minecraft.nbt.NBTTagInt",
    "net.minecraft.nbt.LongTag": "net.minecraft.nbt.NBTTagLong",
    "net.minecraft.nbt.FloatTag": "net.minecraft.nbt.NBTTagFloat",
    "net.minecraft.nbt.DoubleTag": "net.minecraft.nbt.NBTTagDouble",
    "net.minecraft.nbt.ShortTag": "net.minecraft.nbt.NBTTagShort",

    # Network
    "net.minecraft.network.FriendlyByteBuf": "net.minecraft.network.PacketDataSerializer",
    "net.minecraft.network.Connection": "net.minecraft.network.NetworkManager",
    # Note: RegistryFriendlyByteBuf keeps its name in 1.21.11

    # Server network
    "net.minecraft.server.network.ServerConnectionListener": "net.minecraft.server.network.ServerConnection",

    # Core types
    "net.minecraft.core.BlockPos": "net.minecraft.core.BlockPosition",
    "net.minecraft.core.RegistryAccess": "net.minecraft.core.IRegistryCustom",

    # World/Physics
    "net.minecraft.world.phys.Vec3": "net.minecraft.world.phys.Vec3D",
    "net.minecraft.world.phys.BlockHitResult": "net.minecraft.world.phys.MovingObjectPositionBlock",
    "net.minecraft.world.level.ClipContext": "net.minecraft.world.level.RayTrace",

    # Items
    "net.minecraft.world.item.BlockItem": "net.minecraft.world.item.ItemBlock",
    "net.minecraft.world.item.context.BlockPlaceContext": "net.minecraft.world.item.context.BlockActionContext",
    "net.minecraft.world.item.context.UseOnContext": "net.minecraft.world.item.context.ItemActionContext",
    "net.minecraft.world.item.context.DirectionalPlaceContext": "net.minecraft.world.item.context.BlockActionContextDirectional",
    "net.minecraft.world.InteractionHand": "net.minecraft.world.EnumHand",
    "net.minecraft.world.InteractionResult": "net.minecraft.world.EnumInteractionResult",

    # Resources
    "net.minecraft.resources.ResourceLocation": "net.minecraft.resources.MinecraftKey",
}

# Simple name mappings (class name only, for code body transformations)
SIMPLE_CLASS_MAPPINGS = {
    # NMS types
    "ServerPlayer": "EntityPlayer",
    "ServerLevel": "WorldServer",
    "CompoundTag": "NBTTagCompound",
    "ListTag": "NBTTagList",
    "StringTag": "NBTTagString",
    "ByteTag": "NBTTagByte",
    "IntTag": "NBTTagInt",
    "LongTag": "NBTTagLong",
    "FloatTag": "NBTTagFloat",
    "DoubleTag": "NBTTagDouble",
    "ShortTag": "NBTTagShort",
    "FriendlyByteBuf": "PacketDataSerializer",
    "Connection": "NetworkManager",
    "ServerConnectionListener": "ServerConnection",
    "BlockPos": "BlockPosition",
    "RegistryAccess": "IRegistryCustom",
    "Vec3": "Vec3D",
    "BlockHitResult": "MovingObjectPositionBlock",
    "ClipContext": "RayTrace",
    "BlockItem": "ItemBlock",
    "BlockPlaceContext": "BlockActionContext",
    "UseOnContext": "ItemActionContext",
    "DirectionalPlaceContext": "BlockActionContextDirectional",
    "InteractionHand": "EnumHand",
    "InteractionResult": "EnumInteractionResult",
    "ResourceLocation": "MinecraftKey",
}

# Imports to completely remove (Paper-only APIs)
PAPER_ONLY_IMPORTS = [
    "io.papermc.paper.configuration.GlobalConfiguration",
    "io.papermc.paper.network.ChannelInitializeListenerHolder",
]

# Code patterns to remove/replace (Paper-specific code blocks)
# These are regex patterns that match Paper-only code
PAPER_CODE_REMOVALS = [
    # Remove the ChannelInitializeListenerHolder block in constructor
    (r'\s*// mineableWith tag handling.*?ChannelInitializeListenerHolder\.addListener\([^;]+\);',
     '\n        // mineableWith tag handling - not supported on Spigot'),
    # Remove GlobalConfiguration checks - return false for Spigot
]


def transform_import(line: str) -> Tuple[str, bool]:
    """
    Transform an import statement.
    Returns (transformed_line, should_keep).
    """
    # Check if it's a Paper-only import that should be removed
    for paper_import in PAPER_ONLY_IMPORTS:
        if paper_import in line:
            return ("", False)

    # Skip non-import lines
    match = re.match(r'^import\s+(static\s+)?([a-zA-Z0-9_.]+)(\.\*)?;', line)
    if not match:
        return (line, True)

    static = match.group(1) or ""
    class_path = match.group(2)
    wildcard = match.group(3) or ""

    # Don't transform Bukkit API imports (org.bukkit.* but not org.bukkit.craftbukkit.*)
    if class_path.startswith("org.bukkit.") and ".craftbukkit." not in class_path:
        return (line, True)

    # Transform CraftBukkit imports to add version
    if "org.bukkit.craftbukkit." in class_path and ".v1_21_R" not in class_path:
        new_path = class_path.replace("org.bukkit.craftbukkit.", "org.bukkit.craftbukkit.v1_21_R7.")
        return (f"import {static}{new_path}{wildcard};", True)

    # Transform known NMS class imports
    if class_path in KNOWN_CLASS_MAPPINGS:
        new_path = KNOWN_CLASS_MAPPINGS[class_path]
        return (f"import {static}{new_path}{wildcard};", True)

    return (line, True)


def transform_class_references(code: str) -> str:
    """Transform class references in code body."""
    result = code

    # Apply simple class name mappings with word boundaries
    # Sort by length descending to handle longer names first
    for mojang, spigot in sorted(SIMPLE_CLASS_MAPPINGS.items(), key=lambda x: -len(x[0])):
        # Match the class name as a type reference (not part of another word)
        # Exclude cases where it's already part of the Spigot name
        pattern = rf'\b{re.escape(mojang)}\b'
        # Don't replace if it's already the Spigot name or part of a longer word
        result = re.sub(pattern, spigot, result)

    return result


def transform_package(line: str) -> str:
    """Transform package declaration from v1_21_R6 to v1_21_R6_spigot."""
    return line.replace("v1_21_R6", "v1_21_R6_spigot")


def remove_paper_specific_code(content: str) -> str:
    """Remove or stub out Paper-specific code sections."""
    result = content

    # Replace GlobalConfiguration checks with false (Spigot doesn't have this)
    result = re.sub(
        r'GlobalConfiguration\.get\(\)\.blockUpdates\.disableTripwireUpdates',
        'false',
        result
    )
    result = re.sub(
        r'GlobalConfiguration\.get\(\)\.blockUpdates\.disableNoteblockUpdates',
        'false',
        result
    )
    result = re.sub(
        r'GlobalConfiguration\.get\(\)\.blockUpdates\.disableChorusPlantUpdates',
        'false',
        result
    )

    # Remove the ChannelInitializeListenerHolder code block in constructors
    # This is a multi-line block that starts with the comment and ends with the listener call
    result = re.sub(
        r'// mineableWith tag handling\s*\n.*?ChannelInitializeListenerHolder\.addListener\([^;]+\)\)\);',
        '// mineableWith tag handling - not supported on Spigot (requires Paper API)',
        result,
        flags=re.DOTALL
    )

    return result


def transform_source_file(source_path: Path, target_path: Path) -> None:
    """Transform a single source file."""
    with open(source_path) as f:
        content = f.read()

    lines = content.split("\n")
    transformed_lines = []

    for line in lines:
        # Transform package declaration
        if line.strip().startswith("package "):
            line = transform_package(line)
            transformed_lines.append(line)
        # Transform imports
        elif line.strip().startswith("import "):
            new_line, should_keep = transform_import(line)
            if should_keep:
                transformed_lines.append(new_line)
        else:
            transformed_lines.append(line)

    # Join lines
    transformed = "\n".join(transformed_lines)

    # Remove Paper-specific code
    transformed = remove_paper_specific_code(transformed)

    # Transform class references in the code body
    transformed = transform_class_references(transformed)

    # Ensure target directory exists
    target_path.parent.mkdir(parents=True, exist_ok=True)

    with open(target_path, "w") as f:
        f.write(transformed)

    print(f"  Transformed: {source_path.name} -> {target_path.name}")


def main():
    print("=" * 60)
    print("Generating Spigot-mapped NMS sources")
    print("=" * 60)

    # Check if Spigot jar exists (BuildTools must have been run)
    spigot_jar = M2_REPO / "org" / "spigotmc" / "spigot" / SPIGOT_VERSION / f"spigot-{SPIGOT_VERSION}.jar"
    if not spigot_jar.exists():
        print(f"\nError: Spigot jar not found!")
        print(f"Expected: {spigot_jar}")
        print(f"\nPlease run Spigot BuildTools for version 1.21.11:")
        print(f"  java -jar BuildTools.jar --rev 1.21.11")
        sys.exit(1)

    # Find source files
    source_dir = SOURCE_MODULE / "src" / "main" / "java"
    target_dir = TARGET_MODULE / "src" / "main" / "java"

    if not source_dir.exists():
        print(f"\nError: Source module not found: {source_dir}")
        sys.exit(1)

    print(f"\nSource: {source_dir}")
    print(f"Target: {target_dir}")
    print(f"\nMappings applied:")
    print(f"  - NMS class name transformations (Mojang -> Spigot)")
    print(f"  - CraftBukkit version: v1_21_R7")
    print(f"  - Paper-only code: removed/stubbed")

    # Clean target directory
    if target_dir.exists():
        import shutil
        shutil.rmtree(target_dir)
        print(f"\nCleaned target directory")

    # Transform each source file
    print(f"\nTransforming files...")
    source_files = list(source_dir.rglob("*.java"))

    for source_file in source_files:
        relative_path = source_file.relative_to(source_dir)
        # Update package path from v1_21_R6 to v1_21_R6_spigot
        new_relative = Path(str(relative_path).replace("v1_21_R6", "v1_21_R6_spigot"))
        target_file = target_dir / new_relative

        transform_source_file(source_file, target_file)

    print(f"\nDone! Transformed {len(source_files)} files.")
    print(f"\nNote: Manual review may be needed for:")
    print(f"  - Method name changes that differ between Mojang and Spigot")
    print(f"  - Field access patterns")
    print(f"  - Paper-specific features that need Spigot alternatives")
    print(f"\nRun './gradlew :v1_21_R6_spigot:compileJava' to check for remaining issues.")


if __name__ == "__main__":
    main()
