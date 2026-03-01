package io.th0rgal.oraxen.nms.v1_21_R6_spigot;

import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.nms.GlyphHandler;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.FoodComponent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * NMS Handler for Spigot 1.21.11 (uses Spigot mappings).
 *
 * This is a simplified implementation that provides basic functionality.
 * Some features are not available on Spigot servers:
 * - GlobalConfiguration block update settings
 * - ChannelInitializeListenerHolder (mineable tag handling)
 * - Advanced consumable component configuration
 * - Jukebox song playing via NMS
 *
 * For full feature support, use Paper 1.21.11+ with the v1_21_R6 module.
 */
public class NMSHandler implements io.th0rgal.oraxen.nms.NMSHandler {

    private final GlyphHandler glyphHandler;
    private static boolean warningLogged = false;

    public NMSHandler() {
        this.glyphHandler = new io.th0rgal.oraxen.nms.v1_21_R6_spigot.GlyphHandler();

        if (!warningLogged) {
            Logs.logWarning("Spigot 1.21.11 detected. Some features are limited compared to Paper.");
            Logs.logWarning("For full feature support, consider using Paper 1.21.11+");
            warningLogged = true;
        }
    }

    @Override
    public GlyphHandler glyphHandler() {
        return glyphHandler;
    }

    @Override
    public boolean tripwireUpdatesDisabled() {
        // Spigot doesn't have GlobalConfiguration - block updates cannot be disabled
        return false;
    }

    @Override
    public boolean noteblockUpdatesDisabled() {
        // Spigot doesn't have GlobalConfiguration - block updates cannot be disabled
        return false;
    }

    @Override
    public boolean chorusPlantUpdatesDisabled() {
        // Spigot doesn't have GlobalConfiguration - block updates cannot be disabled
        return false;
    }

    @Override
    public ItemStack copyItemNBTTags(ItemStack oldItem, ItemStack newItem) {
        try {
            Class<?> craftItemStackClass = resolveCraftItemStackClass();
            if (craftItemStackClass == null) {
                return newItem;
            }

            Object oldNms = asNmsCopy(craftItemStackClass, oldItem);
            Object newNms = asNmsCopy(craftItemStackClass, newItem);
            if (oldNms == null || newNms == null) {
                return newItem;
            }

            Class<?> tagClass = resolveTagClass();
            if (tagClass == null) {
                return newItem;
            }

            Object oldTag = saveItemTag(oldNms, tagClass);
            Object newTag = saveItemTag(newNms, tagClass);
            if (oldTag == null || newTag == null) {
                return newItem;
            }

            copyCustomTagEntries(oldTag, newTag);
            if (!applyItemTag(newNms, tagClass, newTag)) {
                return newItem;
            }

            ItemStack converted = asBukkitCopy(craftItemStackClass, newNms);
            return converted != null ? converted : newItem;
        } catch (Exception e) {
            if (io.th0rgal.oraxen.config.Settings.DEBUG.toBool()) {
                Logs.logWarning("Failed to copy NBT tags on Spigot: " + e.getMessage());
            }
            return newItem;
        }
    }

    private Class<?> resolveTagClass() {
        try {
            return Class.forName("net.minecraft.nbt.NBTTagCompound");
        } catch (ClassNotFoundException ignored) {
        }
        try {
            return Class.forName("net.minecraft.nbt.CompoundTag");
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    private Class<?> resolveCraftItemStackClass() {
        for (String className : new String[]{
                "org.bukkit.craftbukkit.inventory.CraftItemStack",
                "org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack",
                "org.bukkit.craftbukkit.v1_21_R6.inventory.CraftItemStack"
        }) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private Object asNmsCopy(Class<?> craftItemStackClass, ItemStack item) throws Exception {
        Method asNmsCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
        return asNmsCopy.invoke(null, item);
    }

    private ItemStack asBukkitCopy(Class<?> craftItemStackClass, Object nmsItem) throws Exception {
        for (Method method : craftItemStackClass.getMethods()) {
            if (!"asBukkitCopy".equals(method.getName())) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            if (!method.getParameterTypes()[0].isAssignableFrom(nmsItem.getClass())
                    && !nmsItem.getClass().isAssignableFrom(method.getParameterTypes()[0])) {
                continue;
            }
            Object result = method.invoke(null, nmsItem);
            if (result instanceof ItemStack bukkitItem) {
                return bukkitItem;
            }
        }
        return null;
    }

    private Object saveItemTag(Object nmsItem, Class<?> tagClass) throws Exception {
        Constructor<?> ctor = tagClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object tag = ctor.newInstance();

        Method save = findMethod(nmsItem.getClass(), tagClass, tagClass);
        if (save == null) {
            return null;
        }
        save.setAccessible(true);
        return save.invoke(nmsItem, tag);
    }

    private boolean applyItemTag(Object nmsItem, Class<?> tagClass, Object tag) throws Exception {
        Method setTag = findSetterMethod(nmsItem.getClass(), tagClass);
        if (setTag == null) {
            return false;
        }
        setTag.setAccessible(true);
        setTag.invoke(nmsItem, tag);
        return true;
    }

    @SuppressWarnings("unchecked")
    private void copyCustomTagEntries(Object oldTag, Object newTag) throws Exception {
        Set<String> keys = getTagKeys(oldTag);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        Method getMethod = findTagGetMethod(oldTag.getClass());
        Method putMethod = findMethodWithFirstParamStringAndSecondTag(newTag.getClass());
        Method removeMethod = findTagRemoveMethod(newTag.getClass());
        if (getMethod == null || putMethod == null) {
            return;
        }

        getMethod.setAccessible(true);
        putMethod.setAccessible(true);
        if (removeMethod != null) {
            removeMethod.setAccessible(true);
        }

        for (String key : keys) {
            if (vanillaKeys.contains(key)) {
                continue;
            }

            Object value = getMethod.invoke(oldTag, key);
            if (value != null) {
                putMethod.invoke(newTag, key, value);
            } else if (removeMethod != null) {
                removeMethod.invoke(newTag, key);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getTagKeys(Object tag) throws Exception {
        for (String methodName : new String[]{"getAllKeys", "e", "c", "keySet"}) {
            try {
                Method method = tag.getClass().getMethod(methodName);
                if (Set.class.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    return (Set<String>) method.invoke(tag);
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private Method findMethod(Class<?> owner, Class<?> expectedReturnType, Class<?>... params) {
        for (Method method : owner.getMethods()) {
            if (method.getParameterCount() != params.length) {
                continue;
            }
            Class<?>[] actualParams = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < params.length; i++) {
                if (!actualParams[i].isAssignableFrom(params[i]) && !params[i].isAssignableFrom(actualParams[i])) {
                    matches = false;
                    break;
                }
            }
            if (!matches) {
                continue;
            }
            if (expectedReturnType != null
                    && !expectedReturnType.isAssignableFrom(method.getReturnType())
                    && !method.getReturnType().isAssignableFrom(expectedReturnType)) {
                continue;
            }
            return method;
        }
        return null;
    }

    private Method findMethodWithFirstParamStringAndSecondTag(Class<?> owner) {
        for (Method method : owner.getMethods()) {
            if (method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (!String.class.isAssignableFrom(params[0])) {
                continue;
            }
            if (!params[1].getName().startsWith("net.minecraft.nbt")) {
                continue;
            }
            return method;
        }
        return null;
    }

    private Method findSetterMethod(Class<?> owner, Class<?> paramType) {
        for (Method method : owner.getMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }
            if (!method.getParameterTypes()[0].isAssignableFrom(paramType)
                    && !paramType.isAssignableFrom(method.getParameterTypes()[0])) {
                continue;
            }
            if (method.getReturnType() == Void.TYPE || method.getReturnType() == Boolean.TYPE) {
                return method;
            }
        }
        return null;
    }

    private Method findTagGetMethod(Class<?> owner) {
        for (String methodName : new String[]{"get", "c"}) {
            try {
                Method method = owner.getMethod(methodName, String.class);
                if (!method.getReturnType().getName().startsWith("net.minecraft.nbt")) {
                    continue;
                }
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private Method findTagRemoveMethod(Class<?> owner) {
        for (String methodName : new String[]{"remove", "r"}) {
            try {
                Method method = owner.getMethod(methodName, String.class);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    @Override
    public BlockData correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack) {
        // This requires NMS access to properly handle block placement context
        // On Spigot, we return null and let the default behavior handle it
        return null;
    }

    @Override
    public void customBlockDefaultTools(Player player) {
        // Not supported on Spigot (requires Paper's ChannelInitializeListenerHolder)
    }

    @Override
    public boolean getSupported() {
        return true;
    }

    @Override
    public boolean setComponent(ItemBuilder item, String componentKey, Object component) {
        // Component setting requires NMS access with Spigot-mapped names
        // This is a complex operation that's not fully portable
        Logs.logWarning("setComponent is not fully supported on Spigot. Use Paper for full support.");
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void foodComponent(ItemBuilder item, ConfigurationSection foodSection) {
        try {
            FoodComponent foodComponent = new ItemStack(item.getType()).getItemMeta().getFood();

            int nutrition = Math.max(foodSection.getInt("nutrition"), 0);
            foodComponent.setNutrition(nutrition);

            float saturation = Math.max((float) foodSection.getDouble("saturation", 0.0), 0f);
            foodComponent.setSaturation(saturation);

            foodComponent.setCanAlwaysEat(foodSection.getBoolean("can_always_eat", false));

            item.setFoodComponent(foodComponent);
        } catch (Exception e) {
            Logs.logWarning("Failed to set food component on Spigot: " + e.getMessage());
        }
    }

    @Override
    public void consumableComponent(ItemBuilder item, ConfigurationSection section) {
        // Consumable component requires Paper-specific NMS classes
        Logs.logWarning("Consumable components are not fully supported on Spigot servers.");
    }

    @Override
    public Object consumableComponent(final ItemStack itemStack) {
        return null; // Not supported on Spigot
    }

    @Override
    public ItemStack consumableComponent(final ItemStack itemStack, Object consumable) {
        return itemStack; // Not supported on Spigot
    }

    @Override
    public boolean supportsJukeboxPlaying() {
        return false; // Jukebox playing requires Paper's LevelEvent constants
    }

    @Override
    public void playJukeBoxSong(Location location, ItemStack itemStack) {
        // Not supported on Spigot
    }

    @Override
    public void stopJukeBox(Location location) {
        // Not supported on Spigot
    }
}
