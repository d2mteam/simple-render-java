#version 330 core
in vec3 vNormal;
in vec2 vTexCoord;
in vec3 vWorldPos;
uniform vec3 uLightDir;
uniform vec3 uBaseColor;
uniform sampler2D uTexture;
uniform sampler2D uBaseColorTex;
uniform sampler2D uNormalTex;
uniform sampler2D uMetallicRoughnessTex;
uniform sampler2D uAoTex;
uniform sampler2D uEmissiveTex;
out vec4 FragColor;

float schlickWeight(float cosTheta) {
    float m = clamp(1.0 - cosTheta, 0.0, 1.0);
    return m * m * m * m * m;
}

void main() {
    vec3 n = normalize(vNormal);
    vec3 dp1 = dFdx(vWorldPos);
    vec3 dp2 = dFdy(vWorldPos);
    vec2 duv1 = dFdx(vTexCoord);
    vec2 duv2 = dFdy(vTexCoord);
    vec3 t = dp1 * duv2.y - dp2 * duv1.y;
    vec3 b = -dp1 * duv2.x + dp2 * duv1.x;
    float tLen = length(t);
    float bLen = length(b);
    if (tLen > 0.0 && bLen > 0.0) {
        t /= tLen;
        b /= bLen;
        mat3 tbn = mat3(t, b, n);
        vec3 normalSample = texture(uNormalTex, vTexCoord).rgb * 2.0 - 1.0;
        n = normalize(tbn * normalSample);
    }
    vec3 l = normalize(-uLightDir);
    vec3 v = vec3(0.0, 0.0, 1.0);
    vec3 h = normalize(l + v);

    float ndotl = max(dot(n, l), 0.0);
    float ndotv = max(dot(n, v), 0.0);
    float ldoth = max(dot(l, h), 0.0);

    vec3 metallicRoughness = texture(uMetallicRoughnessTex, vTexCoord).rgb;
    float metallic = metallicRoughness.b;
    float roughness = metallicRoughness.g;
    float ao = texture(uAoTex, vTexCoord).r;
    vec3 emissive = texture(uEmissiveTex, vTexCoord).rgb;
    float fd90 = 0.5 + 2.0 * ldoth * ldoth * roughness;
    float lightScatter = mix(1.0, fd90, schlickWeight(ndotl));
    float viewScatter = mix(1.0, fd90, schlickWeight(ndotv));
    float disneyDiffuse = lightScatter * viewScatter;

    vec3 base = uBaseColor * texture(uBaseColorTex, vTexCoord).rgb;
    vec3 diffuse = base * (0.2 + disneyDiffuse * ndotl * 0.8) * (1.0 - metallic);
    vec3 color = diffuse * ao + emissive;
    FragColor = vec4(color, 1.0);
}
