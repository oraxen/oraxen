// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package io.th0rgal.oraxen.pack.generation.slicer;

import io.th0rgal.oraxen.utils.logs.Logs;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
public class OutputFile {
    private static final Color REMOVED_MARKER = new Color(128, 0, 0, 128);

    public final String path;
    private final Box box;
    private final List<UnaryOperator<BufferedImage>> transformers = new ArrayList<>();
    @Nullable
    private String metadata;

    public OutputFile(final String path, final Box box) {
        this.path = path;
        this.box = box;
    }

    public void process(final Path root, final Path imagePath, final BufferedImage image, final Graphics leftover) throws IOException {
        final int width = image.getWidth();
        final int height = image.getHeight();

        final Path outputPath = root.resolve(path);
        final int x = box.scaleX(width);
        final int y = box.scaleY(height);
        final int w = box.scaleW(width);
        final int h = box.scaleH(height);

        Files.createDirectories(outputPath.getParent());

        if (x == 0 && y == 0 && w == width && h == height && transformers.isEmpty())
            Files.copy(imagePath, outputPath, StandardCopyOption.REPLACE_EXISTING);
        else {
            BufferedImage subImage = image.getSubimage(x, y, w, h);
            for (final UnaryOperator<BufferedImage> op : transformers) subImage = op.apply(subImage);
            Slicer.writeImage(outputPath, subImage);
        }

        final Path inputMetaPath = getMetaPath(imagePath);
        if (Files.exists(inputMetaPath))
            Files.copy(inputMetaPath, getMetaPath(outputPath), StandardCopyOption.REPLACE_EXISTING);
        else if (metadata != null)
            Files.writeString(getMetaPath(outputPath), metadata);

        leftover.setColor(REMOVED_MARKER);
        leftover.fillRect(x, y, w, h);
    }

    private static Path getMetaPath(final Path path) {
        return path.resolveSibling(path.getFileName().toString() + ".mcmeta");
    }

    public OutputFile apply(final UnaryOperator<BufferedImage> transform) {
        transformers.add(transform);
        return this;
    }

    public OutputFile metadata(final String metadata) {
        this.metadata = metadata;
        return this;
    }
}
