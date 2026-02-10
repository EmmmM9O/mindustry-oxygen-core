#ifndef TONEMAP_GLSL
#define TONEMAP_GLSL

#include "/utils/luminance.glsl"

#define L_WHITE 4.0
/// Reinhard Simple
vec3 reinhard(vec3 x) { return x / (1.0 + x); }

vec3 reinhard2(vec3 x) { return (x * (1.0 + x / (L_WHITE * L_WHITE))) / (1.0 + x); }

vec3 reinhardJodie(vec3 v) {
    float l = luminance(v);
    vec3 tv = v / (1.0f + v);
    return mix(v / (1.0f + l), tv, tv);
}

/// Narkowicz 2015, "ACES Filmic Tone Mapping Curve"
vec3 acesFilm(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

/// Unreal 3, Documentation: "Color Grading"
vec3 unreal(vec3 x) { return x / (x + 0.155) * 1.019; }

/// Uncharted 2
vec3 uncharted2Tonemap(vec3 x) {
    float A = 0.15;
    float B = 0.50;
    float C = 0.10;
    float D = 0.20;
    float E = 0.02;
    float F = 0.30;
    float W = 11.2;
    return ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
}
vec3 uncharted2(vec3 color) {
    const float W = 11.2;
    float exposureBias = 2.0;
    vec3 curr = uncharted2Tonemap(exposureBias * color);
    vec3 whiteScale = 1.0 / uncharted2Tonemap(vec3(W));
    return curr * whiteScale;
}

// Filmic Tonemapping Operators http://filmicworlds.com/blog/filmic-tonemapping-operators/
vec3 filmic(vec3 x) {
    vec3 X = max(vec3(0.0), x - 0.004);
    vec3 result = (X * (6.2 * X + 0.5)) / (X * (6.2 * X + 1.7) + 0.06);
    return pow(result, vec3(2.2));
}

// Lottes 2016, "Advanced Techniques and Optimization of HDR Color Pipelines"
vec3 lottes(vec3 x) {
    const vec3 a = vec3(1.6);
    const vec3 d = vec3(0.977);
    const vec3 hdrMax = vec3(8.0);
    const vec3 midIn = vec3(0.18);
    const vec3 midOut = vec3(0.267);

    const vec3 b = (-pow(midIn, a) + pow(hdrMax, a) * midOut) / ((pow(hdrMax, a * d) - pow(midIn, a * d)) * midOut);
    const vec3 c = (pow(hdrMax, a * d) * pow(midIn, a) - pow(hdrMax, a) * pow(midIn, a * d) * midOut) /
                   ((pow(hdrMax, a * d) - pow(midIn, a * d)) * midOut);

    return pow(x, a) / (pow(x, a * d) * b + c);
}

// Khronos PBR Neutral Tone Mapper
vec3 neutral(vec3 color) {
    const float startCompression = 0.8 - 0.04;
    const float desaturation = 0.15;

    float x = min(color.r, min(color.g, color.b));
    float offset = x < 0.08 ? x - 6.25 * x * x : 0.04;
    color -= offset;

    float peak = max(color.r, max(color.g, color.b));
    if (peak < startCompression)
        return color;

    const float d = 1.0 - startCompression;
    float newPeak = 1.0 - d * d / (peak + d - startCompression);
    color *= newPeak / peak;

    float g = 1.0 - 1.0 / (desaturation * (peak - newPeak) + 1.0);
    return mix(color, vec3(newPeak), g);
}

vec3 tonemapMode(vec3 color, int mode) {
    vec3 v = color;
    switch (mode) {
    case 1:
        v = reinhard(color);
        break;
    case 2:
        v = reinhard2(color);
        break;
    case 3:
        v = reinhardJodie(color);
        break;
    case 4:
        v = acesFilm(color);
        break;
    case 5:
        v = unreal(color);
        break;
    case 6:
        v = uncharted2(color);
        break;
    case 7:
        v = filmic(color);
        break;
    case 8:
        v = lottes(color);
        break;
    case 9:
        v = neutral(color);
        break;
    }
    return v;
}

vec3 tonemap(vec3 color) {
#ifdef TONEMAP_MODE
#if TONEMAP_MODE == 0
    return color;
#elif TONEMAP_MODE == 1
    return reinhard(color);
#elif TONEMAP_MODE == 2
    return reinhard2(color);
#elif TONEMAP_MODE == 3
    return reinhardJodie(color);
#elif TONEMAP_MODE == 4
    return acesFilm(color);
#elif TONEMAP_MODE == 5
    return unreal(color);
#elif TONEMAP_MODE == 6
    return uncharted2(color);
#elif TONEMAP_MODE == 7
    return filmic(color);
#elif TONEMAP_MODE == 8
    return lottes(color);
#elif TONEMAP_MODE == 9
    return neutral(color);
#else
    return color;
#endif
#else
    return color;
#endif
}

#endif // TONEMAP_GLSL
