package io.th0rgal.oraxen.command.argument.function;

public interface Mapper<I, O> {

    public O map(I input);

    public default O mapSafe(I input) {
        return input == null ? null : map(input);
    }

}
