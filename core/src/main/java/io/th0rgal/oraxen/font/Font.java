package io.th0rgal.oraxen.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public record Font(String type, String file, Float shift_x, Float shift_y,
                   Float size, Float oversample) {

    public JsonObject toJson() {
        final JsonObject output = new JsonObject();
        final JsonArray shift = new JsonArray();
        shift.add(shift_x);
        shift.add(shift_y);
        output.addProperty("type", type);
        output.addProperty("file", file);
        output.add("shift", shift);
        output.addProperty("size", size);
        output.addProperty("oversample", oversample);
        return output;
    }

}
