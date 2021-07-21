package io.th0rgal.oraxen.utils;

import java.util.Optional;

@FunctionalInterface
public interface ValueProvider<E> {
    
    static <E> Optional<E> option(ValueProvider<E> provider) {
        return provider.optional();
    }

    E get() throws Exception;

    default Optional<E> optional() {
        try {
            return Optional.ofNullable(get());
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

}
