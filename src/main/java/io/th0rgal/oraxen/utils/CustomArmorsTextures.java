package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import net.kyori.adventure.text.minimessage.Template;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Color;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
    private BufferedImage layer2;
    private int layer2Width = 0;

    public CustomArmorsTextures() {
        this(DEFAULT_RESOLUTION);
    }

    public CustomArmorsTextures(int resolution) {
        this.resolution = resolution;
    }

    public boolean registerImage(File file) throws IOException {
        String name = file.getName();

        if (!name.endsWith(".png")) return false;
        if (!name.contains("armor_layer") && !name.contains("leather_layer")) return false;

        if (name.equals("leather_layer_1.png")) {
            layer1 = initLayer(ImageIO.read(file));
            layer1Width += layer1.getWidth();
            return true;
        }

        if (name.equals("leather_layer_2.png")) {
            layer2 = initLayer(ImageIO.read(file));
            layer2Width += layer2.getWidth();
            return true;
        }

        return name.contains("armor_layer_") && handleArmorLayer(name, file);
    }

    private int getLayerHeight() {
        return resolution * HEIGHT_RATIO;
    }

    private BufferedImage initLayer(BufferedImage original) {
        if (original.getWidth() == resolution * WIDTH_RATIO && original.getHeight() == getLayerHeight()) {
            return original;
        }
        Image scaled = original.getScaledInstance(
                resolution * WIDTH_RATIO, getLayerHeight(), Image.SCALE_DEFAULT);
        BufferedImage output = new BufferedImage(
                resolution * WIDTH_RATIO, getLayerHeight(), BufferedImage.TYPE_INT_ARGB);
        output.getGraphics().drawImage(scaled, 0, 0, null);
        return output;
    }

    private boolean handleArmorLayer(String name, File file) throws IOException {
        if (name.endsWith("_e.png"))
            return true;

        String prefix = name.split("armor_layer_")[0];
        ItemBuilder builder = null;
        for (String suffix : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
            builder = OraxenItems.getItemById(prefix + suffix);
            if (builder != null)
                break;
        }
        if (builder == null) {
            Message.NO_ARMOR_ITEM.log(Template.template("name", prefix + "<part>"),
                    Template.template("armor_layer_file", name));
            return true;
        }
        BufferedImage image = initLayer(ImageIO.read(file));
        File emissiveFile = new File(file.getPath().replace(".png", "_e.png"));
        if (emissiveFile.exists()) {
            BufferedImage emissiveImage = initLayer(ImageIO.read(emissiveFile));
            image = mergeImages(image.getWidth() + emissiveImage.getWidth(),
                    image.getHeight(),
                    image, emissiveImage);
            setPixel(image.getRaster(), 2, 0, Color.fromRGB(1, 0, 0));
        }
        addPixel(image, builder, name, prefix);

        return true;
    }

    private void addPixel(BufferedImage image, ItemBuilder builder, String name, String prefix) {
        Color stuffColor = builder.getColor();
        if (usedColors.containsKey(stuffColor.asRGB())) {
            String detectedPrefix = usedColors.get(stuffColor.asRGB());
            if (!detectedPrefix.equals(prefix))
                Message.DUPLICATE_ARMOR_COLOR.log(
                        Template.template("first_armor_prefix", prefix),
                        Template.template("second_armor_prefix", detectedPrefix));
        } else usedColors.put(stuffColor.asRGB(), prefix);

        setPixel(image.getRaster(), 0, 0, stuffColor);
        if (name.contains("armor_layer_1")) {
            layers1.add(image);
            layer1Width += image.getWidth();
        } else {
            layers2.add(image);
            layer2Width += image.getWidth();
        }
    }

    public boolean hasCustomArmors() {
        return !(layers1.isEmpty() || layers2.isEmpty() || layer1 == null || layer2 == null);
    }

    public InputStream getLayerOne() throws IOException {
        return getInputStream(layer1Width, getLayerHeight(), layer1, layers1);
    }

    public InputStream getLayerTwo() throws IOException {
        return getInputStream(layer2Width, getLayerHeight(), layer2, layers2);
    }

    private final String OPTIFINE_ARMOR_PATH = "assets/minecraft/optifine/cit/armors/";

    public boolean shouldGenerateOptifineFiles() {
        return Settings.AUTOMATICALLY_GENERATE_SHADER_COMPATIBLE_ARMOR.toBool();
    }

    public Set<VirtualFile> getOptifineFiles() throws FileNotFoundException {
        Set<VirtualFile> optifineFiles = new HashSet<>(generateLeatherArmors());

        for (Map.Entry<String, InputStream> armorFile : getAllArmors().entrySet()) {
            String fileName = armorFile.getKey().split("/")[armorFile.getKey().split("/").length - 1];
            String parentFolder = fileName.split("_")[0];
            String path = OPTIFINE_ARMOR_PATH + parentFolder;
            optifineFiles.add(new VirtualFile(path, fileName, armorFile.getValue()));

            // Avoid duplicate properties files as this is called for both layers, but only needs 1 property file
            if (optifineFiles.stream().map(VirtualFile::getPath).anyMatch(
                    p -> Objects.equals(p, path + "/" + parentFolder + ".properties"))) continue;

            // Queries all items and finds custom armors custommodeldata
            String cmdProperty = "nbt.CustomModelData=" + OraxenItems.getEntries().stream().filter(e ->
                    e.getValue().build().getType().toString().startsWith("LEATHER_") &&
                    e.getValue().hasOraxenMeta() && !e.getValue().getOraxenMeta().getLayers().isEmpty() &&
                            e.getValue().getOraxenMeta().getLayers().get(0).contains(parentFolder)
            ).map(s -> s.getValue().getOraxenMeta().getCustomModelData()).findFirst().orElse(0);

            String propContent = getArmorPropertyFile(fileName, cmdProperty);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(propContent.getBytes(StandardCharsets.UTF_8));
            optifineFiles.add(new VirtualFile(path, parentFolder + ".properties", inputStream));
        }

        return optifineFiles;
    }

    private List<VirtualFile> generateLeatherArmors() {
        List<VirtualFile> leatherArmors = new ArrayList<>();
        File leatherFile1 = new File(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/pack/textures/required/leather_layer_1.png");
        File leatherFile2 = new File(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/pack/textures/required/leather_layer_2.png");
        String leatherPath = OPTIFINE_ARMOR_PATH + "leather";

        // If someone deletes required or compiles, don't fail simply break leather shader armor
        if (!leatherFile1.exists() || !leatherFile2.exists()) return leatherArmors;

        try(final FileInputStream layerOne = new FileInputStream(leatherFile1); final FileInputStream layerTwo = new FileInputStream(leatherFile2)) {
            leatherArmors.add(new VirtualFile(leatherPath, "leather_layer_1.png", layerOne));
            leatherArmors.add(new VirtualFile(leatherPath, "leather_layer_2.png", layerTwo));
        } catch (IOException e) {
            return leatherArmors;
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(getArmorPropertyFile("leather_layer_1.png", "").getBytes(StandardCharsets.UTF_8));
        leatherArmors.add(new VirtualFile(leatherPath, "leather.properties", inputStream));
        return leatherArmors;
    }

    private String getArmorPropertyFile(String fileName, String cmdProperty) {
        return """
                type=armor
                items=minecraft:leather_helmet minecraft:leather_chestplate minecraft:leather_leggings minecraft:leather_boots
                texture.leather_layer_1=""" + fileName.replace("_2.png", "_1.png") + "\n" + """
                texture.leather_layer_1_overlay=""" + fileName.replace("_2.png", "_1.png") + "\n" + """
                texture.leather_layer_2=""" + fileName.replace("_1.png", "_2.png") + "\n" + """
                texture.leather_layer_2_overlay=""" + fileName.replace("_1.png", "_2.png") + "\n" +
                cmdProperty;
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

            if (isArmor && !inLayerList && builder.hasOraxenMeta() && layerList.size() == 2) {
                for (String file : layerList) {
                    int id = layers.keySet().stream().anyMatch(s -> s.contains(armorType)) ? 2 : 1;

                    String fileName = armorType + "_armor_layer_" + id + ".png";
                    String absolutePath = OraxenPlugin.get().getDataFolder().getAbsolutePath() + "/pack/textures/";
                    String fileFolder =  absolutePath + StringUtils.substringBeforeLast(file, itemId) + fileName;

                    try(final FileInputStream f = new FileInputStream(fileFolder)) {
                        layers.put(fileName, f);
                    } catch (IOException ignored) {
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
        ImageIO.write(mergeImages(layerWidth, layerHeight, layers.toArray(new BufferedImage[0])),
                "png", outputStream);
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
