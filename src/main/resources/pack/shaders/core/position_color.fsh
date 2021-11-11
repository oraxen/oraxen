#version 150
in vec4 vertexColor;uniform vec4 ColorModulator;out vec4 fragColor;void main() { vec4 color = vertexColor; if (color.a == 0.0){discard;} if (ColorModulator == vec4(0.0f, 1.0f, 0.0f, 0.75f)) fragColor = color; else fragColor=color*ColorModulator;}
