package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Color;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CustomArmorsTextures {

    static final int DEFAULT_RESOLUTION = 16;
    static final int HEIGHT_RATIO = 2;
    static final int WIDTH_RATIO = 4;

    private final Map<Integer, String> usedColors = new HashMap<>();
    private final List<BufferedImage> layers1 = new ArrayList<>();
    private final List<BufferedImage> layers2 = new ArrayList<>();
    private final int resolution;
    private BufferedImage layer1;
    private int layer1Width = 0;
    private int layer1Height = 0;
    private BufferedImage layer2;
    private int layer2Width = 0;
    private int layer2Height = 0;

    public CustomArmorsTextures() {
        this(DEFAULT_RESOLUTION);
    }

    public CustomArmorsTextures(int resolution) {
        this.resolution = resolution;
        //this.layer1Height = resolution * HEIGHT_RATIO;
    }

    public boolean registerImage(File file) {
        String name = file.getName();

        if (!name.endsWith(".png")) return false;
        if (!name.contains("armor_layer") && !name.contains("leather_layer")) return false;
        if (!Settings.GENERATE_CUSTOM_ARMOR_TEXTURES.toBool()) return false;

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
            setPixel(img.getRaster(), 0,1,Color.WHITE);
            layer1 = img;
            layer1Width += layer1.getWidth();
            return true;
        } else if (name.equals("leather_layer_2.png")) {
            img = rescaleArmorImage(img);
            img = initLayer(img);
            setPixel(img.getRaster(), 0,1,Color.WHITE);
            layer2 = img;
            layer2Width += layer2.getWidth();
            return true;
        }

        return name.contains("armor_layer_") && handleArmorLayer(name, file);
    }

    private int getLayerHeight() {
        return resolution * HEIGHT_RATIO;
    }

    private BufferedImage initLayer(BufferedImage original) {
        int newWidth = resolution * WIDTH_RATIO;
        int width = original.getWidth();
        int height = original.getHeight();
        if (width == newWidth && height == getLayerHeight()) return original;

        Image scaled = original.getScaledInstance(newWidth, height, Image.SCALE_DEFAULT);
        BufferedImage output = new BufferedImage(newWidth, height, BufferedImage.TYPE_INT_ARGB);
        output.getGraphics().drawImage(scaled, 0, 0, null);
        return output;
    }

    private boolean handleArmorLayer(String name, File file) {
        String prefix = name.split("armor_layer_")[0];
        ItemBuilder builder = null;

        // Skip actually editing the emissive image,
        // should check for file with same name + e to properly apply everything
        if (name.endsWith("_e.png")) return false;

        for (String suffix : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
            builder = OraxenItems.getItemById(prefix + suffix);
            ItemMeta meta = builder != null ? builder.build().getItemMeta() : null;
            if (builder != null && (meta instanceof LeatherArmorMeta || meta instanceof PotionMeta))
                break;
        }
        if (builder == null) {
            Message.NO_ARMOR_ITEM.log(AdventureUtils.tagResolver("name", prefix + "<part>"),
                    AdventureUtils.tagResolver("armor_layer_file", name));
            return true;
        }
        BufferedImage original;
        try {
            original = ImageIO.read(file);
        } catch (IOException e) {
            OraxenPlugin.get().getLogger().warning("Error while reading " + name + ": " + e.getMessage());
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

        if(image.getColorModel().getPixelSize() < 32) {
            int width = image.getWidth(), height = image.getHeight();
            Image resizedImage = original.getScaledInstance(width, height, Image.SCALE_DEFAULT);
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            image.getGraphics().drawImage(resizedImage, 0, 0, null);
        }
        addPixel(image, builder, name, prefix, isAnimated);

        return true;
    }

    private void addPixel(BufferedImage image, ItemBuilder builder, String name, String prefix, boolean isAnimated) {
        Color stuffColor = builder.getColor();
        if (stuffColor == null) return;
        if (usedColors.containsKey(stuffColor.asRGB())) {
            String detectedPrefix = usedColors.get(stuffColor.asRGB());
            if (!detectedPrefix.equals(prefix))
                Message.DUPLICATE_ARMOR_COLOR.log(
                        AdventureUtils.tagResolver("first_armor_prefix", prefix),
                        AdventureUtils.tagResolver("second_armor_prefix", detectedPrefix));
        } else usedColors.put(stuffColor.asRGB(), prefix);

        setPixel(image.getRaster(), 0, 0, stuffColor);
        if (isAnimated)
            setPixel(image.getRaster(), 1, 0, Color.fromRGB(image.getHeight() / (int) Settings.ARMOR_RESOLUTION.getValue(), getAnimatedArmorFramerate(), 1));
        if (name.contains("armor_layer_1")) {
            layers1.add(image);
            layer1Width += image.getWidth();
            layer1Height = Math.max(layer1Height, image.getHeight());
        } else {
            layers2.add(image);
            layer2Width += image.getWidth();
            layer2Height = Math.max(layer2Height, image.getHeight());
        }


        if (!isAnimated && image.getHeight() > getLayerHeight()) {
            Logs.logError("The height of " + name + " is greater than " + getLayerHeight() + "px.");
            Logs.logWarning("Since it is not an animated armor-file, this will potentially break other armor sets.");
            Logs.logWarning("If it is meant to be an animated armor-file, make sure it ends with _a.png or _a_e.png if emissive");
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

    private final String OPTIFINE_ARMOR_PATH = "assets/minecraft/optifine/cit/armors/";
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
            return Integer.parseInt(Settings.ANIMATED_ARMOR_FRAMERATE.getValue().toString());
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

    private InputStream getInputStream(int layerWidth, int layerHeight,
                                       BufferedImage layer, List<BufferedImage> layers) throws IOException {
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
        for (BufferedImage bufferedImage : images) {
            g2d.drawImage(bufferedImage, currentWidth, 0, null);
            currentWidth += bufferedImage.getWidth();
        }
        g2d.dispose();
        return concatImage;
    }

}
