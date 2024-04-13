package io.th0rgal.oraxen.items;

import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;
import team.unnamed.creative.texture.Texture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OraxenKeyTexturesMeta implements OraxenTexturesMeta {

    private final OraxenMeta meta;
    private List<ModelTexture> textureLayers = new ArrayList<>();
    private Map<String, ModelTexture> textureVariables = new HashMap<>();
    private final ModelTextures modelTextures;

    public OraxenKeyTexturesMeta(@NotNull OraxenMeta meta, @NotNull ConfigurationSection section) {
        this.meta = meta;

        ConfigurationSection textureSection = section.getConfigurationSection("textures");
        if (textureSection != null) {
            ConfigurationSection texturesSection = section.getConfigurationSection("textures");
            assert texturesSection != null;
            Map<String, ModelTexture> variables = new HashMap<>();
            texturesSection.getKeys(false).forEach(key -> variables.put(key, ModelTexture.ofKey(Key.key(texturesSection.getString(key).replace(".png", "")))));
            this.textureVariables = variables;
        }
        else if (section.isList("textures")) this.textureLayers = section.getStringList("textures").stream().map(t -> ModelTexture.ofKey(Key.key(t.replace(".png", "")))).toList();
        else if (section.isString("textures")) this.textureLayers = List.of(ModelTexture.ofKey(Key.key(section.getString("textures").replace(".png", ""))));
        else if (section.isString("texture")) this.textureLayers = List.of(ModelTexture.ofKey(Key.key(section.getString("texture").replace(".png", ""))));

        this.textureVariables = textureVariables != null ? textureVariables : new HashMap<>();
        this.textureLayers = textureLayers != null ? textureLayers : new ArrayList<>();

        this.modelTextures = ModelTextures.builder()
                .particle(textureVariables.get("particle"))
                .variables(textureVariables)
                .layers(textureLayers)
                .build();
    }

    @Nullable
    @Override
    public List<Texture> textures() {
        return null;
    }

    @NotNull
    @Override
    public ModelTextures modelTextures() {
        return modelTextures;
    }

    @NotNull
    @Override
    public Model.Builder model() {
        final String parent = meta.parentModelKey().asMinimalString();
        ModelTextures.Builder textures = modelTextures.toBuilder();

        if (modelTextures.variables().isEmpty()) {
            final List<ModelTexture> layers = modelTextures.layers();
            textures.layers(List.of());
            if (parent.equals("block/cube") || parent.equals("block/cube_directional") || parent.equals("block/cube_mirrored")) {
                textures.addVariable("particle", layers.get(2));
                textures.addVariable("down", layers.get(0));
                textures.addVariable("up", layers.get(1));
                textures.addVariable("north", layers.get(2));
                textures.addVariable("south", layers.get(3));
                textures.addVariable("west", layers.get(4));
                textures.addVariable("east", layers.get(5));
            } else if (parent.equals("block/cube_all") || parent.equals("block/cube_mirrored_all")) {
                textures.addVariable("all", layers.get(0));
            } else if (parent.equals("block/cross")) {
                textures.addVariable("cross", layers.get(0));
            } else if (parent.startsWith("block/orientable")) {
                textures.addVariable("front", layers.get(0));
                textures.addVariable("side", layers.get(1));
                if (!parent.endsWith("vertical"))
                    textures.addVariable("top", layers.get(2));
                if (parent.endsWith("with_bottom"))
                    textures.addVariable("bottom", layers.get(3));
            } else if (parent.startsWith("block/cube_column")) {
                textures.addVariable("end", layers.get(0));
                textures.addVariable("side", layers.get(1));
            } else if (parent.equals("block/cube_bottom_top")) {
                textures.addVariable("top", layers.get(0));
                textures.addVariable("side", layers.get(1));
                textures.addVariable("bottom", layers.get(2));
            } else if (parent.equals("block/cube_top")) {
                textures.addVariable("top", layers.get(0));
                textures.addVariable("side", layers.get(1));
            } else textures.layers(layers);
        }

        return Model.model()
                .key(meta.modelKey())
                .parent(meta.parentModelKey())
                .textures(textures.build());
    }
}
