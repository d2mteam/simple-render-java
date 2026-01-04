#version 330 core
in vec3 vNormal;
out vec4 FragColor;

void main() {
    // Visualize normals as RGB colors
    vec3 n = normalize(vNormal);
    FragColor = vec4(n * 0.5 + 0.5, 1.0);
}
