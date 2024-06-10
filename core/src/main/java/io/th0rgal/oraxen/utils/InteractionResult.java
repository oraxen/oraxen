package io.th0rgal.oraxen.utils;

public enum InteractionResult {
    SUCCESS,
    CONSUME,
    CONSUME_PARTIAL,
    PASS,
    FAIL;

    public boolean consumesAction() {
        return this == SUCCESS || this == CONSUME || this == CONSUME_PARTIAL;
    }

    public boolean shouldSwing() {
        return this == SUCCESS;
    }

    public boolean shouldAwardStats() {
        return this == SUCCESS || this == CONSUME;
    }

    public static InteractionResult sidedSuccess(boolean swingHand) {
        return swingHand ? SUCCESS : CONSUME;
    }

    public static InteractionResult fromNms(Enum<?> nmsEnum) {
        return valueOf(nmsEnum.name());
    }
}
