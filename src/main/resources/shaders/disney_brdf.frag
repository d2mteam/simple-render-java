#version 330 core
in vec3 vNormal;
in vec3 vTangent;
in vec2 vTexCoord0;
in vec2 vTexCoord1;
in vec3 vWorldPos;
const int MAX_LIGHTS = 8;
uniform int uLightCount;
uniform int uLightType[MAX_LIGHTS];
uniform vec3 uLightColor[MAX_LIGHTS];
uniform vec3 uLightPosition[MAX_LIGHTS];
uniform vec3 uLightDirection[MAX_LIGHTS];
uniform vec4 uLightParams[MAX_LIGHTS];
uniform vec3 uBaseColor;
uniform sampler2D uTexture;
uniform sampler2D uBaseColorTex;
uniform sampler2D uNormalTex;
uniform sampler2D uMetallicRoughnessTex;
uniform sampler2D uAoTex;
uniform sampler2D uEmissiveTex;
uniform int uBaseColorTexCoord;
uniform int uNormalTexCoord;
uniform int uMetallicRoughnessTexCoord;
uniform int uAoTexCoord;
uniform int uEmissiveTexCoord;
uniform int uAlphaMode; // 0=OPAQUE, 1=MASK, 2=BLEND
uniform float uAlphaCutoff;
uniform vec3 uCameraPos;
out vec4 FragColor;

vec2 selectTexCoord(int index) {
    return index == 1 ? vTexCoord1 : vTexCoord0;
}

float schlickWeight(float cosTheta) {
    float m = clamp(1.0 - cosTheta, 0.0, 1.0);
    return m * m * m * m * m;
}

vec3 applyLighting(vec3 n, vec3 v, vec3 base, float metallic, float roughness, float ao, vec3 emissive) {
    float ambient = 0.2;
    vec3 totalDiffuse = vec3(0.0);
    for (int i = 0; i < uLightCount; i++) {
        vec3 l;
        float attenuation = 1.0;
        if (uLightType[i] == 0) {
            l = normalize(-uLightDirection[i]);
        } else {
            vec3 toLight = uLightPosition[i] - vWorldPos;
            float dist = length(toLight);
            l = dist > 0.0 ? toLight / dist : vec3(0.0, 1.0, 0.0);
            float range = max(uLightParams[i].y, 0.001);
            float falloff = clamp(1.0 - dist / range, 0.0, 1.0);
            attenuation = falloff * falloff;
            if (uLightType[i] == 2) {
                vec3 lightToPoint = normalize(vWorldPos - uLightPosition[i]);
                float spotCos = dot(lightToPoint, normalize(uLightDirection[i]));
                float spot = smoothstep(uLightParams[i].w, uLightParams[i].z, spotCos);
                attenuation *= spot;
            }
        }
        vec3 h = normalize(l + v);
        float ndotl = max(dot(n, l), 0.0);
        float ndotv = max(dot(n, v), 0.0);
        float ldoth = max(dot(l, h), 0.0);
        float fd90 = 0.5 + 2.0 * ldoth * ldoth * roughness;
        float lightScatter = mix(1.0, fd90, schlickWeight(ndotl));
        float viewScatter = mix(1.0, fd90, schlickWeight(ndotv));
        float disneyDiffuse = lightScatter * viewScatter;
        vec3 radiance = uLightColor[i] * uLightParams[i].x * attenuation;
        totalDiffuse += radiance * disneyDiffuse * ndotl;
    }
    vec3 diffuse = base * (ambient + totalDiffuse) * (1.0 - metallic);
    vec3 color = diffuse * ao + emissive;
    return color;
}

void main() {
    vec2 normalUv = selectTexCoord(uNormalTexCoord);
    vec2 baseUv = selectTexCoord(uBaseColorTexCoord);
    vec2 metallicUv = selectTexCoord(uMetallicRoughnessTexCoord);
    vec2 aoUv = selectTexCoord(uAoTexCoord);
    vec2 emissiveUv = selectTexCoord(uEmissiveTexCoord);
    vec3 n = normalize(vNormal);
    vec3 t = normalize(vTangent);
    vec3 b = normalize(cross(n, t));
    mat3 tbn = mat3(t, b, n);
    vec3 normalSample = texture(uNormalTex, normalUv).rgb * 2.0 - 1.0;
    n = normalize(tbn * normalSample);
    vec3 metallicRoughness = texture(uMetallicRoughnessTex, metallicUv).rgb;
    float metallic = metallicRoughness.b;
    float roughness = metallicRoughness.g;
    float ao = texture(uAoTex, aoUv).r;
    vec3 emissive = texture(uEmissiveTex, emissiveUv).rgb;
    vec3 baseSample = texture(uBaseColorTex, baseUv).rgb;
    float alphaSample = texture(uBaseColorTex, baseUv).a;
    vec3 base = uBaseColor * baseSample;
    
    vec3 v = normalize(uCameraPos - vWorldPos);
    vec3 color = applyLighting(n, v, base, metallic, roughness, ao, emissive);
    
    // Gamma correction
    color = color / (color + vec3(1.0));
    color = pow(color, vec3(1.0 / 2.2));
    
    FragColor = vec4(color, alphaSample);
}
