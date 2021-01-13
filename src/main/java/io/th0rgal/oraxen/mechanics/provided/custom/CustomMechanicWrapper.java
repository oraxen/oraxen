package io.th0rgal.oraxen.mechanics.provided.custom;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomMechanicWrapper {


    public enum Field {
        PLAYER("player"),
        TARGET("target"),
        SERVER("server"),
        ALL("all"),
        CONSOLE("console");

        private final String name;
        private static final Map<String, Field> ENUM_MAP;

        Field(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        static {
            Map<String, Field> map = new ConcurrentHashMap<>();
            for (Field instance : Field.values())
                map.put(instance.getName().toLowerCase(), instance);
            ENUM_MAP = Collections.unmodifiableMap(map);
        }

        public static Field get(String name) {
            return ENUM_MAP.get(name.toLowerCase());
        }
    }


    private Player player;

    public void setPlayer(Player player) {
        this.player = player;
    }

    private Player target;

    public void setTarget(Player target) {
        this.target = target;
    }

    public Player getPlayer(Field field) {
        switch (field) {

            case PLAYER:
                return this.player;

            case TARGET:
                return this.target;

            default:
                return null;
        }
    }

}
