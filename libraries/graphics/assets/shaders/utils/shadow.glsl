#ifndef SHADOW_GLSL
#define SHADOW_GLSL
const float c_bias = 0.005;
#define AUTO_BIOS max(0.05 * (1.0 - dot(normal, lightDir)), c_bias)
// Basic shadow function
float basicShadow(sampler2D shadowMap, vec3 projCoords, float bias) {
    if (projCoords.z > 1.0 || projCoords.x < 0.0 || projCoords.x > 1.0 || projCoords.y < 0.0 || projCoords.y > 1.0) {
        return 0.0;
    }

    float closestDepth = texture2D(shadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;
    return (currentDepth - bias) > closestDepth ? 1.0 : 0.0;
}

float baiscShadow(sampler2D shadowMap, vec3 projCoords) { return basicShadow(shadowMap, projCoords, c_bias); }
float baiscShadow(sampler2D shadowMap, vec3 projCoords, vec3 normal, vec3 lightDir) {
    return basicShadow(shadowMap, projCoords, AUTO_BIOS);
}
// PCF shadow function
float pcfShadow(sampler2D shadowMap, vec3 projCoords, float bias) {
    if (projCoords.z > 1.0 || projCoords.x < 0.0 || projCoords.x > 1.0 || projCoords.y < 0.0 || projCoords.y > 1.0) {
        return 0.0;
    }
    float currentDepth = projCoords.z;
    float shadow = 0.0;
    vec2 texelSize = vec2(1.0) / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;
    return shadow;
}
float pcfShadow(sampler2D shadowMap, vec3 projCoords, vec3 normal, vec3 lightDir) {
    return pcfShadow(shadowMap, projCoords, AUTO_BIOS);
}
float pcfShadow(sampler2D shadowMap, vec3 projCoords) { return pcfShadow(shadowMap, projCoords, c_bias); }
#endif // SHADOW_GLSL
