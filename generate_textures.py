#!/usr/bin/env python3
"""Generate Minecraft-style pixel art textures for Oraxen blocks and items using OpenAI's DALL-E API."""

import base64
import os
import json
import requests
from pathlib import Path

# Read API key from secrets
with open('secrets.json', 'r') as f:
    secrets = json.load(f)
    API_KEY = secrets['shared']['openai']['api_key']

TEXTURE_DIR = Path("core/src/main/resources/pack/textures")
BLOCK_DIR = TEXTURE_DIR / "block"
ITEM_DIR = TEXTURE_DIR / "item"

# Ensure directories exist
BLOCK_DIR.mkdir(parents=True, exist_ok=True)
ITEM_DIR.mkdir(parents=True, exist_ok=True)


def generate_texture(prompt: str, output_path: Path, size: str = "1024x1024"):
    """Generate a texture using OpenAI's image generation API."""
    print(f"Generating: {output_path.name}")

    full_prompt = f"""Create a 16x16 pixel art texture for Minecraft in the style of vanilla Minecraft textures.
    The image should be exactly 16x16 pixels, pixelated, with the classic Minecraft aesthetic.
    No text, no watermarks, seamless tileable texture.

    Subject: {prompt}"""

    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }

    data = {
        "model": "dall-e-3",
        "prompt": full_prompt,
        "n": 1,
        "size": size,
        "quality": "standard",
        "response_format": "b64_json"
    }

    response = requests.post(
        "https://api.openai.com/v1/images/generations",
        headers=headers,
        json=data
    )

    if response.status_code != 200:
        print(f"Error: {response.status_code} - {response.text}")
        return False

    result = response.json()
    image_data = base64.b64decode(result['data'][0]['b64_json'])

    with open(output_path, 'wb') as f:
        f.write(image_data)

    print(f"Saved: {output_path}")
    return True


def main():
    textures = [
        # Custom wood plank textures for copper blocks
        ("custom_oak_planks", BLOCK_DIR, "Dark oak wooden planks with a rustic, weathered look. Rich brown color with visible wood grain."),
        ("custom_oak_planks_2", BLOCK_DIR, "Lighter oak wooden planks with subtle knots and natural wood texture."),
        ("custom_oak_planks_3", BLOCK_DIR, "Medium brown wooden planks with a warm tone and detailed grain pattern."),
        ("custom_oak_planks_4", BLOCK_DIR, "Pale wooden planks with a clean, fresh appearance."),

        # Backpack textures (item textures)
        ("leather_backpack", ITEM_DIR, "A brown leather backpack/satchel viewed from behind, with straps and buckles. Medieval fantasy style."),
        ("adventurer_backpack", ITEM_DIR, "A green adventurer's backpack with multiple pockets, seen from behind. Explorer/hiking style."),
        ("explorer_pack", ITEM_DIR, "A blue explorer's pack/rucksack with rope and tools attached, seen from behind."),
    ]

    for name, directory, prompt in textures:
        output_path = directory / f"{name}.png"
        if not output_path.exists():
            success = generate_texture(prompt, output_path)
            if not success:
                print(f"Failed to generate {name}")
        else:
            print(f"Skipping {name} (already exists)")


if __name__ == "__main__":
    main()
