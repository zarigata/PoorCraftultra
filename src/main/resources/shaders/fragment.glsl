#version 330 core

in vec3 fragColor;
in vec2 fragTexCoord;
in vec2 fragFaceUV;
in vec2 fragTileSpan;
out vec4 outColor;

uniform sampler2D textureSampler;

void main() {
    // Calculate repeating UV within the atlas tile bounds
    // fragTexCoord contains the base tile position (u0, v0)
    // fragFaceUV contains face-local coordinates in block units
    // fragTileSpan contains the tile size (u1-u0, v1-v0)
    // Use fract() to repeat the texture across merged quads without crossing tile boundaries
    vec2 sampleUV = fragTexCoord + fract(fragFaceUV) * fragTileSpan;
    
    // Sample texture from atlas at the computed UV
    vec4 texColor = texture(textureSampler, sampleUV);
    
    // Multiply by vertex color for lighting/shading
    vec3 finalColor = texColor.rgb * fragColor;
    
    // Output with texture alpha
    outColor = vec4(finalColor, texColor.a);
}
