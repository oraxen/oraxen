package org.playuniverse.snowypine.command.condition;

import java.util.Arrays;

public class MixedAndCondition<E> implements ICondition<E> {

    @SafeVarargs
    public static <E> MixedAndCondition<E> mixed(ICondition<E>... conditions) {
        return new MixedAndCondition<>(conditions);
    }

    private final ICondition<E>[] conditions;

    public MixedAndCondition(ICondition<E>[] conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean isTrue(E sender) {
        return Arrays.stream(conditions).allMatch(condition -> condition.isTrue(sender));
    }

}
