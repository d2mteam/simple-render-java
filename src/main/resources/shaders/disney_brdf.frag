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

float schlickWeight(float cosTheta) {
    float m = clamp(1.0 - cosTheta, 0.0, 1.0);
    return m * m * m * m * m;
}

void main() {
    vec2 uv = vec2(0.5, 0.5);
    vec3 normalSample = texture(uNormalTexture, uv).xyz * 2.0 - 1.0;
    vec3 n = normalize(normalSample);
    vec3 l = normalize(-uLightDir);
    vec3 v = vec3(0.0, 0.0, 1.0);
    vec3 h = normalize(l + v);

    float ndotl = max(dot(n, l), 0.0);
    float ndotv = max(dot(n, v), 0.0);
    float ndoth = max(dot(n, h), 0.0);
    float ldoth = max(dot(l, h), 0.0);

    vec2 metallicRoughness = texture(uMetallicRoughnessTexture, uv).rg;
    float metallic = metallicRoughness.r;
    float roughness = metallicRoughness.g;
    float fd90 = 0.5 + 2.0 * ldoth * ldoth * roughness;
    float lightScatter = mix(1.0, fd90, schlickWeight(ndotl));
    float viewScatter = mix(1.0, fd90, schlickWeight(ndotv));
    float disneyDiffuse = lightScatter * viewScatter;

    vec3 base = uBaseColor * texture(uBaseColorTexture, uv).rgb;
    float diffuseScale = (1.0 - metallic);

    float ao = texture(uAoTexture, uv).r;
    vec3 emissive = texture(uEmissiveTexture, uv).rgb;

    vec3 color = base * diffuseScale * (0.2 + disneyDiffuse * ndotl * 0.8);
    color *= ao;
    color += emissive;
    FragColor = vec4(color, 1.0);
}
