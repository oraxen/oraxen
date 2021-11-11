#version 150

#define TEX_RES {#TEXTURE_RESOLUTION#}

#moj_import <fog.glsl>
#moj_import <light.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GameTime;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec4 normal;
flat in vec4 tint;
flat in vec3 vNormal;
flat in vec4 texel;

out vec4 fragColor;

#define ANIM_SPEED 50 /* runs every 24 seconds */
#define DEBUG fragColor=vec4(1,0,0,1);return;

void main() {
    
    ivec2 atlasSize = textureSize(Sampler0, 0);
    vec2 pixelSize = 1.0 / atlasSize;
    vec2 coords = texCoord0;
    float armorAmount = atlasSize.x / (TEX_RES * 4.0);
    coords.x /= armorAmount;
    float maxFrames = atlasSize.y / (TEX_RES * 2.0);
    coords.y /= maxFrames;
    float interpolClock = 0;
    vec2 nextFrame = vec2(0);
    float h_offset = 1.0 / armorAmount;
    vec4 leatherCheck = texture(Sampler0, vec2(0.0, 1.0 / atlasSize.y));
    // texture properties contains extra info about the armor texture, such as to enable shading
    vec4 textureProperties = vec4(0);
    vec4 customColor = vec4(0);
    for (int i = 1; i < (armorAmount + 1); i++)
    {
        customColor = texelFetch(Sampler0, ivec2(TEX_RES * 4 * i + 0.5, 0), 0);
        if (tint == customColor){
            
            coords.x += (h_offset * i);
            vec4 animInfo = texelFetch(Sampler0, ivec2(TEX_RES * 4 * i + 1.5, 0), 0);
            animInfo.rgb *= animInfo.a * 255;
            textureProperties = texelFetch(Sampler0, ivec2(TEX_RES * 4 * i + 2.5, 0), 0);
            textureProperties.rgb *= textureProperties.a * 255;
            if (animInfo != vec4(0)){
                /* oh god it's animated
                animInfo = amount of frames, speed, interpolation (1||0)
                textureProperties = emissive, tint
                fract(GameTime * 1200) blinks every second so [0,1] every second */
                float timer = floor(mod(GameTime * ANIM_SPEED * animInfo.g, animInfo.r));
                if (animInfo.b > 0) interpolClock = fract(GameTime * ANIM_SPEED * animInfo.g);
                float v_offset = (TEX_RES * 2.0) / atlasSize.y * timer;
                nextFrame = coords;
                coords.y += v_offset;
                nextFrame.y += (TEX_RES * 2.0) / atlasSize.y * mod(timer + 1, animInfo.r);
            }
            break;
        }
    }
    vec4 vtc = vertexColor;
    // they always ask if the texture but never how is the texture :pensive:
    if (textureProperties.g == 1){
        if (textureProperties.r > 1){
            vtc = tint;
        }else if (textureProperties.r == 1){
            if (texture(Sampler0, vec2(coords.x + h_offset, coords.y)).a != 0){
                vtc = tint * texture(Sampler0, vec2(coords.x + h_offset, coords.y)).a;
            }
        }
    }else if(textureProperties.g == 0){
        if (textureProperties.r > 1){
            vtc = vec4(1);
        }else if (textureProperties.r == 1){
            if (texture(Sampler0, vec2(coords.x + h_offset, coords.y)).a != 0){
                vtc = vec4(1) * texture(Sampler0, vec2(coords.x + h_offset, coords.y)).a;
            }else{
                vtc = minecraft_mix_light(Light0_Direction, Light1_Direction, vNormal, vec4(1)) * texel;
            }
        }else{
            vtc = minecraft_mix_light(Light0_Direction, Light1_Direction, vNormal, vec4(1)) * texel;
        }
    }else{
        vtc = minecraft_mix_light(Light0_Direction, Light1_Direction, vNormal, vec4(1)) * texel;
    }
    
    vec4 armor = mix(texture(Sampler0, coords), texture(Sampler0, nextFrame), interpolClock);
    vec4 color;
    if (coords.x < (1 / armorAmount)){
        color = armor * vertexColor * ColorModulator;
    }else if(leatherCheck == vec4(1)){
        color = armor * vtc * ColorModulator;
    }else{
        color = armor * vertexColor * ColorModulator;
    }
    if (color.a < 0.1) discard;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
