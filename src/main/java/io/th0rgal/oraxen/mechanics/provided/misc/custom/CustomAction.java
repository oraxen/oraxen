package io.th0rgal.oraxen.mechanics.provided.misc.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum CustomAction {

    COMMAND,
    MESSAGE,
    ACTIONBAR;

    private final List<String> params = new ArrayList<>();

    public static CustomAction get(String action) {
        String[] actionParams = action.split(":");
        CustomAction customAction = CustomAction.valueOf(actionParams[0]);
        customAction.params.addAll(Arrays.asList(actionParams).subList(1, actionParams.length));
        return customAction;
    }

    public List<String> getParams() {
        return params;
    }

}
