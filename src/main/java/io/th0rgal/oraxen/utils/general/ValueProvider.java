package io.th0rgal.oraxen.utils.general;

import java.util.Optional;

@FunctionalInterface
public interface ValueProvider<E> {

    public static <E> Optional<E> option(ValueProvider<E> provider) {
        return provider.optional();
    }

    public E get() throws Exception;

    public default Optional<E> optional() {
        try {
            return Optional.ofNullable(get());
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

}
