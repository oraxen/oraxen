package io.th0rgal.oraxen.mechanics.provided.misc.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum CustomCondition {

    HAS_PERMISSION;

    private final List<String> params = new ArrayList<>();

    public static CustomCondition get(String action) {
        String[] actionParams = action.split(":");
        CustomCondition customCondition = CustomCondition.valueOf(actionParams[0]);
        customCondition.params.addAll(Arrays.asList(actionParams).subList(1, actionParams.length));
        return customCondition;
    }

    public List<String> getParams() {
        return params;
    }

}
