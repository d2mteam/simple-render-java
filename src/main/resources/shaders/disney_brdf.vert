#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord0;
layout(location = 3) in vec2 aTexCoord1;
layout(location = 4) in vec3 aTangent;
layout(location = 5) in vec3 aBitangent;
uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
out vec3 vNormal;
out vec3 vTangent;
out vec3 vBitangent;
out vec2 vTexCoord0;
out vec2 vTexCoord1;
void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    mat3 normalMatrix = transpose(inverse(mat3(uModel)));
    vNormal = normalMatrix * aNormal;
    vTangent = normalMatrix * aTangent;
    vBitangent = normalMatrix * aBitangent;
    vTexCoord0 = aTexCoord0;
    vTexCoord1 = aTexCoord1;
    gl_Position = uProjection * uView * worldPos;
}
