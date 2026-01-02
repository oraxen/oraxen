package io.th0rgal.oraxen.mechanics.provided.gameplay.shaped;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MechanicInfo(
    category = "gameplay",
    description = "Allows creating custom stairs, slabs, doors, trapdoors, and grates using waxed copper blocks as base"
)
public class ShapedBlockMechanicFactory extends MechanicFactory {

    private static ShapedBlockMechanicFactory instance;

    // Map of Material -> Mechanic for quick lookup
    private final Map<Material, ShapedBlockMechanic> mechanicByMaterial = new HashMap<>();

    // Track which variations are used per block type
    private final Map<ShapedBlockType, boolean[]> usedVariations = new HashMap<>();

    // Store model names for blockstate generation
    private final Map<Material, String> modelByMaterial = new HashMap<>();

    // Store texture info for generating variant models
    private final Map<Material, String> textureByMaterial = new HashMap<>();

    // Store block type for each material
    private final Map<Material, ShapedBlockType> typeByMaterial = new HashMap<>();

    private final List<String> toolTypes;
    private final boolean convertVanillaWaxed;
    private final boolean handleWorldGeneration;

    @ConfigProperty(type = PropertyType.STRING, description = "Type of shaped block: STAIR, SLAB, DOOR, TRAPDOOR, GRATE")
    public static final String PROP_TYPE = "type";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Custom variation (1-4, maps to copper oxidation states)", defaultValue = "1", min = 1, max = 4)
    public static final String PROP_CUSTOM_VARIATION = "custom_variation";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Block hardness for mining", defaultValue = "3", min = -1)
    public static final String PROP_HARDNESS = "hardness";

    public ShapedBlockMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        toolTypes = section.getStringList("tool_types");
        convertVanillaWaxed = section.getBoolean("convert_vanilla_waxed", true);
        handleWorldGeneration = section.getBoolean("handle_world_generation", true);

        // Initialize variation tracking
        for (ShapedBlockType type : ShapedBlockType.values()) {
            usedVariations.put(type, new boolean[4]);
        }

        // Register pack modifier to generate blockstate files after all items are parsed
        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(), packFolder -> {
            generateBlockstates();
        });

        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
            new ShapedBlockMechanicListener(this));

        Logs.logSuccess("ShapedBlockMechanicFactory initialized with tool types: " + toolTypes);
    }

    public static ShapedBlockMechanicFactory getInstance() {
        return instance;
    }

    /**
     * Whether to convert vanilla waxed copper blocks to non-waxed variants
     * with a marker to prevent oxidation. This allows vanilla copper appearance
     * while reserving waxed materials for custom blocks.
     */
    public boolean convertVanillaWaxed() {
        return convertVanillaWaxed;
    }

    /**
     * Whether to handle waxed copper blocks in world generation (e.g., Trial Chambers).
     * Converts them to non-waxed variants with oxidation prevention.
     */
    public boolean handleWorldGeneration() {
        return handleWorldGeneration;
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        ShapedBlockMechanic mechanic = new ShapedBlockMechanic(this, section);

        // Validate and register the variation
        ShapedBlockType type = mechanic.getBlockType();
        int variation = mechanic.getCustomVariation();

        boolean[] variations = usedVariations.get(type);
        if (variations[variation - 1]) {
            Logs.logError("Duplicate custom_variation " + variation + " for " + type +
                " shaped block in item: " + mechanic.getItemID());
            return null;
        }
        variations[variation - 1] = true;

        // Register the mechanic by material
        mechanicByMaterial.put(mechanic.getPlacedMaterial(), mechanic);

        // Store model name for blockstate generation
        String modelName = mechanic.getModel(section.getParent().getParent());
        Logs.logInfo("Shaped block " + mechanic.getItemID() + " model: " + modelName);
        if (modelName != null) {
            modelByMaterial.put(mechanic.getPlacedMaterial(), modelName);
            typeByMaterial.put(mechanic.getPlacedMaterial(), type);
            Logs.logInfo("Stored model for " + mechanic.getPlacedMaterial() + " -> " + modelName);

            // Store texture for generating variant models
            ConfigurationSection packSection = section.getParent().getParent().getConfigurationSection("Pack");
            if (packSection != null) {
                List<String> textures = packSection.getStringList("textures");
                Logs.logInfo("Shaped block " + mechanic.getItemID() + " textures from config: " + textures);
                if (!textures.isEmpty()) {
                    String texture = textures.get(0);
                    Logs.logInfo("Storing texture for " + mechanic.getPlacedMaterial() + ": " + texture);
                    textureByMaterial.put(mechanic.getPlacedMaterial(), texture);
                }
            }
        } else {
            Logs.logWarning("No model found for shaped block " + mechanic.getItemID());
        }

        addToImplemented(mechanic);
        Logs.logSuccess("Registered shaped block item: " + mechanic.getItemID() + " -> " +
            type + " variation " + variation + " (" + mechanic.getPlacedMaterial() + ")");
        return mechanic;
    }

    /**
     * Generate blockstate files and variant models for all registered shaped blocks
     */
    private void generateBlockstates() {
        Logs.logInfo("Generating blockstates for " + modelByMaterial.size() + " shaped blocks");
        for (Map.Entry<Material, String> entry : modelByMaterial.entrySet()) {
            Material material = entry.getKey();
            String modelName = entry.getValue();
            String texture = textureByMaterial.get(material);
            ShapedBlockType type = typeByMaterial.get(material);

            // Generate variant models if needed
            if (texture != null && type != null) {
                generateVariantModels(type, modelName, texture);
            }

            String blockstateName = material.name().toLowerCase() + ".json";
            String blockstateContent = generateBlockstateForMaterial(material, modelName);

            OraxenPlugin.get().getResourcePack().writeStringToVirtual(
                "assets/minecraft/blockstates", blockstateName, blockstateContent);

            Logs.logSuccess("Generated blockstate for " + material + " -> " + modelName);
        }
    }

    /**
     * Generate variant models for block types that need multiple models
     */
    private void generateVariantModels(ShapedBlockType type, String modelName, String texture) {
        // Always generate the base block model in the block/ folder
        generateBaseBlockModel(type, modelName, texture);

        switch (type) {
            case STAIR -> {
                // Generate inner and outer stair models for corner shapes
                generateStairVariantModels(modelName, texture);
            }
            case SLAB -> {
                // Generate slab_top and double slab models
                generateSlabVariantModels(modelName, texture);
            }
            case DOOR -> {
                // Generate all door variant models
                generateDoorVariantModels(modelName, texture);
            }
            case TRAPDOOR -> {
                // Generate trapdoor variants
                generateTrapdoorVariantModels(modelName, texture);
            }
            default -> {
                // Grates use the base model only
            }
        }
    }

    /**
     * Generate the base block model in the block/ folder for blockstate references
     */
    private void generateBaseBlockModel(ShapedBlockType type, String modelName, String texture) {
        JsonObject model = new JsonObject();
        JsonObject textures = new JsonObject();

        String texturePath = normalizeTexturePath(texture);

        Logs.logInfo("Generating base block model " + modelName + " with texture: " + texturePath);

        switch (type) {
            case STAIR -> {
                model.addProperty("parent", "block/stairs");
                textures.addProperty("bottom", texturePath);
                textures.addProperty("top", texturePath);
                textures.addProperty("side", texturePath);
            }
            case SLAB -> {
                model.addProperty("parent", "block/slab");
                textures.addProperty("bottom", texturePath);
                textures.addProperty("top", texturePath);
                textures.addProperty("side", texturePath);
            }
            case TRAPDOOR -> {
                model.addProperty("parent", "block/template_trapdoor_bottom");
                textures.addProperty("texture", texturePath);
            }
            case DOOR -> {
                model.addProperty("parent", "block/door_bottom_left");
                textures.addProperty("bottom", texturePath);
                textures.addProperty("top", texturePath);
            }
            case GRATE -> {
                model.addProperty("parent", "block/cube_all");
                textures.addProperty("all", texturePath);
            }
        }

        model.add("textures", textures);

        OraxenPlugin.get().getResourcePack().writeStringToVirtual(
            "assets/minecraft/models/block", modelName + ".json", model.toString());

        Logs.logInfo("Generated base block model for " + modelName);
    }

    /**
     * Generate slab variant models (_top and _double)
     */
    private void generateSlabVariantModels(String modelName, String texture) {
        String texturePath = normalizeTexturePath(texture);

        // Top slab model
        JsonObject topModel = new JsonObject();
        topModel.addProperty("parent", "block/slab_top");
        JsonObject topTextures = new JsonObject();
        topTextures.addProperty("bottom", texturePath);
        topTextures.addProperty("top", texturePath);
        topTextures.addProperty("side", texturePath);
        topModel.add("textures", topTextures);

        // Write to block/ subdirectory since blockstates look there by default
        OraxenPlugin.get().getResourcePack().writeStringToVirtual(
            "assets/minecraft/models/block", modelName + "_top.json", topModel.toString());

        // Double slab model (full block)
        JsonObject doubleModel = new JsonObject();
        doubleModel.addProperty("parent", "block/cube_all");
        JsonObject doubleTextures = new JsonObject();
        doubleTextures.addProperty("all", texturePath);
        doubleModel.add("textures", doubleTextures);

        OraxenPlugin.get().getResourcePack().writeStringToVirtual(
            "assets/minecraft/models/block", modelName + "_double.json", doubleModel.toString());

        Logs.logInfo("Generated slab variant models for " + modelName);
    }

    /**
     * Generate stair variant models (_inner and _outer for corner shapes)
     */
    private void generateStairVariantModels(String modelName, String texture) {
        String texturePath = normalizeTexturePath(texture);

        // Inner corner stair model
        JsonObject innerModel = new JsonObject();
        innerModel.addProperty("parent", "block/inner_stairs");
        JsonObject innerTextures = new JsonObject();
        innerTextures.addProperty("bottom", texturePath);
        innerTextures.addProperty("top", texturePath);
        innerTextures.addProperty("side", texturePath);
        innerModel.add("textures", innerTextures);

        OraxenPlugin.get().getResourcePack().writeStringToVirtual(
            "assets/minecraft/models/block", modelName + "_inner.json", innerModel.toString());

        // Outer corner stair model
        JsonObject outerModel = new JsonObject();
        outerModel.addProperty("parent", "block/outer_stairs");
        JsonObject outerTextures = new JsonObject();
        outerTextures.addProperty("bottom", texturePath);
        outerTextures.addProperty("top", texturePath);
        outerTextures.addProperty("side", texturePath);
        outerModel.add("textures", outerTextures);

        OraxenPlugin.get().getResourcePack().writeStringToVirtual(
            "assets/minecraft/models/block", modelName + "_outer.json", outerModel.toString());

        Logs.logInfo("Generated stair variant models for " + modelName);
    }

    /**
     * Generate door variant models (all 8 combinations of half/hinge/open)
     */
    private void generateDoorVariantModels(String modelName, String texture) {
        String texturePath = normalizeTexturePath(texture);
        // For doors, we need both bottom and top textures - use same texture for both
        String bottomTexture = texturePath;
        String topTexture = texturePath;

        // Model variants: {half}_{hinge}_{open}
        String[][] doorVariants = {
            {"lower", "left", "false", "block/door_bottom_left"},
            {"lower", "left", "true", "block/door_bottom_left_open"},
            {"lower", "right", "false", "block/door_bottom_right"},
            {"lower", "right", "true", "block/door_bottom_right_open"},
            {"upper", "left", "false", "block/door_top_left"},
            {"upper", "left", "true", "block/door_top_left_open"},
            {"upper", "right", "false", "block/door_top_right"},
            {"upper", "right", "true", "block/door_top_right_open"}
        };

        for (String[] variant : doorVariants) {
            String half = variant[0];
            String hinge = variant[1];
            String open = variant[2];
            String parentModel = variant[3];

            String suffix = "_" + half;
            if (hinge.equals("right")) suffix += "_hinge";
            if (open.equals("true")) suffix += "_open";

            JsonObject model = new JsonObject();
            model.addProperty("parent", parentModel);
            JsonObject textures = new JsonObject();
            textures.addProperty("bottom", bottomTexture);
            textures.addProperty("top", topTexture);
            model.add("textures", textures);

            OraxenPlugin.get().getResourcePack().writeStringToVirtual(
                "assets/minecraft/models/block", modelName + suffix + ".json", model.toString());
        }

        Logs.logInfo("Generated door variant models for " + modelName);
    }

    /**
     * Normalize texture path - add default/ prefix if no folder specified
     */
    private String normalizeTexturePath(String texture) {
        if (!texture.contains("/")) {
            return "default/" + texture;
        }
        return texture;
    }

    /**
     * Generate trapdoor variant models (_bottom, _open and _top)
     */
    private void generateTrapdoorVariantModels(String modelName, String texture) {
        String texturePath = normalizeTexturePath(texture);

        // Bottom trapdoor model (closed, on bottom half)
        JsonObject bottomModel = new JsonObject();
        bottomModel.addProperty("parent", "block/template_trapdoor_bottom");
        JsonObject bottomTextures = new JsonObject();
        bottomTextures.addProperty("texture", texturePath);
        bottomModel.add("textures", bottomTextures);

        // Write to block/ subdirectory since blockstates look there by default
        OraxenPlugin.get().getResourcePack().writeStringToVirtual(
            "assets/minecraft/models/block", modelName + "_bottom.json", bottomModel.toString());

        // Open trapdoor model
        JsonObject openModel = new JsonObject();
        openModel.addProperty("parent", "block/template_trapdoor_open");
        JsonObject openTextures = new JsonObject();
        openTextures.addProperty("texture", texturePath);
        openModel.add("textures", openTextures);

        OraxenPlugin.get().getResourcePack().writeStringToVirtual(
            "assets/minecraft/models/block", modelName + "_open.json", openModel.toString());

        // Top trapdoor model (closed, on top half)
        JsonObject topModel = new JsonObject();
        topModel.addProperty("parent", "block/template_trapdoor_top");
        JsonObject topTextures = new JsonObject();
        topTextures.addProperty("texture", texturePath);
        topModel.add("textures", topTextures);

        OraxenPlugin.get().getResourcePack().writeStringToVirtual(
            "assets/minecraft/models/block", modelName + "_top.json", topModel.toString());

        Logs.logInfo("Generated trapdoor variant models for " + modelName);
    }

    /**
     * Generate blockstate JSON for a specific material type
     */
    private String generateBlockstateForMaterial(Material material, String modelName) {
        ShapedBlockType type = ShapedBlockType.fromMaterial(material);
        if (type == null) return "{}";

        // Model names need block/ prefix for blockstate references
        // Strip any leading path segments to get just the model name
        String simpleModelName = modelName;
        if (modelName.contains("/")) {
            simpleModelName = modelName.substring(modelName.lastIndexOf("/") + 1);
        }
        // Add block/ prefix for blockstate model references
        String blockModelName = "block/" + simpleModelName;

        return switch (type) {
            case STAIR -> generateStairsBlockstate(blockModelName);
            case SLAB -> generateSlabBlockstate(blockModelName);
            case DOOR -> generateDoorBlockstate(blockModelName);
            case TRAPDOOR -> generateTrapdoorBlockstate(blockModelName);
            case GRATE -> generateGrateBlockstate(blockModelName);
        };
    }

    /**
     * Generate blockstate for stairs with all facing/half/shape combinations
     */
    private String generateStairsBlockstate(String modelName) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();

        String[] facings = {"east", "north", "south", "west"};
        String[] halves = {"bottom", "top"};
        String[] shapes = {"inner_left", "inner_right", "outer_left", "outer_right", "straight"};

        // Model rotations for each facing direction
        Map<String, Integer> yRotations = Map.of(
            "east", 0, "south", 90, "west", 180, "north", 270
        );

        for (String facing : facings) {
            for (String half : halves) {
                for (String shape : shapes) {
                    String variantKey = "facing=" + facing + ",half=" + half + ",shape=" + shape + ",waterlogged=false";
                    String variantKeyWater = "facing=" + facing + ",half=" + half + ",shape=" + shape + ",waterlogged=true";

                    JsonObject model = createStairModelVariant(modelName, facing, half, shape, yRotations.get(facing));

                    variants.add(variantKey, model);
                    variants.add(variantKeyWater, model.deepCopy());
                }
            }
        }

        blockstate.add("variants", variants);
        return blockstate.toString();
    }

    private JsonObject createStairModelVariant(String baseModel, String facing, String half, String shape, int baseY) {
        JsonObject model = new JsonObject();

        // Select the appropriate model based on shape
        String modelPath = baseModel;
        if (shape.startsWith("inner_")) {
            modelPath = baseModel + "_inner";
        } else if (shape.startsWith("outer_")) {
            modelPath = baseModel + "_outer";
        }
        model.addProperty("model", modelPath);

        // Calculate rotations based on facing, half, and shape
        int x = half.equals("top") ? 180 : 0;
        int y = baseY;

        // Adjust y rotation for shape variants
        if (shape.equals("inner_left") || shape.equals("outer_left")) {
            y = (y + 270) % 360;
        }
        if (half.equals("top")) {
            if (shape.equals("straight")) {
                y = (y + 180) % 360;
            } else if (shape.equals("inner_left")) {
                y = (y + 270) % 360;
            } else if (shape.equals("inner_right")) {
                y = (y + 180) % 360;
            } else if (shape.equals("outer_left")) {
                y = (y + 270) % 360;
            } else if (shape.equals("outer_right")) {
                y = (y + 180) % 360;
            }
        }

        if (x != 0) model.addProperty("x", x);
        if (y != 0) model.addProperty("y", y);
        model.addProperty("uvlock", true);

        return model;
    }

    /**
     * Generate blockstate for slabs
     */
    private String generateSlabBlockstate(String modelName) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();

        String[] types = {"bottom", "double", "top"};

        for (String type : types) {
            String variantKey = "type=" + type + ",waterlogged=false";
            String variantKeyWater = "type=" + type + ",waterlogged=true";

            JsonObject model = new JsonObject();
            if (type.equals("double")) {
                // Double slab uses a full block model
                model.addProperty("model", modelName + "_double");
            } else if (type.equals("top")) {
                model.addProperty("model", modelName + "_top");
            } else {
                model.addProperty("model", modelName);
            }

            variants.add(variantKey, model);
            variants.add(variantKeyWater, model.deepCopy());
        }

        blockstate.add("variants", variants);
        return blockstate.toString();
    }

    /**
     * Generate blockstate for doors
     */
    private String generateDoorBlockstate(String modelName) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();

        String[] facings = {"east", "north", "south", "west"};
        String[] halves = {"lower", "upper"};
        String[] hinges = {"left", "right"};
        String[] opens = {"false", "true"};

        Map<String, Integer> yRotations = Map.of(
            "east", 0, "south", 90, "west", 180, "north", 270
        );

        for (String facing : facings) {
            for (String half : halves) {
                for (String hinge : hinges) {
                    for (String open : opens) {
                        String variantKey = "facing=" + facing + ",half=" + half + ",hinge=" + hinge + ",open=" + open + ",powered=false";
                        String variantKeyPowered = "facing=" + facing + ",half=" + half + ",hinge=" + hinge + ",open=" + open + ",powered=true";

                        JsonObject model = new JsonObject();

                        // Determine model suffix
                        String modelSuffix = "_" + half;
                        if (hinge.equals("right")) {
                            modelSuffix += "_hinge";
                        }
                        if (open.equals("true")) {
                            modelSuffix += "_open";
                        }

                        model.addProperty("model", modelName + modelSuffix);

                        // Calculate y rotation
                        int y = yRotations.get(facing);
                        if (open.equals("true")) {
                            if (hinge.equals("left")) {
                                y = (y + 90) % 360;
                            } else {
                                y = (y + 270) % 360;
                            }
                        }
                        if (y != 0) model.addProperty("y", y);

                        variants.add(variantKey, model);
                        variants.add(variantKeyPowered, model.deepCopy());
                    }
                }
            }
        }

        blockstate.add("variants", variants);
        return blockstate.toString();
    }

    /**
     * Generate blockstate for trapdoors
     */
    private String generateTrapdoorBlockstate(String modelName) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();

        String[] facings = {"east", "north", "south", "west"};
        String[] halves = {"bottom", "top"};
        String[] opens = {"false", "true"};

        Map<String, Integer> yRotations = Map.of(
            "east", 90, "south", 180, "west", 270, "north", 0
        );

        for (String facing : facings) {
            for (String half : halves) {
                for (String open : opens) {
                    String variantKey = "facing=" + facing + ",half=" + half + ",open=" + open + ",powered=false,waterlogged=false";
                    String variantKeyPowered = "facing=" + facing + ",half=" + half + ",open=" + open + ",powered=true,waterlogged=false";
                    String variantKeyWater = "facing=" + facing + ",half=" + half + ",open=" + open + ",powered=false,waterlogged=true";
                    String variantKeyBoth = "facing=" + facing + ",half=" + half + ",open=" + open + ",powered=true,waterlogged=true";

                    JsonObject model = new JsonObject();

                    // Determine model suffix
                    String modelSuffix = "";
                    if (half.equals("top") && open.equals("false")) {
                        modelSuffix = "_top";
                    } else if (open.equals("true")) {
                        modelSuffix = "_open";
                    } else {
                        modelSuffix = "_bottom";
                    }

                    model.addProperty("model", modelName + modelSuffix);

                    int y = yRotations.get(facing);
                    if (y != 0) model.addProperty("y", y);

                    variants.add(variantKey, model);
                    variants.add(variantKeyPowered, model.deepCopy());
                    variants.add(variantKeyWater, model.deepCopy());
                    variants.add(variantKeyBoth, model.deepCopy());
                }
            }
        }

        blockstate.add("variants", variants);
        return blockstate.toString();
    }

    /**
     * Generate blockstate for grates (simple cube)
     */
    private String generateGrateBlockstate(String modelName) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();

        JsonObject model = new JsonObject();
        model.addProperty("model", modelName);

        variants.add("waterlogged=false", model);
        variants.add("waterlogged=true", model.deepCopy());

        blockstate.add("variants", variants);
        return blockstate.toString();
    }

    /**
     * Get the mechanic for a placed shaped block material
     */
    public ShapedBlockMechanic getMechanicByMaterial(Material material) {
        return mechanicByMaterial.get(material);
    }

    /**
     * Check if a material is a custom shaped block
     */
    public boolean isCustomShapedBlock(Material material) {
        return mechanicByMaterial.containsKey(material);
    }

    /**
     * Check if a material is used by the shaped block system (for vanilla prevention)
     */
    public boolean isShapedBlockMaterial(Material material) {
        return ShapedBlockType.fromMaterial(material) != null;
    }

    public List<String> getToolTypes() {
        return toolTypes;
    }

    /**
     * Get all registered shaped block mechanics
     */
    public Map<Material, ShapedBlockMechanic> getAllMechanics() {
        return new HashMap<>(mechanicByMaterial);
    }
}
