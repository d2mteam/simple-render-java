#version 330 core
in vec3 vNormal;
uniform vec3 uLightDir;
uniform vec3 uBaseColor;
uniform sampler2D uBaseColorTexture;
uniform sampler2D uNormalTexture;
uniform sampler2D uMetallicRoughnessTexture;
uniform sampler2D uAoTexture;
uniform sampler2D uEmissiveTexture;
out vec4 FragColor;
void main() {
    float diff = max(dot(normalize(vNormal), normalize(-uLightDir)), 0.0);
    vec3 base = uBaseColor * texture(uBaseColorTexture, vec2(0.5, 0.5)).rgb;
    vec3 color = base * (0.2 + diff * 0.8);
    FragColor = vec4(color, 1.0);
}
