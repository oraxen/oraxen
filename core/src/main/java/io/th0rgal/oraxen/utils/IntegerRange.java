package io.th0rgal.oraxen.utils;

/**
 * Simple integer range holding a lower and upper bound.
 * Replaces the CommandAPI IntegerRange to avoid runtime dependency on CommandAPI plugin.
 */
public record IntegerRange(int lowerBound, int upperBound) {

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }
}
