package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.bbmodel.OraxenBBModel;
import io.th0rgal.oraxen.bbmodel.OraxenBBModelGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTextures;
import team.unnamed.creative.texture.Texture;

import java.util.List;

public class OraxenBBModelTexturesMeta implements OraxenTexturesMeta {

    private final OraxenBBModel model;

    public OraxenBBModelTexturesMeta(@NotNull OraxenMeta meta, @NotNull OraxenBBModelGenerator generator, int[] animations) {
        model = generator.build(meta.modelKey(), animations);
        model.builder().key(meta.modelKey());
    }

    @Nullable
    @Override
    public List<Texture> textures() {
        return model.textures();
    }

    @NotNull
    @Override
    public Model.Builder model() {
        return model.builder();
    }

    @NotNull
    @Override
    public ModelTextures modelTextures() {
        return model.modelTextures();
    }
}
