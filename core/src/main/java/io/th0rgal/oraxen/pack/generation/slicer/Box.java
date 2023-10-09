// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package io.th0rgal.oraxen.pack.generation.slicer;

public record Box(int x, int y, int w, int h, int totalW, int totalH) {
    public int scaleX(final int imgWidth) {
        return x * imgWidth / totalW;
    }

    public int scaleY(final int imgHeight) {
        return y * imgHeight / totalH;
    }

    public int scaleW(final int imgWidth) {
        return w * imgWidth / totalW;
    }

    public int scaleH(final int imgHeight) {
        return h * imgHeight / totalH;
    }
}
