#version 330 core
in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uSceneColor;
uniform sampler2D uSceneDepth;
uniform sampler2D uRayTraceTex;
uniform int uFrameIndex;
uniform vec2 uTexelSize;

uniform int uEnableToneMap;
uniform int uEnableBloom;
uniform int uEnableColorGrade;
uniform int uEnableDof;
uniform int uEnableMotionBlur;
uniform int uEnableVignette;
uniform int uEnableFilmGrain;
uniform int uEnableSsao;
uniform int uEnableSsr;
uniform int uEnableSsgi;
uniform int uEnableContactShadows;
uniform int uEnableRayTracing;

uniform float uExposure;
uniform float uBloomStrength;
uniform float uBloomThreshold;
uniform float uColorGradeSaturation;
uniform vec3 uColorGradeTint;
uniform float uVignetteIntensity;
uniform float uFilmGrainIntensity;
uniform float uDofFocus;
uniform float uDofScale;
uniform float uMotionBlurStrength;
uniform vec2 uMotionBlurDir;
uniform float uSsaoStrength;
uniform float uSsaoRadius;
uniform float uSsrStrength;
uniform float uSsgiStrength;
uniform float uContactShadowStrength;
uniform float uRayTracingMix;

float rand(vec2 co, float seed) {
    return fract(sin(dot(co + seed, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 toneMapAces(vec3 color) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0, 1.0);
}

vec3 sampleScene(vec2 uv) {
    return texture(uSceneColor, uv).rgb;
}

vec3 applyBloom(vec3 color, vec2 uv) {
    vec3 bloom = vec3(0.0);
    int taps = 0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(float(x), float(y)) * uTexelSize;
            vec3 sampleColor = sampleScene(uv + offset);
            vec3 bright = max(sampleColor - vec3(uBloomThreshold), 0.0);
            bloom += bright;
            taps++;
        }
    }
    bloom /= float(taps);
    return color + bloom * uBloomStrength;
}

vec3 applyDof(vec3 color, vec2 uv, float depth) {
    float blur = clamp(abs(depth - uDofFocus) * uDofScale, 0.0, 1.0);
    vec3 accum = vec3(0.0);
    float total = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(float(x), float(y)) * uTexelSize * (1.0 + blur * 4.0);
            accum += sampleScene(uv + offset);
            total += 1.0;
        }
    }
    vec3 blurred = accum / total;
    return mix(color, blurred, blur);
}

vec3 applyMotionBlur(vec3 color, vec2 uv) {
    vec2 dir = normalize(uMotionBlurDir + vec2(0.0001));
    vec3 accum = vec3(0.0);
    float total = 0.0;
    for (int i = -2; i <= 2; i++) {
        float t = float(i) * uMotionBlurStrength;
        accum += sampleScene(uv + dir * t * uTexelSize * 12.0);
        total += 1.0;
    }
    vec3 blurred = accum / total;
    return mix(color, blurred, uMotionBlurStrength);
}

float computeSsao(vec2 uv, float depth) {
    float occlusion = 0.0;
    float radius = uSsaoRadius;
    vec2 offsets[4] = vec2[4](
        vec2(-radius, 0.0),
        vec2(radius, 0.0),
        vec2(0.0, -radius),
        vec2(0.0, radius)
    );
    for (int i = 0; i < 4; i++) {
        float sampleDepth = texture(uSceneDepth, uv + offsets[i]).r;
        occlusion += step(depth + radius, sampleDepth);
    }
    occlusion /= 4.0;
    return 1.0 - occlusion * uSsaoStrength;
}

float computeContactShadows(vec2 uv, float depth) {
    float shadow = 0.0;
    float offset = uSsaoRadius * 0.5;
    float sampleDepth = texture(uSceneDepth, uv + vec2(offset)).r;
    shadow += step(depth + offset, sampleDepth);
    return 1.0 - shadow * uContactShadowStrength;
}

void main() {
    vec2 uv = vTexCoord;
    vec3 color = sampleScene(uv);
    float depth = texture(uSceneDepth, uv).r;

    if (uEnableSsao == 1) {
        color *= computeSsao(uv, depth);
    }
    if (uEnableContactShadows == 1) {
        color *= computeContactShadows(uv, depth);
    }
    if (uEnableDof == 1) {
        color = applyDof(color, uv, depth);
    }
    if (uEnableMotionBlur == 1) {
        color = applyMotionBlur(color, uv);
    }
    if (uEnableBloom == 1) {
        color = applyBloom(color, uv);
    }
    if (uEnableColorGrade == 1) {
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        color = mix(vec3(luma), color, uColorGradeSaturation);
        color *= uColorGradeTint;
    }
    if (uEnableRayTracing == 1) {
        vec3 rayColor = texture(uRayTraceTex, uv).rgb;
        color = mix(color, rayColor, uRayTracingMix);
    }
    if (uEnableToneMap == 1) {
        color *= uExposure;
        color = toneMapAces(color);
    }
    if (uEnableVignette == 1) {
        vec2 pos = uv * 2.0 - 1.0;
        float vig = smoothstep(0.9, 0.2, dot(pos, pos));
        color *= mix(1.0 - uVignetteIntensity, 1.0, vig);
    }
    if (uEnableFilmGrain == 1) {
        float noise = rand(gl_FragCoord.xy, float(uFrameIndex));
        color += (noise - 0.5) * uFilmGrainIntensity;
    }
    if (uEnableSsr == 1) {
        color += vec3(0.0) * uSsrStrength;
    }
    if (uEnableSsgi == 1) {
        color += vec3(0.0) * uSsgiStrength;
    }

    FragColor = vec4(color, 1.0);
}
