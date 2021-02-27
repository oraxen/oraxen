package org.playuniverse.snowypine.command.argument.function;

public interface Mapper<I, O> {

    public O map(I input);

    public default O mapSafe(I input) {
        return input == null ? null : map(input);
    }

}
