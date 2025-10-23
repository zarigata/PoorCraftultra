#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 color;
layout(location = 2) in vec2 texCoord;
layout(location = 3) in vec2 faceUV;
layout(location = 4) in vec2 tileSpan;

out vec3 fragColor;
out vec2 fragTexCoord;
out vec2 fragFaceUV;
out vec2 fragTileSpan;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
    gl_Position = projection * view * model * vec4(position, 1.0);
    // Color is now used for lighting/shading, not base color
    fragColor = color;
    fragTexCoord = texCoord;
    fragFaceUV = faceUV;
    fragTileSpan = tileSpan;
}
