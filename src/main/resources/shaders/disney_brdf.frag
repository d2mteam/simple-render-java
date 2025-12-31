#version 330 core
in vec3 vNormal;
in vec2 vTexCoord;
uniform vec3 uLightDir;
uniform vec3 uBaseColor;
uniform sampler2D uTexture;
out vec4 FragColor;

float schlickWeight(float cosTheta) {
    float m = clamp(1.0 - cosTheta, 0.0, 1.0);
    return m * m * m * m * m;
}

void main() {
    vec3 n = normalize(vNormal);
    vec3 l = normalize(-uLightDir);
    vec3 v = vec3(0.0, 0.0, 1.0);
    vec3 h = normalize(l + v);

    float ndotl = max(dot(n, l), 0.0);
    float ndotv = max(dot(n, v), 0.0);
    float ldoth = max(dot(l, h), 0.0);

    float roughness = 0.5;
    float fd90 = 0.5 + 2.0 * ldoth * ldoth * roughness;
    float lightScatter = mix(1.0, fd90, schlickWeight(ndotl));
    float viewScatter = mix(1.0, fd90, schlickWeight(ndotv));
    float disneyDiffuse = lightScatter * viewScatter;

    vec3 base = uBaseColor * texture(uTexture, vTexCoord).rgb;
    vec3 color = base * (0.2 + disneyDiffuse * ndotl * 0.8);
    FragColor = vec4(color, 1.0);
}
