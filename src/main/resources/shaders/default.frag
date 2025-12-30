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
    vec2 uv = vec2(0.5, 0.5);
    vec3 normalSample = texture(uNormalTexture, uv).xyz * 2.0 - 1.0;
    vec3 n = normalize(normalSample);
    vec3 l = normalize(-uLightDir);
    float diff = max(dot(n, l), 0.0);

    vec3 base = uBaseColor * texture(uBaseColorTexture, uv).rgb;
    vec2 metallicRoughness = texture(uMetallicRoughnessTexture, uv).rg;
    float metallic = metallicRoughness.r;
    float roughness = metallicRoughness.g;
    float diffuseScale = (1.0 - metallic) * (1.0 - roughness * 0.5);

    float ao = texture(uAoTexture, uv).r;
    vec3 emissive = texture(uEmissiveTexture, uv).rgb;

    vec3 color = base * diffuseScale * (0.2 + diff * 0.8);
    color *= ao;
    color += emissive;
    FragColor = vec4(color, 1.0);
}
