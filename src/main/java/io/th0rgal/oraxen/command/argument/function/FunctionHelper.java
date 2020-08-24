package io.th0rgal.oraxen.command.argument.function;

import java.util.Optional;
import java.util.function.Supplier;

public interface FunctionHelper {

    public static <E> Optional<E> of(Execute<E> execute, E value) {
        return Optional.ofNullable(execute.orElse(value));
    }

    public static <E> Optional<E> ofGet(Execute<E> execute, Supplier<E> supplier) {
        return Optional.ofNullable(execute.orElseGet(supplier));
    }

    public static <E, T> Optional<T> map(Execute<E> execute, E value, Mapper<E, T> mapper) {
        return Optional.ofNullable(mapper.mapSafe(execute.orElse(value)));
    }

    public static <E, T> Optional<T> mapGet(Execute<E> execute, Supplier<E> supplier, Mapper<E, T> mapper) {
        return Optional.ofNullable(mapper.mapSafe(execute.orElseGet(supplier)));
    }

}
