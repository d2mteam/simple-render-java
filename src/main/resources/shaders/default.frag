#version 330 core
in vec3 vNormal;
in vec3 vTangent;
in vec3 vBitangent;
in vec2 vTexCoord;
uniform vec3 uLightDir;
uniform vec3 uBaseColor;
uniform sampler2D uTexture;
uniform sampler2D uBaseColorTex;
uniform sampler2D uNormalTex;
uniform sampler2D uMetallicRoughnessTex;
uniform sampler2D uAoTex;
uniform sampler2D uEmissiveTex;
out vec4 FragColor;
void main() {
    vec3 n = normalize(vNormal);
    vec3 t = normalize(vTangent);
    vec3 b = normalize(vBitangent);
    mat3 tbn = mat3(t, b, n);
    vec3 normalSample = texture(uNormalTex, vTexCoord).rgb * 2.0 - 1.0;
    n = normalize(tbn * normalSample);

    vec3 l = normalize(-uLightDir);
    float diff = max(dot(n, l), 0.0);

    vec3 base = uBaseColor * texture(uBaseColorTex, vTexCoord).rgb;
    vec3 metallicRoughness = texture(uMetallicRoughnessTex, vTexCoord).rgb;
    float metallic = metallicRoughness.b;
    float roughness = metallicRoughness.g;
    float ao = texture(uAoTex, vTexCoord).r;
    vec3 emissive = texture(uEmissiveTex, vTexCoord).rgb;

    float specularStrength = mix(0.04, 0.5, metallic);
    float spec = pow(max(dot(reflect(-l, n), vec3(0.0, 0.0, 1.0)), 0.0), mix(4.0, 64.0, 1.0 - roughness));
    vec3 diffuse = base * (0.2 + diff * 0.8) * (1.0 - metallic);
    vec3 color = (diffuse + specularStrength * spec) * ao + emissive;
    FragColor = vec4(color, 1.0);
}
