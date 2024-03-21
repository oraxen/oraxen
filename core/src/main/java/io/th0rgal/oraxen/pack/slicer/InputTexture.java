// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package io.th0rgal.oraxen.pack.slicer;

import net.kyori.adventure.key.Key;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.texture.Texture;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
public class InputTexture {
    public final Key key;
    public final List<OutputFile> outputs = new ArrayList<>();

    public InputTexture(final Key key) {
        this.key = key;
    }

    public InputTexture outputs(final OutputFile... files) {
        Collections.addAll(outputs, files);
        return this;
    }

    public void process(ResourcePack resourcePack) throws IOException {
        Texture texture = resourcePack.texture(key);
        if (texture == null) return;

        try (final InputStream is = new ByteArrayInputStream(texture.data().toByteArray())) {
            final BufferedImage image = ImageIO.read(is);
            final BufferedImage leftoverImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            final Graphics2D leftoverGraphics = leftoverImage.createGraphics();
            leftoverGraphics.drawImage(image, 0, 0, null);

            for (final OutputFile outputFile : outputs) {
                outputFile.process(resourcePack, texture, image, leftoverGraphics);
            }

            leftoverGraphics.dispose();
        }
    }
}
