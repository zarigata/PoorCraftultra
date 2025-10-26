#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aColor;

out vec3 vertexColor;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

void main() {
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
    vertexColor = aColor;
}
