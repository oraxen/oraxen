/*
 * This file is part of creative, licensed under the MIT license
 *
 * Copyright (c) 2021-2023 Unnamed Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.th0rgal.oraxen.pack.creative;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import team.unnamed.creative.sound.SoundRegistry;
import team.unnamed.creative.texture.Texture;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.StringJoiner;

final class MinecraftResourcePackStructure {

    // file extensions
    public static final String TEXTURE_EXTENSION = ".png";
    public static final String METADATA_EXTENSION = ".mcmeta";
    public static final String OBJECT_EXTENSION = ".json";

    public static final String FILE_SEPARATOR = "/";

    // special root files
    public static final String PACK_METADATA_FILE = "pack" + METADATA_EXTENSION;
    public static final String PACK_ICON_FILE = "pack" + TEXTURE_EXTENSION;

    // overlays folder (overlays/<directory>/...)
    public static final String OVERLAYS_FOLDER = "overlays";

    // assets folder (assets/<namespace>)
    public static final String ASSETS_FOLDER = "assets";

    // known special folders
    public static final String OPTIFINE_FOLDER = "optifine";

    // sounds.json file (assets/<namespace>/sounds.json)
    public static final String SOUNDS_FILE = "sounds" + OBJECT_EXTENSION;

    // resource categories (assets/<namespace>/<category>/...)
    public static final String TEXTURES_FOLDER = "textures";
    public static final String TEXTS_FOLDER = "texts";

    private MinecraftResourcePackStructure() {
    }

    public static String pathOf(SoundRegistry registry) {
        // assets/<namespace>/sounds.json
        return path(ASSETS_FOLDER, registry.namespace(), SOUNDS_FILE);
    }

    public static String pathOf(Texture texture) {
        Key key = texture.key();
        // assets/<namespace>/textures/<path>
        return path(ASSETS_FOLDER, key.namespace(), TEXTURES_FOLDER, key.value());
    }

    public static String pathOfMeta(Texture texture) {
        // assets/<namespace>/textures/<path>.png.mcmeta
        return withCategory(TEXTURES_FOLDER, texture, METADATA_EXTENSION);
    }

    public static Queue<String> tokenize(String path) {
        return new LinkedList<>(Arrays.asList(path.split(FILE_SEPARATOR)));
    }

    public static String path(String... path) {
        StringJoiner joiner = new StringJoiner(FILE_SEPARATOR);
        for (String part : path) {
            joiner.add(part);
        }
        return joiner.toString();
    }

    public static String path(Iterable<String> path) {
        StringJoiner joiner = new StringJoiner(FILE_SEPARATOR);
        for (String part : path) {
            joiner.add(part);
        }
        return joiner.toString();
    }

    // helper methods
    public static String withCategory(String categoryFolder, Keyed resource, String extension) {
        Key key = resource.key();
        // assets/<namespace>/<category>/<path>.<extension>
        return path(ASSETS_FOLDER, key.namespace(), categoryFolder, key.value() + extension);
    }

}
