#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord0;
layout(location = 3) in vec2 aTexCoord1;
layout(location = 4) in vec3 aTangent;
uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
out vec3 vNormal;
out vec3 vTangent;
out vec2 vTexCoord0;
out vec2 vTexCoord1;
out vec3 vWorldPos;
void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    mat3 normalMatrix = transpose(inverse(mat3(uModel)));
    vNormal = normalize(normalMatrix * aNormal);
    vTangent = normalize(normalMatrix * aTangent);
    vTexCoord0 = aTexCoord0;
    vTexCoord1 = aTexCoord1;
    vWorldPos = worldPos.xyz;
    gl_Position = uProjection * uView * worldPos;
}
