// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package io.th0rgal.oraxen.pack.slicer;

import net.kyori.adventure.key.Key;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.metadata.Metadata;
import team.unnamed.creative.metadata.animation.AnimationMeta;
import team.unnamed.creative.metadata.gui.GuiBorder;
import team.unnamed.creative.metadata.gui.GuiMeta;
import team.unnamed.creative.metadata.gui.GuiScaling;
import team.unnamed.creative.texture.Texture;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
public class OutputTexture {
    private static final Color REMOVED_MARKER = new Color(128, 0, 0, 128);

    public final Key key;
    private final Box box;
    private final List<UnaryOperator<BufferedImage>> transformers = new ArrayList<>();
    @Nullable
    private Metadata metadata;

    public OutputTexture(final String key, final Box box) {
        this.key = Key.key(key);
        this.box = box;
    }

    public OutputTexture(final Key key, final Box box) {
        this.key = key;
        this.box = box;
    }

    public void process(ResourcePack resourcePack, final Texture inputTexture, final BufferedImage image, final Graphics leftover) throws IOException {
        final int width = image.getWidth();
        final int height = image.getHeight();

        final int x = box.scaleX(width);
        final int y = box.scaleY(height);
        final int w = box.scaleW(width);
        final int h = box.scaleH(height);

        if (x == 0 && y == 0 && w == width && h == height && transformers.isEmpty())
            resourcePack.texture(key, inputTexture.data());
        else {
            BufferedImage subImage = image.getSubimage(x, y, w, h);
            for (final UnaryOperator<BufferedImage> op : transformers) subImage = op.apply(subImage);
            Slicer.writeImage(resourcePack, key, subImage);
        }

        if (inputTexture.hasMetadata()) resourcePack.texture(key).meta(inputTexture.meta());
        else if (metadata != null) resourcePack.texture(key).meta(metadata);

        leftover.setColor(REMOVED_MARKER);
        leftover.fillRect(x, y, w, h);
    }

    public OutputTexture apply(final UnaryOperator<BufferedImage> transform) {
        transformers.add(transform);
        return this;
    }

    public OutputTexture nineSliceMeta(int width, int height, int border) {
        this.metadata = Metadata.metadata().addPart(GuiMeta.of(GuiScaling.nineSlice(width, height, border))).build();
        return this;
    }

    public OutputTexture nineSliceMeta(int width, int height, GuiBorder border) {
        this.metadata = Metadata.metadata().addPart(GuiMeta.of(GuiScaling.nineSlice(width, height, border))).build();
        return this;
    }

    public OutputTexture animationMeta(AnimationMeta.Builder meta) {
        this.metadata = Metadata.metadata().addPart(meta.build()).build();
        return this;
    }
}
