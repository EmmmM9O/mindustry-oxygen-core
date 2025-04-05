#define HIGHP
varying vec2 uv;

uniform float gamma;
uniform float enabled;
uniform sampler2D texture0;
///----
/// Narkowicz 2015, "ACES Filmic Tone Mapping Curve"
vec3 aces(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

float aces(float x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}
///----

void main() {
    gl_FragColor = texture2D(texture0, uv);

    if (enabled > 0.5) {
        // ACES filmic tone mapping
        gl_FragColor.rgb = aces(fragColor.rgb);

        // Gamma correction
        gl_FragColor.rgb = pow(fragColor.rgb, vec3(1.0 / gamma));
    }
}
