package io.th0rgal.oraxen.utils;

import java.util.Optional;
import java.util.function.Consumer;

public interface ValueConsumer<E> extends Consumer<E> {

    static <E> Optional<E> option(ValueProvider<E> provider) {
        return provider.optional();
    }

    void consume(E value) throws Exception;

    default void accept(E value) {
        try {
            consume(value);
        } catch (Exception ignore) {
        }
    }

    default void accept(Optional<E> optional) {
        optional.ifPresent(this);
    }

}
