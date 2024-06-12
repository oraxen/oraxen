// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package io.th0rgal.oraxen.pack.slicer;

import net.kyori.adventure.key.Key;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.Writable;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

@ParametersAreNonnullByDefault
public class Slicer {
    private final ResourcePack resourcePack;

    public Slicer(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
    }

//    public static Slicer parse(final String[] argv) {
//        final int argc = argv.length;
//        if (argc != 2 && argc != 3) {
//            throw new IllegalArgumentException("Usage: <input dir or zip> <output dir> [<leftover dir>]");
//        }
//
//        final Path inputPath = Paths.get(argv[0]);
//        final Path outputPath = Paths.get(argv[1]);
//        final Path leftoverPath = argc == 3 ? Paths.get(argv[2]) : null;
//
//        return new Slicer();
//    }

    public static void writeImage(ResourcePack resourcePack, final Key newKey, final BufferedImage image) throws IOException {
        if (resourcePack.texture(newKey) != null) return;
        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", os);
            resourcePack.texture(newKey, Writable.bytes(os.toByteArray()));
        }
    }

    public void process(final Collection<InputTexture> inputs) throws IOException {
        for (InputTexture input : inputs) {
            input.process(resourcePack);
        }
    }
}
