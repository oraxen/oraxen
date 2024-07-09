package io.th0rgal.oraxen.pack;

import org.bukkit.Material;
import team.unnamed.creative.base.Vector3Float;
import team.unnamed.creative.model.ItemTransform;

import java.util.LinkedHashMap;

public class DisplayProperties {

    public static LinkedHashMap<ItemTransform.Type, ItemTransform> SHIELD;
    public static LinkedHashMap<ItemTransform.Type, ItemTransform> BOW;
    public static LinkedHashMap<ItemTransform.Type, ItemTransform> CROSSBOW;
    public static LinkedHashMap<ItemTransform.Type, ItemTransform> CHEST;
    public static LinkedHashMap<ItemTransform.Type, ItemTransform> LARGE_AMETHYST_BUD;
    public static LinkedHashMap<ItemTransform.Type, ItemTransform> SMALL_DRIPLEAF;
    public static LinkedHashMap<ItemTransform.Type, ItemTransform> BIG_DRIPLEAF;
    public static LinkedHashMap<ItemTransform.Type, ItemTransform> HANGING_ROOTS;
    public static LinkedHashMap<ItemTransform.Type, ItemTransform> DRAGON_HEAD;
    public static LinkedHashMap<ItemTransform.Type, ItemTransform> CONDUIT;
    public static LinkedHashMap<ItemTransform.Type, ItemTransform> DECORATED_POT;

    public static LinkedHashMap<ItemTransform.Type, ItemTransform> fromMaterial(Material material) {
        return switch (material) {
            case SHIELD -> DisplayProperties.SHIELD;
            case BOW -> DisplayProperties.BOW;
            case CROSSBOW -> DisplayProperties.CROSSBOW;
            case CHEST -> DisplayProperties.CHEST;
            case LARGE_AMETHYST_BUD -> DisplayProperties.LARGE_AMETHYST_BUD;
            case SMALL_DRIPLEAF -> DisplayProperties.SMALL_DRIPLEAF;
            case BIG_DRIPLEAF -> DisplayProperties.BIG_DRIPLEAF;
            case HANGING_ROOTS -> DisplayProperties.HANGING_ROOTS;
            case DRAGON_HEAD -> DisplayProperties.DRAGON_HEAD;
            case CONDUIT -> DisplayProperties.CONDUIT;
            case DECORATED_POT -> DisplayProperties.DECORATED_POT;
            default -> new LinkedHashMap<>();
        };
    }

    static {
        SHIELD = new LinkedHashMap<>();
        SHIELD.put(ItemTransform.Type.THIRDPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(0, 90, 0), new Vector3Float(10, 6, 12), new Vector3Float(1,1,1)));
        SHIELD.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 90, 0), new Vector3Float(10, 6, -4), new Vector3Float(1,1,1)));
        SHIELD.put(ItemTransform.Type.FIRSTPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(0, 180, 5), new Vector3Float(10, 0, -10), new Vector3Float(1.25f,1.25f,1.25f)));
        SHIELD.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 180, 5), new Vector3Float(-10, 2, -10), new Vector3Float(1.25f,1.25f,1.25f)));
        SHIELD.put(ItemTransform.Type.GUI, ItemTransform.transform(new Vector3Float(15, -25, -5), new Vector3Float(2, 3, 0), new Vector3Float(0.65f,0.65f,0.65f)));
        SHIELD.put(ItemTransform.Type.FIXED, ItemTransform.transform(new Vector3Float(0, 180, 0), new Vector3Float(-4.5f, 4.5f, -5), new Vector3Float(0.55f,0.55f,0.55f)));
        SHIELD.put(ItemTransform.Type.GROUND, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(2, 4, 2), new Vector3Float(0.25f,0.25f,0.25f)));

        BOW = new LinkedHashMap<>();
        BOW.put(ItemTransform.Type.THIRDPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(-80, -280, 40), new Vector3Float(-1, -2, 2.5f), new Vector3Float(0.9f, 0.9f, 0.9f)));
        BOW.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(-80, 260, -40), new Vector3Float(-1, -2, 2.5f), new Vector3Float(0.9f, 0.9f, 0.9f)));
        BOW.put(ItemTransform.Type.FIRSTPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(0, 90, -25), new Vector3Float(1.13f, 3.2f, 1.13f), new Vector3Float(0.68f, 0.68f, 0.68f)));
        BOW.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, -90, 25), new Vector3Float(1.13f, 3.2f, 1.13f), new Vector3Float(0.68f, 0.68f, 0.68f)));

        CROSSBOW = new LinkedHashMap<>();
        CROSSBOW.put(ItemTransform.Type.THIRDPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(-90, 0, 30), new Vector3Float(2, 0.1f, -3), new Vector3Float(0.9f, 0.9f, 0.9f)));
        CROSSBOW.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(-90, 0, -60), new Vector3Float(2, 0.1f, -3), new Vector3Float(0.9f, 0.9f, 0.9f)));
        CROSSBOW.put(ItemTransform.Type.FIRSTPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(-90, 0, 35), new Vector3Float(1.13f, 3.2f, 1.13f), new Vector3Float(0.68f, 0.68f, 0.68f)));
        CROSSBOW.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(-90, 0, -55), new Vector3Float(1.13f, 3.2f, 1.13f), new Vector3Float(0.68f, 0.68f, 0.68f)));

        CHEST = new LinkedHashMap<>();
        CHEST.put(ItemTransform.Type.GUI, ItemTransform.transform(new Vector3Float(30, 45, 0), new Vector3Float(0, 0, 0), new Vector3Float(0.625f, 0.625f, 0.625f)));
        CHEST.put(ItemTransform.Type.GROUND, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(0, 3, 0), new Vector3Float(0.25f, 0.25f, 0.25f)));
        CHEST.put(ItemTransform.Type.HEAD, ItemTransform.transform(new Vector3Float(0, 180, 0), new Vector3Float(0, 0, 0), new Vector3Float(1, 1, 1)));
        CHEST.put(ItemTransform.Type.FIXED, ItemTransform.transform(new Vector3Float(0, 180, 0), new Vector3Float(0, 0, 0), new Vector3Float(0.5f, 0.5f, 0.5f)));
        CHEST.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(75, 315, 0), new Vector3Float(0, 2.5f, 0), new Vector3Float(0.375f, 0.375f, 0.375f)));
        CHEST.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 315, 0), new Vector3Float(0, 0, 0), new Vector3Float(0.4f, 0.4f, 0.4f)));

        LARGE_AMETHYST_BUD = new LinkedHashMap<>();
        LARGE_AMETHYST_BUD.put(ItemTransform.Type.FIXED, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(0, 4, 0), new Vector3Float(1, 1, 1)));

        SMALL_DRIPLEAF = new LinkedHashMap<>();
        SMALL_DRIPLEAF.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(0, 4, 1f), new Vector3Float(0.55f, 0.55f, 0.55f)));
        SMALL_DRIPLEAF.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 45, 0), new Vector3Float(0, 3.2f, 0), new Vector3Float(0.4f, 0.4f, 0.4f)));

        BIG_DRIPLEAF = new LinkedHashMap<>();
        BIG_DRIPLEAF.put(ItemTransform.Type.GUI, ItemTransform.transform(new Vector3Float(30, 225, 0), new Vector3Float(0, -2, 0), new Vector3Float(0.625f, 0.625f, 0.625f)));
        BIG_DRIPLEAF.put(ItemTransform.Type.FIXED, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(0, 0, -1f), new Vector3Float(0.5f, 0.5f, 0.5f)));
        BIG_DRIPLEAF.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(0, 1, 0), new Vector3Float(0.55f, 0.55f, 0.55f)));
        BIG_DRIPLEAF.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(1.13f, 0, 1.13f), new Vector3Float(0.68f, 0.68f, 0.68f)));

        HANGING_ROOTS = new LinkedHashMap<>();
        HANGING_ROOTS.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(0, 0, 1f), new Vector3Float(0.55f, 0.55f, 0.55f)));
        HANGING_ROOTS.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, -90, 25), new Vector3Float(1.13f, 0, 1.13f), new Vector3Float(0.68f, 0.68f, 0.68f)));

        DRAGON_HEAD = new LinkedHashMap<>();
        DRAGON_HEAD.put(ItemTransform.Type.GUI, ItemTransform.transform(new Vector3Float(30, 45, 0), new Vector3Float(-2, 2, 0), new Vector3Float(0.6f, 0.6f, 0.6f)));
        DRAGON_HEAD.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 180, 0), new Vector3Float(0, -1, 2f), new Vector3Float(0.5f, 0.5f, 0.5f)));

        CONDUIT = new LinkedHashMap<>();
        CONDUIT.put(ItemTransform.Type.GUI, ItemTransform.transform(new Vector3Float(30, 45, 0), new Vector3Float(0, 0, 0), new Vector3Float(1, 1, 1)));
        CONDUIT.put(ItemTransform.Type.GROUND, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(0, 3, 0), new Vector3Float(0.5f, 0.5f, 0.5f)));
        CONDUIT.put(ItemTransform.Type.HEAD, ItemTransform.transform(new Vector3Float(0, 180, 0), new Vector3Float(0, 0, 0), new Vector3Float(1, 1, 1)));
        CONDUIT.put(ItemTransform.Type.FIXED, ItemTransform.transform(new Vector3Float(0, 180, 0), new Vector3Float(0, 0, 0), new Vector3Float(1, 1, 1)));
        CONDUIT.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(75, 315, 0), new Vector3Float(0, 2.5f, 0), new Vector3Float(0.5f, 0.5f, 0.5f)));
        CONDUIT.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 315, 0), new Vector3Float(0, 0, 0), new Vector3Float(0.8f, 0.8f, 0.8f)));

        DECORATED_POT = new LinkedHashMap<>();
        DECORATED_POT.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 90, 0), new Vector3Float(0, 2, 0.5f), new Vector3Float(0.375f, 0.375f, 0.375f)));
        DECORATED_POT.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 90, 0), new Vector3Float(0, 0, 0), new Vector3Float(0.375f, 0.375f, 0.375f)));
        DECORATED_POT.put(ItemTransform.Type.GUI, ItemTransform.transform(new Vector3Float(30, 45, 0), new Vector3Float(0, 0, 0), new Vector3Float(0.6f, 0.6f, 0.6f)));
        DECORATED_POT.put(ItemTransform.Type.GROUND, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(0, 1, 0), new Vector3Float(0.25f, 0.25f, 0.25f)));
        DECORATED_POT.put(ItemTransform.Type.HEAD, ItemTransform.transform(new Vector3Float(0, 180, 0), new Vector3Float(0, 16, 0), new Vector3Float(1.5f, 1.5f, 1.5f)));
        DECORATED_POT.put(ItemTransform.Type.FIXED, ItemTransform.transform(new Vector3Float(0, 180, 0), new Vector3Float(0, 0, 0), new Vector3Float(0.5f, 0.5f, 0.5f)));

    }
}
