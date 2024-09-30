package io.th0rgal.oraxen.pack;

import team.unnamed.creative.base.Writable;

public class ShaderUtils {

    public static class ScoreboardNumbers {
        public static Writable vsh() {
            return Writable.stringUtf8(
                    """
                #version 150
                 
                 #moj_import <fog.glsl>
                 
                 in vec3 Position;
                 in vec4 Color;
                 in vec2 UV0;
                 in ivec2 UV2;
                 
                 uniform sampler2D Sampler2;
                 
                 uniform mat4 ModelViewMat;
                 uniform mat4 ProjMat;
                 uniform mat3 IViewRotMat;
                 uniform int FogShape;
                 
                 uniform vec2 ScreenSize;
                 
                 out float vertexDistance;
                 out vec4 vertexColor;
                 out vec2 texCoord0;
                 
                 void main() {
                     gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                 
                     vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
                     vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                     texCoord0 = UV0;
                    
                 	// delete sidebar numbers
                 	if(	Position.z == 0.0 && // check if the depth is correct (0 for gui texts)
                 			gl_Position.x >= 0.94 && gl_Position.y >= -0.35 && // check if the position matches the sidebar
                 			vertexColor.g == 84.0/255.0 && vertexColor.g == 84.0/255.0 && vertexColor.r == 252.0/255.0 && // check if the color is the sidebar red color
                 			gl_VertexID <= 7 // check if it's the first character of a string !! if you want two characters removed replace '3' with '7'
                 		) gl_Position = ProjMat * ModelViewMat * vec4(ScreenSize + 100.0, 0.0, 0.0); // move the vertices offscreen, idk if this is a good solution for that but vec4(0.0) doesnt do the trick for everyone
                 }
                """
            );
        }

        public static Writable json() {
            return Writable.stringUtf8(
                    """
                {
                     "blend": {
                         "func": "add",
                         "srcrgb": "srcalpha",
                         "dstrgb": "1-srcalpha"
                     },
                     "vertex": "rendertype_text",
                     "fragment": "rendertype_text",
                     "attributes": [
                         "Position",
                         "Color",
                         "UV0",
                         "UV2"
                     ],
                     "samplers": [
                         { "name": "Sampler0" },
                         { "name": "Sampler2" }
                     ],
                     "uniforms": [
                         { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                         { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                         { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
                         { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                         { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                         { "name": "FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
                        { "name": "ScreenSize", "type": "float", "count": 2,  "values": [ 1.0, 1.0 ] }
                     ]
                 }
                """
            );
        }
    }

    public static class ScoreboardBackground {
        public static Writable modernFile() {
            return Writable.stringUtf8(
                    """
                #version 150
                                    
                in vec3 Position;
                in vec4 Color;
                                    
                uniform mat4 ModelViewMat;
                uniform mat4 ProjMat;
                                    
                out vec4 vertexColor;
                                    
                void main() {
                    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                                    
                    vertexColor = Color;
                    
                    //Isolating Scoreboard Display
                    if(gl_Position.y > -0.5 && gl_Position.y < 0.85 && gl_Position.x > 0.0 && gl_Position.x <= 1.0 && Position.z == 0.0) {
                        //vertexColor = vec4(vec3(0.0,0.0,1.0),1.0); // Debugger
                        vertexColor.a = 0.0;
                    }
                    else {
                        //vertexColor = vec4(vec3(1.0,0.0,0.0),1.0);
                    }
                }
                """
            );
        }
    }
}
