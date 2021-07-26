package io.th0rgal.oraxen.mechanics.provided.misc.custom.fields;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CustomAction {

    public final CustomActionType type;
    private final List<String> params = new ArrayList<>();

    public CustomAction(String action) {
        String[] actionParams = action.split(":");
        type = CustomActionType.valueOf(actionParams[0]);
        params.addAll(Arrays.asList(actionParams).subList(1, actionParams.length));
    }

    public List<String> getParams() {
        return params;
    }

}
