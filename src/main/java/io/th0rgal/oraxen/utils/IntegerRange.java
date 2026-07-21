package io.th0rgal.oraxen.utils;

/**
 * Simple integer range holding a lower and upper bound.
 * Simple replacement for external integer range helpers.
 */
public record IntegerRange(int lowerBound, int upperBound) {

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }
}
