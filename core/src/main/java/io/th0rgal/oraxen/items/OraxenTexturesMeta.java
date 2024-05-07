package io.th0rgal.oraxen.items;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTextures;
import team.unnamed.creative.texture.Texture;

import java.util.List;

public interface OraxenTexturesMeta {
    @Nullable
    List<Texture> textures();

    @NotNull
    ModelTextures modelTextures();

    @NotNull
    Model.Builder model();
}
