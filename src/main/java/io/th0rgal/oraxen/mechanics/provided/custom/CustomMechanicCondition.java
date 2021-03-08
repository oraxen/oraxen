package io.th0rgal.oraxen.mechanics.provided.custom;

import java.util.ArrayList;
import java.util.List;

public class CustomMechanicCondition {

    private final List<SingleCondition> conditions;

    public CustomMechanicCondition(List<String> conditionNames) {

        conditions = new ArrayList<>();
        for (String conditionString : conditionNames) {
            String[] conditionFields = conditionString.split(":");

            switch (conditionFields[0]) {

            case "has_permission":
                conditions.add(new HasPermission(conditionFields));
                break;

            default:
                break;
            }
        }
    }

    public boolean passes(CustomMechanicWrapper wrapper) {
        for (SingleCondition condition : conditions)
            if (!condition.passes(wrapper))
                return false;
        return true;
    }

}

abstract class SingleCondition {

    abstract boolean passes(CustomMechanicWrapper wrapper);

}

class HasPermission extends SingleCondition {

    private final CustomMechanicWrapper.Field target;
    private final String permission;

    public HasPermission(String[] fields) {
        this.target = CustomMechanicWrapper.Field.get(fields[1]);
        this.permission = fields[2];
    }

    @Override
    boolean passes(CustomMechanicWrapper wrapper) {
        return wrapper.getPlayer(target).hasPermission(permission);
    }
}