package org.playuniverse.snowypine.command.condition;

import java.util.Arrays;

public class MixedOrCondition<E> implements ICondition<E> {

    @SafeVarargs
    public static <E> MixedOrCondition<E> mixed(ICondition<E>... conditions) {
        return new MixedOrCondition<>(conditions);
    }

    private final ICondition<E>[] conditions;

    public MixedOrCondition(ICondition<E>[] conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean isTrue(E sender) {
        return Arrays.stream(conditions).anyMatch(condition -> condition.isTrue(sender));
    }

}
