#ifndef DIFFUSE_GLSL
#define DIFFUSE_GLSL

#include "/utils/mathConstant.glsl"
// Lambert
float lambert(vec3 normal, vec3 light) { return max(dot(normal, light), 0.0); }

// Half Lambert
float halfLambert(vec3 normal, vec3 light) { return dot(normal, light) * 0.5 + 0.5; }

//Burley (2012)
float burley(vec3 N, vec3 L, vec3 V, float roughness) {
    float dotNL = max(dot(N, L), 0.0);
    float dotNV = max(dot(N, V), 0.0);
    float dotLH = max(dot(normalize(L + V), N), 0.0);

    float FD90 = 0.5 + 2.0 * dotLH * dotLH * roughness;
    float FdV = 1.0 + (FD90 - 1.0) * pow(1.0 - dotNV, 5.0);
    float FdL = 1.0 + (FD90 - 1.0) * pow(1.0 - dotNL, 5.0);

    return dotNL * (1.0 / PI) * FdV * FdL;
}

// Gotanda (2012)
float gotanda(vec3 N, vec3 L, vec3 V, float roughness) {
    float dotNL = dot(N, L);
    float dotNV = dot(N, V);
    float dotLH = dot(normalize(L + V), N);

    if (dotNL <= 0.0 || dotNV <= 0.0)
        return 0.0;

    // 优化的多项式近似
    float a = roughness;

    float Fd90 = 0.5 + 2.0 * a * dotLH * dotLH;
    float FdV = 1.0 + (Fd90 - 1.0) * exp2((-5.55473 * dotNV - 6.98316) * dotNV);
    float FdL = 1.0 + (Fd90 - 1.0) * exp2((-5.55473 * dotNL - 6.98316) * dotNL);

    return dotNL * (1.0 / PI) * FdV * FdL;
}

float mobileDiffuse(vec3 N, vec3 L, vec3 V, float roughness) {
    float dotNL = max(dot(N, L), 0.0);
    float dotNV = max(dot(N, V), 0.0);

    float wrap = 0.5;
    float wrapped = (dotNL + wrap) / (1.0 + wrap);

    float fresnel = pow(1.0 - dotNV, 5.0);
    return wrapped * (1.0 - fresnel * 0.5);
}

#endif // DIFFUSE_GLSL
