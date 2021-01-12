package io.th0rgal.oraxen.mechanics.provided.custom;

import java.util.List;

public class CustomMechanicCondition {

    public CustomMechanicCondition(List<String> conditionNames) {

    }

    public boolean passes(CustomMechanicWrapper wrapper) {
        return true;
    }

}

abstract class SingleCondition {

    public SingleCondition(String conditionName) {

    }

    abstract boolean passes(CustomMechanicWrapper wrapper);

}
