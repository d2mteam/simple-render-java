#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec3 aTangent;
layout(location = 4) in vec3 aBitangent;
uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
out vec3 vNormal;
out vec3 vTangent;
out vec3 vBitangent;
out vec2 vTexCoord;
void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    mat3 normalMatrix = mat3(uModel);
    vNormal = normalMatrix * aNormal;
    vTangent = normalMatrix * aTangent;
    vBitangent = normalMatrix * aBitangent;
    vTexCoord = aTexCoord;
    gl_Position = uProjection * uView * worldPos;
}
