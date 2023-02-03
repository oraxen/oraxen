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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

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
           img = rescaleVanillaArmor(img);
            layer1 = initLayer(img);
            layer1Width += layer1.getWidth();
            return true;
        }

        if (name.equals("leather_layer_2.png")) {
            img = rescaleVanillaArmor(img);
            layer2 = initLayer(img);
            layer2Width += layer2.getWidth();
            return true;
        }

        return name.contains("armor_layer_") && handleArmorLayer(name, file);
    }

    private int getLayerHeight() {
        return resolution * HEIGHT_RATIO;
    }

    private BufferedImage initLayer(BufferedImage original) {
        if (original.getWidth() == resolution * WIDTH_RATIO && original.getHeight() == getLayerHeight())
            return original;

        Image scaled = original.getScaledInstance(
                resolution * WIDTH_RATIO, original.getHeight(), Image.SCALE_DEFAULT);
        BufferedImage output = new BufferedImage(
                resolution * WIDTH_RATIO, original.getHeight(), BufferedImage.TYPE_INT_ARGB);
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

        addPixel(image, builder, file, name, prefix, isEmissive, isAnimated);

        return true;
    }

    private void addPixel(BufferedImage image, ItemBuilder builder, File file, String name, String prefix, boolean isEmissive, boolean isAnimated) {
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

    /*public void rescaleVanillaArmorFiles(List<VirtualFile> output) {
        List<VirtualFile> armorFiles = new ArrayList<>(output.stream().filter(v -> v.getPath().startsWith("assets/minecraft/textures/models/armor/")
                && (v.getPath().endsWith("_layer_1.png") || v.getPath().endsWith("_layer_2.png") || v.getPath().endsWith("_layer_1_overlay.png") || v.getPath().endsWith("_layer_2_overlay.png"))).toList());

        // If there is no need to rescale, do not include the vanilla armor files
        if (resolution == DEFAULT_RESOLUTION) {
            armorFiles.removeIf(v -> v.getPath().endsWith("_layer_1.png") || v.getPath().endsWith("_layer_2.png"));
            output.removeAll(armorFiles);
        } else {
            Logs.logSuccess("Starting rescaling of vanilla armor due to armor_resolution and use of higher resolution custom armor...");
            // Remove all non-leather, diamond, gold, iron and chainmail sets
            Logs.debug("Removed any vanilla file: " + armorFiles.removeIf(v -> {
                String mat = Utils.getLastStringInSplit(v.getPath(), "/").split("\\.")[0].toUpperCase();
                return Arrays.stream(Material.values()).anyMatch(m -> m.toString().equals(mat)) && !mat.equals("CHAINMAIL");
            }));

            for (VirtualFile file : armorFiles) {
                BufferedImage original;
                try {
                    original = ImageIO.read(file.getInputStream());
                } catch (IOException e) {
                    Logs.logWarning("Error while upscaling " + file.getPath());
                    return;
                }
                //TODO Figure out how to handle custom armor and leather_layer files
                // Ideally we resize leather armor before those are generated so that leather fits
                int width = resolution * WIDTH_RATIO;
                int height = resolution * HEIGHT_RATIO;
                if (original.getWidth() == width && original.getHeight() == height) return;

                Image resizedImage = original.getScaledInstance(width, height, Image.SCALE_DEFAULT);
                BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                outputImage.getGraphics().drawImage(resizedImage, 0, 0, null);

                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ImageIO.write(outputImage, "png", os);
                    InputStream fis = new ByteArrayInputStream(os.toByteArray());
                    file.setInputStream(fis);
                } catch (IOException e) {
                    Logs.logWarning("Error while upscaling " + file.getPath());
                    return;
                }
            }
        }
        Logs.logSuccess("Finished rescaling and adding vanilla armor files!");
    }*/

    public BufferedImage rescaleVanillaArmor(BufferedImage original) {
        Logs.logSuccess("Starting rescaling of vanilla armor due to armor_resolution and use of higher resolution custom armor...");
        int width = resolution * WIDTH_RATIO;
        int height = resolution * HEIGHT_RATIO;
        Image resizedImage = original.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        outputImage.getGraphics().drawImage(resizedImage, 0, 0, null);
        Logs.logSuccess("Finished rescaling and adding vanilla armor files!");
        return outputImage;
    }

    public boolean shouldGenerateOptifineFiles() {
        return Settings.AUTOMATICALLY_GENERATE_SHADER_COMPATIBLE_ARMOR.toBool();
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
            } catch (IOException ignored) {}

            if (fileName.endsWith("_a.png"))
                optifineFiles.addAll(getOptifineAnimFiles(armorFile.getValue(), fileName, parentFolder));
        }

        try {
            InputStream inputStream = new ByteArrayInputStream("suffix.emissive=_e".getBytes(StandardCharsets.UTF_8));
            optifineFiles.add(new VirtualFile("assets/minecraft/optifine", "emissive.properties", inputStream));
            inputStream.close();
        } catch (IOException ignored) {
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

        BufferedImage leatherLayer1;
        BufferedImage leatherLayer2;
        BufferedImage leatherOverlay;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        ByteArrayOutputStream osOverlay = new ByteArrayOutputStream();

        try {
            leatherLayer1 = ImageIO.read(leatherFile1);
            leatherLayer2 = ImageIO.read(leatherFile2);
            leatherOverlay = ImageIO.read(leatherFileOverlay);

            ImageIO.write(leatherLayer1, "png", os);
            ImageIO.write(leatherLayer2, "png", os2);
            ImageIO.write(leatherOverlay, "png", osOverlay);

            InputStream is = new ByteArrayInputStream(os.toByteArray());
            InputStream is2 = new ByteArrayInputStream(os2.toByteArray());
            InputStream isOverlay = new ByteArrayInputStream(osOverlay.toByteArray());

            leatherArmors.add(new VirtualFile(leatherPath, "leather_armor_layer_1.png", is));
            leatherArmors.add(new VirtualFile(leatherPath, "leather_armor_layer_2.png", is2));
            leatherArmors.add(new VirtualFile(leatherPath, "leather_armor_overlay.png", isOverlay));

            is.close();
            is2.close();
            isOverlay.close();

            os.close();
            os2.close();
            osOverlay.close();
        } catch (IOException e) {
            return leatherArmors;
        }

        String content = correctLeatherPropertyFile(getArmorPropertyFile("leather_armor_layer_1.png", "", 0));
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        leatherArmors.add(new VirtualFile(leatherPath, "leather.properties", inputStream));
        return leatherArmors;
    }

    private String correctLeatherPropertyFile(String content) {
        return content
                .replace("texture.leather_layer_1_overlay=leather_armor_layer_1.png",
                        "texture.leather_layer_1_overlay=leather_armor_overlay.png")
                .replace("texture.leather_layer_2_overlay=leather_armor_layer_2.png",
                        "texture.leather_layer_2_overlay=leather_armor_overlay.png");
    }

    private String getArmorPropertyFile(String fileName, String cmdProperty, int weight) {
        return """
                type=armor
                items=minecraft:leather_helmet minecraft:leather_chestplate minecraft:leather_leggings minecraft:leather_boots
                texture.leather_layer_1=""" + fileName.replace("_2.png", "_1.png") + "\n" + """
                texture.leather_layer_1_overlay=""" + fileName.replace("_2.png", "_1.png") + "\n" + """
                texture.leather_layer_2=""" + fileName.replace("_1.png", "_2.png") + "\n" + """
                texture.leather_layer_2_overlay=""" + fileName.replace("_1.png", "_2.png") + "\n" +
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
