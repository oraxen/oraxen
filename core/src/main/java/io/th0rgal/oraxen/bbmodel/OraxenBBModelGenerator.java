package io.th0rgal.oraxen.bbmodel;

import com.google.gson.*;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.base.Axis3D;
import team.unnamed.creative.base.CubeFace;
import team.unnamed.creative.base.Vector2Float;
import team.unnamed.creative.base.Vector3Float;
import team.unnamed.creative.metadata.Metadata;
import team.unnamed.creative.metadata.animation.AnimationMeta;
import team.unnamed.creative.model.*;
import team.unnamed.creative.texture.Texture;
import team.unnamed.creative.texture.TextureUV;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OraxenBBModelGenerator {
    private static final JsonArray DEFAULT_SCALE = new JsonArray();
    static {
        DEFAULT_SCALE.add(1F);
        DEFAULT_SCALE.add(1F);
        DEFAULT_SCALE.add(1F);
    }

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(Float3.class, (JsonDeserializer<Object>) (json, typeOfT, context) -> new Float3(json.getAsJsonArray()))
            .registerTypeAdapter(Float4.class, (JsonDeserializer<Object>) (json, typeOfT, context) -> new Float4(json.getAsJsonArray()))
            .registerTypeAdapter(ItemTransform.Type.class, (JsonDeserializer<Object>) (json, typeOfT, context) -> ItemTransform.Type.valueOf(json.getAsString().toUpperCase()))
            .registerTypeAdapter(ItemTransform.class, (JsonDeserializer<Object>) (json, typeOfT, context) -> {
                JsonObject object = json.getAsJsonObject();
                Float3 rotation = new Float3(defaultValue(object.getAsJsonArray("rotation"), DEFAULT_SCALE));
                Float3 translation = new Float3(defaultValue(object.getAsJsonArray("translation"), DEFAULT_SCALE));
                Float3 scale = new Float3(defaultValue(object.getAsJsonArray("scale"), DEFAULT_SCALE));
                return ItemTransform.transform()
                        .rotation(new Vector3Float(rotation.x, rotation.y, rotation.z))
                        .translation(new Vector3Float(translation.x, translation.y, translation.z))
                        .scale(new Vector3Float(scale.x, scale.y, scale.z))
                        .build();
            })
            .create();


    private static <T> T defaultValue(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private final BBModelData data;

    public OraxenBBModelGenerator(@NotNull JsonElement string) {
        data = GSON.fromJson(string, BBModelData.class);
    }

    public @NotNull OraxenBBModel build(@NotNull Key key, int[] animation) {
        List<Texture> textures = data.buildTextures(key, animation);
        ModelTextures model = ModelTextures.builder()
                .variables(IntStream.range(0, textures.size()).boxed().collect(Collectors.toMap(i -> Integer.toString(i), i -> ModelTexture.ofKey(Key.key(textures.get(i).key().asString().replace(".png", ""))))))
                .build();
        return new OraxenBBModel(
                textures,
                model,
                data.buildModels(model)
        );
    }

    private record BBModelData(
            @NotNull List<BBModelTextures> textures,
            @NotNull BBModelResolution resolution,
            @NotNull List<BBModelElement> elements,
            @NotNull Map<ItemTransform.Type, ItemTransform> display
    ) {

        @Subst("")
        private List<Texture> buildTextures(@Subst("") @NotNull Key key, int[] animations) {
            int imageIndex = 0;
            List<Texture> data = new ArrayList<>();
            for (BBModelTextures texture : textures) {
                byte[] bytes = Base64.getDecoder().decode(texture.source.split(",")[1]);
                Texture.Builder textureBuilder = Texture.texture()
                        .key(Key.key(key.namespace(), key.value() + "/" + (imageIndex++) + ".png"))
                        .data(w -> w.write(bytes));
                try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); BufferedInputStream stream = new BufferedInputStream(bis)) {
                    BufferedImage image = ImageIO.read(stream);
                    int div = image.getHeight() / image.getWidth();
                    if (div > 1) {
                        if (div != animations.length) Logs.logWarning("Animation frame length mismatched in " + key + ":" + div + " != " + animations.length);
                        AnimationMeta.Builder animation = AnimationMeta.animation();
                        for (int t = 0; t < Math.min(div, animations.length); t++) {
                            animation.addFrame(t, animations[t]);
                        }
                        textureBuilder.meta(Metadata.metadata()
                                .addPart(animation.build())
                                .build());
                    }
                } catch (IOException e) {
                    Logs.logWarning("Invalid image: " + key);
                }
                data.add(textureBuilder.build());
            }
            return data;
        }

        private void applyFace(@NotNull Element.Builder builder, @NotNull CubeFace face, @NotNull Float4 float4, @NotNull String texture, float uvScale) {
            builder.addFace(face, ElementFace.face()
                    .uv(TextureUV.uv(new Vector2Float(float4.fx / uvScale, float4.fy / uvScale), new Vector2Float(float4.tx / uvScale, float4.ty / uvScale)))
                    .texture("#" + texture)
                    .build());
        }

        public @NotNull Model.Builder buildModels(@NotNull ModelTextures modelTextures) {
            float uvScale = Math.max(resolution.height, resolution.width);
            return Model.model()
                    .textures(modelTextures)
                    .elements(elements.stream().map(e -> {
                        Element.Builder builder = Element.element()
                                .from(e.from.x, e.from.y, e.from.z)
                                .to(e.to.x, e.to.y, e.to.z);
                        applyFace(builder, CubeFace.NORTH, e.faces.north.uv, e.faces.north.texture, uvScale);
                        applyFace(builder, CubeFace.SOUTH, e.faces.south.uv, e.faces.south.texture, uvScale);
                        applyFace(builder, CubeFace.EAST, e.faces.east.uv, e.faces.east.texture, uvScale);
                        applyFace(builder, CubeFace.WEST, e.faces.west.uv, e.faces.west.texture, uvScale);
                        applyFace(builder, CubeFace.UP, e.faces.up.uv, e.faces.up.texture, uvScale);
                        applyFace(builder, CubeFace.DOWN, e.faces.down.uv, e.faces.down.texture, uvScale);
                        if (e.rotation != null) {
                            ElementRotation.Builder rotation = ElementRotation.builder();

                            if (e.rotation.x != 0F) {
                                rotation.axis(Axis3D.X);
                                rotation.angle(e.rotation.x);
                            } else if (e.rotation.y != 0F) {
                                rotation.axis(Axis3D.Y);
                                rotation.angle(e.rotation.y);
                            } else {
                                rotation.axis(Axis3D.Z);
                                rotation.angle(e.rotation.z);
                            }

                            if (e.origin != null) rotation.origin(new Vector3Float(
                                    e.origin.x,
                                    e.origin.y,
                                    e.origin.z
                            ));
                            builder.rotation(rotation.build());
                        }
                        return builder.build();
                    }).toArray(Element[]::new))
                    .display(display);
        }
    }

    private record BBModelTextures(@NotNull String source) {
    }
    private record BBModelResolution(float height, float width) {
    }

    private record BBModelElement(
            @NotNull Float3 from,
            @NotNull Float3 to,
            @Nullable Float3 origin,
            @Nullable Float3 rotation,
            @NotNull ElementFace faces
    ) {
        private record ElementFace(
            @NotNull ElementLocation north,
            @NotNull ElementLocation south,
            @NotNull ElementLocation east,
            @NotNull ElementLocation west,
            @NotNull ElementLocation up,
            @NotNull ElementLocation down
        ) {
        }
        private record ElementLocation(@NotNull Float4 uv, @NotNull String texture) {

        }
    }

    private static class Float3 {
        private final float x;
        private final float y;
        private final float z;
        private Float3(JsonArray array) {
            x = array.get(0).getAsFloat();
            y = array.get(1).getAsFloat();
            z = array.get(2).getAsFloat();
        }
    }
    private static class Float4 {
        private final float fx;
        private final float fy;
        private final float tx;
        private final float ty;
        private Float4(JsonArray array) {
            fx = array.get(0).getAsFloat();
            fy = array.get(1).getAsFloat();
            tx = array.get(2).getAsFloat();
            ty = array.get(3).getAsFloat();
        }
    }
}
