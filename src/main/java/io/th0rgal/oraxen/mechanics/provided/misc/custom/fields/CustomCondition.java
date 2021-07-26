package io.th0rgal.oraxen.mechanics.provided.misc.custom.fields;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomCondition {

    public final CustomConditionType type;
    private final List<String> params = new ArrayList<>();

    public CustomCondition(String action) {
        String[] actionParams = action.split(":");
        type = CustomConditionType.valueOf(actionParams[0]);
        params.addAll(Arrays.asList(actionParams).subList(1, actionParams.length));
    }

    public List<String> getParams() {
        return params;
    }

}
