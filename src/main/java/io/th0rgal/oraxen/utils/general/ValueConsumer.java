package io.th0rgal.oraxen.utils.general;

import java.util.Optional;
import java.util.function.Consumer;

public interface ValueConsumer<E> extends Consumer<E> {

    public static <E> Optional<E> option(ValueProvider<E> provider) {
        return provider.optional();
    }

    void consume(E value) throws Exception;

    public default void accept(E value) {
        try {
            consume(value);
        } catch (Exception ignore) {
            return;
        }
    }

    public default void accept(Optional<E> optional) {
        optional.ifPresent(value -> accept(value));
    }

}
