#version 450

// Vertex inputs matching ChunkVertex layout
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in float aAO;

// Push constants for matrices
layout(push_constant) uniform PushConstants {
    mat4 view;
    mat4 projection;
    mat4 model;
} pc;

// Outputs to fragment shader
layout(location = 0) out vec3 vNormal;
layout(location = 1) out vec2 vTexCoord;
layout(location = 2) out float vAO;

void main() {
    // Transform position
    vec4 worldPos = pc.model * vec4(aPosition, 1.0);
    gl_Position = pc.projection * pc.view * worldPos;
    
    // Pass through to fragment shader
    vNormal = mat3(pc.model) * aNormal;
    vTexCoord = aTexCoord;
    vAO = aAO;
}
