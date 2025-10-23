#version 330 core

in vec3 fragColor;
in vec2 fragTexCoord;
out vec4 outColor;

uniform sampler2D textureSampler;

void main() {
    // Sample texture from atlas
    vec4 texColor = texture(textureSampler, fragTexCoord);
    // Multiply by vertex color for lighting/shading
    vec3 finalColor = texColor.rgb * fragColor;
    // Output with texture alpha
    outColor = vec4(finalColor, texColor.a);
}
