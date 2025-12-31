#version 330 core
in vec3 vNormal;
in vec2 vTexCoord;
uniform vec3 uLightDir;
uniform vec3 uBaseColor;
uniform sampler2D uTexture;
out vec4 FragColor;
void main() {
    float diff = max(dot(normalize(vNormal), normalize(-uLightDir)), 0.0);
    vec3 base = uBaseColor * texture(uTexture, vTexCoord).rgb;
    vec3 color = base * (0.2 + diff * 0.8);
    FragColor = vec4(color, 1.0);
}
