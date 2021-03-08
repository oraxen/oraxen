package io.th0rgal.oraxen.settings.update;

import org.bukkit.configuration.file.YamlConfiguration;

import io.th0rgal.oraxen.settings.Update;

public class ExampleUpdate {

    @Update(path = { "settings" }, version = 20200901145120L)
    public static void updateSomething(YamlConfiguration config) {
        // Do updates here
        System.out.println(config.getName() + " hehe");
    }

}
