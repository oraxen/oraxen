package io.th0rgal.oraxen.bbmodel;

import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTextures;
import team.unnamed.creative.texture.Texture;

import java.util.List;

public record OraxenBBModel(
        @NotNull List<Texture> textures,
        @NotNull ModelTextures modelTextures,
        @NotNull Model.Builder builder
) {
}
