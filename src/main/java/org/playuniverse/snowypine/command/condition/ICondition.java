package org.playuniverse.snowypine.command.condition;

import java.util.function.Predicate;

public interface ICondition<E> extends Predicate<E> {

    @Override
    default boolean test(E input) {
        return isTrue(input);
    }

    public default boolean isFalse(E input) {
        return !isTrue(input);
    }

    public boolean isTrue(E input);

}
