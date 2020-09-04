package io.th0rgal.oraxen.command.condition;

import java.util.Arrays;

public class MixedCondition<E> implements ICondition<E> {

    @SafeVarargs
    public static <E> MixedCondition<E> mixed(ICondition<E>... conditions) {
        return new MixedCondition<>(conditions);
    }

    private final ICondition<E>[] conditions;

    public MixedCondition(ICondition<E>[] conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean isTrue(E sender) {
        return Arrays.stream(conditions).allMatch(condition -> condition.isTrue(sender));
    }

}
