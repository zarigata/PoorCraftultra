#version 450

// Inputs from vertex shader
layout(location = 0) in vec3 vNormal;
layout(location = 1) in vec2 vTexCoord;
layout(location = 2) in float vAO;

// Descriptor set bindings
layout(set = 0, binding = 0) uniform sampler2D uTexture;

layout(set = 0, binding = 1) uniform LightingParams {
    vec4 sunDirAndIntensity;  // xyz = direction, w = intensity
    vec4 sunColor;            // rgb = color, w unused
    vec4 ambientColorAndIntensity; // rgb = color, w = intensity
} uLight;

// Output
layout(location = 0) out vec4 outColor;

void main() {
    // Sample texture atlas
    vec4 texColor = texture(uTexture, vTexCoord);
    
    // Normalize normal (interpolation may have denormalized it)
    vec3 normal = normalize(vNormal);
    
    // Extract lighting parameters
    vec3 sunDirection = normalize(uLight.sunDirAndIntensity.xyz);
    float sunIntensity = uLight.sunDirAndIntensity.w;
    vec3 sunColor = uLight.sunColor.rgb;
    vec3 ambientColor = uLight.ambientColorAndIntensity.rgb;
    float ambientIntensity = uLight.ambientColorAndIntensity.w;
    
    // Calculate ambient lighting
    vec3 ambient = ambientColor * ambientIntensity;
    
    // Calculate directional (sun) lighting
    float diffuse = max(dot(normal, -sunDirection), 0.0);
    vec3 directional = sunColor * sunIntensity * diffuse;
    
    // Combine lighting
    vec3 lighting = ambient + directional;
    
    // Apply ambient occlusion (clamp to valid range)
    float ao = clamp(vAO, 0.0, 1.0);
    
    // Final color = texture * lighting * AO
    outColor = vec4(texColor.rgb * lighting * ao, texColor.a);
}
