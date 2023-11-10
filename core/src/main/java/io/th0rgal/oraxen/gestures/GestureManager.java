package io.th0rgal.oraxen.gestures;

import com.ticxo.playeranimator.api.PlayerAnimator;
import com.ticxo.playeranimator.api.animation.pack.AnimationPack;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class GestureManager {
    private final Map<Player, OraxenPlayerModel> gesturingPlayers;

    public GestureManager() {
        gesturingPlayers = new HashMap<>();
        if (Settings.GESTURES_ENABLED.toBool()) {
            loadGestures();
            Bukkit.getPluginManager().registerEvents(new GestureListener(this), OraxenPlugin.get());
        }
    }

    public void playGesture(Player player, String gesture) {
        if (isPlayerGesturing(player)) return;
        OraxenPlayerModel model = new OraxenPlayerModel(player, QuitMethod.SNEAK, false, false);
        addPlayerToGesturing(player, model);
        model.playAnimation(gesture);

    }

    public void stopGesture(Player player) {
        OraxenPlayerModel model = getPlayerModel(player);
        if (model == null) return;
        removePlayerFromGesturing(player);
        model.despawn();
    }

    public boolean isPlayerGesturing(Player player) {
        return gesturingPlayers.containsKey(player);
    }

    public OraxenPlayerModel getPlayerModel(Player player) {
        return gesturingPlayers.getOrDefault(player, null);
    }

    public void addPlayerToGesturing(Player player, OraxenPlayerModel gesture) {
        gesturingPlayers.put(player, gesture);
    }

    public void removePlayerFromGesturing(Player player) {
        gesturingPlayers.remove(player);
    }

    public void loadGestures() {
        PlayerAnimator.api.getAnimationManager().clearRegistry();
        gestures.clear();

        File gestureDir = new File(OraxenPlugin.get().getDataFolder().getPath() + "/gestures/");
        if (!gestureDir.exists()) return;
        File[] gestureFiles = gestureDir.listFiles();
        if (gestureFiles == null || gestureFiles.length == 0) return;
        gestureFiles = Arrays.stream(gestureFiles).filter(f -> f.getPath().endsWith(".bbmodel")).distinct().toArray(File[]::new);
        if (gestureFiles.length == 0) return;
        for (File animationFile : gestureFiles) {
            String animationKey = Utils.removeExtension(animationFile.getName());
            PlayerAnimator.api.getAnimationManager().importAnimations(animationKey, animationFile);
        }
        for (Map.Entry<String, AnimationPack> packEntry : PlayerAnimator.api.getAnimationManager().getRegistry().entrySet()) {
            Set<String> animationNames = packEntry.getValue().getAnimations().keySet().stream().map(animation -> packEntry.getKey().replace(":", ".") + "." + animation).collect(Collectors.toSet());
            gestures.addAll(animationNames);
        }
    }

    public static Set<String> gestures = new HashSet<>();

    public static Set<String> getGestures() {
        return gestures;
    }

    public void reload() {
        for (Map.Entry<Player, OraxenPlayerModel> entry : gesturingPlayers.entrySet()) {
            stopGesture(entry.getKey());
            gesturingPlayers.remove(entry.getKey());
        }
        loadGestures();
    }

    public Map<String, String> getPlayerHeadJsons() {
        return Map.of(
                "assets/minecraft/models/required/player/norm/arm.json", "{\"parent\":\"item/player_head\",\"display\":{\"thirdperson_righthand\":{\"rotation\":[90,180,0],\"translation\":[-1,-2,10],\"scale\":[0.46875,0.703125,0.46875]}}}",
                "assets/minecraft/models/required/player/norm/left_shoulder.json", "{\"parent\":\"item/player_head\",\"display\":{\"thirdperson_righthand\":{\"rotation\":[90,180,0],\"translation\":[-1.625,-2,11.5],\"scale\":[0.46875,0.703125,0.46875]}}}",
                "assets/minecraft/models/required/player/norm/right_shoulder.json", "{\"parent\":\"item/player_head\",\"display\":{\"thirdperson_righthand\":{\"rotation\":[90,180,0],\"translation\":[-0.375,-2,11.5],\"scale\":[0.46875,0.703125,0.46875]}}}",
                "assets/minecraft/models/required/player/slim/arm.json", "{\"parent\":\"item/player_head\",\"display\":{\"thirdperson_righthand\":{\"rotation\":[90,180,0],\"translation\":[-1,-2,10],\"scale\":[0.3515625,0.703125,0.46875]}}}",
                "assets/minecraft/models/required/player/slim/left_shoulder.json", "{\"parent\":\"item/player_head\",\"display\":{\"thirdperson_righthand\":{\"rotation\":[90,180,0],\"translation\":[-1.40625,-2,11.5],\"scale\":[0.3515625,0.703125,0.46875]}}}",
                "assets/minecraft/models/required/player/slim/right_shoulder.json", "{\"parent\":\"item/player_head\",\"display\":{\"thirdperson_righthand\":{\"rotation\":[90,180,0],\"translation\":[-0.59375,-2,11.5],\"scale\":[0.3515625,0.703125,0.46875]}}}",
                "assets/minecraft/models/required/player/body.json", "{\"parent\":\"item/player_head\",\"display\":{\"thirdperson_righthand\":{\"rotation\":[90,180,0],\"translation\":[-1,-2,13.75],\"scale\":[0.9375,0.46875,0.46875]}}}",
                "assets/minecraft/models/required/player/head.json", "{\"parent\":\"item/player_head\",\"display\":{\"thirdperson_righthand\":{\"rotation\":[90,180,0],\"translation\":[-1,-2,17.5],\"scale\":[0.9375,0.9375,0.9375]}}}",
                "assets/minecraft/models/required/player/leg.json", "{\"parent\":\"item/player_head\",\"display\":{\"thirdperson_righthand\":{\"rotation\":[90,180,0],\"translation\":[-1,-2,10],\"scale\":[0.46875,0.703125,0.46875]}}}"
        );
    }

    public String getSkullJson() {
        return """
                {
                   "parent": "minecraft:item/template_skull",
                   "overrides": [
                     { "predicate": { "custom_model_data": 1 }, "model": "required/player/head" },
                     { "predicate": { "custom_model_data": 2 }, "model": "required/player/norm/right_shoulder" },
                     { "predicate": { "custom_model_data": 3 }, "model": "required/player/norm/left_shoulder" },
                     { "predicate": { "custom_model_data": 4 }, "model": "required/player/norm/arm" },
                     { "predicate": { "custom_model_data": 5 }, "model": "required/player/slim/right_shoulder" },
                     { "predicate": { "custom_model_data": 6 }, "model": "required/player/slim/left_shoulder" },
                     { "predicate": { "custom_model_data": 7 }, "model": "required/player/slim/arm" },
                     { "predicate": { "custom_model_data": 8 }, "model": "required/player/body" },
                     { "predicate": { "custom_model_data": 9 }, "model": "required/player/leg" }
                   ]
                 }""";
    }

    public String getShaderFsh() {
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
                in vec4 lightMapColor;
                in vec4 overlayColor;
                in vec2 texCoord0;
                in vec2 texCoord1;
                in vec4 normal;
                                
                in vec3 a, b;
                                
                out vec4 fragColor;
                                
                vec2 getSize() {
                    vec2 uv1 = a.xy / a.z;
                    vec2 uv2 = b.xy / b.z;
                    return round((max(uv1, uv2) - min(uv1, uv2)) * 64);
                }
                                
                void main() {
                                
                    if(texCoord0.x < 0) {
                        discard;
                    }
                                
                    vec2 uv = texCoord0;
                    if(getSize() != vec2(8, 8))
                        uv = texCoord1;
                                
                    vec4 color = texture(Sampler0, uv);
                    if (color.a < 0.1) {
                        discard;
                    }
                    color *= vertexColor * ColorModulator;
                    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
                    color *= lightMapColor;
                    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
                }
                """;
    }

    public String getShaderVsh() {
        return """
                #version 150
                                
                #moj_import <light.glsl>
                #moj_import <fog.glsl>
                                
                in vec3 Position, Normal;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV1, UV2;
                                
                uniform sampler2D Sampler0, Sampler1, Sampler2;
                                
                uniform mat4 ModelViewMat, ProjMat;
                uniform int FogShape;
                                
                uniform vec3 Light0_Direction, Light1_Direction;
                                
                out float vertexDistance;
                out vec4 vertexColor, lightMapColor, overlayColor, normal;
                out vec2 texCoord0, texCoord1;
                out vec3 a, b;
                                
                vec3 getCubeSize(int cube) {
                    if(cube >= 2 && cube <= 7)
                        return vec3(8, 4, 4);
                                
                    if(cube >= 8 && cube <= 15)
                        return vec3(3, 6, 4);
                                
                    if(cube >= 16 && cube <= 23)
                        return vec3(4, 6, 4);
                                
                    return vec3(8, 8, 8);
                }
                                
                vec2 getBoxUV(int cube) {
                    switch(cube) {
                        case 0: // Head
                            return vec2(0, 0);
                        case 1: // Hat
                            return vec2(32, 0);
                        case 2: // Hip
                        case 4: // Waist
                        case 6: // Chest
                            return vec2(16, 16);
                        case 3:
                        case 5:
                        case 7: // Jacket
                            return vec2(16, 32);
                        case 8:
                        case 10: // Right Arm
                            return vec2(40, 16);
                        case 9:
                        case 11: // Right Sleeve
                            return vec2(40, 32);
                        case 12:
                        case 14: // Left Arm
                            return vec2(32, 48);
                        case 13:
                        case 15: // Left Sleeve
                            return vec2(48, 48);
                        case 16:
                        case 18: // Right Leg
                            return vec2(0, 16);
                        case 17:
                        case 19: // Right Pant
                            return vec2(0, 32);
                        case 20:
                        case 22: // Left Leg
                            return vec2(16, 48);
                        case 21:
                        case 23: // Left Pant
                            return vec2(0, 48);
                                
                    }
                    return vec2(0, 0);
                }
                                
                float getYOffset(int cube) {
                    float r = 0;
                    switch(cube) {
                        case 2:
                        case 3:
                            r = 8;
                            break;
                        case 4:
                        case 5:
                            r = 4;
                            break;
                        case 10: // Right Arm
                        case 11: // Right Sleeve
                        case 14: // Left Arm
                        case 15: // Left Sleeve
                        case 18: // Right Leg
                        case 19: // Right Pant
                        case 22: // Left Leg
                        case 23: // Left Pant
                            r = 6;
                            break;
                    }
                    return r / 64.;
                }
                                
                vec2 getUVOffset(int corner, vec3 cubeSize, float yOffset) {
                    vec2 offset, uv;
                    switch(corner / 4) {
                        case 0: // Up
                            offset = vec2(cubeSize.z, 0);
                            uv = vec2(cubeSize.x, cubeSize.z);
                            break;
                        case 1: // Down
                            offset = vec2(cubeSize.z + cubeSize.x, 0);
                            uv = vec2(cubeSize.x, cubeSize.z);
                            break;
                        case 2: // Right
                            offset = vec2(0, cubeSize.z);
                            offset.y += yOffset;
                            uv = vec2(cubeSize.z, cubeSize.y);
                            break;
                        case 3: // Front
                            offset = vec2(cubeSize.z, cubeSize.z);
                            offset.y += yOffset;
                            uv = vec2(cubeSize.x, cubeSize.y);
                            break;
                        case 4: // Left
                            offset = vec2(cubeSize.z + cubeSize.x, cubeSize.z);
                            offset.y += yOffset;
                            uv = vec2(cubeSize.z, cubeSize.y);
                            break;
                        case 5: // Back
                            offset = vec2(2 * cubeSize.z + cubeSize.x, cubeSize.z);
                            offset.y += yOffset;
                            uv = vec2(cubeSize.x, cubeSize.y);
                            break;
                    }
                                
                    switch(corner % 4) {
                        case 0:
                            offset += vec2(uv.x, 0);
                            break;
                        case 2:
                            offset += vec2(0, uv.y);
                            break;
                        case 3:
                            offset += vec2(uv.x, uv.y);
                            break;
                    }
                                
                    return offset;
                }
                                
                bool shouldRender(int cube, int corner) {
                    if(corner / 8 != 1 || cube % 2 == 0 || cube == 1)
                        return true;
                                
                    if(cube == 5)
                        return false;
                                
                    bool r = false;
                    switch(cube) {
                        case 7:
                        case 9:
                        case 13:
                        case 17:
                        case 21:
                            r = true;
                    }
                                
                    if(corner / 4 == 3)
                        r = !r;
                                
                    return r;
                }
                                
                void main() {
                    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                                
                    a = b = vec3(0);
                    if(textureSize(Sampler0, 0) == vec2(64, 64) && UV0.y <= 0.25 && (gl_VertexID / 24 != 6 || UV0.x <= 0.5)) {
                                
                        switch(gl_VertexID % 4) {
                            case 0: a = vec3(UV0, 1); break;
                            case 2: b = vec3(UV0, 1); break;
                        }
                		// 1 3 5 9 11 13 15 17
                        int cube = (gl_VertexID / 24) % 24;
                        int corner = gl_VertexID % 24;
                        if(shouldRender(cube, corner)) {
                            vec3 cubeSize = getCubeSize(cube) / 64;
                            vec2 boxUV = getBoxUV(cube) / 64;
                            vec2 uvOffset = getUVOffset(corner, cubeSize, getYOffset(cube));
                            texCoord0 = boxUV + uvOffset;
                        }else {
                            texCoord0 = vec2(-1);
                        }
                    }else {
                        texCoord0 = UV0;
                    }
                                
                    // vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
                    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
                    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
                    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
                    overlayColor = texelFetch(Sampler1, UV1, 0);
                    texCoord1 = UV0;
                    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
                }
                                
                                
                """;
    }

    public String getShaderJson() {
        return """
                {
                    "blend": {
                        "func": "add",
                        "srcrgb": "srcalpha",
                        "dstrgb": "1-srcalpha"
                    },
                    "vertex": "rendertype_entity_translucent",
                    "fragment": "rendertype_entity_translucent",
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
                        { "name": "FogShape", "type": "int", "count": 1, "values": [ 0 ] }
                    ]
                }
                                
                """;
    }
}
