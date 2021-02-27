package org.playuniverse.snowypine.command.argument.function;

import java.util.function.Supplier;

@FunctionalInterface
public interface Execute<E> {

    public E execute() throws Exception;

    public default E orElse(E value) {
        try {
            return execute();
        } catch (Exception ignore) {
            return value;
        }
    }

    public default E orElseGet(Supplier<E> supplier) {
        try {
            return execute();
        } catch (Exception ignore) {
            return supplier.get();
        }
    }

}
