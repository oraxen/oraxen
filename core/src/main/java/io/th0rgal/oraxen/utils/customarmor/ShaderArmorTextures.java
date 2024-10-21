package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

public class ShaderArmorTextures {

    static final int DEFAULT_RESOLUTION = 16;
    static final int HEIGHT_RATIO = 2;
    static final int WIDTH_RATIO = 4;

    private final List<BufferedImage> layers1 = new ArrayList<>();
    private final List<BufferedImage> layers2 = new ArrayList<>();
    private final int resolution;
    private BufferedImage layer1;
    private int layer1Width = 0;
    private int layer1Height = 0;
    private BufferedImage layer2;
    private int layer2Width = 0;
    private int layer2Height = 0;
    private static ShaderType shaderType;

    public enum ShaderType {
        FANCY, LESS_FANCY
    }

    public ShaderArmorTextures() {
        int resolution = DEFAULT_RESOLUTION;
        try {
            resolution = (int) Settings.CUSTOM_ARMOR_SHADER_RESOLUTION.getValue();
        } catch (NumberFormatException ignored) {

        }
        this.resolution = resolution;
        try {
            shaderType = ShaderType.valueOf(Settings.CUSTOM_ARMOR_SHADER_TYPE.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            Logs.logError("Invalid value for CUSTOM_ARMOR_SHADER_TYPE: " + Settings.CUSTOM_ARMOR_SHADER_TYPE.getValue());
            Logs.logWarning("Valid values are: FANCY, LESS_FANCY");
            Logs.logWarning("Defaulting to FANCY");
            shaderType = ShaderType.FANCY;
        }
    }

    public static boolean isSameArmorType(ItemStack firstItem, ItemStack secondItem) {
        return Objects.equals(getArmorNameFromItem(firstItem), getArmorNameFromItem(secondItem));
    }
    public static String getArmorNameFromItem(ItemStack item) {
        return getArmorNameFromId(OraxenItems.getIdByItem(item));
    }
    public static String getArmorNameFromId(String itemId) {
        return StringUtils.substringBeforeLast(itemId, "_");
    }

    public boolean registerImage(File file) {
        String name = file.getName();

        if (!name.endsWith(".png")) return false;
        if (!name.contains("armor_layer") && !name.contains("leather_layer")) return false;
        if (!Settings.CUSTOM_ARMOR_SHADER_GENERATE_CUSTOM_TEXTURES.toBool()) return false;

        BufferedImage img;
        try {
            img = ImageIO.read(file);
        } catch (IOException e) {
            OraxenPlugin.get().getLogger().warning("Error while reading " + name + ": " + e.getMessage());
            return false;
        }

        if (name.equals("leather_layer_1.png")) {
            img = rescaleArmorImage(img);
            img = initLayer(img);
            if (shaderType == ShaderType.FANCY) setPixel(img.getRaster(), 0, 1, Color.WHITE);
            layer1 = img;
            layer1Width = shaderType == ShaderType.FANCY ? layer1Width + layer1.getWidth() : getLayerWidth();
            layer1Height = shaderType == ShaderType.FANCY ? getLayerHeight() : layer1Height + layer1.getHeight();
            return true;
        } else if (name.equals("leather_layer_2.png")) {
            img = rescaleArmorImage(img);
            img = initLayer(img);
            if (shaderType == ShaderType.FANCY) setPixel(img.getRaster(), 0, 1, Color.WHITE);
            layer2 = img;
            layer2Width = shaderType == ShaderType.FANCY ? layer2Width + layer2.getWidth() : getLayerWidth();
            layer2Height = shaderType == ShaderType.FANCY ? getLayerHeight() : layer2Height + layer2.getHeight();
            return true;
        }

        return name.contains("armor_layer_") && handleArmorLayer(name, file);
    }

    private int getLayerWidth() {
        return resolution * WIDTH_RATIO;
    }

    private int getLayerHeight() {
        return resolution * HEIGHT_RATIO;
    }

    private BufferedImage initLayer(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        Image scaled;
        BufferedImage output;

        if (shaderType == ShaderType.FANCY) {
            if (width == resolution * WIDTH_RATIO && height == getLayerHeight()) return original;

            scaled = original.getScaledInstance(resolution * WIDTH_RATIO, height, Image.SCALE_DEFAULT);
            output = new BufferedImage(resolution * WIDTH_RATIO, height, BufferedImage.TYPE_INT_ARGB);
        } else {
            if (width == getLayerWidth() && height == resolution * HEIGHT_RATIO) return original;

            scaled = original.getScaledInstance(width, resolution * HEIGHT_RATIO, Image.SCALE_DEFAULT);
            output = new BufferedImage(width, resolution * HEIGHT_RATIO, BufferedImage.TYPE_INT_ARGB);
        }

        output.getGraphics().drawImage(scaled, 0, 0, null);
        return output;
    }

    private boolean handleArmorLayer(String name, File file) {
        String prefix = name.split("armor_layer_")[0];

        // Skip actually editing the emissive image,
        // should check for file with same name + e to properly apply everything
        if (name.endsWith("_e.png")) return false;

        BufferedImage original;
        try {
            original = ImageIO.read(file);
        } catch (IOException e) {
            OraxenPlugin.get().getLogger().warning("Error while reading " + name + ": " + e.getMessage());
            return false;
        }
        if (original == null) {
            OraxenPlugin.get().getLogger().warning("Error while reading " + name + ": Image is null");
            return false;
        }

        BufferedImage image = initLayer(original);

        boolean isAnimated = name.endsWith("_a.png");
        // if a file exists with same name + _e it should be emissive
        // This should not be edited, simply added to the width and pixel should be edited
        // on the original image
        File emissiveFile = file.getParentFile().toPath().toAbsolutePath().resolve(name.replace(".png", "_e.png")).toFile();
        boolean isEmissive = Files.exists(emissiveFile.toPath());
        if (isEmissive) {
            BufferedImage emissive;
            try {
                emissive = ImageIO.read(emissiveFile);
            } catch (IOException e) {
                OraxenPlugin.get().getLogger().warning("Error while reading " + name + ": " + e.getMessage());
                return false;
            }
            BufferedImage emissiveImage = initLayer(emissive);
            image = mergeImages(image.getWidth() + emissiveImage.getWidth(),
                    emissiveImage.getHeight(),
                    image, emissiveImage);

            setPixel(image.getRaster(), 2, 0, Color.fromRGB(1, 0, 0));
        }

        // Ensure 32-bit image
        if (image.getColorModel().getPixelSize() < 32) {
            int width = image.getWidth(), height = image.getHeight();
            Image resizedImage = original.getScaledInstance(width, height, Image.SCALE_DEFAULT);
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            image.getGraphics().drawImage(resizedImage, 0, 0, null);
        }
        addPixel(image, name, prefix, isAnimated);

        return true;
    }

    private final Map<String, Integer> allSpecifiedArmorColors = getAllSpecifiedArmorColors();
    /**
     * Finds Armor Items tied to this prefix and fixes their colors
     * This removes the need for manually specifying a color
     * It also adds support for LessFancyPants' color system
     *
     * @param prefix The prefix of the armor item
     * @param name   The name of the armor layer file
     */
    private Color fixArmorColors(String prefix, String name) {
        Color color = null;
        // No need to run for layer 1 and 2 so skip 2 :)
        for (String suffix : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
            ItemBuilder builder = OraxenItems.getItemById(prefix + suffix);
            ItemMeta meta = builder != null ? builder.build().getItemMeta() : null;

            if (!(meta instanceof LeatherArmorMeta) && builder != null && builder.build().getType().toString().toLowerCase(Locale.ROOT).endsWith(suffix)) {
                Logs.logError("Material of " + prefix + suffix + " is not a LeatherArmor material!");
                Logs.logWarning("Custom Armor requires that the item is LeatherArmor");
                Logs.logWarning("You can add fake armor values via AttributeModifiers", true);
            }

            boolean missingArmor = switch (suffix) {
                case "helmet", "chestplate" ->
                        OraxenItems.getItemById(prefix + "helmet") == null && OraxenItems.getItemById(prefix + "chestplate") == null;
                case "leggings", "boots" ->
                        OraxenItems.getItemById(prefix + "leggings") == null && OraxenItems.getItemById(prefix + "boots") == null;
                default -> true;
            };

            if (missingArmor) {
                if (name.endsWith("_1.png") && Set.of("helmet", "chestplate").contains(suffix)) {
                    Message.NO_ARMOR_ITEM.log(AdventureUtils.tagResolver("name", prefix + suffix),
                            AdventureUtils.tagResolver("armor_layer_file", name));
                } else if (name.endsWith("_2.png") && Set.of("leggings", "boots").contains(suffix)) {
                    Message.NO_ARMOR_ITEM.log(AdventureUtils.tagResolver("name", prefix + suffix),
                            AdventureUtils.tagResolver("armor_layer_file", name));
                }
            }

            boolean duplicateColor = allSpecifiedArmorColors.entrySet().stream().filter(e-> builder != null && builder.hasColor() && e.getValue() == builder.getColor().asRGB()).toList().size() >= 2;
            if (builder != null && (!builder.hasColor() || duplicateColor  || shaderType == ShaderType.LESS_FANCY)) {
                // If builder has no color or the shader-type is LESS_FANCY
                // Then assign a color based on the armor ID
                String itemPrefix = prefix.replace("_", "");
                int tempColor = allSpecifiedArmorColors.entrySet().stream().filter(e -> e.getKey().startsWith(itemPrefix)).findFirst().orElseGet(() -> Map.entry(prefix, layers1.size() + 1)).getValue();
                Color armorColor = Color.fromRGB(getColorInt(itemPrefix, tempColor));
                if (allSpecifiedArmorColors.entrySet().stream().filter(e -> !e.getKey().equals(itemPrefix)).map(Map.Entry::getValue).toList().contains(armorColor.asRGB())) {
                    // If the color is already used, then assign a new one
                    armorColor = Color.fromRGB(getColorInt(itemPrefix, tempColor + 1));
                }
                builder.setColor(armorColor);
                builder.save();
                if (Settings.DEBUG.toBool()) Logs.logInfo("Assigned color " + armorColor.asRGB() + " to " + prefix + suffix);
            }

            color = builder != null && builder.hasColor() ? builder.getColor() : null;
        }
        return color;
    }

    /**
     * Recursive function to get the next unused color that is unspecified
     */
    private int getColorInt(String itemId, int start) {
        int color;
        if (allSpecifiedArmorColors.get(itemId) != null) color = allSpecifiedArmorColors.get(itemId);
        else if (allSpecifiedArmorColors.entrySet().stream().filter(e -> !e.getKey().equals(itemId)).anyMatch(e -> e.getValue() == start)) {
            Logs.logWarning("Color " + start + " is already used! Reassigning " + itemId + " with a new color...");
            color = getColorInt(itemId, start + 1);
        } else color = start;

        allSpecifiedArmorColors.put(itemId, color);
        return color;
    }

    private void addPixel(BufferedImage image, String name, String prefix, boolean isAnimated) {
        // Ensures armorColor is set. If no Color is specified, assigns one.
        // If ShaderType = LESS_FANCY then it will assign a color regardless following armor-ID
        Color armorColor = fixArmorColors(prefix, name);
        if (armorColor != null) {
            if (shaderType == ShaderType.FANCY) {
                setPixel(image.getRaster(), 0, 0, armorColor);
                if (isAnimated)
                    setPixel(image.getRaster(), 1, 0, Color.fromRGB(image.getHeight() / (int) Settings.CUSTOM_ARMOR_SHADER_RESOLUTION.getValue(), getAnimatedArmorFramerate(), 1));
            }

            if (name.contains("armor_layer_1")) {
                layers1.add(image);
                layer1Width = shaderType == ShaderType.FANCY ? layer1Width + image.getWidth() : Math.max(layer1Width, image.getWidth());
                layer1Height = shaderType == ShaderType.FANCY ? Math.max(layer1Height, image.getHeight()) : layer1Height + image.getHeight();
            } else {
                layers2.add(image);
                layer2Width = shaderType == ShaderType.FANCY ? layer2Width + image.getWidth() : Math.max(layer2Width, image.getWidth());
                layer2Height = shaderType == ShaderType.FANCY ? Math.max(layer2Height, image.getHeight()) : layer2Height + image.getHeight();
            }

            if (!isAnimated && image.getHeight() > getLayerHeight()) {
                Logs.logError("The height of " + name + " is greater than " + getLayerHeight() + "px.");
                Logs.logWarning("Since it is not an animated armor-file, this will potentially break other armor sets.");
                Logs.logWarning("Adjust the " + Settings.CUSTOM_ARMOR_SHADER_RESOLUTION.getPath() + " setting to fix this issue.");
                Logs.logWarning("If it is meant to be an animated armor-file, make sure it ends with _a.png or _a_e.png if emissive");
            }
        }
    }

    public boolean hasCustomArmors() {
        return !(layers1.isEmpty() || layers2.isEmpty() || layer1 == null || layer2 == null);
    }

    public InputStream getLayerOne() throws IOException {
        return getInputStream(layer1Width, layer1Height, layer1, layers1);
    }

    public InputStream getLayerTwo() throws IOException {
        return getInputStream(layer2Width, layer2Height, layer2, layers2);
    }

    private final String OPTIFINE_ARMOR_PATH = "assets/minecraft/optifine/%s/armors/".formatted(VersionUtil.atOrAbove("1.21") ? "cit_single" : "cit");
    private final String OPTIFINE_ARMOR_ANIMATION_PATH = "assets/minecraft/optifine/anim/";

    private InputStream rescaleArmorImage(File original) {
        try {
            return rescaleArmorImage(Files.newInputStream(original.toPath().toAbsolutePath()));
        } catch (IOException e) {
            return null;
        }
    }

    private InputStream rescaleArmorImage(InputStream original) {
        BufferedImage img;
        try {
            img = ImageIO.read(original);
        } catch (IOException e) {
            OraxenPlugin.get().getLogger().warning("Error while reading InputStream: " + e.getMessage());
            return original;
        }

        try {
            img = rescaleArmorImage(img);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(img, "png", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            is.close();
            os.close();
            return is;
        } catch (IOException ignored) {
            return original;
        }
    }

    private BufferedImage rescaleArmorImage(BufferedImage original) {
        int width = resolution * WIDTH_RATIO;
        int height = resolution * HEIGHT_RATIO;
        Image resizedImage = original.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        outputImage.getGraphics().drawImage(resizedImage, 0, 0, null);
        return outputImage;
    }

    public int getAnimatedArmorFramerate() {
        try {
            return Integer.parseInt(Settings.CUSTOM_ARMOR_SHADER_ANIMATED_FRAMERATE.getValue().toString());
        } catch (NumberFormatException e) {
            return 24;
        }
    }

    public Set<VirtualFile> getOptifineFiles() throws FileNotFoundException {
        Set<VirtualFile> optifineFiles = new HashSet<>(generateLeatherArmors());

        for (Map.Entry<String, InputStream> armorFile : getAllArmors().entrySet()) {
            String fileName = armorFile.getKey();
            String parentFolder = StringUtils.substringBefore(fileName, "_");
            String path = OPTIFINE_ARMOR_PATH + parentFolder;
            optifineFiles.add(new VirtualFile(path, fileName, armorFile.getValue()));
            if (fileName.endsWith("_e.png")) continue;

            // Avoid duplicate properties files as this is called for both layers, but only needs 1 property file
            if (optifineFiles.stream().map(VirtualFile::getPath).anyMatch(
                    p -> Objects.equals(p, path + "/" + parentFolder + ".properties"))) continue;

            String colorProperty = "nbt.display.color=" + getArmorColor(parentFolder);
            String propContent = getArmorPropertyFile(fileName, colorProperty, 1);

            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(propContent.getBytes(StandardCharsets.UTF_8));
                optifineFiles.add(new VirtualFile(path, parentFolder + ".properties", inputStream));
                inputStream.close();
            } catch (IOException ignored) {
            }

            if (fileName.endsWith("_a.png"))
                optifineFiles.addAll(getOptifineAnimFiles(armorFile.getValue(), fileName, parentFolder));
        }

        return optifineFiles;
    }

    private int getArmorColor(String parentFolder) {
        return OraxenItems.getEntries().stream().filter(e ->
                e.getValue().build().getType().toString().startsWith("LEATHER_") &&
                        e.getValue().hasOraxenMeta() && e.getValue().getOraxenMeta().getLayers() != null &&
                        !e.getValue().getOraxenMeta().getLayers().isEmpty() &&
                        e.getValue().getOraxenMeta().getLayers().get(0).contains(parentFolder)
        ).map(s -> s.getValue().getColor()).findFirst().orElse(Color.WHITE).asRGB();
    }

    private List<VirtualFile> getOptifineAnimFiles(InputStream armorFile, String fileName, String parentFolder) {
        List<VirtualFile> optifineFiles = new ArrayList<>();
        int height;
        int width;
        try {
            BufferedImage image = ImageIO.read(armorFile);
            height = image.getHeight();
            width = image.getWidth();
        } catch (IOException e) {
            Logs.logError("Error while reading " + fileName + ": " + e.getMessage());
            return optifineFiles;
        }
        String animPropContent = getOptifineArmorAnimPropertyFile(parentFolder, fileName, width, height, height / getLayerHeight());
        ByteArrayInputStream animInputStream = new ByteArrayInputStream(animPropContent.getBytes(StandardCharsets.UTF_8));
        optifineFiles.add(new VirtualFile(OPTIFINE_ARMOR_ANIMATION_PATH + parentFolder, parentFolder + "_anim.properties", animInputStream));
        //TODO Adds a corrupted file, probably the armorFile inputstream
        optifineFiles.add(new VirtualFile(OPTIFINE_ARMOR_ANIMATION_PATH + parentFolder, fileName, armorFile));
        return optifineFiles;
    }

    private String getOptifineArmorAnimPropertyFile(String parentFolder, String fileName, int width, int height, int frames) {
        StringBuilder string = new StringBuilder("""
                from=~/anim/""" + fileName + "\n" + """
                to=""" + OPTIFINE_ARMOR_PATH + parentFolder + "/" + fileName + "\n" + """
                y=0""" + "\n" + """
                x=0""" + "\n" + """
                h=""" + height + "\n" + """
                w=""" + width + "\n");

        for (int i = 0; i < frames; i++) {
            string.append("""
                    tile.""").append(i).append("=").append(i).append("\n").append("""
                    duration.""").append(i).append("=").append(getAnimatedArmorFramerate() / frames).append("\n");
        }
        return string.toString();
    }

    private List<VirtualFile> generateLeatherArmors() {
        List<VirtualFile> leatherArmors = new ArrayList<>();
        String absolute = OraxenPlugin.get().getDataFolder().getAbsolutePath() + "/pack/textures/models/armor";
        File leatherFile1 = new File(absolute, "/leather_layer_1.png");
        File leatherFile2 = new File(absolute, "/leather_layer_2.png");
        File leatherFileOverlay = new File(absolute, "/leather_layer_1_overlay.png");
        String leatherPath = OPTIFINE_ARMOR_PATH + "leather";

        // If someone deletes required or compiles, don't fail simply break leather shader armor
        if (!leatherFile1.exists() || !leatherFile2.exists() || !leatherFileOverlay.exists()) return leatherArmors;

        leatherArmors.add(new VirtualFile(leatherPath, "leather_armor_layer_1.png", rescaleArmorImage(leatherFile1)));
        leatherArmors.add(new VirtualFile(leatherPath, "leather_armor_layer_2.png", rescaleArmorImage(leatherFile2)));
        leatherArmors.add(new VirtualFile(leatherPath, "leather_armor_overlay.png", rescaleArmorImage(leatherFileOverlay)));

        String content = correctLeatherPropertyFile(getArmorPropertyFile("leather_armor_layer_1.png", "", 0));
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        leatherArmors.add(new VirtualFile(leatherPath, "leather.properties", inputStream));

        return leatherArmors;
    }

    private String correctLeatherPropertyFile(String content) {
        return content
                .replace("texture." + "leather_layer_1_overlay=" + "leather_armor_layer_1.png",
                        "texture." + "leather_layer_1_overlay=" + "leather_armor_overlay.png")
                .replace("texture." + "leather_layer_2_overlay=" + "leather_armor_layer_2.png",
                        "texture." + "leather_layer_2_overlay=" + "leather_armor_overlay.png");
    }

    private String getArmorPropertyFile(String fileName, String cmdProperty, int weight) {
        return """
                type=armor
                items=minecraft:leather_helmet minecraft:leather_chestplate minecraft:leather_leggings minecraft:leather_boots
                texture.""" + "leather_layer_1=" + fileName.replace("_2.png", "_1.png") + "\n" + """
                texture.""" + "leather_layer_1_overlay=" + fileName.replace("_2.png", "_1.png") + "\n" + """
                texture.""" + "leather_layer_2=" + fileName.replace("_1.png", "_2.png") + "\n" + """
                texture.""" + "leather_layer_2_overlay=" + fileName.replace("_1.png", "_2.png") + "\n" +
                cmdProperty + "\n" + """
                weight=""" + weight;
    }

    private Map<String, InputStream> getAllArmors() {
        Map<String, InputStream> layers = new HashMap<>();

        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            String itemId = entry.getKey();
            ItemBuilder builder = entry.getValue();
            String armorType = StringUtils.substringBeforeLast(itemId, "_");
            if (!builder.hasOraxenMeta()) continue;
            List<String> layerList = builder.getOraxenMeta().getLayers();

            boolean isArmor = builder.build().getType().toString().contains("LEATHER_");
            boolean inLayerList = layers.keySet().stream().anyMatch(s -> s.contains(armorType));

            if (isArmor && !inLayerList && builder.hasOraxenMeta() && layerList != null && layerList.size() == 2) {
                for (String file : layerList) {
                    int id = layers.keySet().stream().anyMatch(s -> s.contains(armorType)) ? 2 : 1;

                    String fileName = armorType + "_armor_layer_" + id + ".png";
                    String absolutePath = OraxenPlugin.get().getDataFolder().getAbsolutePath() + "/pack/textures/";
                    String fileFolder = absolutePath + StringUtils.substringBeforeLast(file, itemId) + fileName;
                    File armorFile = new File(fileFolder);

                    if (!armorFile.exists()) {
                        fileName = fileName.replace(".png", "_a.png");
                        armorFile = new File(fileFolder.replace(".png", "_a.png"));
                        //TODO Animated might wanna strip away everything except the first frame for base
                        if (!armorFile.exists()) {
                            continue;
                        }
                    }

                    try {
                        BufferedImage armorLayer = ImageIO.read(armorFile);
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(armorLayer, "png", os);
                        InputStream is = new ByteArrayInputStream(os.toByteArray());
                        layers.put(fileName, is);
                        os.close();
                        is.close();
                    } catch (IOException ignored) {
                    }

                    File emissiveFile = new File(fileFolder.replace(".png", "_e.png"));
                    if (emissiveFile.exists()) {
                        try {
                            BufferedImage armorLayer = ImageIO.read(emissiveFile);
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            ImageIO.write(armorLayer, "png", os);
                            InputStream is = new ByteArrayInputStream(os.toByteArray());
                            layers.put(fileName.replace(".png", "_e.png"), is);
                            os.close();
                            is.close();
                        } catch (IOException ignored) {
                        }
                    }

                }
            }
        }
        return layers;
    }

    private static Map<String, Integer> getAllSpecifiedArmorColors() {
        Map<String, Integer> specifiedColors = new HashMap<>();

        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            String itemId = entry.getKey();
            ItemBuilder builder = entry.getValue();
            if (!builder.build().getType().toString().contains("LEATHER_")) continue;
            if (!builder.hasColor()) continue;

            specifiedColors.putIfAbsent(StringUtils.substringBeforeLast(itemId, "_"), builder.getColor().asRGB());
        }
        return specifiedColors;
    }

    private InputStream getInputStream(int layerWidth, int layerHeight, BufferedImage layer, List<BufferedImage> layers) throws IOException {
        layers.add(0, layer);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(mergeImages(layerWidth, layerHeight, layers.toArray(new BufferedImage[0])), "png", outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private void setPixel(WritableRaster raster, int x, int y, Color color) {
        raster.setPixel(x, y, new int[]{color.getRed(), color.getGreen(), color.getBlue(), 255});
    }

    private BufferedImage mergeImages(int width, int height, BufferedImage... images) {
        BufferedImage concatImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = concatImage.createGraphics();
        int currentWidth = 0;
        int currentHeight = 0;

        for (BufferedImage bufferedImage : images) {
            g2d.drawImage(bufferedImage, currentWidth, currentHeight, null);
            if (shaderType == ShaderType.FANCY) currentWidth += bufferedImage.getWidth();
            else if (shaderType == ShaderType.LESS_FANCY) currentHeight += bufferedImage.getHeight();
        }

        g2d.dispose();
        return concatImage;
    }

    public static void generateArmorShaderFiles() {
        if (shaderType == ShaderType.LESS_FANCY) {
            ShaderArmorTextures.LessFancyArmorShaders.generateArmorShaderFiles();
        } else if (shaderType == ShaderType.FANCY) {
            ShaderArmorTextures.FancyArmorShaders.generateArmorShaderFiles();
        }
    }

    private static final String SHADER_PARAMETER_PLACEHOLDER = "{#TEXTURE_RESOLUTION#}";

    private static class FancyArmorShaders {
        private static void generateArmorShaderFiles() {
            String parent = "assets/minecraft/shaders/core/";
            String file = "rendertype_armor_cutout_no_cull";
            ResourcePack.writeStringToVirtual(parent, file + ".json", getShaderJson());
            ResourcePack.writeStringToVirtual(parent, file + ".vsh", getShaderVsh());
            ResourcePack.writeStringToVirtual(parent, file + ".fsh", getShaderFsh());
            ResourcePack.writeStringToVirtual(parent, "LICENSE.md", getLicense());
        }

        private static String getShaderVsh() {
            return """
                    #version 150
                     
                     #moj_import <light.glsl>
                     
                     in vec3 Position;
                     in vec4 Color;
                     in vec2 UV0;
                     in vec2 UV1;
                     in ivec2 UV2;
                     in vec3 Normal;
                     
                     uniform sampler2D Sampler2;
                     
                     uniform mat4 ModelViewMat;
                     uniform mat4 ProjMat;
                     
                     uniform vec3 Light0_Direction;
                     uniform vec3 Light1_Direction;
                     
                     out float vertexDistance;
                     out vec4 vertexColor;
                     out vec2 texCoord0;
                     out vec2 texCoord1;
                     out vec4 normal;
                     flat out vec4 tint;
                     flat out vec3 vNormal;
                     flat out vec4 texel;
                     
                     void main() {
                         vNormal = Normal;
                         texel = texelFetch(Sampler2, UV2 / 16, 0);
                         gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                     
                         vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
                         vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color) * texelFetch(Sampler2, UV2 / 16, 0);
                         tint = Color;
                         texCoord0 = UV0;
                         texCoord1 = UV1;
                         normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
                     }
                    """.trim();
        }

        private static String getShaderFsh() {
            return """
                    #version 150
                     
                     #moj_import <fog.glsl>
                     #moj_import <light.glsl>
                     
                     #define TEX_RES {#TEXTURE_RESOLUTION#}
                     #define ANIM_SPEED 50 // Runs every 24 seconds
                     #define IS_LEATHER_LAYER texelFetch(Sampler0, ivec2(0, 1), 0) == vec4(1) // If it's leather_layer_X.png texture
                     
                     uniform sampler2D Sampler0;
                     
                     uniform vec4 ColorModulator;
                     uniform float FogStart;
                     uniform float FogEnd;
                     uniform vec4 FogColor;
                     uniform float GameTime;
                     uniform vec3 Light0_Direction;
                     uniform vec3 Light1_Direction;
                     
                     in float vertexDistance;
                     in vec4 vertexColor;
                     in vec2 texCoord0;
                     in vec2 texCoord1;
                     in vec4 normal;
                     flat in vec4 tint;
                     flat in vec3 vNormal;
                     flat in vec4 texel;
                     
                     out vec4 fragColor;
                     
                     void main()
                     {
                         ivec2 atlasSize = textureSize(Sampler0, 0);
                         float armorAmount = atlasSize.x / (TEX_RES * 4.0);
                         float maxFrames = atlasSize.y / (TEX_RES * 2.0);
                     
                         vec2 coords = texCoord0;
                         coords.x /= armorAmount;
                         coords.y /= maxFrames;
                     
                         vec4 color;
                     
                         if(IS_LEATHER_LAYER)
                         {
                             // Texture properties contains extra info about the armor texture, such as to enable shading
                             vec4 textureProperties = vec4(0);
                             vec4 customColor = vec4(0);
                     
                             float h_offset = 1.0 / armorAmount;
                             vec2 nextFrame = vec2(0);
                             float interpolClock = 0;
                             vec4 vtc = vertexColor;
                     
                             for (int i = 1; i < (armorAmount + 1); i++)
                             {
                                 customColor = texelFetch(Sampler0, ivec2(TEX_RES * 4 * i + 0.5, 0), 0);
                                 if (tint == customColor){
                     
                                     coords.x += (h_offset * i);
                                     vec4 animInfo = texelFetch(Sampler0, ivec2(TEX_RES * 4 * i + 1.5, 0), 0);
                                     animInfo.rgb *= animInfo.a * 255;
                                     textureProperties = texelFetch(Sampler0, ivec2(TEX_RES * 4 * i + 2.5, 0), 0);
                                     textureProperties.rgb *= textureProperties.a * 255;
                                     if (animInfo != vec4(0))
                                     {
                                         // oh god it's animated
                                         // animInfo = amount of frames, speed, interpolation (1||0)
                                         // textureProperties = emissive, tint
                                         // fract(GameTime * 1200) blinks every second so [0,1] every second
                                         float timer = floor(mod(GameTime * ANIM_SPEED * animInfo.g, animInfo.r));
                                         if (animInfo.b > 0)
                                             interpolClock = fract(GameTime * ANIM_SPEED * animInfo.g);
                                         float v_offset = (TEX_RES * 2.0) / atlasSize.y * timer;
                                         nextFrame = coords;
                                         coords.y += v_offset;
                                         nextFrame.y += (TEX_RES * 2.0) / atlasSize.y * mod(timer + 1, animInfo.r);
                                     }
                                     break;
                                 }
                             }
                     
                             if (textureProperties.g == 1)
                             {
                                 if (textureProperties.r > 1)
                                 {
                                     vtc = tint;
                                 }
                                 else if (textureProperties.r == 1)
                                 {
                                     if (texture(Sampler0, vec2(coords.x + h_offset, coords.y)).a != 0)
                                     {
                                         vtc = tint * texture(Sampler0, vec2(coords.x + h_offset, coords.y)).a;
                                     }
                                 }
                             }
                             else if(textureProperties.g == 0)
                             {
                                 if (textureProperties.r > 1)
                                 {
                                     vtc = vec4(1);
                                 }
                                 else if (textureProperties.r == 1)
                                 {
                                     if (texture(Sampler0, vec2(coords.x + h_offset, coords.y)).a != 0)
                                     {
                                         vtc = vec4(1) * texture(Sampler0, vec2(coords.x + h_offset, coords.y)).a;
                                     }
                                     else
                                     {
                                         vtc = minecraft_mix_light(Light0_Direction, Light1_Direction, vNormal, vec4(1)) * texel;
                                     }
                                 }
                                 else
                                 {
                                     vtc = minecraft_mix_light(Light0_Direction, Light1_Direction, vNormal, vec4(1)) * texel;
                                 }
                             }
                             else
                             {
                                 vtc = minecraft_mix_light(Light0_Direction, Light1_Direction, vNormal, vec4(1)) * texel;
                             }
                     
                             vec4 armor = mix(texture(Sampler0, coords), texture(Sampler0, nextFrame), interpolClock);
                     
                             // If it's the first leather texture in the atlas (used for the vanilla leather texture, with no custom color specified)
                             if (coords.x < (1 / armorAmount))
                                 color = armor * vertexColor * ColorModulator;
                             else // If it's a custom texture
                                 color = armor * vtc * ColorModulator;
                         }
                         else // If it's another vanilla armor, for example diamond_layer_1.png or diamond_layer_2.png
                         {
                             color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
                         }
                     
                         if (color.a < 0.1)
                             discard;
                     
                         fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
                     }
                    """.replace(SHADER_PARAMETER_PLACEHOLDER, String.valueOf((int) Settings.CUSTOM_ARMOR_SHADER_RESOLUTION.getValue())).trim();
        }

        private static String getShaderJson() {
            return """ 
                    {
                         "blend": {
                             "func": "add",
                             "srcrgb": "srcalpha",
                             "dstrgb": "1-srcalpha"
                         },
                         "vertex": "rendertype_armor_cutout_no_cull",
                         "fragment": "rendertype_armor_cutout_no_cull",
                         "attributes": [
                             "Position",
                             "Color",
                             "UV0",
                             "UV1",
                             "UV2",
                             "Normal"
                         ],
                         "samplers": [
                             { "name": "Sampler0" },
                             { "name": "Sampler2" }
                         ],
                         "uniforms": [
                             { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                             { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                             { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
                             { "name": "Light0_Direction", "type": "float", "count": 3, "values": [0.0, 0.0, 0.0] },
                             { "name": "Light1_Direction", "type": "float", "count": 3, "values": [0.0, 0.0, 0.0] },
                             { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                             { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                             { "name": "FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
                             { "name": "GameTime", "type": "float", "count": 1, "values": [ 1.0 ] }
                         ]
                     }
                    """.trim();
        }

        private static String getLicense() {
            return """
                    Author of this shader is Ancientkingg
                                    
                    This allowed to commercially use with reference to original author.
                    Original license: https://github.com/Ancientkingg/fancyPants/blob/master/README.md
                    """.trim();
        }

    }

    private static class LessFancyArmorShaders {
        private static void generateArmorShaderFiles() {
            String shaders = "assets/minecraft/shaders";
            ResourcePack.writeStringToVirtual(shaders + "/core", "rendertype_armor_cutout_no_cull.json", getShaderJson());
            ResourcePack.writeStringToVirtual(shaders + "/core", "rendertype_outline.json", getOutlineJson());
            ResourcePack.writeStringToVirtual(shaders + "/core/render", "armor.vsh", getArmorVsh());
            ResourcePack.writeStringToVirtual(shaders + "/core/render", "armor.fsh", getArmorFsh());
            ResourcePack.writeStringToVirtual(shaders + "/core/render", "glowing.vsh", getGlowingVsh());
            ResourcePack.writeStringToVirtual(shaders + "/core/render", "glowing.fsh", getGlowingFsh());
            ResourcePack.writeStringToVirtual(shaders + "/include", "LICENSE.md", getFogGlsl());
        }

        public static String getShaderJson() {
            return """ 
                    {
                         "blend": {
                             "func": "add",
                             "srcrgb": "srcalpha",
                             "dstrgb": "1-srcalpha"
                         },
                         "vertex": "render/armor",
                         "fragment": "render/armor",
                         "attributes": [
                             "Position",
                             "Color",
                             "UV0",
                             "UV1",
                             "UV2",
                             "Normal"
                         ],
                         "samplers": [
                             { "name": "Sampler0" },
                             { "name": "Sampler1" },
                             { "name": "Sampler2" }
                         ],
                         "uniforms": [
                             { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                             { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                             { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
                             { "name": "Light0_Direction", "type": "float", "count": 3, "values": [0.0, 0.0, 0.0] },
                             { "name": "Light1_Direction", "type": "float", "count": 3, "values": [0.0, 0.0, 0.0] },
                             { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                             { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                             { "name": "FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
                             { "name": "GameTime", "type": "float", "count": 1, "values": [ 1.0 ] }
                         ]
                     }""".trim();
        }

        public static String getOutlineJson() {
            return """
                    {
                        "blend": {
                            "func": "add",
                            "srcrgb": "srcalpha",
                            "dstrgb": "1-srcalpha"
                        },
                        "vertex": "render/glowing",
                        "fragment": "render/glowing",
                        "attributes": [
                            "Position",
                            "Color",
                            "UV0"
                        ],
                        "samplers": [
                            { "name": "Sampler0" }
                        ],
                        "uniforms": [
                            { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                            { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                            { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] }
                        ]
                    }""".trim();
        }

        public static String getArmorVsh() {
            return """
                    #version 150
                                        
                    #moj_import <light.glsl>
                    #moj_import <fog.glsl>
                                        
                    in vec3 Position;
                    in vec4 Color;
                    in vec2 UV0;
                    in ivec2 UV1;
                    in ivec2 UV2;
                    in vec3 Normal;
                                        
                    uniform sampler2D Sampler0;
                    uniform sampler2D Sampler1;
                    uniform sampler2D Sampler2;
                                        
                    uniform mat4 ModelViewMat;
                    uniform mat4 ProjMat;
                    uniform mat3 IViewRotMat;
                    uniform int FogShape;
                                        
                    uniform vec3 Light0_Direction;
                    uniform vec3 Light1_Direction;
                                        
                    out float vertexDistance;
                    out vec4 vertexColor;
                    out vec4 tintColor;
                    out vec4 lightColor;
                    out vec4 overlayColor;
                    out vec2 uv;
                    out vec4 normal;
                                        
                    int toint(vec3 c) {
                        ivec3 v = ivec3(c*255);
                        return (v.r<<16)+(v.g<<8)+v.b;
                    }
                                        
                    void main() {
                        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                                        
                        vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
                        tintColor = Color;
                        vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, vec4(1));
                        lightColor = minecraft_sample_lightmap(Sampler2, UV2);
                        overlayColor = texelFetch(Sampler1, UV1, 0);
                        uv = UV0;
                        normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
                                        
                        //number of armors from texture size
                        vec2 size = textureSize(Sampler0, 0);
                        int n = int(2*size.y/size.x);
                        //if theres more than 1 custom armor
                        if (n > 1 && size.x < 256) {
                            //divide uv by number of armors, it is now on the first armor
                            uv.y /= n;
                            //if color index is within number of armors
                            int i = toint(Color.rgb);
                            if (i < n) {
                                //move uv down to index
                                uv.y += i*size.x/size.y/2.;
                                //remove tint color
                                tintColor = vec4(1);
                            }
                        }
                    }""".trim();
        }

        public static String getArmorFsh() {
            return """
                    #version 150
                                        
                    #moj_import <fog.glsl>
                                        
                    uniform sampler2D Sampler0;
                                        
                    uniform vec4 ColorModulator;
                    uniform float FogStart;
                    uniform float FogEnd;
                    uniform vec4 FogColor;
                                        
                    in float vertexDistance;
                    in vec4 vertexColor;
                    in vec4 tintColor;
                    in vec4 lightColor;
                    in vec4 overlayColor;
                    in vec2 uv;
                    in vec4 normal;
                                        
                    out vec4 fragColor;
                                        
                    void main() {
                        vec4 color = texture(Sampler0, uv);
                        if (color.a < 0.1) discard;
                        color *= tintColor * ColorModulator;
                        color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
                        color *= vertexColor * lightColor; //shading
                        fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
                    }""".trim();
        }

        public static String getGlowingVsh() {
            return """
                    #version 150
                                        
                    uniform sampler2D Sampler0;
                                        
                    in vec3 Position;
                    in vec4 Color;
                    in vec2 UV0;
                                        
                    uniform mat4 ModelViewMat;
                    uniform mat4 ProjMat;
                                        
                    out vec4 vertexColor;
                    out vec2 uv;
                                        
                    void main() {
                        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                        vertexColor = Color;
                        uv = UV0;
                                        
                        //we assume if y >= 2x it is an armor and divide uv
                        //cannot pass tint color here so it's the only option
                        vec2 size = textureSize(Sampler0, 0);
                        if (size.y >= 2*size.x && size.x < 256) {
                            uv.y /= 2.*size.y/size.x;
                        }
                    }""".trim();
        }

        public static String getGlowingFsh() {
            return """
                    #version 150
                                        
                    uniform sampler2D Sampler0;
                                        
                    uniform vec4 ColorModulator;
                                        
                    in vec4 vertexColor;
                    in vec2 uv;
                                        
                    out vec4 fragColor;
                                        
                    void main() {
                        vec4 color = texture(Sampler0, uv);
                        if (color.a == 0.0) discard;
                        fragColor = vec4(ColorModulator.rgb * vertexColor.rgb, ColorModulator.a);
                    }""".trim();
        }

        public static String getFogGlsl() {
            return """
                    #version 150
                                        
                    vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
                        if (vertexDistance <= fogStart) {
                            return inColor;
                        }
                        float fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogEnd, vertexDistance) : 1.0;
                        return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
                    }
                                        
                    float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
                        if (vertexDistance <= fogStart) {
                            return 1.0;
                        } else if (vertexDistance >= fogEnd) {
                            return 0.0;
                        }
                        return smoothstep(fogEnd, fogStart, vertexDistance);
                    }
                                        
                    float fog_distance(mat4 modelViewMat, vec3 pos, int shape) {
                        if (shape == 0) {
                            return length((modelViewMat * vec4(pos, 1.0)).xyz);
                        } else {
                            float distXZ = length((modelViewMat * vec4(pos.x, 0.0, pos.z, 1.0)).xyz);
                            float distY = length((modelViewMat * vec4(0.0, pos.y, 0.0, 1.0)).xyz);
                            return max(distXZ, distY);
                        }
                    }""".trim();
        }
    }

}
