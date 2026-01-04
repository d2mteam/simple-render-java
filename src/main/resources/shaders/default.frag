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
uniform vec3 uCameraPos;
out vec4 FragColor;

vec2 selectTexCoord(int index) {
    return index == 1 ? vTexCoord1 : vTexCoord0;
}

vec3 applyLighting(vec3 n, vec3 v, float shininess, float specularStrength, vec3 base, float metallic, float ao, vec3 emissive) {
    float ambient = 0.2;
    vec3 totalDiffuse = vec3(0.0);
    vec3 totalSpecular = vec3(0.0);
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
        float ndotl = max(dot(n, l), 0.0);
        vec3 radiance = uLightColor[i] * uLightParams[i].x * attenuation;
        totalDiffuse += radiance * ndotl;
        float spec = pow(max(dot(reflect(-l, n), v), 0.0), shininess);
        totalSpecular += radiance * spec;
    }
    vec3 lit = base * (ambient + totalDiffuse) * (1.0 - metallic);
    vec3 color = (lit + specularStrength * totalSpecular) * ao + emissive;
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

    vec3 base = uBaseColor * texture(uBaseColorTex, baseUv).rgb;
    vec3 metallicRoughness = texture(uMetallicRoughnessTex, metallicUv).rgb;
    float metallic = metallicRoughness.b;
    float roughness = metallicRoughness.g;
    float ao = texture(uAoTex, aoUv).r;
    vec3 emissive = texture(uEmissiveTex, emissiveUv).rgb;

    float specularStrength = mix(0.04, 0.5, metallic);
    float shininess = mix(4.0, 64.0, 1.0 - roughness);
    vec3 v = normalize(uCameraPos - vWorldPos);
    vec3 color = applyLighting(n, v, shininess, specularStrength, base, metallic, ao, emissive);
    FragColor = vec4(color, 1.0);
}
