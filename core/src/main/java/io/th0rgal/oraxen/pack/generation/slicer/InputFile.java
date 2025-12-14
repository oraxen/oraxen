// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package io.th0rgal.oraxen.pack.generation.slicer;

import io.th0rgal.oraxen.utils.logs.Logs;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
public class InputFile {
    public final String path;
    public final List<OutputFile> outputs = new ArrayList<>();

    public InputFile(final String path) {
        this.path = path;
    }

    public InputFile outputs(final OutputFile... files) {
        Collections.addAll(outputs, files);
        return this;
    }

    public void process(final Path inputRoot, final Path outputRoot, @Nullable final Path leftoverRoot) throws IOException {
        final Path inputPath = inputRoot.resolve(this.path);
        if (Files.exists(inputPath)) {
            try (final InputStream is = Files.newInputStream(inputPath)) {
                final BufferedImage image = ImageIO.read(is);
                if (image == null) {
                    Logs.logWarning("Skipping unreadable image during slicing: " + inputPath);
                    return;
                }
                final BufferedImage leftoverImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                final Graphics2D leftoverGraphics = leftoverImage.createGraphics();
                leftoverGraphics.drawImage(image, 0, 0, null);

                for (final OutputFile outputFile : outputs) {
                    outputFile.process(outputRoot, inputPath, image, leftoverGraphics);
                }

                leftoverGraphics.dispose();

                if (leftoverRoot != null) {
                    final Path leftoverPath = leftoverRoot.resolve(this.path);
                    Slicer.writeImage(leftoverPath, leftoverImage);
                }
            }
        }
    }
}
